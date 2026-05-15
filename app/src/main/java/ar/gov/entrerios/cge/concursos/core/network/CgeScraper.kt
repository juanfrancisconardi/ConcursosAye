package ar.gov.entrerios.cge.concursos.core.network

import ar.gov.entrerios.cge.concursos.core.model.Category
import ar.gov.entrerios.cge.concursos.core.network.dto.ConcursoDetailDto
import ar.gov.entrerios.cge.concursos.core.network.dto.ConcursoListDto
import ar.gov.entrerios.cge.concursos.core.util.Constants
import ar.gov.entrerios.cge.concursos.core.util.DateFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scraping del sitio del CGE Entre Ríos.
 *
 * El sitio está construido con WordPress, así que cada categoría es un listado
 * de "posts". El scraper es defensivo: tolera cambios parciales en la estructura
 * intentando varios selectores conocidos antes de rendirse.
 *
 * NO se usa WebView: se descarga HTML crudo vía Retrofit/OkHttp y se parsea con Jsoup.
 */
@Singleton
class CgeScraper @Inject constructor(
    private val api: CgeApi
) {

    /**
     * Descarga el listado de una categoría. Si `category` es [Category.GENERAL]
     * se trae el índice general de concursos.
     */
    suspend fun fetchList(category: Category): List<ConcursoListDto> = withContext(Dispatchers.IO) {
        val html = if (category == Category.GENERAL) {
            api.listIndex()
        } else {
            api.listByCategorySlug(category.slug)
        }
        parseList(html, category)
    }

    /** Descarga y parsea el contenido detallado de una publicación. */
    suspend fun fetchDetail(url: String, fallbackCategory: Category): ConcursoDetailDto =
        withContext(Dispatchers.IO) {
            val html = api.fetchUrl(url)
            parseDetail(html, url, fallbackCategory)
        }

    // ---------------------------------------------------------------------------------
    // Parsing - listado
    // ---------------------------------------------------------------------------------

    internal fun parseList(html: String, category: Category): List<ConcursoListDto> {
        val doc: Document = Jsoup.parse(html, Constants.BASE_URL)

        // Probamos selectores comunes de WordPress (article, .post, .entry, etc.)
        val articleSelectors = listOf(
            "article.post",
            "article",
            "div.post",
            "div.entry",
            "main article",
            "#content article"
        )

        var articles: List<Element> = emptyList()
        for (sel in articleSelectors) {
            articles = doc.select(sel)
            if (articles.isNotEmpty()) break
        }

        if (articles.isEmpty()) {
            // Fallback: cualquier link bajo el contenido principal
            articles = doc.select("main a[href]")
                .map { it.parents().firstOrNull() ?: it }
        }

        val result = mutableListOf<ConcursoListDto>()
        val seenUrls = mutableSetOf<String>()

        for (article in articles) {
            val parsed = parseListItem(article, category)
            if (parsed != null && seenUrls.add(parsed.url)) {
                result += parsed
            }
        }

        Timber.d("CGE listado [%s]: %d publicaciones", category.displayName, result.size)
        return result
    }

    private fun parseListItem(article: Element, category: Category): ConcursoListDto? {
        val anchor = article.selectFirst("h2 a, h1 a, h3 a, .entry-title a, a.entry-link")
            ?: article.selectFirst("a[href*=/concursos/]")
            ?: return null

        val rawUrl = anchor.absUrl("href").ifBlank { anchor.attr("href") }
        if (rawUrl.isBlank()) return null
        if (!rawUrl.contains("/concursos/")) return null
        // Descartar URLs que apunten al propio listado de categorías
        if (rawUrl.trimEnd('/').endsWith("/concursos") ||
            Category.entries.any { rawUrl.trimEnd('/').endsWith("/concursos/${it.slug}") }
        ) return null

        val title = anchor.text().trim().ifBlank {
            article.selectFirst(".entry-title, h2, h1")?.text()?.trim().orEmpty()
        }
        if (title.isBlank()) return null

        val dateText = article.selectFirst("time")?.let { it.attr("datetime").ifBlank { it.text() } }
            ?: article.selectFirst(".entry-date, .post-date, .published")?.text()
        val publishedAt = DateFormatter.parsePublishedDate(dateText)

        val excerpt = article.selectFirst(".entry-summary, .entry-content p, .excerpt, p")
            ?.text()
            ?.trim()
            .orEmpty()
            .take(MAX_EXCERPT_CHARS)

        return ConcursoListDto(
            url = canonicalize(rawUrl),
            title = title,
            publishedAt = publishedAt,
            excerpt = excerpt,
            category = category
        )
    }

    // ---------------------------------------------------------------------------------
    // Parsing - detalle
    // ---------------------------------------------------------------------------------

    internal fun parseDetail(html: String, url: String, fallbackCategory: Category): ConcursoDetailDto {
        val doc = Jsoup.parse(html, url)

        val title = doc.selectFirst("h1.entry-title")?.text()
            ?: doc.selectFirst("h1")?.text()
            ?: doc.title().substringBefore(" – ").trim()

        val dateText = doc.selectFirst("time")?.let { it.attr("datetime").ifBlank { it.text() } }
            ?: doc.selectFirst(".entry-date, .published, meta[property=article:published_time]")
                ?.let { it.attr("content").ifBlank { it.text() } }
        val publishedAt = DateFormatter.parsePublishedDate(dateText)

        val contentEl = doc.selectFirst(".entry-content")
            ?: doc.selectFirst("article")
            ?: doc.selectFirst("main")
            ?: doc.body()

        // Limpieza: quitar scripts, sidebars, comentarios
        contentEl.select("script, style, .sharedaddy, .jp-relatedposts, nav, footer, aside, .comments").remove()
        val content = contentEl.text().trim()

        val category = detectCategoryFromUrl(url) ?: fallbackCategory

        return ConcursoDetailDto(
            url = canonicalize(url),
            title = title.trim(),
            publishedAt = publishedAt,
            content = content,
            category = category
        )
    }

    // ---------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------

    private fun detectCategoryFromUrl(url: String): Category? {
        val lower = url.lowercase()
        return Category.entries.firstOrNull { it.slug.isNotEmpty() && lower.contains("/concursos/${it.slug}") }
    }

    /** Quita parámetros de tracking y fragmentos para usar la URL como clave única. */
    private fun canonicalize(url: String): String {
        var clean = url.substringBefore("#").substringBefore("?")
        if (!clean.endsWith("/") && !clean.contains(".html")) {
            clean = "$clean/"
        }
        return clean
    }

    companion object {
        private const val MAX_EXCERPT_CHARS = 280
    }
}

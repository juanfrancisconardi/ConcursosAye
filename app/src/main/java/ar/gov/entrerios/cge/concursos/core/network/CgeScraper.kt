package ar.gov.entrerios.cge.concursos.core.network

import ar.gov.entrerios.cge.concursos.core.model.Category
import ar.gov.entrerios.cge.concursos.core.model.Departamental
import ar.gov.entrerios.cge.concursos.core.network.dto.Attachment
import ar.gov.entrerios.cge.concursos.core.network.dto.AttachmentType
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
    private val api: CgeApi,
    private val throttle: CgeRequestThrottle
) {

    /**
     * Una sola petición al índice `/concursos/` con avisos recientes de todos los niveles.
     */
    suspend fun fetchConcursosFeed(): List<ConcursoListDto> =
        withContext(Dispatchers.IO) {
            throttle.run {
                val html = api.listIndex(1)
                CgeHtmlChecks.ensureNotBlocked(html)
                parseIndexFeed(html)
            }
        }

    /**
     * Convocatorias publicadas en la página de una DDE (misma estructura swiper/lista que /concursos/).
     */
    suspend fun fetchDepartamentalFeed(departamental: Departamental): List<ConcursoListDto> =
        withContext(Dispatchers.IO) {
            require(departamental.isActive) { "Departamental inactiva" }
            throttle.run {
                val html = api.listDepartamental(departamental.slug)
                CgeHtmlChecks.ensureNotBlocked(html)
                parseIndexFeed(html)
            }
        }

    /**
     * Descarga una página del listado (`?paged=` en WordPress).
     * [Category.GENERAL] usa el índice `/concursos/`; el resto usa `/category/<archiveSlug>/`.
     */
    suspend fun fetchListPage(category: Category, page: Int = 1): List<ConcursoListDto> =
        withContext(Dispatchers.IO) {
            throttle.run {
                require(page >= 1) { "page debe ser >= 1" }
                val html = if (category == Category.GENERAL) {
                    api.listIndex(page)
                } else {
                    check(category.archiveSlug.isNotEmpty()) {
                        "La categoría ${category.displayName} no tiene archiveSlug"
                    }
                    api.listByCategoryArchive(category.archiveSlug, page)
                }
                CgeHtmlChecks.ensureNotBlocked(html)
                parseList(html, category)
            }
        }

    /** Descarga y parsea el contenido detallado de una publicación. */
    suspend fun fetchDetail(url: String, fallbackCategory: Category): ConcursoDetailDto =
        withContext(Dispatchers.IO) {
            throttle.run {
                val html = api.fetchUrl(url)
                CgeHtmlChecks.ensureNotBlocked(html)
                parseDetail(html, url, fallbackCategory)
            }
        }

    /**
     * Descarga la publicación y extrae sus adjuntos (imágenes y PDF) para la lectura
     * profunda con OCR. Solo considera archivos alojados en el propio CGE.
     */
    suspend fun fetchAttachments(url: String): List<Attachment> =
        withContext(Dispatchers.IO) {
            throttle.run {
                val html = api.fetchUrl(url)
                CgeHtmlChecks.ensureNotBlocked(html)
                parseAttachments(html, url)
            }
        }

    /**
     * Baja la publicación **una sola vez** y devuelve a la vez el detalle y sus adjuntos.
     * Evita pedir dos veces la misma página en la lectura profunda.
     */
    suspend fun fetchDetailWithAttachments(
        url: String,
        fallbackCategory: Category
    ): Pair<ConcursoDetailDto, List<Attachment>> =
        withContext(Dispatchers.IO) {
            throttle.run {
                val html = api.fetchUrl(url)
                CgeHtmlChecks.ensureNotBlocked(html)
                parseDetail(html, url, fallbackCategory) to parseAttachments(html, url)
            }
        }

    // ---------------------------------------------------------------------------------
    // Parsing - listado
    // ---------------------------------------------------------------------------------

    internal fun parseIndexFeed(html: String): List<ConcursoListDto> {
        val doc = Jsoup.parse(html, Constants.BASE_URL)
        val slides = doc.select("div.swiper-slide[data-hash]")
        if (slides.isNotEmpty()) {
            val result = mutableListOf<ConcursoListDto>()
            val seen = mutableSetOf<String>()
            for (slide in slides) {
                val category = categoryFromHash(slide.attr("data-hash")) ?: Category.GENERAL
                for (block in slide.select("div.lista")) {
                    val parsed = parseCgeListBlock(block, category) ?: continue
                    if (seen.add(parsed.url)) result += parsed
                }
            }
            if (result.isNotEmpty()) {
                Timber.d("CGE índice: %d publicaciones (por slide)", result.size)
                return result
            }
        }
        return parseList(html, Category.GENERAL)
    }

    internal fun parseList(html: String, category: Category): List<ConcursoListDto> {
        val doc: Document = Jsoup.parse(html, Constants.BASE_URL)

        // Layout actual del CGE: bloques div.lista con h3 > a (índice y categorías).
        val cgeListBlocks = doc.select("div.page.concursos div.lista")
        if (cgeListBlocks.isNotEmpty()) {
            val result = mutableListOf<ConcursoListDto>()
            val seenUrls = mutableSetOf<String>()
            for (block in cgeListBlocks) {
                val parsed = parseCgeListBlock(block, category)
                if (parsed != null && seenUrls.add(parsed.url)) {
                    result += parsed
                }
            }
            Timber.d("CGE listado [%s]: %d publicaciones (div.lista)", category.displayName, result.size)
            return result
        }

        // Fallback: selectores genéricos de WordPress
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

    private fun parseCgeListBlock(block: Element, category: Category): ConcursoListDto? {
        val anchor = block.selectFirst("h3 a, h2 a") ?: return null
        return buildListDto(anchor, block, category)
    }

    private fun parseListItem(article: Element, category: Category): ConcursoListDto? {
        val anchor = article.selectFirst("h2 a, h1 a, h3 a, .entry-title a, a.entry-link")
            ?: article.selectFirst("a[href]")
            ?: return null
        return buildListDto(anchor, article, category)
    }

    private fun buildListDto(anchor: Element, container: Element, category: Category): ConcursoListDto? {
        val rawUrl = anchor.absUrl("href").ifBlank { anchor.attr("href") }
        if (rawUrl.isBlank() || !isConcursoPostUrl(rawUrl)) return null

        val title = anchor.text().trim().ifBlank {
            anchor.attr("title").trim()
        }.ifBlank {
            container.selectFirst(".entry-title, h3, h2, h1")?.text()?.trim().orEmpty()
        }
        if (title.isBlank()) return null

        val dateText = container.selectFirst("time")?.let { it.attr("datetime").ifBlank { it.text() } }
            ?: container.selectFirst("p i.fa-calendar, p i.far.fa-calendar")?.parent()?.text()
            ?: container.selectFirst(".entry-date, .post-date, .published")?.text()
        val publishedAt = DateFormatter.parsePublishedDate(dateText)

        val excerpt = container.selectFirst(".entry-summary, .entry-content p, .excerpt, p")
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
    // Parsing - adjuntos (para OCR)
    // ---------------------------------------------------------------------------------

    internal fun parseAttachments(html: String, baseUrl: String): List<Attachment> {
        val doc = Jsoup.parse(html, baseUrl)
        val content = doc.selectFirst(".entry-content")
            ?: doc.selectFirst("article")
            ?: doc.selectFirst("main")
            ?: doc.body()

        val result = mutableListOf<Attachment>()
        val seenKeys = mutableSetOf<String>()

        // 1) Links directos a imágenes y PDFs (suelen apuntar al archivo en tamaño completo).
        for (a in content.select("a[href]")) {
            val href = a.absUrl("href")
            if (href.isBlank()) continue
            when {
                isPdfUrl(href) -> {
                    if (seenKeys.add(href.lowercase())) result += Attachment(href, AttachmentType.PDF)
                }
                isImageUrl(href) && isCgeUpload(href) -> {
                    if (seenKeys.add(imageKey(href))) result += Attachment(href, AttachmentType.IMAGE)
                }
            }
        }

        // 2) Imágenes embebidas en el cuerpo (por si no tienen link envolvente).
        for (img in content.select("img[src]")) {
            val src = img.absUrl("src")
            if (src.isBlank()) continue
            if (isImageUrl(src) && isCgeUpload(src) && seenKeys.add(imageKey(src))) {
                result += Attachment(src, AttachmentType.IMAGE)
            }
        }

        Timber.d("CGE adjuntos en %s: %d", baseUrl, result.size)
        return result
    }

    private fun isPdfUrl(url: String): Boolean =
        url.substringBefore("?").endsWith(".pdf", ignoreCase = true) &&
            url.contains("entrerios.gov.ar", ignoreCase = true)

    private fun isImageUrl(url: String): Boolean {
        val path = url.substringBefore("?").lowercase()
        return path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png")
    }

    private fun isCgeUpload(url: String): Boolean =
        url.contains("/wp-content/uploads/", ignoreCase = true)

    /** Agrupa las variantes redimensionadas de WordPress (-1086x1536) bajo una sola clave. */
    private fun imageKey(url: String): String =
        url.substringBefore("?")
            .replace(SIZE_SUFFIX_PATTERN, "$1")
            .lowercase()

    // ---------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------

    private fun categoryFromHash(hash: String): Category? {
        val key = hash.trim().lowercase()
        if (key.isEmpty()) return null
        return Category.entries.firstOrNull { cat ->
            cat.archiveSlug.isNotEmpty() && cat.archiveSlug.equals(key, ignoreCase = true)
        }
    }

    private fun detectCategoryFromUrl(url: String): Category? {
        val lower = url.lowercase()
        return Category.entries.firstOrNull { cat ->
            cat.archiveSlug.isNotEmpty() && lower.contains("/category/${cat.archiveSlug}")
        }
    }

    /** Permalink típico del CGE: https://host/2026/05/titulo-del-aviso/ */
    private fun isConcursoPostUrl(url: String): Boolean {
        if (!url.contains("entrerios.gov.ar", ignoreCase = true)) return false
        val path = runCatching {
            java.net.URI(url).path?.trimEnd('/').orEmpty()
        }.getOrDefault(url)
        if (path.contains("/category/") || path.endsWith("/concursos")) return false
        if (Category.entries.any { cat ->
                cat.slug.isNotEmpty() && (path == "/concursos/${cat.slug}" || path.endsWith("/concursos/${cat.slug}"))
            }
        ) return false
        return POST_URL_PATTERN.containsMatchIn("$path/")
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
        private val POST_URL_PATTERN = Regex("/\\d{4}/\\d{2}/[^/]+/")
        private val SIZE_SUFFIX_PATTERN = Regex("-\\d+x\\d+(\\.(?:jpg|jpeg|png))$", RegexOption.IGNORE_CASE)
    }
}

package ar.gov.entrerios.cge.concursos.data.mapper

import ar.gov.entrerios.cge.concursos.core.database.entity.ConcursoEntity
import ar.gov.entrerios.cge.concursos.core.database.entity.MatchEntity
import ar.gov.entrerios.cge.concursos.core.database.relation.ConcursoWithMatches
import ar.gov.entrerios.cge.concursos.core.model.Category
import ar.gov.entrerios.cge.concursos.core.model.Concurso
import ar.gov.entrerios.cge.concursos.core.model.KeywordMatch
import ar.gov.entrerios.cge.concursos.core.model.MatchLocation

fun ConcursoWithMatches.toDomain(): Concurso = Concurso(
    id = concurso.id,
    url = concurso.url,
    title = concurso.title,
    publishedAt = concurso.publishedAt,
    category = Category.fromSlug(concurso.categorySlug),
    excerpt = concurso.excerpt,
    content = concurso.content,
    contentHash = concurso.contentHash,
    detectedAt = concurso.detectedAt,
    isNew = concurso.isNew,
    isRead = concurso.isRead,
    matches = matches.map { it.toDomain() },
    score = concurso.score
)

fun MatchEntity.toDomain(): KeywordMatch = KeywordMatch(
    keyword = keyword,
    location = runCatching { MatchLocation.valueOf(location) }.getOrDefault(MatchLocation.CONTENT),
    score = score
)

fun KeywordMatch.toEntity(concursoId: Long): MatchEntity = MatchEntity(
    concursoId = concursoId,
    keyword = keyword,
    location = location.name,
    score = score
)

fun ConcursoEntity.toDomain(matches: List<KeywordMatch> = emptyList()): Concurso = Concurso(
    id = id,
    url = url,
    title = title,
    publishedAt = publishedAt,
    category = Category.fromSlug(categorySlug),
    excerpt = excerpt,
    content = content,
    contentHash = contentHash,
    detectedAt = detectedAt,
    isNew = isNew,
    isRead = isRead,
    matches = matches,
    score = score
)

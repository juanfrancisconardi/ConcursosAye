package ar.gov.entrerios.cge.concursos.data.mapper

import ar.gov.entrerios.cge.concursos.core.database.entity.KeywordEntity
import ar.gov.entrerios.cge.concursos.core.model.Keyword
import ar.gov.entrerios.cge.concursos.core.util.TextNormalizer

fun KeywordEntity.toDomain(): Keyword = Keyword(
    id = id,
    text = text,
    enabled = enabled,
    createdAt = createdAt
)

fun Keyword.toEntity(): KeywordEntity = KeywordEntity(
    id = id,
    text = text.trim(),
    normalizedText = TextNormalizer.normalize(text),
    enabled = enabled,
    createdAt = createdAt
)

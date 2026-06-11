package ar.gov.entrerios.cge.concursos.core.network.dto

import ar.gov.entrerios.cge.concursos.core.model.Category

/**
 * DTO de scraping: lo que extraemos del HTML del CGE.
 */
data class ConcursoListDto(
    val url: String,
    val title: String,
    val publishedAt: Long?,
    val excerpt: String,
    val category: Category
)

data class ConcursoDetailDto(
    val url: String,
    val title: String,
    val publishedAt: Long?,
    val content: String,
    val category: Category
)

enum class AttachmentType { IMAGE, PDF }

/** Adjunto descargable de un aviso (imagen o PDF) para lectura profunda con OCR. */
data class Attachment(
    val url: String,
    val type: AttachmentType
)

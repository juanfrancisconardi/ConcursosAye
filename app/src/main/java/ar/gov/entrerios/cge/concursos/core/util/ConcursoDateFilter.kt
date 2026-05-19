package ar.gov.entrerios.cge.concursos.core.util

import ar.gov.entrerios.cge.concursos.core.model.Concurso
import ar.gov.entrerios.cge.concursos.core.network.dto.ConcursoListDto

/**
 * Filtra avisos por antigüedad usando [Concurso.publishedAt] o, si falta, [Concurso.detectedAt].
 */
object ConcursoDateFilter {

    const val DEFAULT_DAYS_BACK = 30
    const val MIN_DAYS_BACK = 7
    const val MAX_DAYS_BACK = 365

    val presetDaysBack: List<Int> = listOf(7, 14, 30, 60, 90)

    fun coerceDaysBack(days: Int): Int = days.coerceIn(MIN_DAYS_BACK, MAX_DAYS_BACK)

    fun cutoffMillis(now: Long = System.currentTimeMillis(), daysBack: Int): Long =
        now - coerceDaysBack(daysBack) * 24L * 60 * 60 * 1000

    fun isWithinDays(
        publishedAt: Long?,
        detectedAt: Long,
        cutoff: Long
    ): Boolean = (publishedAt ?: detectedAt) >= cutoff

    fun isListItemWithinDays(item: ConcursoListDto, cutoff: Long): Boolean {
        val published = item.publishedAt ?: return true
        return published >= cutoff
    }

    fun filterConcursos(concursos: List<Concurso>, daysBack: Int, now: Long = System.currentTimeMillis()): List<Concurso> {
        val cutoff = cutoffMillis(now, daysBack)
        return concursos.filter { isWithinDays(it.publishedAt, it.detectedAt, cutoff) }
    }
}

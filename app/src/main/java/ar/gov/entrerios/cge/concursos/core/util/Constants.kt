package ar.gov.entrerios.cge.concursos.core.util

object Constants {
    const val BASE_URL = "https://cge.entrerios.gov.ar/"
    const val CONCURSOS_PATH = "concursos/"
    const val DATABASE_NAME = "concursos_cge.db"

    /** Unique work name del worker periódico. */
    const val SYNC_WORK_NAME = "cge_periodic_sync"
    /** Tag para encolar OneTime sync (refresh manual). */
    const val SYNC_WORK_TAG = "cge_sync"

    const val NOTIFICATION_CHANNEL_ID = "concursos_channel"
    const val NOTIFICATION_GROUP_KEY = "com.concursosdocentes.er.NEW_CONCURSOS"

    /**
     * Páginas por categoría en sync rutinaria (~20 avisos recientes por nivel en pág. 1).
     */
    const val SYNC_MAX_PAGES_PER_CATEGORY = 1

    /** Marcador: el cuerpo completo se baja al abrir el detalle. */
    const val CONTENT_HASH_PENDING = "pending"

    /** Mínimo entre dos requests al CGE (Wordfence limita por minuto). */
    const val SYNC_MIN_REQUEST_INTERVAL_MS = 12_000L

    /** No volver a lanzar una sync completa antes de este intervalo. */
    const val SYNC_MIN_INTERVAL_BETWEEN_RUNS_MS = 90_000L
}

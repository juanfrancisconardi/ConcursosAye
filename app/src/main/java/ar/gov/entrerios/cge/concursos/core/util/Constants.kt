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
    const val NOTIFICATION_GROUP_KEY = "ar.gov.entrerios.cge.concursos.NEW_CONCURSOS"
}

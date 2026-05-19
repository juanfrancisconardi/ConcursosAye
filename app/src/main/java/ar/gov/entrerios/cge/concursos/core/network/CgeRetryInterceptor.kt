package ar.gov.entrerios.cge.concursos.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/** Sin reintentos: cada reintento empeora el bloqueo Wordfence del CGE. */
class CgeRetryInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        when (response.code) {
            503, 429 -> {
                val peek = response.peekBody(8_192).string()
                response.close()
                if (CgeHtmlChecks.isAccessBlocked(peek)) {
                    throw CgeAccessException()
                }
                throw IOException("HTTP ${response.code} — servidor no disponible")
            }
        }
        return response
    }
}

package ar.gov.entrerios.cge.concursos.core.network

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * Endpoints HTML del sitio del CGE.
 *
 * El sitio CGE Entre Ríos es WordPress y no expone JSON público de concursos,
 * por lo que se descarga el HTML crudo y se parsea con Jsoup.
 */
interface CgeApi {

    /** Archivo WordPress de una categoría (ej: "category/concursos-inicial/"). */
    @GET("category/{slug}/")
    suspend fun listByCategoryArchive(
        @retrofit2.http.Path("slug") slug: String,
        @Query("paged") page: Int = 1
    ): String

    /** Índice general de concursos en `/concursos/`. */
    @GET("concursos/")
    suspend fun listIndex(@Query("paged") page: Int = 1): String

    /** Página de una Dirección Departamental de Escuelas (DDE). */
    @GET("{slug}/")
    suspend fun listDepartamental(@retrofit2.http.Path("slug") slug: String): String

    /** Descarga arbitraria por URL absoluta (para detalle de publicación). */
    @GET
    suspend fun fetchUrl(@Url url: String): String

    /** Descarga binaria (imágenes/PDF) para la lectura profunda con OCR. */
    @Streaming
    @GET
    suspend fun downloadFile(@Url url: String): ResponseBody
}

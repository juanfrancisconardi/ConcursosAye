package ar.gov.entrerios.cge.concursos.core.network

import retrofit2.http.GET
import retrofit2.http.Url

/**
 * Endpoints HTML del sitio del CGE.
 *
 * El sitio CGE Entre Ríos es WordPress y no expone JSON público de concursos,
 * por lo que se descarga el HTML crudo y se parsea con Jsoup.
 */
interface CgeApi {

    /** Listado de una categoría (ej: "concursos/inicial/") o el índice "concursos/". */
    @GET("concursos/{slug}/")
    suspend fun listByCategorySlug(@retrofit2.http.Path("slug") slug: String): String

    @GET("concursos/")
    suspend fun listIndex(): String

    /** Descarga arbitraria por URL absoluta (para detalle de publicación). */
    @GET
    suspend fun fetchUrl(@Url url: String): String
}

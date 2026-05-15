package ar.gov.entrerios.cge.concursos.core.network

import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

/**
 * Convertidor mínimo: el HTML llega como String al servicio Retrofit.
 *
 * Retrofit normalmente trae `converter-scalars` que se encarga de String, pero
 * lo declaramos explícito para garantizar el orden de resolución.
 */
class HtmlStringConverter : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        if (type == String::class.java) {
            return Converter<ResponseBody, String> { body -> body.string() }
        }
        return null
    }
}

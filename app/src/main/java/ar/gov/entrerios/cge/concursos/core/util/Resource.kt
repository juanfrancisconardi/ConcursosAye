package ar.gov.entrerios.cge.concursos.core.util

/**
 * Estado genérico para una operación asincrónica.
 */
sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val throwable: Throwable, val message: String? = throwable.message) : Resource<Nothing>
}

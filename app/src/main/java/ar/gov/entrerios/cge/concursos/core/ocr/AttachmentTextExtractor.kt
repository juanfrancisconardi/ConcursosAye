package ar.gov.entrerios.cge.concursos.core.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import ar.gov.entrerios.cge.concursos.core.network.CgeApi
import ar.gov.entrerios.cge.concursos.core.network.CgeAccessException
import ar.gov.entrerios.cge.concursos.core.network.CgeRequestThrottle
import ar.gov.entrerios.cge.concursos.core.network.dto.Attachment
import ar.gov.entrerios.cge.concursos.core.network.dto.AttachmentType
import ar.gov.entrerios.cge.concursos.core.util.Constants
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Lectura profunda de adjuntos: descarga imágenes/PDF de un aviso y les aplica
 * OCR on-device (ML Kit) para extraer el texto que no está en el HTML
 * (por ejemplo, planillas de cargos publicadas como imágenes o PDFs escaneados).
 *
 * Los PDFs se rasterizan con [PdfRenderer] (nativo de Android) y luego se les
 * aplica OCR, de modo que un único motor cubre imágenes y PDFs sin librerías extra.
 */
@Singleton
class AttachmentTextExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: CgeApi,
    private val throttle: CgeRequestThrottle
) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Descarga y OCR-ea los adjuntos. Devuelve el texto concatenado encontrado.
     * Propaga [CgeAccessException] para frenar si el CGE bloquea; el resto de errores
     * se loguean y se omiten para no abortar toda la corrida.
     */
    suspend fun extractText(attachments: List<Attachment>): String {
        if (attachments.isEmpty()) return ""
        val limited = attachments.take(Constants.OCR_MAX_ATTACHMENTS_PER_POST)
        val builder = StringBuilder()

        for (attachment in limited) {
            try {
                val bytes = throttle.runWith(Constants.OCR_ASSET_REQUEST_INTERVAL_MS) {
                    api.downloadFile(attachment.url).bytes()
                }
                val text = when (attachment.type) {
                    AttachmentType.IMAGE -> ocrImage(bytes)
                    AttachmentType.PDF -> ocrPdf(bytes)
                }
                if (text.isNotBlank()) {
                    builder.append(text.trim()).append('\n')
                }
            } catch (e: CgeAccessException) {
                throw e
            } catch (t: Throwable) {
                Timber.w(t, "OCR falló para %s", attachment.url)
            }
        }
        return builder.toString().trim()
    }

    private suspend fun ocrImage(bytes: ByteArray): String = withContext(Dispatchers.Default) {
        val bitmap = decodeSampled(bytes) ?: return@withContext ""
        try {
            recognize(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun ocrPdf(bytes: ByteArray): String = withContext(Dispatchers.Default) {
        val file = File.createTempFile("cge_ocr_", ".pdf", context.cacheDir)
        try {
            file.writeBytes(bytes)
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val builder = StringBuilder()
                    val pages = minOf(renderer.pageCount, Constants.OCR_MAX_PDF_PAGES)
                    for (index in 0 until pages) {
                        renderer.openPage(index).use { page ->
                            val scale = 2
                            val bitmap = Bitmap.createBitmap(
                                (page.width * scale).coerceAtLeast(1),
                                (page.height * scale).coerceAtLeast(1),
                                Bitmap.Config.ARGB_8888
                            )
                            bitmap.eraseColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            val text = try {
                                recognize(bitmap)
                            } finally {
                                bitmap.recycle()
                            }
                            if (text.isNotBlank()) builder.append(text.trim()).append('\n')
                        }
                    }
                    builder.toString()
                }
            }
        } finally {
            runCatching { file.delete() }
        }
    }

    private suspend fun recognize(bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { result -> if (cont.isActive) cont.resume(result.text) }
                .addOnFailureListener { error -> if (cont.isActive) cont.resumeWithException(error) }
        }

    /** Decodifica reduciendo la resolución si la imagen es muy grande (evita OOM). */
    private fun decodeSampled(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / sample > MAX_IMAGE_DIMENSION ||
            bounds.outHeight / sample > MAX_IMAGE_DIMENSION
        ) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    companion object {
        private const val MAX_IMAGE_DIMENSION = 2200
    }
}

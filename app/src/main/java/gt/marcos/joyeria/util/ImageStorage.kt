package gt.marcos.joyeria.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

private const val MAX_DIMENSION_PX = 1600
private const val MAX_FILE_BYTES = 1024 * 1024 // 1MB, FASES.md Fase 03
private const val INITIAL_JPEG_QUALITY = 90
private const val MIN_JPEG_QUALITY = 50 // piso: no seguir degradando hasta que no se reconozca la pieza
private const val QUALITY_STEP = 10

/**
 * Redimensiona, comprime y guarda la foto de una pieza en almacenamiento
 * interno de la app (`filesDir/photos/`, nunca galería/MediaStore: nadie
 * más que la app necesita leer estos archivos).
 */
class ImageStorage @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val photosDir: File
        get() = File(context.filesDir, "photos").apply { mkdirs() }

    /**
     * Aplica la rotación que reporta CameraX (`imageInfo.rotationDegrees`
     * — separada de los píxeles, si no se aplica la foto sale de costado),
     * redimensiona si el lado mayor supera 1600px y comprime a JPEG bajando
     * la calidad hasta quedar bajo 1MB (o llegar al piso de calidad).
     * Devuelve la ruta absoluta del archivo guardado.
     */
    fun save(bitmap: Bitmap, rotationDegrees: Int): String {
        val rotated = rotateIfNeeded(bitmap, rotationDegrees)
        val (targetWidth, targetHeight) = scaledDimensions(rotated.width, rotated.height, MAX_DIMENSION_PX)
        val resized = if (targetWidth != rotated.width || targetHeight != rotated.height) {
            Bitmap.createScaledBitmap(rotated, targetWidth, targetHeight, true)
        } else {
            rotated
        }
        val file = File(photosDir, "${UUID.randomUUID()}.jpg")
        compressToFile(resized, file)
        return file.absolutePath
    }

    /** Borra un archivo de foto ya guardado (ej. al retomar, o al descartar sin guardar). */
    fun delete(path: String) {
        File(path).takeIf { it.exists() }?.delete()
    }

    private fun rotateIfNeeded(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun compressToFile(bitmap: Bitmap, file: File) {
        var quality = INITIAL_JPEG_QUALITY
        while (true) {
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out) }
            if (file.length() <= MAX_FILE_BYTES || quality <= MIN_JPEG_QUALITY) break
            quality -= QUALITY_STEP
        }
    }

    companion object {
        /**
         * Calcula el tamaño destino manteniendo la proporción, para que el
         * lado mayor no supere `maxSide`. Función pura, sin `Bitmap`, para
         * poder testearla sin Robolectric (FASES.md Fase 03, pedido
         * explícito del humano). Si ya está dentro del límite, no agranda.
         */
        fun scaledDimensions(width: Int, height: Int, maxSide: Int): Pair<Int, Int> {
            val longestSide = maxOf(width, height)
            if (longestSide <= maxSide) return width to height
            return if (width >= height) {
                val scaledHeight = (height.toLong() * maxSide / width).toInt()
                maxSide to scaledHeight
            } else {
                val scaledWidth = (width.toLong() * maxSide / height).toInt()
                scaledWidth to maxSide
            }
        }
    }
}

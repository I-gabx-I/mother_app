package gt.marcos.joyeria.ui.barcode

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

/**
 * Lee un código de barras de un cuadro de la cámara con ZXing. Pura (sin Android):
 * recibe el plano de luminancia (Y) tal como lo da CameraX, así que se prueba en
 * la JVM, incluido un cuadro girado 90° (el celular en vertical).
 *
 * Reconoce Code128 (las etiquetas de ella) y los formatos más comunes de otros
 * productos, para poder decirle «ese código no es de una pieza tuya» en vez de
 * quedarse callado.
 *
 * Con el celular en vertical, una etiqueta horizontal aparece girada 90° en el
 * cuadro crudo de la cámara. `PlanarYUVLuminanceSource` no sabe girarse (el
 * `TRY_HARDER` de ZXing solo prueba el giro con fuentes que lo soportan), así
 * que acá se prueba el cuadro tal cual y, si no hay nada, girado 90°.
 */
class BarcodeDecoder {
    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.CODE_39,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E,
                    BarcodeFormat.QR_CODE,
                ),
                DecodeHintType.TRY_HARDER to true,
            ),
        )
    }

    /**
     * @param luminance plano Y; cada fila ocupa [rowStride] bytes y trae [width] píxeles útiles.
     * @return el texto leído, o `null` si el cuadro no tiene ningún código legible.
     */
    fun decode(luminance: ByteArray, width: Int, height: Int, rowStride: Int = width): String? =
        decodeUpright(luminance, width, height, rowStride)
            ?: decodeUpright(rotate90(luminance, width, height, rowStride), height, width, height)

    private fun decodeUpright(luminance: ByteArray, width: Int, height: Int, rowStride: Int): String? {
        val source = PlanarYUVLuminanceSource(luminance, rowStride, height, 0, 0, width, height, false)
        return try {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
        } catch (notFound: NotFoundException) {
            null
        } finally {
            reader.reset()
        }
    }

    /** El plano girado 90° en sentido horario: el resultado mide [height] de ancho y [width] de alto, sin relleno. */
    private fun rotate90(luminance: ByteArray, width: Int, height: Int, rowStride: Int): ByteArray {
        val rotated = ByteArray(width * height)
        for (y in 0 until height) {
            val row = y * rowStride
            for (x in 0 until width) rotated[x * height + (height - 1 - y)] = luminance[row + x]
        }
        return rotated
    }
}

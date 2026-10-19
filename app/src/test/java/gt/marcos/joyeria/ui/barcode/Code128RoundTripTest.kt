package gt.marcos.joyeria.ui.barcode

import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.domain.barcode.BarcodeLabel
import gt.marcos.joyeria.domain.barcode.ProductUid
import org.junit.Test
import java.util.Locale

/**
 * Criterios 2 y 3 de la Fase 13: lo codificado es exactamente el código de la
 * pieza (sin ningún monto) y generar y leer de vuelta devuelve el mismo código.
 * El lector es el mismo `BarcodeDecoder` que usa la cámara.
 */
class Code128RoundTripTest {

    private fun label(raw: String) = BarcodeLabel(checkNotNull(ProductUid.parse(raw)))

    /** Renderiza la fila de módulos a [pxPerModule] píxeles por módulo, con zona de silencio, como plano de luminancia. */
    private fun render(label: BarcodeLabel, pxPerModule: Int, rotate90: Boolean = false): Triple<ByteArray, Int, Int> {
        val matrix = Code128Encoder.encode(label)
        val quiet = 10
        val width = (matrix.width + 2 * quiet) * pxPerModule
        val height = 60
        val pixels = ByteArray(width * height) { 255.toByte() }
        for (x in 0 until matrix.width) if (matrix.get(x, 0)) {
            for (px in 0 until pxPerModule) for (y in 0 until height) pixels[y * width + (quiet + x) * pxPerModule + px] = 0
        }
        if (!rotate90) return Triple(pixels, width, height)
        val rotated = ByteArray(width * height)
        for (y in 0 until height) for (x in 0 until width) rotated[x * height + (height - 1 - y)] = pixels[y * width + x]
        return Triple(rotated, height, width)
    }

    private fun decode(label: BarcodeLabel, pxPerModule: Int, rotate90: Boolean = false): String? {
        val (pixels, w, h) = render(label, pxPerModule, rotate90)
        return BarcodeDecoder().decode(pixels, w, h)
    }

    @Test
    fun encodedContent_isExactlyTheUidAndNothingElse() {
        val decoded = decode(label("XP-000042"), pxPerModule = 4)
        assertThat(decoded).isEqualTo("XP-000042")
        assertThat(decoded).doesNotContain("Q")
    }

    @Test
    fun roundTrip_XP000042_decodesToItself() {
        assertThat(decode(label("XP-000042"), pxPerModule = 3)).isEqualTo("XP-000042")
    }

    @Test
    fun roundTrip_everyUidFrom1To1999_decodesToItself() {
        val decoder = BarcodeDecoder()
        for (i in 1..1999) {
            val uid = String.format(Locale.ROOT, "XP-%06d", i)
            val (pixels, w, h) = render(label(uid), pxPerModule = 3)
            assertThat(decoder.decode(pixels, w, h)).isEqualTo(uid)
        }
    }

    @Test
    fun roundTrip_sevenDigitUid_decodesToItself() {
        assertThat(decode(label("XP-1234567"), pxPerModule = 3)).isEqualTo("XP-1234567")
    }

    @Test
    fun decode_frameRotated90Degrees_stillReadsTheCode() {
        // El celular en vertical ve la etiqueta horizontal girada 90 grados en el cuadro crudo.
        assertThat(decode(label("XP-000042"), pxPerModule = 3, rotate90 = true)).isEqualTo("XP-000042")
    }

    @Test
    fun decode_atTwoPixelsPerModule_stillReadsTheCode() {
        // El piso medido: ~3.5 px/módulo con la cámara a 1080p a 10 cm; 2 px es el margen.
        assertThat(decode(label("XP-000042"), pxPerModule = 2)).isEqualTo("XP-000042")
    }

    @Test
    fun decode_blankFrame_returnsNullInsteadOfThrowing() {
        val blank = ByteArray(320 * 240) { 255.toByte() }
        assertThat(BarcodeDecoder().decode(blank, 320, 240)).isNull()
    }

    @Test
    fun moduleCount_forSixDigitUid_is112() {
        assertThat(Code128Encoder.moduleCount(label("XP-000042"))).isEqualTo(112)
    }

    @Test
    fun moduleCount_forSevenDigitUid_is123() {
        assertThat(Code128Encoder.moduleCount(label("XP-1234567"))).isEqualTo(123)
    }
}

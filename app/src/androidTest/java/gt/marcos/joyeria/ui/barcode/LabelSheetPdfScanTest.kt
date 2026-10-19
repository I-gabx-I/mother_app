package gt.marcos.joyeria.ui.barcode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.domain.barcode.BarcodeLabel
import gt.marcos.joyeria.domain.barcode.LabelSheetSpec
import gt.marcos.joyeria.domain.barcode.ProductUid
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * La prueba más cercana a «¿se puede escanear impreso a este tamaño?» que se puede
 * hacer sin papel: genera el PDF de etiquetas, lo rasteriza como lo haría una
 * impresora de 300 dpi (módulo de 0.254 mm = 3 puntos) y de 600 dpi (6 puntos),
 * recorta cada etiqueta y la lee con el mismo `BarcodeDecoder` que usa
 * la cámara. **No reemplaza imprimir de verdad** (la óptica de la cámara y el
 * papel no están): para eso está la hoja de prueba. Imprime el resultado con la
 * etiqueta `LabelScan` (`adb logcat -d -s LabelScan`).
 */
@RunWith(AndroidJUnit4::class)
class LabelSheetPdfScanTest {

    private val spec = LabelSheetSpec()
    private val uids = (1..60).map { String.format(Locale.ROOT, "XP-%06d", it) }

    private fun writePdf(context: Context): File {
        val file = File(context.cacheDir, "scan-test.pdf")
        FileOutputStream(file).use { out ->
            LabelSheetPdf.write(uids.map { BarcodeLabel(checkNotNull(ProductUid.parse(it))) }, out, spec)
        }
        return file
    }

    /** Cuántas etiquetas de las [uids] se leen bien al rasterizar el PDF a [dpi]. */
    private fun decodedAt(file: File, dpi: Int): Int {
        val pxPerMm = dpi / 25.4
        var ok = 0
        val renderer = PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
        try {
            val decoder = BarcodeDecoder()
            for (pageIndex in 0 until renderer.pageCount) {
                val width = Math.round(spec.pageWidthMm * pxPerMm).toInt()
                val height = Math.round(spec.pageHeightMm * pxPerMm).toInt()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
                renderer.openPage(pageIndex).use { it.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT) }
                val onPage = uids.drop(pageIndex * spec.labelsPerPage).take(spec.labelsPerPage)
                onPage.forEachIndexed { i, uid ->
                    val left = Math.round(spec.labelLeftMm(i) * pxPerMm).toInt()
                    val top = Math.round(spec.labelTopMm(i) * pxPerMm).toInt()
                    val w = Math.round(spec.labelWidthMm * pxPerMm).toInt()
                    val h = Math.round(spec.labelHeightMm * pxPerMm).toInt()
                    val pixels = IntArray(w * h)
                    bitmap.getPixels(pixels, 0, w, left, top, w, h)
                    val luminance = ByteArray(w * h) { n ->
                        val p = pixels[n]
                        ((Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000).toByte()
                    }
                    if (decoder.decode(luminance, w, h) == uid) ok++
                }
                bitmap.recycle()
            }
        } finally {
            renderer.close()
        }
        return ok
    }

    @Test
    fun pdfRasterizedAt300Dpi_everyLabelDecodesToItsOwnUid() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = writePdf(context)
        try {
            val ok = decodedAt(file, 300)
            Log.i("LabelScan", "300 dpi: $ok de ${uids.size} etiquetas leídas (módulo = ${300 * 0.254 / 25.4} px)")
            assertThat(ok).isEqualTo(uids.size)
        } finally {
            file.delete()
        }
    }

    @Test
    fun pdfRasterizedAt600Dpi_everyLabelDecodesToItsOwnUid() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = writePdf(context)
        try {
            val ok = decodedAt(file, 600)
            Log.i("LabelScan", "600 dpi: $ok de ${uids.size} etiquetas leídas (módulo = ${600 * 0.254 / 25.4} px)")
            assertThat(ok).isEqualTo(uids.size)
            // Solo se anota, no se afirma: a 203 dpi (térmicas de etiquetas) 0.254 mm son 2.03 puntos, que no es
            // un número entero, las barras salen de anchos irregulares y no se lee ninguna. Ver D-060.
            Log.i("LabelScan", "203 dpi (informativo, no es la impresora de ella): ${decodedAt(file, 203)} de ${uids.size}")
        } finally {
            file.delete()
        }
    }

    @Test
    fun labelSheetPdf_hasOnePagePerFiftySixLabels() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = writePdf(context)
        try {
            val renderer = PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
            try {
                assertThat(renderer.pageCount).isEqualTo(2)
            } finally {
                renderer.close()
            }
        } finally {
            file.delete()
        }
    }
}

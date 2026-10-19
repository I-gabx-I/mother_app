package gt.marcos.joyeria.ui.barcode

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import gt.marcos.joyeria.domain.barcode.BarcodeLabel
import gt.marcos.joyeria.domain.barcode.LabelSheetPaging
import gt.marcos.joyeria.domain.barcode.LabelSheetSpec
import gt.marcos.joyeria.domain.barcode.ProductUid
import java.io.OutputStream
import java.util.Locale

/** Los textos fijos de la hoja de prueba, que vienen de `strings.xml`. `moduleFormat` y `moduleRealFormat` llevan `%1$s`. */
data class LabelTestSheetTexts(
    val title: String,
    val ruler: String,
    val instructions: String,
    val moduleFormat: String,
    val moduleRealFormat: String,
)

/**
 * La hoja de etiquetas en PDF (Fase 13). El código se dibuja como **rectángulos
 * vectoriales**, no como imagen: queda nítido a cualquier resolución de impresora
 * y el módulo (0.254 mm) es exacto. Solo recibe [BarcodeLabel]: lo que se imprime
 * es el código de barras y su texto legible, el código de la pieza y nada más
 * (D-003, D-060).
 *
 * Unidades: el PDF se mide en puntos (1/72 de pulgada); la geometría de
 * [LabelSheetSpec] está en milímetros.
 */
object LabelSheetPdf {
    private const val PT_PER_MM = 72f / 25.4f
    private const val MM_BELOW_BARS_TO_TEXT_BASELINE = 3.6
    private const val TEXT_SIZE_PT = 8f

    private val barPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL; isAntiAlias = false }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = TEXT_SIZE_PT
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }
    private val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 9f }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 14f; typeface = Typeface.DEFAULT_BOLD }
    private val linePaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 0.75f }

    /** Cuántas hojas salen para [count] etiquetas. */
    fun pageCount(count: Int, spec: LabelSheetSpec = LabelSheetSpec()): Int = LabelSheetPaging.paginate(List(count) { it }, spec).size

    fun write(labels: List<BarcodeLabel>, out: OutputStream, spec: LabelSheetSpec = LabelSheetSpec()) {
        val pdf = PdfDocument()
        try {
            LabelSheetPaging.paginate(labels, spec).forEachIndexed { pageIndex, pageLabels ->
                val page = pdf.startPage(pageInfo(pageIndex + 1, spec))
                pageLabels.forEachIndexed { i, label ->
                    drawLabel(page.canvas, label, spec.labelLeftMm(i), spec.labelTopMm(i), spec)
                }
                pdf.finishPage(page)
            }
            pdf.writeTo(out)
        } finally {
            pdf.close()
        }
    }

    /**
     * Una hoja para que ella compruebe, con su impresora, que el tamaño sale bien: una
     * regla de 50 mm (si no mide 50 mm, la impresora escaló la hoja) y el mismo código
     * a cuatro tamaños de módulo, para anotar el más chico que la app lee.
     */
    fun writeTestSheet(out: OutputStream, texts: LabelTestSheetTexts, spec: LabelSheetSpec = LabelSheetSpec()) {
        val label = BarcodeLabel(checkNotNull(ProductUid.parse("XP-000042")))
        val pdf = PdfDocument()
        try {
            val page = pdf.startPage(pageInfo(1, spec))
            val canvas = page.canvas
            val marginMm = 15.0
            var y = marginMm
            canvas.drawText(texts.title, mm(marginMm), mm(y) + 14f, titlePaint)
            y += 9
            // La regla: 50 mm exactos, con una marca en cada punta.
            canvas.drawLine(mm(marginMm), mm(y), mm(marginMm + 50), mm(y), linePaint)
            canvas.drawLine(mm(marginMm), mm(y) - 4f, mm(marginMm), mm(y) + 4f, linePaint)
            canvas.drawLine(mm(marginMm + 50), mm(y) - 4f, mm(marginMm + 50), mm(y) + 4f, linePaint)
            y += 6
            canvas.drawText(texts.ruler, mm(marginMm), mm(y), captionPaint)
            y += 5
            canvas.drawText(texts.instructions, mm(marginMm), mm(y), captionPaint)
            y += 12
            for (module in listOf(0.20, spec.moduleMm, 0.30, 0.35)) {
                val real = module == spec.moduleMm
                val format = if (real) texts.moduleRealFormat else texts.moduleFormat
                canvas.drawText(String.format(Locale.ROOT, format, String.format(Locale.ROOT, "%.3f", module)), mm(marginMm), mm(y), captionPaint)
                y += 3
                drawBars(canvas, label, marginMm + 2.6, y, module, spec.barHeightMm)
                y += spec.barHeightMm + 12
            }
            pdf.finishPage(page)
            pdf.writeTo(out)
        } finally {
            pdf.close()
        }
    }

    private fun pageInfo(number: Int, spec: LabelSheetSpec) =
        PdfDocument.PageInfo.Builder(Math.round(mm(spec.pageWidthMm)), Math.round(mm(spec.pageHeightMm)), number).create()

    private fun mm(value: Double): Float = (value * PT_PER_MM).toFloat()

    private fun drawLabel(canvas: android.graphics.Canvas, label: BarcodeLabel, leftMm: Double, topMm: Double, spec: LabelSheetSpec) {
        val modules = Code128Encoder.moduleCount(label)
        val moduleMm = spec.moduleMmFor(modules)
        val barsWidthMm = modules * moduleMm
        val barsLeftMm = leftMm + (spec.labelWidthMm - barsWidthMm) / 2
        val barsTopMm = topMm + spec.topPaddingMm
        drawBars(canvas, label, barsLeftMm, barsTopMm, moduleMm, spec.barHeightMm)
        // El código también en letras: es lo que ella tipea en la búsqueda si no escanea.
        canvas.drawText(
            label.uid.value,
            mm(leftMm + spec.labelWidthMm / 2),
            mm(barsTopMm + spec.barHeightMm + MM_BELOW_BARS_TO_TEXT_BASELINE),
            textPaint,
        )
    }

    /** Las barras como rectángulos; los módulos negros contiguos se juntan en uno solo. */
    private fun drawBars(canvas: android.graphics.Canvas, label: BarcodeLabel, leftMm: Double, topMm: Double, moduleMm: Double, heightMm: Double) {
        val matrix = Code128Encoder.encode(label)
        var x = 0
        while (x < matrix.width) {
            if (!matrix.get(x, 0)) { x++; continue }
            var end = x
            while (end < matrix.width && matrix.get(end, 0)) end++
            canvas.drawRect(mm(leftMm + x * moduleMm), mm(topMm), mm(leftMm + end * moduleMm), mm(topMm + heightMm), barPaint)
            x = end
        }
    }
}

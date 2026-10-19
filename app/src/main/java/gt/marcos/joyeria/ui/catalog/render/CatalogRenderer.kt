package gt.marcos.joyeria.ui.catalog.render

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import gt.marcos.joyeria.domain.catalog.CatalogCaption
import gt.marcos.joyeria.domain.catalog.CatalogItem
import java.util.Locale

/**
 * Los tres textos fijos que el renderizador dibuja y que vienen de
 * `strings.xml` (nada de texto de usuaria hardcodeado, CLAUDE.md sección 5).
 * `pageOfFormat` lleva dos marcadores, `%1$d` y `%2$d`.
 */
internal data class CatalogLabels(
    val placeholderName: String,
    val noPhoto: String,
    val pageOfFormat: String,
)

/**
 * Dibuja una página del catálogo (Fase 12) con `android.graphics`. Solo recibe
 * [CatalogItem]: no tiene acceso a la base ni a ningún repositorio de
 * productos, así que no puede dibujar un costo aunque alguien quisiera
 * (ver D-058). Aspecto y medidas: [CatalogStyle].
 *
 * Las fotos se decodifican **reducidas** (`inSampleSize`): una foto de 1600px
 * decodificada entera pesa ~7MB, y veinte juntas reventarían la memoria de un
 * teléfono modesto. Una foto que no se puede leer sale como un cuadro gris
 * «Sin foto» y el resto de la página sigue: nunca se cae por una pieza.
 *
 * No es seguro entre hilos (reusa los `Paint`): se usa de una página a la vez.
 */
internal class CatalogRenderer(private val labels: CatalogLabels) {

    private val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CatalogStyle.TILE }
    private val noPhotoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CatalogStyle.NO_PHOTO_BOX }
    private val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val namePaint = textPaint(CatalogStyle.NAME_TEXT_SIZE, CatalogStyle.TEXT, Typeface.DEFAULT)
    private val pricePaint = textPaint(CatalogStyle.PRICE_TEXT_SIZE, CatalogStyle.TEXT, Typeface.DEFAULT_BOLD)
    private val codePaint = textPaint(CatalogStyle.CODE_TEXT_SIZE, CatalogStyle.TEXT_MUTED, Typeface.DEFAULT)
    private val noPhotoTextPaint = textPaint(CatalogStyle.CODE_TEXT_SIZE, CatalogStyle.NO_PHOTO_TEXT, Typeface.DEFAULT)
        .apply { textAlign = Paint.Align.CENTER }
    private val footerPaint = textPaint(CatalogStyle.CODE_TEXT_SIZE, CatalogStyle.TEXT_MUTED, Typeface.DEFAULT)
        .apply { textAlign = Paint.Align.CENTER }

    /** Una página como imagen, lista para comprimir a JPEG. Quien la recibe la `recycle()`. */
    fun renderPage(pieces: List<CatalogItem>, pageNumber: Int, pageCount: Int): Bitmap {
        val height = CatalogStyle.pageHeight(pieces.size)
        val bitmap = Bitmap.createBitmap(CatalogStyle.PAGE_WIDTH, height, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap), pieces, pageNumber, pageCount)
        return bitmap
    }

    /**
     * Dibuja sobre cualquier `Canvas` (de un `Bitmap`, o de una página de PDF
     * para comparar formatos). El lienzo mide [CatalogStyle.PAGE_WIDTH] de ancho
     * y [CatalogStyle.pageHeight] de alto.
     */
    fun draw(canvas: Canvas, pieces: List<CatalogItem>, pageNumber: Int, pageCount: Int) {
        val height = CatalogStyle.pageHeight(pieces.size)
        canvas.drawColor(CatalogStyle.BACKGROUND)
        pieces.forEachIndexed { index, piece ->
            drawTile(
                canvas = canvas,
                piece = piece,
                left = CatalogStyle.tileLeft(index % CatalogStyle.COLUMNS),
                top = CatalogStyle.tileTop(index / CatalogStyle.COLUMNS),
            )
        }
        if (pageCount > 1) {
            val label = String.format(Locale.ROOT, labels.pageOfFormat, pageNumber, pageCount)
            val baseline = height - (CatalogStyle.FOOTER_HEIGHT - CatalogStyle.CODE_TEXT_SIZE) / 2f -
                footerPaint.fontMetrics.descent
            canvas.drawText(label, CatalogStyle.PAGE_WIDTH / 2f, baseline, footerPaint)
        }
    }

    private fun drawTile(canvas: Canvas, piece: CatalogItem, left: Int, top: Int) {
        val tile = RectF(
            left.toFloat(), top.toFloat(),
            (left + CatalogStyle.TILE_WIDTH).toFloat(), (top + CatalogStyle.TILE_HEIGHT).toFloat(),
        )
        canvas.drawRoundRect(tile, CatalogStyle.TILE_RADIUS, CatalogStyle.TILE_RADIUS, tilePaint)

        // La foto, completa y centrada en un cuadrado, sin recortar. El
        // `clip` redondeado evita que una foto que llena el cuadrado pise las
        // esquinas de la tarjeta.
        val photoArea = RectF(tile.left, tile.top, tile.right, tile.top + CatalogStyle.PHOTO_SIZE)
        canvas.save()
        canvas.clipPath(Path().apply { addRoundRect(tile, CatalogStyle.TILE_RADIUS, CatalogStyle.TILE_RADIUS, Path.Direction.CW) })
        val photo = piece.photoPath?.let { decodeScaled(it, CatalogStyle.PHOTO_SIZE) }
        if (photo != null) {
            drawPhotoContained(canvas, photo, photoArea)
            photo.recycle()
        } else {
            drawNoPhoto(canvas, photoArea)
        }
        canvas.restore()

        val textLeft = tile.left + CatalogStyle.TEXT_PADDING
        val textWidth = CatalogStyle.TILE_WIDTH - 2 * CatalogStyle.TEXT_PADDING
        var y = photoArea.bottom + CatalogStyle.GAP_AFTER_PHOTO

        val caption = CatalogCaption.of(piece, labels.placeholderName)
        if (caption.isNotEmpty()) {
            val layout = StaticLayout.Builder.obtain(caption, 0, caption.length, namePaint, textWidth)
                .setMaxLines(CatalogStyle.NAME_MAX_LINES)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()
            canvas.save()
            canvas.translate(textLeft, y)
            layout.draw(canvas)
            canvas.restore()
        }
        y += CatalogStyle.NAME_BLOCK_HEIGHT

        canvas.drawText(catalogPriceText(piece.price), textLeft, y - pricePaint.fontMetrics.ascent, pricePaint)
        y += CatalogStyle.PRICE_BLOCK_HEIGHT

        canvas.drawText(piece.uid, textLeft, y - codePaint.fontMetrics.ascent, codePaint)
    }

    private fun drawPhotoContained(canvas: Canvas, photo: Bitmap, area: RectF) {
        val scale = minOf(area.width() / photo.width, area.height() / photo.height)
        val width = photo.width * scale
        val height = photo.height * scale
        val left = area.left + (area.width() - width) / 2f
        val top = area.top + (area.height() - height) / 2f
        canvas.drawBitmap(photo, null, RectF(left, top, left + width, top + height), photoPaint)
    }

    private fun drawNoPhoto(canvas: Canvas, area: RectF) {
        canvas.drawRect(area, noPhotoPaint)
        val baseline = area.centerY() - (noPhotoTextPaint.fontMetrics.ascent + noPhotoTextPaint.fontMetrics.descent) / 2f
        canvas.drawText(labels.noPhoto, area.centerX(), baseline, noPhotoTextPaint)
    }

    /**
     * Decodifica la foto lo bastante reducida para que su lado mayor siga
     * siendo al menos [targetSide], sin cargarla entera. `null` si no se puede
     * leer (archivo borrado, corrupto, sin memoria): la pieza sale «Sin foto».
     */
    private fun decodeScaled(path: String, targetSide: Int): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            null
        } else {
            val options = BitmapFactory.Options().apply {
                inSampleSize = CatalogStyle.sampleSizeFor(maxOf(bounds.outWidth, bounds.outHeight), targetSide)
            }
            BitmapFactory.decodeFile(path, options)
        }
    } catch (error: Exception) {
        null
    } catch (error: OutOfMemoryError) {
        null
    }

    private fun textPaint(size: Float, color: Int, typeface: Typeface) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.textSize = size
        this.color = color
        this.typeface = typeface
    }
}

package gt.marcos.joyeria.ui.catalog.render

/**
 * Aspecto y medidas de las imágenes del catálogo (Fase 12). Todo vive acá y
 * **no sale del tema de la app**: el catálogo es una imagen que se manda a
 * otra gente, así que no puede depender del modo oscuro del teléfono ni de la
 * paleta de la app (que la ronda de diseño va a cambiar).
 *
 * Criterio de color: ella vende joyería dorada, y lo que más resalta en una
 * foto es el dorado. Por eso el catálogo **no usa nada que se le parezca**:
 * fondo y tarjetas en grises neutros sin tinte (nada cálido: ni crema, ni
 * beige, ni amarillo), texto casi negro y **ningún color de acento**, ni en el
 * precio. Lo único con color en la imagen es la foto de la pieza.
 *
 * Medidas en píxeles de la imagen (1080 de ancho). En un teléfono de 360dp son
 * 3px por dp: el nombre de 54px se ve de ~18dp y el precio de 66px de ~22dp.
 * Grande a propósito: la clienta lo ve chico y WhatsApp recomprime.
 */
internal object CatalogStyle {
    // Colores (ARGB). Contrastes: texto sobre fondo 14.2:1, sobre tarjeta 15.9:1;
    // gris sobre fondo 4.8:1, sobre tarjeta 5.3:1.
    const val BACKGROUND = 0xFFF2F2F2.toInt()
    const val TILE = 0xFFFFFFFF.toInt()
    const val TEXT = 0xFF222222.toInt()
    const val TEXT_MUTED = 0xFF6B6B6B.toInt()
    const val NO_PHOTO_BOX = 0xFFE8E8E8.toInt()
    const val NO_PHOTO_TEXT = 0xFF555555.toInt()

    // Página: dos columnas, hasta dos filas (cuatro piezas por imagen).
    const val PAGE_WIDTH = 1080
    const val MARGIN = 36
    const val GUTTER = 36
    const val COLUMNS = 2
    const val TILE_WIDTH = (PAGE_WIDTH - 2 * MARGIN - (COLUMNS - 1) * GUTTER) / COLUMNS // 486
    const val TILE_RADIUS = 24f
    const val FOOTER_HEIGHT = 76

    // Tarjeta: foto cuadrada (nunca recortada), y debajo nombre, precio y código.
    const val PHOTO_SIZE = TILE_WIDTH
    const val TEXT_PADDING = 18
    const val GAP_AFTER_PHOTO = 16
    const val NAME_TEXT_SIZE = 54f
    const val NAME_MAX_LINES = 2
    const val NAME_BLOCK_HEIGHT = 136
    const val PRICE_TEXT_SIZE = 66f
    const val PRICE_BLOCK_HEIGHT = 80
    const val CODE_TEXT_SIZE = 38f
    const val CODE_BLOCK_HEIGHT = 46
    const val BOTTOM_PADDING = 16
    const val TILE_HEIGHT =
        PHOTO_SIZE + GAP_AFTER_PHOTO + NAME_BLOCK_HEIGHT + PRICE_BLOCK_HEIGHT + CODE_BLOCK_HEIGHT + BOTTOM_PADDING

    const val JPEG_QUALITY = 85

    /** Alto de una página con [pieceCount] piezas: solo las filas que hacen falta, para no mandar una imagen medio vacía. */
    fun pageHeight(pieceCount: Int): Int {
        val rows = (pieceCount + COLUMNS - 1) / COLUMNS
        return MARGIN + rows * TILE_HEIGHT + (rows - 1) * GUTTER + FOOTER_HEIGHT
    }

    /** La mayor potencia de 2 que deja el lado mayor de la foto en [targetSide] o más. Pura, sin `Bitmap`. */
    fun sampleSizeFor(longestSide: Int, targetSide: Int): Int {
        var sample = 1
        while (longestSide / (sample * 2) >= targetSide) sample *= 2
        return sample
    }

    fun tileLeft(column: Int): Int = MARGIN + column * (TILE_WIDTH + GUTTER)

    fun tileTop(row: Int): Int = MARGIN + row * (TILE_HEIGHT + GUTTER)
}

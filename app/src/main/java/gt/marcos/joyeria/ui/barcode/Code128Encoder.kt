package gt.marcos.joyeria.ui.barcode

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.oned.Code128Writer
import gt.marcos.joyeria.domain.barcode.BarcodeLabel

/**
 * Genera el Code128 de una etiqueta con ZXing. **Solo recibe [BarcodeLabel]**, cuyo
 * único campo es el código de la pieza: no tiene por dónde recibir un monto (D-003,
 * D-060). Lo que se codifica es `label.uid.value` y nada más.
 *
 * La matriz sale **sin zona de silencio** (`MARGIN = 0`): quien la dibuja la agrega
 * según el tamaño de la etiqueta. ZXing elige solo los juegos de caracteres: con
 * `XP-000042` usa el B para `XP-` y el C para los seis dígitos, y sale de 112
 * módulos (123 con 7 dígitos).
 */
object Code128Encoder {

    /** Una fila de módulos: `get(x, 0)` es `true` si el módulo `x` es una barra. */
    fun encode(label: BarcodeLabel): BitMatrix =
        Code128Writer().encode(
            label.uid.value,
            BarcodeFormat.CODE_128,
            0,
            0,
            mapOf(EncodeHintType.MARGIN to 0),
        )

    /** Cuántos módulos de ancho tiene el código de [label], sin zona de silencio. */
    fun moduleCount(label: BarcodeLabel): Int = encode(label).width
}

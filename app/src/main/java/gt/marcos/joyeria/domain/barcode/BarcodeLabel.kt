package gt.marcos.joyeria.domain.barcode

/**
 * Una etiqueta de código de barras (Fase 13). **Su único campo es el código de la
 * pieza**: no tiene por dónde recibir un precio, un costo ni ningún monto. El
 * generador (`Code128Encoder`) y la hoja de etiquetas solo reciben este tipo, así
 * que lo que se codifica y el texto legible debajo del código son `uid.value` y
 * nada más (D-003, D-060).
 *
 * `BarcodeLabelTest` fija el conjunto exacto de campos: agregarle uno rompe el test.
 */
data class BarcodeLabel(val uid: ProductUid)

package gt.marcos.joyeria.domain.barcode

/** Lo que la base sabe de un código de pieza escaneado. */
data class UidLookup(val productId: Long, val archived: Boolean, val stockQty: Int)

/** Las cuatro salidas posibles de escanear un código. Ninguna es una excepción. */
sealed interface ScanResult {
    /** Una pieza activa de su inventario. */
    data class Found(val productId: Long, val stockQty: Int) : ScanResult

    /** Es una pieza suya, pero está archivada. */
    data object Archived : ScanResult

    /** Tiene forma de código de pieza, pero no existe en su inventario. */
    data class NotFound(val uid: ProductUid) : ScanResult

    /** No es un código de pieza (un código de barras de otro producto, un QR, un monto). */
    data object NotAPiece : ScanResult
}

/**
 * Decide qué hacer con lo que leyó la cámara. Pura: la búsqueda en la base entra
 * como función, así que se prueba sin Android. Un código desconocido o ajeno
 * devuelve un resultado, nunca lanza: «si se escanea un código desconocido,
 * mensaje claro, sin crash» (FASES.md Fase 13).
 */
object ScanResolver {
    suspend fun resolve(raw: String?, find: suspend (ProductUid) -> UidLookup?): ScanResult {
        val uid = ProductUid.parse(raw) ?: return ScanResult.NotAPiece
        val row = find(uid) ?: return ScanResult.NotFound(uid)
        return if (row.archived) ScanResult.Archived else ScanResult.Found(row.productId, row.stockQty)
    }
}

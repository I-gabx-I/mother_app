package gt.marcos.joyeria.data.repository

import gt.marcos.joyeria.data.local.dao.ProductDao
import gt.marcos.joyeria.domain.barcode.LabelPiece
import gt.marcos.joyeria.domain.barcode.ProductUid
import gt.marcos.joyeria.domain.barcode.UidLookup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Códigos de barras (Fase 13): buscar la pieza de un código escaneado y listar las
 * piezas para etiquetar. De solo lectura, y **sin acceso a los tipos de producto
 * que llevan costo** (`ProductEntity`, `ProductSummary`, `ProductDetail`): solo
 * conoce `UidLookup` y `LabelRow`, que no tienen ningún monto.
 */
class BarcodeRepository @Inject constructor(
    private val productDao: ProductDao,
) {
    suspend fun find(uid: ProductUid): UidLookup? = productDao.findByUid(uid.value)

    /** Una pieza cuyo `uid` no tenga el formato de código (no debería existir) no se puede etiquetar y no aparece. */
    fun observeLabelPieces(): Flow<List<LabelPiece>> =
        productDao.observeLabelRows().map { rows ->
            rows.mapNotNull { row ->
                ProductUid.parse(row.uid)?.let { LabelPiece(it, row.name, row.categoryName, row.photoPath) }
            }
        }
}

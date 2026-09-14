package gt.marcos.joyeria.data.repository

import androidx.room.withTransaction
import gt.marcos.joyeria.data.local.AppDatabase
import gt.marcos.joyeria.data.local.ProductUidGenerator
import gt.marcos.joyeria.data.local.dao.PriceHistoryDao
import gt.marcos.joyeria.data.local.dao.ProductDao
import gt.marcos.joyeria.data.local.entity.PriceHistoryEntity
import gt.marcos.joyeria.data.local.entity.ProductEntity
import gt.marcos.joyeria.domain.model.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Puente entre `domain`/`ui` y Room para `product`. Expone solo tipos
 * planos en su firma pública -- nunca `ProductEntity` -- para que
 * `domain/usecase` no termine importando Room de forma indirecta
 * (CLAUDE.md sección 5).
 */
class ProductRepository @Inject constructor(
    private val db: AppDatabase,
    private val productDao: ProductDao,
    private val priceHistoryDao: PriceHistoryDao,
    private val uidGenerator: ProductUidGenerator,
) {
    suspend fun insert(input: AddProductInput): String = db.withTransaction {
        val uid = uidGenerator.next()
        val now = System.currentTimeMillis()
        productDao.insert(
            ProductEntity(
                uid = uid,
                name = input.name,
                categoryId = input.categoryId,
                costCents = input.cost.cents,
                salePriceCents = input.salePrice.cents,
                stockQty = input.stockQty,
                photoPath = input.photoPath,
                supplier = null,
                notes = input.notes,
                createdAt = now,
                updatedAt = now,
            ),
        )
        uid
    }

    fun observeFiltered(query: String, categoryId: Long?): Flow<List<ProductSummary>> =
        productDao.observeFiltered(query, categoryId).map { list -> list.map { it.toSummary() } }

    suspend fun getDetail(id: Long): ProductDetail? = productDao.getById(id)?.toDetail()

    suspend fun archive(id: Long) {
        productDao.archive(id, System.currentTimeMillis())
    }

    /**
     * Actualiza la pieza y, si costo o precio cambiaron respecto al valor
     * guardado, inserta una fila en `price_history` con los valores
     * nuevos (snapshot completo de costo+precio, no solo el campo que
     * cambió) -- todo en una sola transacción. `ESQUEMA.md`: "se inserta
     * una fila cada vez que cambia costo o precio".
     */
    suspend fun update(input: EditProductInput): Unit = db.withTransaction {
        val existing = checkNotNull(productDao.getById(input.id)) {
            "Producto ${input.id} no existe -- no se puede editar algo que no está"
        }
        val now = System.currentTimeMillis()
        val priceChanged = existing.costCents != input.cost.cents || existing.salePriceCents != input.salePrice.cents
        if (priceChanged) {
            priceHistoryDao.insert(
                PriceHistoryEntity(
                    productId = input.id,
                    costCents = input.cost.cents,
                    salePriceCents = input.salePrice.cents,
                    changedAt = now,
                ),
            )
        }
        productDao.update(
            existing.copy(
                name = input.name,
                categoryId = input.categoryId,
                costCents = input.cost.cents,
                salePriceCents = input.salePrice.cents,
                stockQty = input.stockQty,
                supplier = input.supplier,
                notes = input.notes,
                updatedAt = now,
            ),
        )
    }

    private fun ProductEntity.toSummary() = ProductSummary(
        id = id,
        uid = uid,
        name = name,
        photoPath = photoPath,
        stockQty = stockQty,
        cost = Money(costCents),
        salePrice = Money(salePriceCents),
    )

    private fun ProductEntity.toDetail() = ProductDetail(
        id = id,
        uid = uid,
        name = name,
        categoryId = categoryId,
        cost = Money(costCents),
        salePrice = Money(salePriceCents),
        stockQty = stockQty,
        photoPath = photoPath,
        supplier = supplier,
        notes = notes,
    )
}

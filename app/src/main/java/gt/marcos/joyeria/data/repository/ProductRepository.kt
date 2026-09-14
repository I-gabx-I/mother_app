package gt.marcos.joyeria.data.repository

import androidx.room.withTransaction
import gt.marcos.joyeria.data.local.AppDatabase
import gt.marcos.joyeria.data.local.ProductUidGenerator
import gt.marcos.joyeria.data.local.dao.ProductDao
import gt.marcos.joyeria.data.local.entity.ProductEntity
import gt.marcos.joyeria.domain.usecase.AddProductInput
import javax.inject.Inject

/**
 * Puente entre `domain` y Room para `product`. Expone solo tipos planos
 * en su firma pública (`AddProductInput`, `String`) — nunca `ProductEntity`
 * ni nada de `androidx.room` — para que `domain/usecase/AddProductUseCase.kt`
 * no termine importando Room de forma indirecta (CLAUDE.md sección 5:
 * "domain no importa nada de Android").
 */
class ProductRepository @Inject constructor(
    private val db: AppDatabase,
    private val productDao: ProductDao,
    private val uidGenerator: ProductUidGenerator,
) {
    /**
     * Genera el `uid` e inserta la pieza en una sola transacción: si el
     * insert fallara, el `uid` consumido también se revierte (no queda un
     * hueco en la secuencia). `ProductUidGenerator.next()` ya es atómico
     * por sí mismo (Fase 01); anidarlo en esta transacción lo hace parte
     * de una operación más grande, no lo duplica.
     */
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
}

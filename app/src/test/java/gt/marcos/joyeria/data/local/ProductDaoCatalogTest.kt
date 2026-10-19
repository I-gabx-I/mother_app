package gt.marcos.joyeria.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.data.local.entity.CategoryEntity
import gt.marcos.joyeria.data.local.entity.ProductEntity
import gt.marcos.joyeria.data.repository.CatalogRepository
import gt.marcos.joyeria.domain.model.Money
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Robolectric: Room in-memory necesita un Context de Android (CLAUDE.md
// sección 8 / D-012). Solo tests de data/, nunca de domain/.
@RunWith(AndroidJUnit4::class)
class ProductDaoCatalogTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: CatalogRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = CatalogRepository(db.productDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    private fun product(
        uid: String,
        name: String = "Anillo de prueba",
        categoryId: Long? = null,
        costCents: Long = 4000,
        salePriceCents: Long = 10000,
        stockQty: Int = 1,
        archived: Boolean = false,
    ) = ProductEntity(
        uid = uid, name = name, categoryId = categoryId, costCents = costCents,
        salePriceCents = salePriceCents, stockQty = stockQty, photoPath = "/fotos/$uid.jpg",
        supplier = null, notes = null, createdAt = 0L, updatedAt = 0L, archived = archived,
    )

    @Test
    fun catalogQuery_withProductCostingQ777_neverReturnsThatNumber() = runTest {
        db.productDao().insert(product(uid = "XP-000001", costCents = 77777, salePriceCents = 5000))

        val items = repository.observeItems().first()

        assertThat(items).hasSize(1)
        assertThat(items.single().price).isEqualTo(Money(5000))
        // El costo no es un campo de nada que haya salido de la consulta: ni
        // en el tipo, ni escrito por la data class en su `toString()`.
        assertThat(items.single().toString()).doesNotContain("77777")
        assertThat(db.productDao().observeCatalogRows().first().single().toString()).doesNotContain("77777")
    }

    @Test
    fun catalogQuery_excludesArchivedPieceThatStillHasStock() = runTest {
        // El estado que el resto del sistema hoy no impide: archivar no pide stock en cero.
        db.productDao().insert(product(uid = "XP-000001", stockQty = 3, archived = true))
        db.productDao().insert(product(uid = "XP-000002", stockQty = 3))

        val uids = repository.observeItems().first().map { it.uid }

        assertThat(uids).containsExactly("XP-000002")
    }

    @Test
    fun catalogQuery_excludesActivePieceWithZeroStock() = runTest {
        db.productDao().insert(product(uid = "XP-000001", stockQty = 0))
        db.productDao().insert(product(uid = "XP-000002", stockQty = 1))

        val uids = repository.observeItems().first().map { it.uid }

        assertThat(uids).containsExactly("XP-000002")
    }

    @Test
    fun catalogQuery_includesActivePieceWithStockAndNoCategory() = runTest {
        db.productDao().insert(product(uid = "XP-000001", categoryId = null))

        val item = repository.observeItems().first().single()

        assertThat(item.categoryName).isNull()
    }

    @Test
    fun catalogQuery_returnsTheCategoryNameOfAPieceThatHasOne() = runTest {
        val categoryId = db.categoryDao().insert(CategoryEntity(name = "Aretes"))
        db.productDao().insert(product(uid = "XP-000001", categoryId = categoryId))

        val item = repository.observeItems().first().single()

        assertThat(item.categoryName).isEqualTo("Aretes")
    }

    @Test
    fun catalogQuery_carriesUidNamePhotoAndSalePriceOfThePiece() = runTest {
        db.productDao().insert(product(uid = "XP-000042", name = "Collar largo", salePriceCents = 7590))

        val item = repository.observeItems().first().single()

        assertThat(item.uid).isEqualTo("XP-000042")
        assertThat(item.name).isEqualTo("Collar largo")
        assertThat(item.photoPath).isEqualTo("/fotos/XP-000042.jpg")
        assertThat(item.price).isEqualTo(Money(7590))
    }
}

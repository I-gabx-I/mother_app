package gt.marcos.joyeria.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import gt.marcos.joyeria.data.local.dao.LabelRow
import gt.marcos.joyeria.data.local.entity.CategoryEntity
import gt.marcos.joyeria.data.local.entity.ProductEntity
import gt.marcos.joyeria.data.repository.BarcodeRepository
import gt.marcos.joyeria.domain.barcode.ProductUid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Modifier

// Robolectric: Room in-memory necesita un Context de Android (CLAUDE.md sección 8 / D-012).
@RunWith(AndroidJUnit4::class)
class ProductDaoUidLookupTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: BarcodeRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = BarcodeRepository(db.productDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    private fun product(uid: String, stockQty: Int = 1, archived: Boolean = false, categoryId: Long? = null, name: String = "Anillo") =
        ProductEntity(
            uid = uid, name = name, categoryId = categoryId, costCents = 77777, salePriceCents = 9000,
            stockQty = stockQty, photoPath = null, supplier = null, notes = null,
            createdAt = 0L, updatedAt = 0L, archived = archived,
        )

    private fun uid(raw: String) = checkNotNull(ProductUid.parse(raw))

    @Test
    fun find_knownUid_returnsItsIdStockAndNotArchived() = runTest {
        val id = db.productDao().insert(product("XP-000042", stockQty = 3))

        val found = repository.find(uid("XP-000042"))

        assertThat(found?.productId).isEqualTo(id)
        assertThat(found?.stockQty).isEqualTo(3)
        assertThat(found?.archived).isFalse()
    }

    @Test
    fun find_unknownUid_returnsNull() = runTest {
        db.productDao().insert(product("XP-000042"))
        assertThat(repository.find(uid("XP-000043"))).isNull()
    }

    @Test
    fun find_archivedPieceThatStillHasStock_reportsItAsArchived() = runTest {
        // El estado que el resto del sistema no impide: archivar no pide existencias en cero.
        db.productDao().insert(product("XP-000042", stockQty = 5, archived = true))

        val found = repository.find(uid("XP-000042"))

        assertThat(found?.archived).isTrue()
        assertThat(found?.stockQty).isEqualTo(5)
    }

    @Test
    fun observeLabelPieces_excludesArchivedPieces() = runTest {
        db.productDao().insert(product("XP-000001", archived = true))
        db.productDao().insert(product("XP-000002"))

        assertThat(repository.observeLabelPieces().first().map { it.uid.value }).containsExactly("XP-000002")
    }

    @Test
    fun observeLabelPieces_includesActivePiecesWithZeroStock() = runTest {
        db.productDao().insert(product("XP-000001", stockQty = 0))

        assertThat(repository.observeLabelPieces().first().map { it.uid.value }).containsExactly("XP-000001")
    }

    @Test
    fun observeLabelPieces_listsTheNewestPieceFirst() = runTest {
        db.productDao().insert(product("XP-000001"))
        db.productDao().insert(product("XP-000002"))
        db.productDao().insert(product("XP-000003"))

        assertThat(repository.observeLabelPieces().first().map { it.uid.value })
            .containsExactly("XP-000003", "XP-000002", "XP-000001").inOrder()
    }

    @Test
    fun observeLabelPieces_carriesTheCategoryNameWhenThePieceHasOne() = runTest {
        val categoryId = db.categoryDao().insert(CategoryEntity(name = "Aretes"))
        db.productDao().insert(product("XP-000001", categoryId = categoryId))

        assertThat(repository.observeLabelPieces().first().single().categoryName).isEqualTo("Aretes")
    }

    @Test
    fun observeLabelPieces_neverShowsTheCostOfAPieceCostingQ777() = runTest {
        db.productDao().insert(product("XP-000001"))

        val piece = repository.observeLabelPieces().first().single()

        assertThat(piece.toString()).doesNotContain("77777")
        assertThat(db.productDao().observeLabelRows().first().single().toString()).doesNotContain("77777")
    }

    @Test
    fun labelRow_columnsHaveNoMoneyAtAll() {
        val fields = LabelRow::class.java.declaredFields
            .filter { !it.isSynthetic && !Modifier.isStatic(it.modifiers) }.map { it.name }.toSet()
        assertWithMessage("LabelRow no puede tener ninguna columna de monto (D-003, D-060).")
            .that(fields).containsExactly("id", "uid", "name", "categoryName", "photoPath")
    }
}

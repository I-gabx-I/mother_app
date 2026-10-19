package gt.marcos.joyeria.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.data.local.entity.CategoryEntity
import gt.marcos.joyeria.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Robolectric: Room in-memory necesita un Context de Android (D-012).
@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // La semilla (Fase 01) ya inserta las cinco categorías -- los
        // tests de acá agregan una sexta propia para no depender de
        // cuántas trae la semilla.
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.SeedCallback())
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun rename_changesTheNameAndLeavesArchivedUntouched() = runTest {
        val id = db.categoryDao().insert(CategoryEntity(name = "Dijes"))

        db.categoryDao().rename(id, "Dijes y colgantes")

        val renamed = checkNotNull(db.categoryDao().getById(id))
        assertThat(renamed.name).isEqualTo("Dijes y colgantes")
        assertThat(renamed.archived).isFalse()
    }

    @Test
    fun archive_withNoProductsAssigned_disappearsFromActiveAndSelectable() = runTest {
        val id = db.categoryDao().insert(CategoryEntity(name = "Dijes"))

        db.categoryDao().archive(id)

        val activeIds = db.categoryDao().observeActive().first().map { it.id }
        val selectableIds = db.categoryDao().observeSelectable().first().map { it.id }
        assertThat(activeIds).doesNotContain(id)
        assertThat(selectableIds).doesNotContain(id)
    }

    @Test
    fun observeSelectable_includesAnArchivedCategoryWithAnActiveProductAssigned() = runTest {
        val id = db.categoryDao().insert(CategoryEntity(name = "Dijes"))
        db.productDao().insert(sampleProduct(uid = "XP-000001", categoryId = id))

        db.categoryDao().archive(id)

        val selectable = db.categoryDao().observeSelectable().first()
        assertThat(selectable.map { it.id }).contains(id)
        assertThat(selectable.first { it.id == id }.archived).isTrue()
    }

    @Test
    fun observeSelectable_stopsIncludingAnArchivedCategory_onceItsLastProductIsReclassified() = runTest {
        val dijesId = db.categoryDao().insert(CategoryEntity(name = "Dijes"))
        val anillosId = checkNotNull(db.categoryDao().observeActive().first().firstOrNull { it.name == "Anillos" }).id
        val productId = db.productDao().insert(sampleProduct(uid = "XP-000002", categoryId = dijesId))
        db.categoryDao().archive(dijesId)
        check(db.categoryDao().observeSelectable().first().map { it.id }.contains(dijesId))

        db.productDao().update(checkNotNull(db.productDao().getById(productId)).copy(categoryId = anillosId))

        val selectableIds = db.categoryDao().observeSelectable().first().map { it.id }
        assertThat(selectableIds).doesNotContain(dijesId)
    }

    @Test
    fun observeSelectable_stopsIncludingAnArchivedCategory_onceItsLastProductIsArchived() = runTest {
        val dijesId = db.categoryDao().insert(CategoryEntity(name = "Dijes"))
        val productId = db.productDao().insert(sampleProduct(uid = "XP-000003", categoryId = dijesId))
        db.categoryDao().archive(dijesId)
        check(db.categoryDao().observeSelectable().first().map { it.id }.contains(dijesId))

        db.productDao().archive(productId, updatedAt = 1L)

        val selectableIds = db.categoryDao().observeSelectable().first().map { it.id }
        assertThat(selectableIds).doesNotContain(dijesId)
    }

    @Test
    fun observeAll_includesActiveAndArchived_withArchivedLast() = runTest {
        val archivedId = db.categoryDao().insert(CategoryEntity(name = "Dijes"))
        db.categoryDao().archive(archivedId)

        val all = db.categoryDao().observeAll().first()
        assertThat(all.map { it.id }).contains(archivedId)
        val archivedFlags = all.map { it.archived }
        assertThat(archivedFlags).isEqualTo(archivedFlags.sorted())
    }

    @Test
    fun countActiveWithName_matchesIgnoringCaseAndSurroundingWhitespace() = runTest {
        db.categoryDao().insert(CategoryEntity(name = "Dijes"))

        assertThat(db.categoryDao().countActiveWithName("dijes", excludeId = -1L)).isEqualTo(1)
        assertThat(db.categoryDao().countActiveWithName("  DIJES  ", excludeId = -1L)).isEqualTo(1)
        assertThat(db.categoryDao().countActiveWithName("Pendientes", excludeId = -1L)).isEqualTo(0)
    }

    // Ningún test anterior usaba ñ ni tildes en MAYÚSCULA, que es donde un LOWER() solo-ASCII fallaría
    // (en el emulador se comprobó que el SQLite de Android sí las entiende: ver CategoryNameCaseInstrumentedTest).
    @Test
    fun countActiveWithName_matchesUppercaseEnyeAndAccentsAgainstTheirLowercase() = runTest {
        db.categoryDao().insert(CategoryEntity(name = "Niñas"))
        db.categoryDao().insert(CategoryEntity(name = "Ángeles"))

        assertThat(db.categoryDao().countActiveWithName("NIÑAS", excludeId = -1)).isEqualTo(1)
        assertThat(db.categoryDao().countActiveWithName("ÁNGELES", excludeId = -1)).isEqualTo(1)
        assertThat(db.categoryDao().countActiveWithName("ángeles", excludeId = -1)).isEqualTo(1)
    }

    @Test
    fun countActiveWithName_treatsADifferentAccentAsADifferentName() = runTest {
        db.categoryDao().insert(CategoryEntity(name = "Anillos"))

        assertThat(db.categoryDao().countActiveWithName("Ánillos", excludeId = -1)).isEqualTo(0)
    }

    @Test
    fun countActiveWithName_excludesTheCategoryItself_byExcludeId() = runTest {
        val id = db.categoryDao().insert(CategoryEntity(name = "Dijes"))

        assertThat(db.categoryDao().countActiveWithName("Dijes", excludeId = id)).isEqualTo(0)
        assertThat(db.categoryDao().countActiveWithName("Dijes", excludeId = -1L)).isEqualTo(1)
    }

    @Test
    fun countActiveWithName_ignoresAnArchivedCategoryWithTheSameName() = runTest {
        val id = db.categoryDao().insert(CategoryEntity(name = "Dijes"))
        db.categoryDao().archive(id)

        assertThat(db.categoryDao().countActiveWithName("Dijes", excludeId = -1L)).isEqualTo(0)
    }

    private fun sampleProduct(uid: String, categoryId: Long?) = ProductEntity(
        uid = uid,
        name = "Pieza de prueba",
        categoryId = categoryId,
        costCents = 4000,
        salePriceCents = 10000,
        stockQty = 1,
        photoPath = null,
        supplier = null,
        notes = null,
        createdAt = 0L,
        updatedAt = 0L,
    )
}

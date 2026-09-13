package gt.marcos.joyeria.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Robolectric: Room in-memory necesita un Context de Android que la JVM
// pura no tiene (CLAUDE.md sección 8 / D-012). Solo tests de data/, nunca
// de domain/.
@RunWith(AndroidJUnit4::class)
class ProductDaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.SeedCallback())
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insert_and_getById_returns_the_stored_product() = runTest {
        val id = db.productDao().insert(sampleProduct(uid = "XP-000001"))

        val loaded = db.productDao().getById(id)

        assertThat(loaded).isNotNull()
        assertThat(loaded?.uid).isEqualTo("XP-000001")
        assertThat(loaded?.name).isEqualTo("Anillo de prueba")
    }

    @Test
    fun update_changes_the_stored_fields() = runTest {
        val id = db.productDao().insert(sampleProduct(uid = "XP-000002"))
        val original = checkNotNull(db.productDao().getById(id))

        db.productDao().update(original.copy(salePriceCents = 15000, name = "Anillo actualizado"))

        val updated = db.productDao().getById(id)
        assertThat(updated?.salePriceCents).isEqualTo(15000)
        assertThat(updated?.name).isEqualTo("Anillo actualizado")
    }

    @Test
    fun archive_sets_archived_true_and_removes_it_from_the_active_query() = runTest {
        val id = db.productDao().insert(sampleProduct(uid = "XP-000003"))

        db.productDao().archive(id, updatedAt = 111L)

        val loaded = checkNotNull(db.productDao().getById(id))
        assertThat(loaded.archived).isTrue()
        assertThat(loaded.updatedAt).isEqualTo(111L)

        val activeUids = db.productDao().observeActive().first().map { it.uid }
        assertThat(activeUids).doesNotContain(loaded.uid)
    }

    @Test
    fun getByUid_finds_the_product_by_its_uid() = runTest {
        db.productDao().insert(sampleProduct(uid = "XP-000004"))

        val loaded = db.productDao().getByUid("XP-000004")

        assertThat(loaded).isNotNull()
    }

    @Test
    fun getByUid_returns_null_for_an_unknown_uid() = runTest {
        val loaded = db.productDao().getByUid("XP-999999")

        assertThat(loaded).isNull()
    }

    private fun sampleProduct(uid: String) = ProductEntity(
        uid = uid,
        name = "Anillo de prueba",
        categoryId = null,
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

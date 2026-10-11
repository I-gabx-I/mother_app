package gt.marcos.joyeria.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Robolectric: Room in-memory necesita un Context de Android (D-012).
@RunWith(AndroidJUnit4::class)
class CategoryRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: CategoryRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.SeedCallback())
            .build()
        repository = CategoryRepository(categoryDao = db.categoryDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun add_withANameNotUsedByAnyActiveCategory_insertsIt() = runTest {
        repository.add("Dijes")

        val names = repository.observeActive().first().map { it.name }
        assertThat(names).contains("Dijes")
    }

    // D-057: única entre activas, sin índice único en la base -- la garantía
    // es de este repositorio.
    @Test
    fun add_withTheNameOfAnActiveCategory_throwsAndDoesNotInsert() = runTest {
        val before = repository.observeActive().first().size

        try {
            repository.add("Anillos")
            throw AssertionError("Debió lanzar CategoryNameAlreadyExistsException")
        } catch (e: CategoryNameAlreadyExistsException) {
            assertThat(e.name).isEqualTo("Anillos")
        }

        assertThat(repository.observeActive().first()).hasSize(before)
    }

    @Test
    fun add_withTheNameOfAnArchivedCategory_insertsIt() = runTest {
        val archivedId = repository.add("Dijes")
        repository.archive(archivedId)

        repository.add("Dijes")

        val activeNames = repository.observeActive().first().map { it.name }
        assertThat(activeNames.count { it == "Dijes" }).isEqualTo(1)
    }

    @Test
    fun rename_toItsOwnCurrentName_doesNotThrow() = runTest {
        val id = repository.add("Dijes")

        repository.rename(id, "Dijes")

        val renamed = repository.observeActive().first().first { it.id == id }
        assertThat(renamed.name).isEqualTo("Dijes")
    }

    @Test
    fun rename_toTheNameOfAnotherActiveCategory_throwsAndDoesNotRename() = runTest {
        val id = repository.add("Dijes")

        try {
            repository.rename(id, "Anillos")
            throw AssertionError("Debió lanzar CategoryNameAlreadyExistsException")
        } catch (e: CategoryNameAlreadyExistsException) {
            assertThat(e.name).isEqualTo("Anillos")
        }

        val unchanged = repository.observeActive().first().first { it.id == id }
        assertThat(unchanged.name).isEqualTo("Dijes")
    }
}

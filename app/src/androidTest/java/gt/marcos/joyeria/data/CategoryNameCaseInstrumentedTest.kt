package gt.marcos.joyeria.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.data.local.AppDatabase
import gt.marcos.joyeria.data.local.entity.CategoryEntity
import gt.marcos.joyeria.data.repository.CategoryNameAlreadyExistsException
import gt.marcos.joyeria.data.repository.CategoryRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/**
 * La regla de nombres de categoría (D-057) corre sobre `LOWER(TRIM(name))` de SQLite. Se sospechó que el `LOWER()`
 * solo entendía ASCII (`Niñas` y `NIÑAS` serían nombres distintos); **en el SQLite real del emulador (Android 15)
 * no es así**: entiende ñ y tildes. Este test lo deja fijado en el dispositivo (el SQLite de Robolectric es otro
 * y no prueba lo mismo) y lo prueba por el camino real, `CategoryRepository`. Ningún test anterior usaba ñ ni tildes
 * en mayúscula, que es donde un `LOWER()` solo-ASCII fallaría.
 */
@RunWith(AndroidJUnit4::class)
class CategoryNameCaseInstrumentedTest {

    private fun inMemoryDb(): AppDatabase =
        Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()

    @Test
    fun sqliteLower_understandsEnyeAndAccents() {
        val db = inMemoryDb()
        try {
            val cursor = db.openHelper.writableDatabase.query("SELECT LOWER('NIÑAS'), LOWER('ÁÉÍÓÚ')")
            cursor.moveToFirst()
            assertThat(cursor.getString(0)).isEqualTo("niñas")
            assertThat(cursor.getString(1)).isEqualTo("áéíóú")
            cursor.close()
        } finally {
            db.close()
        }
    }

    @Test
    fun add_uppercaseEnyeNameWhenTheLowercaseOneIsActive_isRejected() {
        val db = inMemoryDb()
        try {
            runBlocking { db.categoryDao().insert(CategoryEntity(name = "Niñas")) }
            val repository = CategoryRepository(db.categoryDao())

            assertThrows(CategoryNameAlreadyExistsException::class.java) { runBlocking { repository.add("NIÑAS") } }
        } finally {
            db.close()
        }
    }

    @Test
    fun add_uppercaseAccentedNameWhenTheMixedCaseOneIsActive_isRejected() {
        val db = inMemoryDb()
        try {
            runBlocking { db.categoryDao().insert(CategoryEntity(name = "Ángeles")) }
            val repository = CategoryRepository(db.categoryDao())

            assertThrows(CategoryNameAlreadyExistsException::class.java) { runBlocking { repository.add("ÁNGELES") } }
        } finally {
            db.close()
        }
    }
}

package gt.marcos.joyeria.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Copia EXACTA de app/src/test/.../ProductUidGeneratorTest, para correrla
// una vez sobre SQLite real en un dispositivo/emulador (no Robolectric).
// Motivo (pedido explícito): Robolectric usa shadows del framework y no
// garantiza reproducir la semántica real de bloqueo de conexión de Android.
// Resultado de ambas corridas documentado en ESTADO.md.
@RunWith(AndroidJUnit4::class)
class ProductUidGeneratorInstrumentedTest {
    private lateinit var db: AppDatabase
    private lateinit var generator: ProductUidGenerator

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.SeedCallback())
            .build()
        generator = ProductUidGenerator(db, db.appSettingDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun sequential_calls_produce_increasing_uids() = runTest {
        assertThat(generator.next()).isEqualTo("XP-000001")
        assertThat(generator.next()).isEqualTo("XP-000002")
        assertThat(generator.next()).isEqualTo("XP-000003")
    }

    @Test
    fun concurrent_calls_never_produce_duplicate_uids() = runTest {
        val n = 100

        val uids = (1..n)
            .map { async(Dispatchers.IO) { generator.next() } }
            .awaitAll()

        assertThat(uids.toSet()).hasSize(n)
    }
}

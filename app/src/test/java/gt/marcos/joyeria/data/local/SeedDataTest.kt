package gt.marcos.joyeria.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.data.local.entity.AppSettingEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SeedDataTest {
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
    fun first_open_seeds_the_five_categories() = runTest {
        val names = db.categoryDao().observeActive().first().map { it.name }

        assertThat(names).containsExactly("Anillos", "Cadenas", "Aretes", "Pulseras", "Juegos")
    }

    // Compara la semilla completa contra las claves de ESQUEMA.md en una
    // sola aserción (containsExactly), no clave por clave: si mañana alguien
    // agrega una clave a ESQUEMA.md y olvida sembrarla (o al revés, siembra
    // una de más que no está documentada), este test falla solo — una lista
    // de asserts sueltos, uno por clave ya conocida, no detecta ni una clave
    // faltante ni una de más, solo un valor distinto en una clave que ya se
    // estaba revisando. Siete claves desde la versión 2 (Fase 05, D-033):
    // una instalación nueva siembra `min_margin_bp` directo (no pasa por
    // MIGRATION_1_2, que es solo para quien ya tenía una base en v1).
    @Test
    fun first_open_seeds_exactly_the_seven_esquema_keys_with_their_values() = runTest {
        val seeded = db.appSettingDao().getAll()

        assertThat(seeded).containsExactly(
            AppSettingEntity(AppSettingKeys.DEFAULT_MARKUP_BP, "10000"),
            AppSettingEntity(AppSettingKeys.PRICE_ROUNDING_STEP_CENTS, "500"),
            AppSettingEntity(AppSettingKeys.LOW_STOCK_THRESHOLD, "2"),
            AppSettingEntity(AppSettingKeys.STALE_STOCK_DAYS, "90"),
            AppSettingEntity(AppSettingKeys.NEXT_PRODUCT_UID_SEQ, "1"),
            AppSettingEntity(AppSettingKeys.OWNER_NAME, ""),
            AppSettingEntity(AppSettingKeys.MIN_MARGIN_BP, "2500"),
        )
    }
}

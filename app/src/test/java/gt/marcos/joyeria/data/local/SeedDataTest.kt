package gt.marcos.joyeria.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
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

    @Test
    fun first_open_seeds_all_app_setting_keys_as_parseable_integers() = runTest {
        val dao = db.appSettingDao()

        assertThat(dao.getValue(AppSettingKeys.DEFAULT_MARKUP_PERCENT)?.toInt()).isEqualTo(200)
        assertThat(dao.getValue(AppSettingKeys.PRICE_ROUNDING_STEP_CENTS)?.toLong()).isEqualTo(500L)
        assertThat(dao.getValue(AppSettingKeys.LOW_STOCK_THRESHOLD)?.toInt()).isEqualTo(2)
        assertThat(dao.getValue(AppSettingKeys.STALE_STOCK_DAYS)?.toInt()).isEqualTo(90)
        assertThat(dao.getValue(AppSettingKeys.NEXT_PRODUCT_UID_SEQ)?.toLong()).isEqualTo(1L)
        assertThat(dao.getValue(AppSettingKeys.OWNER_NAME)).isEmpty()
    }
}

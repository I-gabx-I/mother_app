package gt.marcos.joyeria.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Primera migración real del proyecto (D-033): v1 -> v2. `MigrationTestHelper`
 * crea una base de verdad en el schema v1 (con datos reales, no vacía),
 * corre `MIGRATION_1_2`, y valida el resultado contra
 * `app/schemas/.../2.json` -- un test verde acá **no reemplaza** la
 * verificación manual de instalar la versión vieja encima en el emulador
 * (documentada en ESTADO.md): un emulador limpio nunca ejecuta ninguna
 * migración, así que esa prueba manual es la única que ejercita el camino
 * real de un upgrade.
 *
 * En `androidTest`, no en `app/src/test` (Robolectric): `room-testing`
 * 2.8.5 sobre Robolectric tira
 * `IllegalArgumentException: This driver is configured to open a database
 * named 'X' but 'Y' was requested` -- un choque real entre el driver
 * SQLite nuevo de Room y cómo Robolectric resuelve la ruta de la base,
 * no un problema de este código (confirmado leyendo el bytecode de
 * `SupportSQLiteMigrationTestHelper`, sin fix disponible sin tocar la
 * versión de Room). Corriendo sobre SQLite real (emulador/dispositivo)
 * no aparece -- y de paso es una prueba más fiel: SQLite de verdad, no
 * simulado.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val testDbName = "migration-test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate1To2_keepsExistingPriceHistoryRow_withDefaultsForTheNewColumns() {
        var db = helper.createDatabase(testDbName, 1)
        db.execSQL("INSERT INTO category (id, name, archived) VALUES (1, 'Anillos', 0)")
        db.execSQL(
            """
            INSERT INTO product
                (id, uid, name, category_id, cost_cents, sale_price_cents, stock_qty, photo_path, supplier, notes, created_at, updated_at, archived)
            VALUES (1, 'XP-000001', 'Anillo de prueba', 1, 4000, 8000, 3, NULL, NULL, NULL, 0, 0, 0)
            """.trimIndent(),
        )
        db.execSQL(
            "INSERT INTO price_history (id, product_id, cost_cents, sale_price_cents, changed_at) VALUES (1, 1, 4000, 8000, 12345)",
        )
        db.close()

        db = helper.runMigrationsAndValidate(testDbName, 2, true, AppDatabase.MIGRATION_1_2)

        val row = db.query(
            "SELECT product_id, cost_cents, sale_price_cents, purchase_id, cycle_start FROM price_history WHERE id = 1",
        )
        assertThat(row.moveToFirst()).isTrue()
        assertThat(row.getLong(row.getColumnIndexOrThrow("product_id"))).isEqualTo(1L)
        assertThat(row.getLong(row.getColumnIndexOrThrow("cost_cents"))).isEqualTo(4000L)
        assertThat(row.getLong(row.getColumnIndexOrThrow("sale_price_cents"))).isEqualTo(8000L)
        assertThat(row.isNull(row.getColumnIndexOrThrow("purchase_id"))).isTrue()
        assertThat(row.getInt(row.getColumnIndexOrThrow("cycle_start"))).isEqualTo(0)
        row.close()
    }

    @Test
    fun migrate1To2_seedsMinMarginBp_forDatabasesThatAlreadyExisted() {
        val db = helper.createDatabase(testDbName, 1)
        db.close()

        val migrated = helper.runMigrationsAndValidate(testDbName, 2, true, AppDatabase.MIGRATION_1_2)

        val row = migrated.query("SELECT value FROM app_setting WHERE `key` = 'min_margin_bp'")
        assertThat(row.moveToFirst()).isTrue()
        assertThat(row.getString(0)).isEqualTo("2500")
        row.close()
    }
}

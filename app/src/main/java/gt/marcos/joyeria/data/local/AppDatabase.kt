package gt.marcos.joyeria.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import gt.marcos.joyeria.data.local.dao.AppSettingDao
import gt.marcos.joyeria.data.local.dao.CategoryDao
import gt.marcos.joyeria.data.local.dao.PriceHistoryDao
import gt.marcos.joyeria.data.local.dao.ProductDao
import gt.marcos.joyeria.data.local.dao.PurchaseDao
import gt.marcos.joyeria.data.local.dao.PurchaseItemDao
import gt.marcos.joyeria.data.local.dao.SaleDao
import gt.marcos.joyeria.data.local.entity.AppSettingEntity
import gt.marcos.joyeria.data.local.entity.CategoryEntity
import gt.marcos.joyeria.data.local.entity.CustomerEntity
import gt.marcos.joyeria.data.local.entity.PaymentEntity
import gt.marcos.joyeria.data.local.entity.PriceHistoryEntity
import gt.marcos.joyeria.data.local.entity.ProductEntity
import gt.marcos.joyeria.data.local.entity.PurchaseEntity
import gt.marcos.joyeria.data.local.entity.PurchaseItemEntity
import gt.marcos.joyeria.data.local.entity.SaleEntity
import gt.marcos.joyeria.data.local.entity.SaleItemEntity

// Todas las tablas de ESQUEMA.md se declaran en la versión 1 (D-011), aunque
// solo category/product/app_setting tienen DAO en esta fase. Las demás
// entidades quedan declaradas y sin DAO hasta la fase que las use.
@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        PriceHistoryEntity::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        CustomerEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        PaymentEntity::class,
        AppSettingEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun appSettingDao(): AppSettingDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun purchaseItemDao(): PurchaseItemDao
    abstract fun saleDao(): SaleDao

    /**
     * Semilla de las 5 categorías y de las claves de app_setting, en la
     * primera apertura. Usa `execSQL` directo (no DAOs) para que la
     * inserción sea síncrona y termine antes de que `onCreate` devuelva el
     * control: nada que abra la base puede ver un estado a medio sembrar.
     */
    class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            SEED_CATEGORIES.forEach { name ->
                db.execSQL("INSERT INTO category (name, archived) VALUES (?, 0)", arrayOf(name))
            }
            SEED_SETTINGS.forEach { (key, value) ->
                db.execSQL("INSERT INTO app_setting (`key`, value) VALUES (?, ?)", arrayOf(key, value))
            }
        }
    }

    companion object {
        const val DATABASE_NAME = "joyeria.db"

        private val SEED_CATEGORIES = listOf("Anillos", "Cadenas", "Aretes", "Pulseras", "Juegos")

        // Una instalación nueva crea la base directo en la versión vigente
        // (hoy 2): esta lista tiene que llevar TODAS las claves de
        // ESQUEMA.md, no solo las de la v1 -- MIGRATION_1_2 es solo para
        // quien ya tenía una base en v1 y actualiza la app encima.
        private val SEED_SETTINGS = listOf(
            AppSettingKeys.DEFAULT_MARKUP_BP to "10000",
            AppSettingKeys.PRICE_ROUNDING_STEP_CENTS to "500",
            AppSettingKeys.LOW_STOCK_THRESHOLD to "2",
            AppSettingKeys.STALE_STOCK_DAYS to "90",
            AppSettingKeys.NEXT_PRODUCT_UID_SEQ to "1",
            AppSettingKeys.OWNER_NAME to "",
            AppSettingKeys.MIN_MARGIN_BP to "2500",
        )

        /**
         * Primera migración real del proyecto (D-033, ESQUEMA.md "Versión
         * de base de datos: 2"). Agrega `purchase_id`/`cycle_start` a
         * `price_history` y `min_margin_bp` a `app_setting`.
         *
         * `price_history` se recrea completa (tabla nueva -> copiar filas
         * -> borrar la vieja -> renombrar -> recrear índices) en vez de
         * `ALTER TABLE ADD COLUMN` para la columna con `FOREIGN KEY`
         * (`purchase_id`): SQLite no agrega esa restricción de forma
         * confiable con `ALTER TABLE`, y este patrón (recrear tabla) es el
         * que documenta Room para agregar una columna con FK. Filas
         * existentes quedan con `purchase_id = NULL`, `cycle_start = 0`
         * (ninguna fila vieja pudo venir de una compra: `purchase` no
         * tenía DAO hasta esta misma fase).
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `price_history_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `product_id` INTEGER NOT NULL,
                        `cost_cents` INTEGER NOT NULL,
                        `sale_price_cents` INTEGER NOT NULL,
                        `changed_at` INTEGER NOT NULL,
                        `purchase_id` INTEGER,
                        `cycle_start` INTEGER NOT NULL,
                        FOREIGN KEY(`product_id`) REFERENCES `product`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`purchase_id`) REFERENCES `purchase`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `price_history_new`
                        (`id`, `product_id`, `cost_cents`, `sale_price_cents`, `changed_at`, `purchase_id`, `cycle_start`)
                    SELECT `id`, `product_id`, `cost_cents`, `sale_price_cents`, `changed_at`, NULL, 0
                    FROM `price_history`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `price_history`")
                db.execSQL("ALTER TABLE `price_history_new` RENAME TO `price_history`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_price_history_product_id` ON `price_history` (`product_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_price_history_purchase_id` ON `price_history` (`purchase_id`)")
                db.execSQL(
                    "INSERT INTO `app_setting` (`key`, `value`) VALUES ('${AppSettingKeys.MIN_MARGIN_BP}', '2500')",
                )
            }
        }
    }
}

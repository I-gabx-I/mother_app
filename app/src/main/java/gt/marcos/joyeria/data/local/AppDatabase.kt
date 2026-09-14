package gt.marcos.joyeria.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import gt.marcos.joyeria.data.local.dao.AppSettingDao
import gt.marcos.joyeria.data.local.dao.CategoryDao
import gt.marcos.joyeria.data.local.dao.ProductDao
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
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun appSettingDao(): AppSettingDao

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

        private val SEED_SETTINGS = listOf(
            AppSettingKeys.DEFAULT_MARKUP_BP to "10000",
            AppSettingKeys.PRICE_ROUNDING_STEP_CENTS to "500",
            AppSettingKeys.LOW_STOCK_THRESHOLD to "2",
            AppSettingKeys.STALE_STOCK_DAYS to "90",
            AppSettingKeys.NEXT_PRODUCT_UID_SEQ to "1",
            AppSettingKeys.OWNER_NAME to "",
        )
    }
}

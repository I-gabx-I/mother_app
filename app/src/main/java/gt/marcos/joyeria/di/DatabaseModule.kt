package gt.marcos.joyeria.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import gt.marcos.joyeria.data.local.AppDatabase
import gt.marcos.joyeria.data.local.dao.AppSettingDao
import gt.marcos.joyeria.data.local.dao.CategoryDao
import gt.marcos.joyeria.data.local.dao.PriceHistoryDao
import gt.marcos.joyeria.data.local.dao.ProductDao
import gt.marcos.joyeria.data.local.dao.PurchaseDao
import gt.marcos.joyeria.data.local.dao.PurchaseItemDao
import gt.marcos.joyeria.data.local.dao.SaleDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addCallback(AppDatabase.SeedCallback())
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideProductDao(database: AppDatabase): ProductDao = database.productDao()

    @Provides
    fun provideAppSettingDao(database: AppDatabase): AppSettingDao = database.appSettingDao()

    @Provides
    fun providePriceHistoryDao(database: AppDatabase): PriceHistoryDao = database.priceHistoryDao()

    @Provides
    fun providePurchaseDao(database: AppDatabase): PurchaseDao = database.purchaseDao()

    @Provides
    fun providePurchaseItemDao(database: AppDatabase): PurchaseItemDao = database.purchaseItemDao()

    @Provides
    fun provideSaleDao(database: AppDatabase): SaleDao = database.saleDao()
}

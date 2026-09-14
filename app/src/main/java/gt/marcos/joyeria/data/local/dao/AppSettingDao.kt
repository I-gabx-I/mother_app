package gt.marcos.joyeria.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import gt.marcos.joyeria.data.local.entity.AppSettingEntity

@Dao
interface AppSettingDao {
    @Query("SELECT value FROM app_setting WHERE `key` = :key")
    suspend fun getValue(key: String): String?

    @Query("UPDATE app_setting SET value = :value WHERE `key` = :key")
    suspend fun setValue(key: String, value: String)

    // Todas las filas de una sola vez, no clave por clave: la usa
    // SeedDataTest para comparar la semilla completa contra la lista de
    // ESQUEMA.md en una sola aserción, así una clave de más o de menos
    // (no solo un valor distinto) también hace fallar el test.
    @Query("SELECT * FROM app_setting")
    suspend fun getAll(): List<AppSettingEntity>
}

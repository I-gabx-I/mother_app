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

    // Para una clave que puede no existir todavía (Fase 14: `last_backup_at`, que no se siembra).
    // `setValue` es un UPDATE y no haría nada si la fila no está.
    @Query("INSERT OR REPLACE INTO app_setting (`key`, value) VALUES (:key, :value)")
    suspend fun upsert(key: String, value: String)

    // Para la tarjeta de «hace falta un respaldo» del inicio: se entera sola cuando termina un respaldo.
    @Query("SELECT value FROM app_setting WHERE `key` = :key")
    fun observeValue(key: String): kotlinx.coroutines.flow.Flow<String?>

    // Todas las filas de una sola vez, no clave por clave: la usa
    // SeedDataTest para comparar la semilla completa contra la lista de
    // ESQUEMA.md en una sola aserción, así una clave de más o de menos
    // (no solo un valor distinto) también hace fallar el test.
    @Query("SELECT * FROM app_setting")
    suspend fun getAll(): List<AppSettingEntity>
}

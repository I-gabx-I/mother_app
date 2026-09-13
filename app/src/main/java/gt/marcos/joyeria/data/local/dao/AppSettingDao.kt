package gt.marcos.joyeria.data.local.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface AppSettingDao {
    @Query("SELECT value FROM app_setting WHERE `key` = :key")
    suspend fun getValue(key: String): String?

    @Query("UPDATE app_setting SET value = :value WHERE `key` = :key")
    suspend fun setValue(key: String, value: String)
}

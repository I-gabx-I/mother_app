package gt.marcos.joyeria.data.repository

import gt.marcos.joyeria.data.local.AppSettingKeys
import gt.marcos.joyeria.data.local.dao.AppSettingDao
import gt.marcos.joyeria.data.local.dao.ProductDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Los dos datos que hacen falta para saber si toca avisarle de un respaldo (Fase 14): ver [gt.marcos.joyeria.domain.backup.BackupReminder]. */
data class BackupReminderInputs(val lastBackupAt: Long?, val firstDataAt: Long?)

/**
 * Cuándo fue el último respaldo. La clave `last_backup_at` no se siembra: ausente significa «nunca» (D-061), así que
 * no hizo falta una migración. `ui` habla con este repositorio y nunca con los DAOs.
 */
class BackupStatusRepository @Inject constructor(
    private val appSettingDao: AppSettingDao,
    private val productDao: ProductDao,
) {
    fun observeReminderInputs(): Flow<BackupReminderInputs> =
        combine(
            appSettingDao.observeValue(AppSettingKeys.LAST_BACKUP_AT).map { it?.toLongOrNull() },
            productDao.observeFirstCreatedAt(),
        ) { last, first -> BackupReminderInputs(lastBackupAt = last, firstDataAt = first) }

    suspend fun lastBackupAt(): Long? = appSettingDao.getValue(AppSettingKeys.LAST_BACKUP_AT)?.toLongOrNull()

    suspend fun markBackupDone(at: Long) {
        appSettingDao.upsert(AppSettingKeys.LAST_BACKUP_AT, at.toString())
    }
}

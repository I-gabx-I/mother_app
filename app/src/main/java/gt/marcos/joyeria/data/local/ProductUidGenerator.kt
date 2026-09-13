package gt.marcos.joyeria.data.local

import androidx.room.withTransaction
import gt.marcos.joyeria.data.local.dao.AppSettingDao
import java.util.Locale
import javax.inject.Inject

/**
 * Genera el próximo `uid` de producto (formato `XP-000001`), a partir del
 * contador `next_product_uid_seq` en `app_setting`.
 *
 * La lectura y el incremento del contador van dentro de un solo
 * `db.withTransaction { }`: no son dos llamadas sueltas al DAO. Ver
 * ESTADO.md ("Fase 01 — Diseño de la atomicidad del generador de uid" y su
 * corrección) para el mecanismo exacto por el que esto evita que dos
 * corrutinas concurrentes lean el mismo valor y generen el mismo `uid`.
 */
class ProductUidGenerator @Inject constructor(
    private val db: AppDatabase,
    private val appSettingDao: AppSettingDao,
) {
    suspend fun next(): String = db.withTransaction {
        val raw = appSettingDao.getValue(AppSettingKeys.NEXT_PRODUCT_UID_SEQ)
        val seq = checkNotNull(raw) {
            "app_setting.${AppSettingKeys.NEXT_PRODUCT_UID_SEQ} no existe; " +
                "¿se sembró la base de datos?"
        }.toLong()
        appSettingDao.setValue(AppSettingKeys.NEXT_PRODUCT_UID_SEQ, (seq + 1).toString())
        format(seq)
    }

    companion object {
        // Locale.ROOT: este string se persiste como uid único y se imprime
        // en códigos de barras (regla de CLAUDE.md sección 5).
        fun format(seq: Long): String = String.format(Locale.ROOT, "XP-%06d", seq)
    }
}

package gt.marcos.joyeria.data.repository

import gt.marcos.joyeria.data.local.AppSettingKeys
import gt.marcos.joyeria.data.local.dao.AppSettingDao
import gt.marcos.joyeria.domain.model.Money
import javax.inject.Inject

/**
 * Envuelve `AppSettingDao` para que `ui` nunca importe `data/local`
 * (Room) directo — `ui` habla con repositorios de `data/repository`,
 * nunca con DAOs. Expone solo los dos valores que Fase 03 necesita para
 * el precio sugerido en vivo, ya convertidos a su tipo real (`Int`/`Money`,
 * nunca el `String` crudo de la tabla clave/valor).
 */
class AppSettingRepository @Inject constructor(
    private val appSettingDao: AppSettingDao,
) {
    suspend fun getDefaultMarkupBp(): Int {
        val raw = appSettingDao.getValue(AppSettingKeys.DEFAULT_MARKUP_BP)
        return checkNotNull(raw) {
            "app_setting.${AppSettingKeys.DEFAULT_MARKUP_BP} no existe; ¿se sembró la base de datos?"
        }.toInt()
    }

    suspend fun getPriceRoundingStep(): Money {
        val raw = appSettingDao.getValue(AppSettingKeys.PRICE_ROUNDING_STEP_CENTS)
        val cents = checkNotNull(raw) {
            "app_setting.${AppSettingKeys.PRICE_ROUNDING_STEP_CENTS} no existe; ¿se sembró la base de datos?"
        }.toLong()
        return Money(cents)
    }
}

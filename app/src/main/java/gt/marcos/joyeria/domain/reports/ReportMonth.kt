package gt.marcos.joyeria.domain.reports

import java.util.Calendar
import java.util.TimeZone

/**
 * Un mes calendario (1-12), sin hora ni zona -- la zona la define
 * [ReportDates] al convertirlo a límites reales, no este tipo.
 * `Comparable` por año y mes, para que la pantalla pueda decidir si
 * "mes siguiente" la llevaría a un mes futuro sin tener que repetir la
 * comparación a mano en cada lugar que lo necesite.
 */
data class ReportMonth(val year: Int, val month: Int) : Comparable<ReportMonth> {
    init {
        require(month in 1..12) { "ReportMonth: month ($month) tiene que estar entre 1 y 12" }
    }

    override fun compareTo(other: ReportMonth): Int =
        compareValuesBy(this, other, ReportMonth::year, ReportMonth::month)
}

/**
 * Límites de mes fijados a la zona horaria de Guatemala (Fase 11), nunca a
 * la del dispositivo -- ver `ESTADO.md`, "Fase 11 — Plan", punto 3: si el
 * teléfono cambiara de zona (viaje, configuración accidental), los cortes
 * de mes se moverían solos y un reporte cerrado dejaría de coincidir con
 * lo que ella vio en su momento. Guatemala es `UTC-6` todo el año, sin
 * horario de verano, así que no hay ambigüedad de cambio de hora que
 * resolver acá.
 *
 * `java.util.Calendar`/`TimeZone`, no `java.time` (`ZoneId`, `LocalDate`,
 * API 26+): el proyecto tiene `minSdk = 24` sin `coreLibraryDesugaring`
 * (CLAUDE.md sección 2, `app/build.gradle.kts` verificado) -- usar
 * `java.time` rompería en un teléfono real con Android 7/7.1, justo el
 * tipo de equipo que `minSdk 24` existe para cubrir.
 *
 * Ninguna función de acá lee el reloj: reciben `epochMillis`/`ReportMonth`
 * de quien llama (mismo criterio que D-055) para quedar deterministas y
 * testeables sin depender de la hora real.
 */
object ReportDates {
    val GUATEMALA_ZONE: TimeZone = TimeZone.getTimeZone("America/Guatemala")

    /** A qué mes de calendario (hora de Guatemala) pertenece este instante. */
    fun monthOf(epochMillis: Long, zone: TimeZone = GUATEMALA_ZONE): ReportMonth {
        val calendar = Calendar.getInstance(zone).apply { timeInMillis = epochMillis }
        return ReportMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
    }

    /**
     * `[inicio, fin)` del mes, en milisegundos epoch -- mismo patrón
     * `>=`/`<` que ya usa `SaleDao.observeBetween` (Fase 06, "Ventas de
     * hoy"). Una venta a las 23:59:59.999 del último día del mes cae
     * dentro de `[inicio, fin)`; una a las 00:00:00.000 del día 1
     * siguiente ya no (criterio 5 de `FASES.md`, Fase 11).
     */
    fun bounds(month: ReportMonth, zone: TimeZone = GUATEMALA_ZONE): Pair<Long, Long> {
        val start = Calendar.getInstance(zone).apply {
            clear()
            set(month.year, month.month - 1, 1)
        }
        val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        return start.timeInMillis to end.timeInMillis
    }

    /** Diciembre → enero del año siguiente, sin caso especial para quien llama. */
    fun previous(month: ReportMonth): ReportMonth =
        if (month.month == 1) ReportMonth(month.year - 1, 12) else ReportMonth(month.year, month.month - 1)

    /** Enero → ... del mismo año; diciembre → enero del año siguiente. */
    fun next(month: ReportMonth): ReportMonth =
        if (month.month == 12) ReportMonth(month.year + 1, 1) else ReportMonth(month.year, month.month + 1)
}

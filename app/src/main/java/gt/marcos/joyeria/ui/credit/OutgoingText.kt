package gt.marcos.joyeria.ui.credit

import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.ui.format.formatForOtherPhone
import java.util.Locale

/**
 * Los textos de la Fase 10 que **salen del teléfono de ella hacia el de otra
 * persona** (el recordatorio de WhatsApp y el estado de cuenta compartido).
 *
 * Los montos van con [formatForOtherPhone] (`Locale.ROOT`) y no con
 * `Money.format()`: el texto no se vuelve a formatear al llegar, así que tiene
 * que verse igual sin importar el idioma del teléfono de quien lo manda (D-058).
 * Lo que ella lee en SU pantalla sigue con `Money.format()`.
 *
 * Reciben la plantilla ya sin formatear (`stringResource(id)` sin argumentos
 * devuelve el texto con `%1$s`, `%2$s`...) y la completan acá, con
 * `Locale.ROOT`, para poder probarlo sin Android: nada de esto necesita
 * `Context`.
 */
internal object OutgoingText {

    /** `template` lleva `%1$s` (nombre) y `%2$s` (monto). */
    fun reminderMessage(template: String, customerName: String, balance: Money): String =
        String.format(Locale.ROOT, template, customerName, balance.formatForOtherPhone())

    /** `template` lleva `%1$s` (fecha), `%2$s` (total de la venta) y `%3$s` (saldo). */
    fun statementLine(template: String, date: String, total: Money, balance: Money): String =
        String.format(Locale.ROOT, template, date, total.formatForOtherPhone(), balance.formatForOtherPhone())

    /** `template` lleva `%1$s` (saldo total). */
    fun statementTotal(template: String, totalBalance: Money): String =
        String.format(Locale.ROOT, template, totalBalance.formatForOtherPhone())
}

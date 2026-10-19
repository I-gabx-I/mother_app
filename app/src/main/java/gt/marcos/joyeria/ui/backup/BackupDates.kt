package gt.marcos.joyeria.ui.backup

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** La fecha tal como se escribe en Guatemala (`dd/MM/yyyy`), en la zona horaria del teléfono, igual en cualquier idioma. */
fun backupDate(millis: Long): String = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(Date(millis))

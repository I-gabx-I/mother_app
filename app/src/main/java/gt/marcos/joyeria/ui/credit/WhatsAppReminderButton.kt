package gt.marcos.joyeria.ui.credit

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import gt.marcos.joyeria.R
import gt.marcos.joyeria.util.WhatsAppLink

/**
 * Botón "Recordar por WhatsApp" (Fase 10), versión completa con texto --
 * se usa en el perfil de clienta, donde hay lugar. Para la fila compacta
 * de "¿Quién me debe?" está [WhatsAppReminderIconButton], misma lógica.
 *
 * No aparece si `phone` es `null` o está vacío (FASES.md: "si el cliente
 * no tiene teléfono, el botón no aparece"). Si tiene un valor que
 * [WhatsAppLink.normalizePhone] no puede interpretar, el botón **sí**
 * aparece -- un teléfono con un dato raro no es lo mismo que no tener
 * teléfono (D-050) -- y avisa por `onInvalidPhone` en vez de quedarse
 * sin hacer nada. `message` ya viene armado por quien llama (necesita
 * `Money.format()`, que es de `ui`, CLAUDE.md 3.3).
 */
@Composable
fun WhatsAppReminderButton(
    phone: String?,
    message: String,
    onInvalidPhone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (phone.isNullOrBlank()) return
    val context = LocalContext.current

    // heightIn, no height fijo (CLAUDE.md sección 6, mínimo 56dp): con dos
    // botones a media pantalla el texto puede partirse en dos líneas, y un
    // alto fijo lo recorta en vez de darle lugar.
    OutlinedButton(
        onClick = { launchWhatsAppReminder(phone, message, context, onInvalidPhone) },
        modifier = modifier.heightIn(min = 56.dp),
    ) {
        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
        Text(
            text = stringResource(R.string.whatsapp_reminder_button),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Misma acción que [WhatsAppReminderButton], solo el ícono -- para una fila de lista compacta. */
@Composable
fun WhatsAppReminderIconButton(
    phone: String?,
    message: String,
    onInvalidPhone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (phone.isNullOrBlank()) return
    val context = LocalContext.current

    IconButton(
        onClick = { launchWhatsAppReminder(phone, message, context, onInvalidPhone) },
        modifier = modifier,
    ) {
        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = stringResource(R.string.whatsapp_reminder_button))
    }
}

/**
 * El link `https://wa.me/<numero>?text=<mensaje>` abre WhatsApp con el
 * texto ya precargado en su cuadro de escritura -- WhatsApp es quien
 * exige el toque en su propio botón de enviar, esta app nunca manda nada
 * por su cuenta (ver ESTADO.md, "Fase 10 -- Plan", punto 3).
 */
private fun launchWhatsAppReminder(
    phone: String,
    message: String,
    context: android.content.Context,
    onInvalidPhone: () -> Unit,
) {
    val normalized = WhatsAppLink.normalizePhone(phone)
    if (normalized == null) {
        onInvalidPhone()
        return
    }
    val url = WhatsAppLink.buildReminderUrl(normalized, message)
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

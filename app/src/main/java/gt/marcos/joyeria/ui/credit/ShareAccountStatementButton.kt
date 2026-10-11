package gt.marcos.joyeria.ui.credit

import android.content.Intent
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import gt.marcos.joyeria.R
import gt.marcos.joyeria.data.repository.SaleAccountStatement
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.ui.format.format
import java.text.DateFormat
import java.util.Date

/**
 * "Compartir estado de cuenta" (entregable de Fase 10, FASES.md): texto
 * plano por el selector de Android (`Intent.createChooser`), no
 * específico de WhatsApp -- a diferencia de [WhatsAppReminderButton],
 * que sí abre WhatsApp directo. Mismo criterio de visibilidad que la
 * sección de deuda del perfil: solo tiene sentido si hay algo que
 * compartir.
 */
@Composable
fun ShareAccountStatementButton(
    customerName: String,
    statements: List<SaleAccountStatement>,
    totalBalance: Money,
    modifier: Modifier = Modifier,
) {
    if (statements.isEmpty()) return
    val context = LocalContext.current
    val chooserTitle = stringResource(R.string.account_statement_share_chooser_title)
    val header = stringResource(R.string.account_statement_share_header, customerName)
    val lines = statements.map { statement ->
        stringResource(
            R.string.account_statement_share_line,
            formatDate(statement.soldAt),
            (statement.total - statement.discount).format(),
            statement.balance.format(),
        )
    }
    val totalLine = stringResource(R.string.account_statement_share_total, totalBalance.format())

    OutlinedButton(
        onClick = {
            val text = buildString {
                appendLine(header)
                appendLine()
                lines.forEach { line -> appendLine(line) }
                appendLine()
                append(totalLine)
            }
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
        },
        modifier = modifier.heightIn(min = 56.dp),
    ) {
        Icon(Icons.Default.Share, contentDescription = null)
        Text(
            text = stringResource(R.string.account_statement_share_button),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

private fun formatDate(millis: Long): String =
    DateFormat.getDateInstance(DateFormat.SHORT).format(Date(millis))

package gt.marcos.joyeria.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import gt.marcos.joyeria.R
import gt.marcos.joyeria.data.repository.InventoryReport
import gt.marcos.joyeria.data.repository.LowStockProductSummary
import gt.marcos.joyeria.data.repository.MonthlyReport
import gt.marcos.joyeria.data.repository.StaleProductSummary
import gt.marcos.joyeria.data.repository.TopSellerSummary
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.domain.reports.ReportDates
import gt.marcos.joyeria.domain.reports.ReportMonth
import gt.marcos.joyeria.ui.format.format
import gt.marcos.joyeria.ui.theme.JoyeriaTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

/**
 * Pantalla de reportes (Fase 11). Dos bloques, en el orden decidido en
 * `ESTADO.md` ("Fase 11 -- Plan", punto 4): "Este mes" (ganancia primero,
 * top 5 al final) y "Ahora mismo" (alertas antes que el inventario
 * general). Sin la palabra "capital" ni porcentajes en ningún texto --
 * quetzales y frases directas, pensadas para alguien que nunca leyó un
 * estado financiero. Stateless y `@Preview`-able (CLAUDE.md sección 5).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    state: ReportsUiState,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reports_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        val monthly = state.monthly
        val inventory = state.inventory
        if (monthly == null || inventory == null) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                MonthNavigator(
                    month = monthly.month,
                    canGoNext = state.canGoToNextMonth,
                    onPreviousClick = onPreviousMonthClick,
                    onNextClick = onNextMonthClick,
                )
            }
            item { ProfitCard(monthly) }
            item {
                Text(stringResource(R.string.reports_top_qty_title), style = MaterialTheme.typography.titleMedium)
            }
            item { TopSellerList(monthly.topByQty, byProfit = false) }
            item {
                Text(stringResource(R.string.reports_top_profit_title), style = MaterialTheme.typography.titleMedium)
            }
            item { TopSellerList(monthly.topByProfit, byProfit = true) }

            item { HorizontalDivider() }
            item {
                Text(stringResource(R.string.reports_now_section_title), style = MaterialTheme.typography.titleLarge)
            }
            item { LowStockCard(inventory.lowStockProducts) }
            item { StaleStockCard(inventory) }
            item { InventoryValueCard(inventory) }
        }
    }
}

@Composable
private fun MonthNavigator(month: ReportMonth, canGoNext: Boolean, onPreviousClick: () -> Unit, onNextClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousClick) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.reports_month_previous))
        }
        Text(monthLabel(month), style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = onNextClick, enabled = canGoNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.reports_month_next))
        }
    }
}

@Composable
private fun ProfitCard(monthly: MonthlyReport) {
    val profit = monthly.profit
    val difference = monthly.differenceFromPreviousMonth
    val profitColor = when {
        profit.cents < 0L -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(
                    if (profit.cents < 0L) R.string.reports_profit_negative else R.string.reports_profit_positive,
                    Money(abs(profit.cents)).format(),
                ),
                style = MaterialTheme.typography.headlineSmall,
                color = profitColor,
            )
            val comparisonText = when {
                difference.cents > 0L -> stringResource(
                    R.string.reports_comparison_more,
                    Money(difference.cents).format(),
                    monthLabel(ReportDates.previous(monthly.month)),
                )
                difference.cents < 0L -> stringResource(
                    R.string.reports_comparison_less,
                    Money(abs(difference.cents)).format(),
                    monthLabel(ReportDates.previous(monthly.month)),
                )
                else -> stringResource(R.string.reports_comparison_same, monthLabel(ReportDates.previous(monthly.month)))
            }
            Text(comparisonText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TopSellerList(sellers: List<TopSellerSummary>, byProfit: Boolean) {
    if (sellers.isEmpty()) {
        Text(
            stringResource(R.string.reports_top_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val anyDiscounted = byProfit && sellers.any { it.possiblyInflatedByDiscount }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        sellers.forEachIndexed { index, seller ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${index + 1}. ${seller.productName}", style = MaterialTheme.typography.bodyLarge)
                val valueText = if (byProfit) {
                    seller.profit.format() + if (seller.possiblyInflatedByDiscount) stringResource(R.string.reports_top_discount_marker) else ""
                } else {
                    stringResource(R.string.reports_top_qty_line, seller.qty)
                }
                Text(valueText, style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (anyDiscounted) {
            Text(
                stringResource(R.string.reports_top_discount_caveat),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LowStockCard(lowStock: List<LowStockProductSummary>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.reports_alerts_low_stock_title), style = MaterialTheme.typography.titleMedium)
            if (lowStock.isEmpty()) {
                Text(
                    stringResource(R.string.reports_alerts_low_stock_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                lowStock.forEach { product ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(product.productName, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.reports_alerts_low_stock_line, product.stockQty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StaleStockCard(inventory: InventoryReport) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.reports_alerts_stale_title), style = MaterialTheme.typography.titleMedium)
            if (inventory.staleProducts.isEmpty()) {
                Text(
                    stringResource(R.string.reports_alerts_stale_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                inventory.staleProducts.forEach { product -> StaleProductRow(product) }
                Text(
                    stringResource(R.string.reports_alerts_stale_total, inventory.staleTotal.format()),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun StaleProductRow(product: StaleProductSummary) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(product.productName, style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(R.string.reports_alerts_stale_line, product.daysSinceLastMovement),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InventoryValueCard(inventory: InventoryReport) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.reports_inventory_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.reports_inventory_invested, inventory.capitalInvertido.format()),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(R.string.reports_inventory_recovered, inventory.capitalRecuperado.format()),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(R.string.reports_inventory_recovered_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Explícito, no el locale del dispositivo: toda la app es en español
// (CLAUDE.md sección 5), así que el nombre del mes no puede depender de
// en qué idioma esté configurado el teléfono.
private val SPANISH_GUATEMALA: Locale = Locale.Builder().setLanguage("es").setRegion("GT").build()

/** "Octubre de 2026". */
private fun monthLabel(month: ReportMonth): String {
    val calendar = Calendar.getInstance(ReportDates.GUATEMALA_ZONE).apply {
        clear()
        set(month.year, month.month - 1, 1)
    }
    val formatted = SimpleDateFormat("MMMM 'de' yyyy", SPANISH_GUATEMALA).format(calendar.time)
    return formatted.replaceFirstChar { it.uppercase(SPANISH_GUATEMALA) }
}

@Preview(showBackground = true)
@Composable
private fun ReportsScreenPreview() {
    JoyeriaTheme {
        ReportsScreen(
            state = ReportsUiState(
                currentMonth = ReportMonth(2026, 10),
                monthly = MonthlyReport(
                    month = ReportMonth(2026, 10),
                    profit = Money(245000),
                    previousMonthProfit = Money(198000),
                    topByQty = listOf(
                        TopSellerSummary("XP-000001", "Anillo corazón dorado", 4, Money(16000), false),
                    ),
                    topByProfit = listOf(
                        TopSellerSummary("XP-000001", "Anillo corazón dorado", 4, Money(16000), true),
                    ),
                ),
                inventory = InventoryReport(
                    capitalInvertido = Money(1000000),
                    capitalRecuperado = Money(800000),
                    staleProducts = listOf(StaleProductSummary("XP-000002", "Cadena fina", 94, Money(45000))),
                    lowStockProducts = listOf(LowStockProductSummary("XP-000003", "Aretes luna", 1)),
                ),
            ),
            onPreviousMonthClick = {},
            onNextMonthClick = {},
            onBackClick = {},
        )
    }
}

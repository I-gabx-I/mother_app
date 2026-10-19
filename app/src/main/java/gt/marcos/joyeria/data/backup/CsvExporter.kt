package gt.marcos.joyeria.data.backup

import androidx.sqlite.db.SupportSQLiteDatabase
import gt.marcos.joyeria.data.local.AppDatabase
import gt.marcos.joyeria.domain.backup.CsvFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/** Todo texto que aparece en un CSV viene de `strings.xml` (lo lee ella en Excel): acá no se escribe ninguno. */
class CsvTexts(
    val inventoryHeaders: List<String>,
    val salesHeaders: List<String>,
    val noCategory: String,
    val yes: String,
    val no: String,
    val typeCash: String,
    val typeCredit: String,
    val statusPaid: String,
    val statusPending: String,
    val statusCancelled: String,
)

/**
 * Los dos CSV para Excel (Fase 14, D-061): inventario y ventas. Coma de separador, UTF-8 con BOM, CRLF, montos en
 * quetzales con punto decimal armados con aritmética entera, y fechas `dd/MM/yyyy HH:mm` en hora de Guatemala.
 * Las ventas salen **de los snapshots de la línea** (nombre, código, costo y precio de ese día), nunca de un JOIN al
 * precio actual de la pieza (CLAUDE.md 3.7).
 */
class CsvExporter @Inject constructor(
    private val db: AppDatabase,
) {
    suspend fun exportInventory(texts: CsvTexts, target: File): File = withContext(Dispatchers.IO) {
        write(target) { out ->
            out.append(CsvFormat.line(texts.inventoryHeaders))
            db.openHelper.readableDatabase.query(
                "SELECT p.uid, p.name, c.name, p.stock_qty, p.cost_cents, p.sale_price_cents, p.supplier, p.notes, p.archived " +
                    "FROM product p LEFT JOIN category c ON c.id = p.category_id ORDER BY p.archived, p.uid",
            ).use { c ->
                while (c.moveToNext()) {
                    val cost = c.getLong(4)
                    val price = c.getLong(5)
                    out.append(
                        CsvFormat.line(
                            listOf(
                                CsvFormat.text(c.getString(0)),
                                CsvFormat.text(c.getString(1)),
                                CsvFormat.text(c.getString(2) ?: texts.noCategory),
                                c.getInt(3).toString(),
                                CsvFormat.money(cost),
                                CsvFormat.money(price),
                                CsvFormat.money(price - cost),
                                CsvFormat.text(c.getString(6)),
                                CsvFormat.text(c.getString(7)),
                                if (c.getInt(8) == 1) texts.yes else texts.no,
                            ),
                        ),
                    )
                }
            }
        }
    }

    suspend fun exportSales(texts: CsvTexts, target: File): File = withContext(Dispatchers.IO) {
        write(target) { out ->
            out.append(CsvFormat.line(texts.salesHeaders))
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone(GUATEMALA) }
            var previousSale = -1L
            query(db.openHelper.readableDatabase).use { c ->
                while (c.moveToNext()) {
                    val saleId = c.getLong(0)
                    val firstLineOfSale = saleId != previousSale
                    previousSale = saleId
                    val hasLine = !c.isNull(7)
                    val qty = if (hasLine) c.getInt(9) else 0
                    val price = if (hasLine) c.getLong(10) else 0L
                    val cost = if (hasLine) c.getLong(11) else 0L
                    out.append(
                        CsvFormat.line(
                            listOf(
                                saleId.toString(),
                                format.format(Date(c.getLong(1))),
                                CsvFormat.text(c.getString(2)),
                                if (c.getString(3) == "CREDIT") texts.typeCredit else texts.typeCash,
                                when (c.getString(4)) {
                                    "CANCELLED" -> texts.statusCancelled
                                    "PENDING" -> texts.statusPending
                                    else -> texts.statusPaid
                                },
                                if (hasLine) CsvFormat.text(c.getString(7)) else "",
                                if (hasLine) CsvFormat.text(c.getString(8)) else "",
                                if (hasLine) qty.toString() else "",
                                if (hasLine) CsvFormat.money(price) else "",
                                if (hasLine) CsvFormat.money(cost) else "",
                                if (hasLine) CsvFormat.money(price * qty) else "",
                                if (hasLine) CsvFormat.money((price - cost) * qty) else "",
                                // El descuento (de legado) es de la VENTA, no de cada línea: va solo en la primera, para que sumar la columna no lo repita.
                                if (firstLineOfSale) CsvFormat.money(c.getLong(5)) else "",
                                if (firstLineOfSale) CsvFormat.text(c.getString(6)) else "",
                            ),
                        ),
                    )
                }
            }
        }
    }

    private fun query(sdb: SupportSQLiteDatabase) = sdb.query(
        "SELECT s.id, s.sold_at, cu.name, s.type, s.status, s.discount_cents, s.notes, " +
            "si.product_uid_snapshot, si.product_name_snapshot, si.qty, si.unit_price_cents, si.unit_cost_cents " +
            "FROM sale s LEFT JOIN customer cu ON cu.id = s.customer_id LEFT JOIN sale_item si ON si.sale_id = s.id " +
            "ORDER BY s.sold_at, s.id, si.id",
    )

    private fun write(target: File, body: (Appendable) -> Unit): File {
        target.parentFile?.mkdirs()
        val text = StringBuilder().also(body).toString()
        target.outputStream().use { out ->
            out.write(CsvFormat.BOM)
            out.write(text.toByteArray(Charsets.UTF_8))
        }
        return target
    }

    private companion object {
        const val GUATEMALA = "America/Guatemala"
    }
}

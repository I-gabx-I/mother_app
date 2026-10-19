package gt.marcos.joyeria.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.data.local.entity.CustomerEntity
import gt.marcos.joyeria.data.local.entity.ProductEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class CsvExporterTest {
    private lateinit var world: BackupWorld
    private lateinit var csv: CsvExporter
    private val previousLocale: Locale = Locale.getDefault()

    private val texts = CsvTexts(
        inventoryHeaders = listOf("Código de la pieza", "Nombre", "Categoría", "Cantidad", "Costo (Q)", "Precio de venta (Q)", "Ganancia por pieza (Q)", "Proveedor", "Notas", "Archivada"),
        salesHeaders = listOf("Número de venta", "Fecha", "Clienta", "Tipo de venta", "Estado", "Código de la pieza", "Pieza", "Cantidad", "Precio de venta (Q)", "Costo (Q)", "Total de la línea (Q)", "Ganancia de la línea (Q)", "Descuento de la venta (Q)", "Notas de la venta"),
        noCategory = "Sin categoría", yes = "Sí", no = "No",
        typeCash = "Contado", typeCredit = "A plazos",
        statusPaid = "Pagada", statusPending = "Pendiente", statusCancelled = "Anulada",
    )

    @Before
    fun setUp() {
        world = BackupWorld()
        csv = CsvExporter(world.db)
    }

    @After
    fun tearDown() {
        Locale.setDefault(previousLocale)
        world.close()
    }

    private fun piece(uid: String, name: String, cost: Long = 100, price: Long = 200) =
        ProductEntity(uid = uid, name = name, categoryId = null, costCents = cost, salePriceCents = price, stockQty = 1, photoPath = null, supplier = null, notes = null, createdAt = 1, updatedAt = 1)

    /** Las filas del archivo, sin el BOM, separadas por el fin de línea CRLF. */
    private fun lines(file: File): List<String> {
        val bytes = file.readBytes()
        return String(bytes, 3, bytes.size - 3, Charsets.UTF_8).split("\r\n").filter { it.isNotEmpty() }
    }

    @Test
    fun inventoryCsv_startsWithTheUtf8Bom_soExcelKeepsTheAccents() = runTest {
        world.populate()
        val file = csv.exportInventory(texts, File(world.paths.exportDir, "i.csv"))

        assertThat(file.readBytes().take(3)).containsExactly(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()).inOrder()
        assertThat(lines(file).first()).isEqualTo("Código de la pieza,Nombre,Categoría,Cantidad,Costo (Q),Precio de venta (Q),Ganancia por pieza (Q),Proveedor,Notas,Archivada")
    }

    @Test
    fun inventoryCsv_writesTildesEnyeAndMoneyWithADecimalPoint() = runTest {
        world.populate()
        val rows = lines(csv.exportInventory(texts, File(world.paths.exportDir, "i.csv")))

        assertThat(rows).contains("XP-000001,Anillo ñandú,Niñas,3,40.00,90.00,50.00,,Nota con tildes: canción,No")
    }

    @Test
    fun inventoryCsv_aPieceWithoutCategoryAndWithPriceBelowCost_stillGetsItsRowWithANegativeProfit() = runTest {
        world.populate()
        world.db.productDao().insert(piece("XP-000050", "Suelta", cost = 7590, price = 5000))
        val rows = lines(csv.exportInventory(texts, File(world.paths.exportDir, "i.csv")))

        assertThat(rows).contains("XP-000050,Suelta,Sin categoría,1,75.90,50.00,-25.90,,,No")
    }

    @Test
    fun inventoryCsv_aNameThatStartsWithAnEqualsSign_isNotWrittenAsAFormula() = runTest {
        world.populate()
        world.db.productDao().insert(piece("XP-000051", "=1+1"))
        val rows = lines(csv.exportInventory(texts, File(world.paths.exportDir, "i.csv")))

        assertThat(rows.single { it.startsWith("XP-000051") }).startsWith("XP-000051,'=1+1,")
    }

    @Test
    fun inventoryCsv_aNameWithACommaAndQuotes_staysInOneCell() = runTest {
        world.populate()
        world.db.productDao().insert(piece("XP-000052", "Aretes, \"Luna\""))
        val rows = lines(csv.exportInventory(texts, File(world.paths.exportDir, "i.csv")))

        assertThat(rows.single { it.startsWith("XP-000052") }).startsWith("XP-000052,\"Aretes, \"\"Luna\"\"\",")
    }

    @Test
    fun inventoryCsv_withASpanishSpainLocale_stillUsesADecimalPointAndCommaSeparators() = runTest {
        Locale.setDefault(Locale("es", "ES"))
        world.populate()
        val rows = lines(csv.exportInventory(texts, File(world.paths.exportDir, "i.csv")))

        assertThat(rows).contains("XP-000001,Anillo ñandú,Niñas,3,40.00,90.00,50.00,,Nota con tildes: canción,No")
    }

    @Test
    fun salesCsv_oneRowPerSaleLine_withDatesInGuatemalaTime() = runTest {
        world.populate()
        val rows = lines(csv.exportSales(texts, File(world.paths.exportDir, "v.csv")))

        // 1_700_000_010_000 ms = 2023-11-14 22:13:30 UTC = 16:13 en Guatemala (UTC-6, sin horario de verano).
        assertThat(rows).contains("1,14/11/2023 16:13,,Contado,Pagada,XP-000001,Anillo ñandú,1,90.00,40.00,90.00,50.00,5.00,")
    }

    @Test
    fun salesCsv_theLegacyDiscountAppearsOnlyOnTheFirstLineOfItsSale() = runTest {
        world.populate()
        // La venta de contado (id 1) pasa a tener dos líneas; el descuento de 5.00 no debe repetirse en la segunda.
        world.db.openHelper.writableDatabase.execSQL(
            "INSERT INTO sale_item (sale_id, product_id, product_uid_snapshot, product_name_snapshot, qty, unit_price_cents, unit_cost_cents) VALUES (1, 2, 'XP-000002', 'Aretes CORAZÓN', 1, 6000, 2500)",
        )
        val rows = lines(csv.exportSales(texts, File(world.paths.exportDir, "v.csv"))).filter { it.startsWith("1,") }

        assertThat(rows).hasSize(2)
        assertThat(rows[0]).endsWith(",5.00,")
        assertThat(rows[1]).endsWith(",,")
    }

    @Test
    fun salesCsv_usesTheSnapshotOfTheDayOfTheSale_notTheCurrentPriceOfThePiece() = runTest {
        world.populate()
        world.db.openHelper.writableDatabase.execSQL("UPDATE product SET sale_price_cents = 99999, cost_cents = 88888 WHERE uid = 'XP-000001'")
        val rows = lines(csv.exportSales(texts, File(world.paths.exportDir, "v.csv")))

        assertThat(rows.single { it.startsWith("1,") }).contains(",1,90.00,40.00,90.00,50.00,")
    }

    @Test
    fun salesCsv_aCancelledSaleKeepsItsRowMarkedAsCancelled() = runTest {
        world.populate()
        val rows = lines(csv.exportSales(texts, File(world.paths.exportDir, "v.csv")))

        assertThat(rows.single { it.startsWith("3,") }).contains(",María José Peña,A plazos,Anulada,XP-000003,Pulsera retirada,")
    }

    @Test
    fun salesCsv_aPendingCreditSaleToACustomer_namesTheCustomerAndTheStatus() = runTest {
        world.populate()
        val rows = lines(csv.exportSales(texts, File(world.paths.exportDir, "v.csv")))

        assertThat(rows.single { it.startsWith("2,") }).contains(",María José Peña,A plazos,Pendiente,XP-000002,Aretes CORAZÓN,2,60.00,25.00,120.00,70.00,")
    }

    @Test
    fun salesCsv_whenThereAreNoSales_isJustTheHeaderRow() = runTest {
        world.db.customerDao().insert(CustomerEntity(name = "Sin ventas", phone = null, notes = null, createdAt = 1))
        val rows = lines(csv.exportSales(texts, File(world.paths.exportDir, "v.csv")))

        assertThat(rows).hasSize(1)
    }
}

package gt.marcos.joyeria.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import gt.marcos.joyeria.data.local.AppDatabase
import gt.marcos.joyeria.data.local.DATABASE_SCHEMA_VERSION
import gt.marcos.joyeria.data.local.entity.CategoryEntity
import gt.marcos.joyeria.data.local.entity.CustomerEntity
import gt.marcos.joyeria.data.local.entity.PaymentEntity
import gt.marcos.joyeria.data.local.entity.PriceHistoryEntity
import gt.marcos.joyeria.data.local.entity.ProductEntity
import gt.marcos.joyeria.data.local.entity.PurchaseEntity
import gt.marcos.joyeria.data.local.entity.PurchaseItemEntity
import gt.marcos.joyeria.data.local.entity.SaleEntity
import gt.marcos.joyeria.data.local.entity.SaleItemEntity
import gt.marcos.joyeria.domain.backup.BackupData
import gt.marcos.joyeria.domain.backup.BackupFormat
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Una base Room en memoria, con las rutas de archivos de Robolectric, y todo lo del respaldo armado sobre ella. */
class BackupWorld(openDb: ((Context) -> AppDatabase)? = null) {
    val context: Context = ApplicationProvider.getApplicationContext()
    val db: AppDatabase = openDb?.invoke(context) ?: Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .addCallback(AppDatabase.SeedCallback())
        .build()
    val paths = BackupPaths(context)
    val exporter = BackupExporter(db, paths, context)
    val importer = BackupImporter(db, paths, exporter, context)

    fun close() {
        db.close()
        paths.photosDir.deleteRecursively()
        paths.exportDir.deleteRecursively()
        paths.safetyDir.deleteRecursively()
        paths.importTmpDir.deleteRecursively()
    }

    fun dump(): Map<String, List<Map<String, Any?>>> = SqlSchema.dump(db.openHelper.writableDatabase).tables

    fun schema(): Map<String, List<ColumnInfo>> =
        db.openHelper.writableDatabase.let { sdb -> SqlSchema.userTables(sdb).associateWith { SqlSchema.columns(sdb, it) } }

    fun photoFiles(): Map<String, String> =
        paths.photosDir.listFiles().orEmpty().associate { it.name to sha(it.readBytes()) }

    /** Datos con los casos raros: tildes y ñ, mayúsculas, piezas sin foto, descuento de legado, abonos a medias, archivados. */
    suspend fun populate() {
        paths.photosDir.mkdirs()
        val photoA = File(paths.photosDir, "aaaa-1111.jpg").apply { writeBytes(ByteArray(2048) { (it % 251).toByte() }) }
        val photoB = File(paths.photosDir, "bbbb-2222.jpg").apply { writeBytes(ByteArray(4096) { (it % 97).toByte() }) }
        val ninas = db.categoryDao().insert(CategoryEntity(name = "Niñas"))
        val oldCollection = db.categoryDao().insert(CategoryEntity(name = "Colección ÁÉÍÓÚ", archived = true))

        val ring = db.productDao().insert(product("XP-000001", "Anillo ñandú", ninas, photoA.absolutePath, 4000, 9000, 3))
        val earrings = db.productDao().insert(product("XP-000002", "Aretes CORAZÓN", ninas, null, 2500, 6000, 0))
        val archivedPiece = db.productDao().insert(
            product("XP-000003", "Pulsera retirada", oldCollection, photoB.absolutePath, 1000, 3500, 1, archived = true),
        )
        // Una pieza cuya foto ya no está en el teléfono: la fila conserva el nombre, el archivo no viaja.
        db.productDao().insert(product("XP-000004", "Dije sin archivo", ninas, File(paths.photosDir, "perdida.jpg").absolutePath, 500, 1500, 2))

        val purchase = db.purchaseDao().insert(PurchaseEntity(purchasedAt = 1_700_000_000_000, supplier = "Mayorista Ñ", extraCostCents = 300, notes = null))
        db.purchaseItemDao().insertAll(listOf(PurchaseItemEntity(purchaseId = purchase, productId = ring, qty = 3, unitCostCents = 4000, allocatedExtraCents = 300)))
        db.priceHistoryDao().insert(PriceHistoryEntity(productId = ring, costCents = 4000, salePriceCents = 9000, changedAt = 1_700_000_001_000, purchaseId = purchase, cycleStart = true))

        val maria = db.customerDao().insert(CustomerEntity(name = "María José Peña", phone = "5555-1234", notes = "Paga los viernes", createdAt = 1_700_000_002_000))
        db.customerDao().insert(CustomerEntity(name = "Zoila Niño", phone = null, notes = null, createdAt = 1_700_000_003_000, archived = true))

        // Venta de contado con el descuento de legado (columna que la UI ya no escribe).
        val cash = db.saleDao().insert(sale(1_700_000_010_000, null, "CASH", "PAID", 8500, 4000, discount = 500))
        db.saleItemDao().insertAll(listOf(SaleItemEntity(saleId = cash, productId = ring, productUidSnapshot = "XP-000001", productNameSnapshot = "Anillo ñandú", qty = 1, unitPriceCents = 9000, unitCostCents = 4000)))

        // Crédito con abonos a medias: debe 12,000 - 5,000 - 3,000.
        val credit = db.saleDao().insert(sale(1_700_000_020_000, maria, "CREDIT", "PENDING", 12000, 5000))
        db.saleItemDao().insertAll(
            listOf(
                SaleItemEntity(saleId = credit, productId = earrings, productUidSnapshot = "XP-000002", productNameSnapshot = "Aretes CORAZÓN", qty = 2, unitPriceCents = 6000, unitCostCents = 2500),
            ),
        )
        db.paymentDao().insert(PaymentEntity(saleId = credit, paidAt = 1_700_000_030_000, amountCents = 5000, method = "CASH", notes = null))
        db.paymentDao().insert(PaymentEntity(saleId = credit, paidAt = 1_700_000_040_000, amountCents = 3000, method = "TRANSFER", notes = "Banco Ñ"))

        // La app real deja el contador por encima del mayor código; acá se inserta sin pasar por el generador.
        db.appSettingDao().setValue(BackupFormat.SETTING_NEXT_UID_SEQ, "5")

        // Una venta anulada: sigue en el historial.
        val cancelled = db.saleDao().insert(
            sale(1_700_000_050_000, maria, "CREDIT", "CANCELLED", 3500, 1000).copy(cancelledAt = 1_700_000_060_000, cancelReason = "Se devolvió la pieza"),
        )
        db.saleItemDao().insertAll(listOf(SaleItemEntity(saleId = cancelled, productId = archivedPiece, productUidSnapshot = "XP-000003", productNameSnapshot = "Pulsera retirada", qty = 1, unitPriceCents = 3500, unitCostCents = 1000)))
    }

    private fun product(uid: String, name: String, category: Long?, photo: String?, cost: Long, price: Long, stock: Int, archived: Boolean = false) =
        ProductEntity(
            uid = uid, name = name, categoryId = category, costCents = cost, salePriceCents = price, stockQty = stock,
            photoPath = photo, supplier = null, notes = "Nota con tildes: canción", createdAt = 1_700_000_000_500, updatedAt = 1_700_000_000_900, archived = archived,
        )

    private fun sale(at: Long, customer: Long?, type: String, status: String, total: Long, cost: Long, discount: Long = 0) =
        SaleEntity(soldAt = at, customerId = customer, type = type, status = status, totalCents = total, totalCostCents = cost, discountCents = discount, notes = null, cancelledAt = null, cancelReason = null)

    /** Un respaldo armado a mano, para fabricar archivos rotos o de otra versión con las huellas que se le pidan. */
    fun craft(
        target: File,
        data: Map<String, List<Map<String, Any?>>>,
        photos: Map<String, ByteArray> = emptyMap(),
        schemaVersion: Int = DATABASE_SCHEMA_VERSION,
        formatVersion: Int = BackupFormat.FORMAT_VERSION,
        encryption: String = BackupFormat.ENCRYPTION_NONE,
        exportedAt: Long = 1_800_000_000_000,
        dataShaOverride: String? = null,
        photoShaOverride: String? = null,
        extraEntries: Map<String, ByteArray> = emptyMap(),
    ): File {
        val dataBytes = BackupJson.encode(BackupData(data))
        val manifest = BackupManifest(
            formatVersion = formatVersion,
            schemaVersion = schemaVersion,
            appVersion = "test",
            exportedAt = exportedAt,
            encryption = encryption,
            rowCounts = data.mapValues { it.value.size },
            dataSha256 = dataShaOverride ?: sha(dataBytes),
            photos = photos.map { (name, bytes) -> BackupPhoto(name, photoShaOverride ?: sha(bytes), bytes.size.toLong()) },
            photosMissing = 0,
        )
        target.parentFile?.mkdirs()
        ZipOutputStream(target.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry(BackupFormat.ENTRY_MANIFEST)); zip.write(manifest.toJson()); zip.closeEntry()
            zip.putNextEntry(ZipEntry(BackupFormat.ENTRY_DATA)); zip.write(dataBytes); zip.closeEntry()
            photos.forEach { (name, bytes) -> zip.putNextEntry(ZipEntry(BackupFormat.PHOTO_DIR + name)); zip.write(bytes); zip.closeEntry() }
            extraEntries.forEach { (name, bytes) -> zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry() }
        }
        return target
    }

    companion object {
        fun sha(bytes: ByteArray): String = BackupExporter.hex(MessageDigest.getInstance("SHA-256").digest(bytes))
    }
}

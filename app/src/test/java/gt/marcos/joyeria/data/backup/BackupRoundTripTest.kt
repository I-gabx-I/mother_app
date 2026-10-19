package gt.marcos.joyeria.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import gt.marcos.joyeria.data.local.entity.CategoryEntity
import gt.marcos.joyeria.data.local.entity.ProductEntity
import gt.marcos.joyeria.domain.backup.BackupFormat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

// Robolectric: Room en memoria necesita un Context (D-012). La lógica de formato y versiones se prueba sin Android en
// los tests de domain/backup; acá se prueba que el respaldo pase por una base real.
@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {
    private lateinit var world: BackupWorld

    @Before
    fun setUp() {
        world = BackupWorld()
    }

    @After
    fun tearDown() {
        world.close()
    }

    private suspend fun exportNow(now: Long = 1_800_000_000_000): File =
        world.exporter.export(File(world.paths.exportDir, "r.zip"), now).file

    @Test
    fun roundTrip_everyColumnOfEveryTable_comesBackIdentical() = runTest {
        world.populate()
        val before = world.dump()
        val file = exportNow()

        // Se ensucia la base DESPUÉS del respaldo: si importar no reemplazara, estas diferencias sobrevivirían.
        world.db.productDao().insert(
            ProductEntity(uid = "XP-000099", name = "Posterior", categoryId = null, costCents = 1, salePriceCents = 2, stockQty = 1, photoPath = null, supplier = null, notes = null, createdAt = 1, updatedAt = 1),
        )
        world.db.categoryDao().insert(CategoryEntity(name = "Solo después"))
        world.db.openHelper.writableDatabase.execSQL("DELETE FROM payment")
        assertThat(world.dump()).isNotEqualTo(before)

        val preview = world.importer.prepare(file)
        world.importer.import(preview, now = 1_800_000_100_000)

        val after = world.dump()
        for (table in before.keys - "app_setting") {
            assertWithMessage(table).that(after[table]).isEqualTo(before[table])
        }
        // app_setting: todo igual salvo las dos claves que la importación maneja a propósito.
        fun settings(d: Map<String, List<Map<String, Any?>>>) =
            d.getValue("app_setting").filter { it["key"] != BackupFormat.SETTING_LAST_BACKUP_AT && it["key"] != BackupFormat.SETTING_NEXT_UID_SEQ }
        assertThat(settings(after)).isEqualTo(settings(before))
    }

    @Test
    fun roundTrip_tildesAndEnye_surviveByteForByte() = runTest {
        world.populate()
        val file = exportNow()
        world.importer.import(world.importer.prepare(file), now = 2)

        val names = world.dump().getValue("product").map { it["name"] }
        assertThat(names).containsAtLeast("Anillo ñandú", "Aretes CORAZÓN")
        val customers = world.dump().getValue("customer").map { it["name"] }
        assertThat(customers).containsExactly("María José Peña", "Zoila Niño")
    }

    @Test
    fun roundTrip_aSaleWithLegacyDiscount_keepsTheDiscountAndItsSnapshot() = runTest {
        world.populate()
        val file = exportNow()
        world.importer.import(world.importer.prepare(file), now = 2)

        val sales = world.dump().getValue("sale")
        val legacy = sales.single { it["discount_cents"] == 500L }
        assertThat(legacy["total_cents"]).isEqualTo(8500L)
        val items = world.dump().getValue("sale_item").filter { it["sale_id"] == legacy["id"] }
        assertThat(items.single()["unit_price_cents"]).isEqualTo(9000L)
    }

    @Test
    fun roundTrip_creditSaleWithHalfPaidAbonos_keepsEveryPaymentAndTheirMethod() = runTest {
        world.populate()
        val file = exportNow()
        world.importer.import(world.importer.prepare(file), now = 2)

        val payments = world.dump().getValue("payment")
        assertThat(payments.map { it["amount_cents"] }).containsExactly(5000L, 3000L).inOrder()
        assertThat(payments.map { it["method"] }).containsExactly("CASH", "TRANSFER").inOrder()
    }

    @Test
    fun roundTrip_archivedCategoryWithPieces_keepsTheLinkAndTheArchivedFlag() = runTest {
        world.populate()
        val file = exportNow()
        world.importer.import(world.importer.prepare(file), now = 2)

        val archived = world.dump().getValue("category").single { it["archived"] == 1L }
        val pieces = world.dump().getValue("product").filter { it["category_id"] == archived["id"] }
        assertThat(pieces.map { it["uid"] }).containsExactly("XP-000003")
    }

    @Test
    fun export_aPhotoPath_isWrittenAsJustTheFileName_andImportRewritesItToThisPhonesFolder() = runTest {
        world.populate()
        val file = exportNow()

        java.util.zip.ZipFile(file).use { zip ->
            val data = String(zip.getInputStream(zip.getEntry(BackupFormat.ENTRY_DATA)).readBytes(), Charsets.UTF_8)
            assertThat(data).doesNotContain(world.paths.photosDir.absolutePath)
            assertThat(data).contains("aaaa-1111.jpg")
        }

        world.importer.import(world.importer.prepare(file), now = 2)
        val photoPaths = world.dump().getValue("product").mapNotNull { it["photo_path"] as String? }
        assertThat(photoPaths).contains(File(world.paths.photosDir, "aaaa-1111.jpg").absolutePath)
    }

    @Test
    fun roundTrip_photoFiles_comeBackWithTheSameBytes() = runTest {
        world.populate()
        val photosBefore = world.photoFiles()
        val file = exportNow()
        world.paths.photosDir.listFiles()!!.forEach { it.delete() }

        world.importer.import(world.importer.prepare(file), now = 2)

        // Vuelven las dos que existían; la de la pieza «sin archivo» nunca estuvo en el teléfono.
        assertThat(world.photoFiles()).isEqualTo(photosBefore)
    }

    @Test
    fun import_deletesPhotosThatNoPieceOfTheBackupUses() = runTest {
        world.populate()
        val file = exportNow()
        File(world.paths.photosDir, "huerfana.jpg").writeBytes(byteArrayOf(1, 2, 3))

        world.importer.import(world.importer.prepare(file), now = 2)

        assertThat(world.photoFiles().keys).containsExactly("aaaa-1111.jpg", "bbbb-2222.jpg")
    }

    @Test
    fun import_setsLastBackupAtToWhenTheFileWasMade_notToNow() = runTest {
        world.populate()
        val file = exportNow(now = 1_800_000_000_000)

        world.importer.import(world.importer.prepare(file), now = 1_900_000_000_000)

        val setting = world.dump().getValue("app_setting").single { it["key"] == BackupFormat.SETTING_LAST_BACKUP_AT }
        assertThat(setting["value"]).isEqualTo("1800000000000")
    }

    @Test
    fun export_whenLastBackupAtIsSet_itIsNotWrittenInsideTheFile() = runTest {
        world.populate()
        world.db.appSettingDao().upsert(BackupFormat.SETTING_LAST_BACKUP_AT, "123")
        val file = exportNow()

        java.util.zip.ZipFile(file).use { zip ->
            val data = String(zip.getInputStream(zip.getEntry(BackupFormat.ENTRY_DATA)).readBytes(), Charsets.UTF_8)
            assertThat(data).doesNotContain(BackupFormat.SETTING_LAST_BACKUP_AT)
        }
    }

    @Test
    fun import_uidSequence_isRaisedAboveTheHighestUid_whenTheFilesCounterWasBehind() = runTest {
        world.populate()
        // El contador de la base de origen quedó atrasado respecto de las piezas (XP-000004 es la mayor).
        world.db.appSettingDao().setValue(BackupFormat.SETTING_NEXT_UID_SEQ, "2")
        val file = exportNow()

        world.importer.import(world.importer.prepare(file), now = 2)

        val seq = world.dump().getValue("app_setting").single { it["key"] == BackupFormat.SETTING_NEXT_UID_SEQ }["value"]
        assertThat(seq).isEqualTo("5")
    }

    @Test
    fun prepare_reportsTheBackupAsOlder_whenThePhoneHasSomethingNewer() = runTest {
        world.populate()
        val file = exportNow()
        world.db.productDao().insert(
            ProductEntity(uid = "XP-000099", name = "Posterior", categoryId = null, costCents = 1, salePriceCents = 2, stockQty = 1, photoPath = null, supplier = null, notes = null, createdAt = 1_900_000_000_000, updatedAt = 1_900_000_000_000),
        )

        val preview = world.importer.prepare(file)

        assertThat(preview.backupIsOlder).isTrue()
        assertThat(preview.currentCounts["product"]).isEqualTo(preview.backupCounts["product"]!! + 1)
    }

    @Test
    fun prepare_doesNotReportTheBackupAsOlder_whenNothingChangedSinceTheExport() = runTest {
        world.populate()
        val file = exportNow()

        assertThat(world.importer.prepare(file).backupIsOlder).isFalse()
    }
}

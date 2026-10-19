package gt.marcos.joyeria.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.data.local.DATABASE_SCHEMA_VERSION
import gt.marcos.joyeria.domain.backup.BackupException
import gt.marcos.joyeria.domain.backup.BackupFailure
import gt.marcos.joyeria.domain.backup.BackupFormat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

private typealias Tables = Map<String, List<Map<String, Any?>>>

/**
 * Criterio 3 de la Fase 14: un archivo roto, de otra versión o con datos imposibles falla con una causa clara y
 * **deja la base exactamente como estaba** (las filas de todas las tablas y las fotos), sin copia previa colgada.
 */
@RunWith(AndroidJUnit4::class)
class BackupRejectionTest {
    private lateinit var world: BackupWorld
    private lateinit var tablesBefore: Tables
    private lateinit var photosBefore: Map<String, String>

    @Before
    fun setUp() = runTest {
        world = BackupWorld()
        world.populate()
        tablesBefore = world.dump()
        photosBefore = world.photoFiles()
    }

    @After
    fun tearDown() {
        world.close()
    }

    private val dir get() = File(world.paths.importTmpDir, "armados")

    /** Los datos de la base, como los escribiría el exportador: sin la ruta absoluta de las fotos. */
    private fun validData(edit: (MutableMap<String, List<Map<String, Any?>>>) -> Unit = {}): Tables {
        val copy = world.dump().toMutableMap()
        copy["product"] = copy.getValue("product").map { row ->
            (row["photo_path"] as String?)?.let { row + ("photo_path" to File(it).name) } ?: row
        }
        edit(copy)
        return copy
    }

    private suspend fun assertUntouched() {
        assertThat(world.dump()).isEqualTo(tablesBefore)
        assertThat(world.photoFiles()).isEqualTo(photosBefore)
        assertThat(world.importer.latestSafetyCopy()).isNull()
    }

    private suspend fun failureOf(file: File): BackupFailure {
        val error = runCatching { world.importer.import(world.importer.prepare(file), now = 5) }.exceptionOrNull()
        assertThat(error).isInstanceOf(BackupException::class.java)
        return (error as BackupException).failure
    }

    @Test
    fun aTextFile_isNotABackup_andNothingChanges() = runTest {
        val file = File(dir, "nota.zip").apply { parentFile!!.mkdirs(); writeText("esto no es un zip") }

        assertThat(failureOf(file)).isEqualTo(BackupFailure.NOT_A_BACKUP)
        assertUntouched()
    }

    @Test
    fun aZipWithoutManifest_isNotABackup_andNothingChanges() = runTest {
        val file = File(dir, "sin-manifiesto.zip").apply {
            parentFile!!.mkdirs()
            java.util.zip.ZipOutputStream(outputStream()).use { it.putNextEntry(java.util.zip.ZipEntry("data.json")); it.write("{}".toByteArray()); it.closeEntry() }
        }

        assertThat(failureOf(file)).isEqualTo(BackupFailure.NOT_A_BACKUP)
        assertUntouched()
    }

    @Test
    fun aBackupCutInHalf_isRejected_andNothingChanges() = runTest {
        val good = world.exporter.export(File(world.paths.exportDir, "bueno.zip"), 1).file
        val cut = File(dir, "cortado.zip").apply { parentFile!!.mkdirs(); writeBytes(good.readBytes().copyOf((good.length() / 2).toInt())) }

        assertThat(failureOf(cut)).isEqualTo(BackupFailure.NOT_A_BACKUP)
        assertUntouched()
    }

    @Test
    fun dataThatDoesNotMatchItsFingerprint_isCorrupt_andNothingChanges() = runTest {
        val file = world.craft(File(dir, "huella.zip"), validData(), dataShaOverride = "0".repeat(64))

        assertThat(failureOf(file)).isEqualTo(BackupFailure.CORRUPT)
        assertUntouched()
    }

    @Test
    fun aPhotoThatDoesNotMatchItsFingerprint_isCorrupt_andNothingChanges() = runTest {
        val file = world.craft(File(dir, "foto.zip"), validData(), photos = mapOf("aaaa-1111.jpg" to byteArrayOf(1, 2, 3)), photoShaOverride = "f".repeat(64))

        assertThat(failureOf(file)).isEqualTo(BackupFailure.CORRUPT)
        assertUntouched()
    }

    @Test
    fun aBackupOfANewerSchema_isRejectedBeforeReadingItsData() = runTest {
        // Los datos son basura a propósito: si se rechaza por versión nunca se llegan a interpretar.
        val file = world.craft(File(dir, "nuevo.zip"), mapOf("tabla_futura" to emptyList()), schemaVersion = DATABASE_SCHEMA_VERSION + 1)

        assertThat(failureOf(file)).isEqualTo(BackupFailure.SCHEMA_TOO_NEW)
        assertUntouched()
    }

    @Test
    fun aBackupOfAnOlderSchemaWithNoUpgrader_isRejected_andNothingChanges() = runTest {
        val file = world.craft(File(dir, "viejo.zip"), validData(), schemaVersion = DATABASE_SCHEMA_VERSION - 1)

        assertThat(failureOf(file)).isEqualTo(BackupFailure.SCHEMA_NO_UPGRADER)
        assertUntouched()
    }

    @Test
    fun anUnknownFormatVersion_isRejected() = runTest {
        val file = world.craft(File(dir, "formato.zip"), validData(), formatVersion = BackupFormat.FORMAT_VERSION + 1)

        assertThat(failureOf(file)).isEqualTo(BackupFailure.UNSUPPORTED_FORMAT)
        assertUntouched()
    }

    @Test
    fun anEncryptedBackup_isRejectedBecauseThisVersionCannotOpenIt() = runTest {
        val file = world.craft(File(dir, "cifrado.zip"), validData(), encryption = "aes-256")

        assertThat(failureOf(file)).isEqualTo(BackupFailure.UNSUPPORTED_FORMAT)
        assertUntouched()
    }

    @Test
    fun anEntryThatEscapesTheFolder_isUnsafe() = runTest {
        val file = world.craft(File(dir, "escape.zip"), validData(), extraEntries = mapOf("../../evil.txt" to byteArrayOf(1)))

        assertThat(failureOf(file)).isEqualTo(BackupFailure.UNSAFE_FILE)
        assertUntouched()
    }

    @Test
    fun anAbsoluteEntryName_isUnsafe() = runTest {
        val file = world.craft(File(dir, "absoluta.zip"), validData(), extraEntries = mapOf("/data/evil.txt" to byteArrayOf(1)))

        assertThat(failureOf(file)).isEqualTo(BackupFailure.UNSAFE_FILE)
        assertUntouched()
    }

    @Test
    fun aPhotoNameWithABackslash_isUnsafe() = runTest {
        val file = world.craft(File(dir, "barra.zip"), validData(), extraEntries = mapOf("photos/a\\b.jpg" to byteArrayOf(1)))

        assertThat(failureOf(file)).isEqualTo(BackupFailure.UNSAFE_FILE)
        assertUntouched()
    }

    @Test
    fun aProductPhotoPathThatIsAnAbsolutePath_isUnsafe() = runTest {
        val data = validData { t -> t["product"] = t.getValue("product").map { it + ("photo_path" to "/data/data/otra.app/foto.jpg") } }

        assertThat(failureOf(world.craft(File(dir, "ruta.zip"), data))).isEqualTo(BackupFailure.UNSAFE_FILE)
        assertUntouched()
    }

    @Test
    fun aMoneyColumnHoldingText_isInvalidData() = runTest {
        val data = validData { t -> t["product"] = t.getValue("product").mapIndexed { i, r -> if (i == 0) r + ("cost_cents" to "40.00") else r } }

        assertThat(failureOf(world.craft(File(dir, "tipo.zip"), data))).isEqualTo(BackupFailure.INVALID_DATA)
        assertUntouched()
    }

    @Test
    fun aSaleStatusOutsideTheKnownOnes_isInvalidData() = runTest {
        val data = validData { t -> t["sale"] = t.getValue("sale").mapIndexed { i, r -> if (i == 0) r + ("status" to "REFUNDED") else r } }

        assertThat(failureOf(world.craft(File(dir, "estado.zip"), data))).isEqualTo(BackupFailure.INVALID_DATA)
        assertUntouched()
    }

    @Test
    fun aRowWithAnExtraColumn_isInvalidData() = runTest {
        val data = validData { t -> t["category"] = t.getValue("category").map { it + ("color" to "rojo") } }

        assertThat(failureOf(world.craft(File(dir, "columna.zip"), data))).isEqualTo(BackupFailure.INVALID_DATA)
        assertUntouched()
    }

    @Test
    fun aMissingTable_isInvalidData() = runTest {
        val data = validData { t -> t.remove("payment") }

        assertThat(failureOf(world.craft(File(dir, "tabla.zip"), data))).isEqualTo(BackupFailure.INVALID_DATA)
        assertUntouched()
    }

    @Test
    fun aNullInANotNullColumn_isInvalidData() = runTest {
        val data = validData { t -> t["customer"] = t.getValue("customer").map { it + ("name" to null) } }

        assertThat(failureOf(world.craft(File(dir, "nulo.zip"), data))).isEqualTo(BackupFailure.INVALID_DATA)
        assertUntouched()
    }

    @Test
    fun aPaymentOfASaleThatDoesNotExist_rollsBackTheWholeImport() = runTest {
        val data = validData { t -> t["payment"] = t.getValue("payment") + mapOf("id" to 99L, "sale_id" to 12345L, "paid_at" to 1L, "amount_cents" to 100L, "method" to "CASH", "notes" to null) }

        assertThat(failureOf(world.craft(File(dir, "huerfano.zip"), data))).isEqualTo(BackupFailure.INVALID_DATA)
        assertUntouched()
    }

    @Test
    fun aRepeatedPrimaryKey_rollsBackTheWholeImport() = runTest {
        val data = validData { t -> t["customer"] = t.getValue("customer") + t.getValue("customer").first() }

        assertThat(failureOf(world.craft(File(dir, "repetido.zip"), data))).isEqualTo(BackupFailure.INVALID_DATA)
        assertUntouched()
    }

    @Test
    fun twoActiveCategoriesThatDifferOnlyInCase_rollBackTheWholeImport() = runTest {
        // «Niñas» y «NIÑAS» activas a la vez: lo que CategoryRepository nunca deja crear (D-057).
        val data = validData { t -> t["category"] = t.getValue("category") + mapOf("id" to 500L, "name" to "NIÑAS", "archived" to 0L) }

        assertThat(failureOf(world.craft(File(dir, "categorias.zip"), data))).isEqualTo(BackupFailure.CATEGORY_CONFLICT)
        assertUntouched()
    }

    @Test
    fun twoCategoriesThatDifferOnlyInCase_whenOneIsArchived_importFine() = runTest {
        val data = validData { t -> t["category"] = t.getValue("category") + mapOf("id" to 500L, "name" to "NIÑAS", "archived" to 1L) }

        world.importer.import(world.importer.prepare(world.craft(File(dir, "archivada.zip"), data)), now = 5)

        assertThat(world.dump().getValue("category").map { it["name"] }).containsAtLeast("Niñas", "NIÑAS")
    }

    @Test
    fun streamBounded_aStreamLargerThanTheCap_isRejectedAsTooBig() {
        val error = runCatching { BackupReader.streamBounded(ByteArray(10_000).inputStream(), max = 4_096) { _, _ -> } }.exceptionOrNull()

        assertThat((error as BackupException).failure).isEqualTo(BackupFailure.TOO_BIG)
    }
}

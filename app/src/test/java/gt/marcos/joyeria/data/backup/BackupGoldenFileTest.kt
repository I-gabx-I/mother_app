package gt.marcos.joyeria.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.data.local.AppSettingKeys
import gt.marcos.joyeria.domain.backup.BackupFormat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * `golden-backup-schema2.zip` es un respaldo REAL del esquema 2, generado una vez con el exportador y commiteado.
 * Mientras la app lo siga importando, un respaldo hecho hoy sigue sirviendo mañana (D-061). Si este test falla
 * después de subir la versión del esquema, falta el mejorador de respaldos: no se «arregla» regenerando el archivo.
 */
@RunWith(AndroidJUnit4::class)
class BackupGoldenFileTest {
    private lateinit var world: BackupWorld

    @Before
    fun setUp() {
        world = BackupWorld()
    }

    @After
    fun tearDown() {
        world.close()
    }

    private fun golden(): File {
        val target = File(world.paths.importTmpDir, "golden.zip")
        val stream = checkNotNull(javaClass.classLoader!!.getResourceAsStream("backup/golden-backup-schema2.zip")) { "falta el archivo dorado" }
        stream.use { input -> target.outputStream().use { input.copyTo(it) } }
        return target
    }

    @Test
    fun theGoldenSchema2Backup_stillImportsWithItsPiecesSalesPaymentsAndPhotos() = runTest {
        world.importer.import(world.importer.prepare(golden()), now = 1)

        val tables = world.dump()
        assertThat(tables.getValue("product").map { it["uid"] }).containsExactly("XP-000001", "XP-000002", "XP-000003", "XP-000004").inOrder()
        assertThat(tables.getValue("product").map { it["name"] }).containsAtLeast("Anillo ñandú", "Aretes CORAZÓN")
        assertThat(tables.getValue("sale")).hasSize(3)
        assertThat(tables.getValue("payment").map { it["amount_cents"] }).containsExactly(5000L, 3000L).inOrder()
        assertThat(tables.getValue("sale").single { it["discount_cents"] == 500L }["total_cents"]).isEqualTo(8500L)
        assertThat(world.photoFiles().keys).containsExactly("aaaa-1111.jpg", "bbbb-2222.jpg")
    }

    @Test
    fun theGoldenSchema2Backup_leavesLastBackupAtOnTheDateOfTheFile() = runTest {
        world.importer.import(world.importer.prepare(golden()), now = 1)

        val value = world.dump().getValue("app_setting").single { it["key"] == AppSettingKeys.LAST_BACKUP_AT }["value"]
        assertThat(value).isEqualTo("1800000000000")
    }

    @Test
    fun theSettingKeysTheBackupUsesAreTheSameStringsTheAppUses() {
        assertThat(BackupFormat.SETTING_LAST_BACKUP_AT).isEqualTo(AppSettingKeys.LAST_BACKUP_AT)
        assertThat(BackupFormat.SETTING_NEXT_UID_SEQ).isEqualTo(AppSettingKeys.NEXT_PRODUCT_UID_SEQ)
    }
}

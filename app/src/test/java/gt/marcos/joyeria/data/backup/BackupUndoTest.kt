package gt.marcos.joyeria.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.data.local.entity.CategoryEntity
import gt.marcos.joyeria.domain.backup.BackupException
import gt.marcos.joyeria.domain.backup.BackupFailure
import gt.marcos.joyeria.domain.backup.BackupFormat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

/** «Deshacer» una importación: la copia previa vive en `filesDir` (no en la caché) y devuelve todo, fotos incluidas. */
@RunWith(AndroidJUnit4::class)
class BackupUndoTest {
    private lateinit var world: BackupWorld

    @Before
    fun setUp() {
        world = BackupWorld()
    }

    @After
    fun tearDown() {
        world.close()
    }

    /** Un respaldo de «otro teléfono»: una sola categoría nueva y nada más que la semilla. */
    private suspend fun otherBackup(): File {
        val other = BackupWorld()
        try {
            other.db.categoryDao().insert(CategoryEntity(name = "Del otro teléfono"))
            return other.exporter.export(File(world.paths.exportDir, "otro.zip"), 1_700_000_000_000).file
        } finally {
            other.db.close()
        }
    }

    @Test
    fun theSafetyCopy_isStoredUnderFilesDir_notUnderCache() = runTest {
        world.populate()
        world.importer.import(world.importer.prepare(otherBackup()), now = 100)

        val copy = checkNotNull(world.importer.latestSafetyCopy())
        assertThat(copy.file.absolutePath).startsWith(world.context.filesDir.absolutePath)
        assertThat(copy.file.absolutePath).doesNotContain(world.context.cacheDir.absolutePath)
    }

    @Test
    fun undo_bringsBackEveryRowAndEveryPhotoOfBeforeTheImport() = runTest {
        world.populate()
        val tablesBefore = world.dump()
        val photosBefore = world.photoFiles()
        world.importer.import(world.importer.prepare(otherBackup()), now = 100)
        assertThat(world.dump()).isNotEqualTo(tablesBefore)
        assertThat(world.photoFiles()).isEmpty()

        world.importer.undo()

        assertThat(world.dump()).isEqualTo(tablesBefore)
        assertThat(world.photoFiles()).isEqualTo(photosBefore)
    }

    @Test
    fun undo_putsLastBackupAtBackToWhatItWas_whenThereWasOne() = runTest {
        world.populate()
        world.db.appSettingDao().upsert(BackupFormat.SETTING_LAST_BACKUP_AT, "777")
        world.importer.import(world.importer.prepare(otherBackup()), now = 100)

        world.importer.undo()

        val value = world.dump().getValue("app_setting").single { it["key"] == BackupFormat.SETTING_LAST_BACKUP_AT }["value"]
        assertThat(value).isEqualTo("777")
    }

    @Test
    fun undo_leavesLastBackupAtAbsent_whenThereHadNeverBeenABackup() = runTest {
        world.populate()
        world.importer.import(world.importer.prepare(otherBackup()), now = 100)

        world.importer.undo()

        assertThat(world.dump().getValue("app_setting").map { it["key"] }).doesNotContain(BackupFormat.SETTING_LAST_BACKUP_AT)
    }

    @Test
    fun undo_consumesTheSafetyCopy_soItCannotBeUndoneTwice() = runTest {
        world.populate()
        world.importer.import(world.importer.prepare(otherBackup()), now = 100)
        world.importer.undo()

        val error = runCatching { world.importer.undo() }.exceptionOrNull()

        assertThat((error as BackupException).failure).isEqualTo(BackupFailure.NOTHING_TO_UNDO)
    }

    @Test
    fun undo_withNoImportBefore_saysThereIsNothingToUndo() = runTest {
        val error = runCatching { world.importer.undo() }.exceptionOrNull()

        assertThat((error as BackupException).failure).isEqualTo(BackupFailure.NOTHING_TO_UNDO)
    }

    @Test
    fun aFailedImport_doesNotLeaveASafetyCopyBehind() = runTest {
        world.populate()
        // Pasa toda la validación previa y falla DENTRO de la transacción (dos categorías activas iguales): es el caso
        // en que la copia previa ya se había guardado.
        val data = world.dump().toMutableMap()
        // Como lo escribe el exportador: la ruta de cada foto viaja solo como nombre de archivo.
        data["product"] = data.getValue("product").map { row ->
            (row["photo_path"] as String?)?.let { row + ("photo_path" to File(it).name) } ?: row
        }
        data["category"] = data.getValue("category") + mapOf("id" to 500L, "name" to "NIÑAS", "archived" to 0L)
        val broken = world.craft(File(world.paths.importTmpDir, "conflicto.zip"), data)

        val error = runCatching { world.importer.import(world.importer.prepare(broken), now = 100) }.exceptionOrNull()

        assertThat((error as BackupException).failure).isEqualTo(BackupFailure.CATEGORY_CONFLICT)
        assertThat(world.importer.latestSafetyCopy()).isNull()
    }

    @Test
    fun theLatestSafetyCopy_isTheOneFromTheMostRecentImport() = runTest {
        world.populate()
        world.importer.import(world.importer.prepare(otherBackup()), now = 100)
        world.importer.import(world.importer.prepare(otherBackup()), now = 200)

        assertThat(world.importer.latestSafetyCopy()!!.createdAt).isEqualTo(200)
    }

    @Test
    fun discardSafetyCopies_removesEveryCopy_afterANewBackup() = runTest {
        world.populate()
        world.importer.import(world.importer.prepare(otherBackup()), now = 100)

        world.importer.discardSafetyCopies()

        assertThat(world.importer.latestSafetyCopy()).isNull()
    }

    @Test
    fun discardExpiredSafetyCopies_keepsA29DayOldCopyAndRemovesA31DayOldOne() = runTest {
        world.populate()
        val day = TimeUnit.DAYS.toMillis(1)
        val now = 100 * day
        world.importer.import(world.importer.prepare(otherBackup()), now = now - 31 * day)
        world.importer.import(world.importer.prepare(otherBackup()), now = now - 29 * day)

        world.importer.discardExpiredSafetyCopies(now)

        val names = world.paths.safetyDir.listFiles()!!.map { it.name }
        assertThat(names).containsExactly("pre-import-${now - 29 * day}.zip")
    }
}

package gt.marcos.joyeria.data.backup

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.data.local.AppDatabase
import gt.marcos.joyeria.domain.backup.BackupFormat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Criterio 2 de la Fase 14 contra una base REAL: la del emulador, cargada con los casos raros, guardada en
 * `verification/db/` (D-061). Esa carpeta no se commitea (los datos de una usuaria real tampoco irían al repositorio),
 * así que **si el archivo no está, el test se salta y lo dice**: no pasa en silencio.
 */
@RunWith(AndroidJUnit4::class)
class RealDatabaseRoundTripTest {
    private val realDb = File("../verification/db/emulador-fase14.db")
    private val realPhotos = File("../verification/db/photos")
    private var world: BackupWorld? = null

    @Before
    fun setUp() {
        assumeTrue("falta ${realDb.path}: ver ESTADO.md, Fase 14", realDb.isFile)
        world = BackupWorld { context ->
            val target = context.getDatabasePath("real.db").apply { parentFile!!.mkdirs() }
            realDb.copyTo(target, overwrite = true)
            Room.databaseBuilder(context, AppDatabase::class.java, "real.db").build()
        }
    }

    @After
    fun tearDown() {
        world?.close()
    }

    @Test
    fun theRealDatabase_exportsAndImportsBackIdenticalFieldByField() = runTest {
        val w = checkNotNull(world)
        w.paths.photosDir.mkdirs()
        realPhotos.listFiles()!!.forEach { it.copyTo(File(w.paths.photosDir, it.name), overwrite = true) }
        val before = w.dump()
        // Esta base real tiene piezas, ventas con legado y abonos a medias; que no esté vacía es parte de la prueba.
        assertThat(before.getValue("product").size).isAtLeast(8)
        assertThat(before.getValue("payment").size).isAtLeast(3)
        val file = w.exporter.export(File(w.paths.exportDir, "real.zip"), 1_800_000_000_000).file

        // Se pierde todo y se recupera solo desde el respaldo.
        w.db.openHelper.writableDatabase.apply {
            execSQL("PRAGMA foreign_keys = OFF")
            before.keys.filter { it != "app_setting" }.forEach { execSQL("DELETE FROM `$it`") }
            execSQL("PRAGMA foreign_keys = ON")
        }
        w.paths.photosDir.listFiles()!!.forEach { it.delete() }
        assertThat(w.dump().getValue("product")).isEmpty()

        w.importer.import(w.importer.prepare(file), now = 1_800_000_100_000)

        val after = w.dump()
        for (table in before.keys - "app_setting") {
            // Las fotos de la base del emulador apuntan a la carpeta del emulador; acá se reescriben a la de la prueba.
            fun normalized(rows: List<Map<String, Any?>>) = rows.map { it - "photo_path" }
            assertThat(normalized(after.getValue(table))).isEqualTo(normalized(before.getValue(table)))
        }
        assertThat(after.getValue("product").mapNotNull { (it["photo_path"] as String?)?.let { p -> File(p).name } })
            .containsExactlyElementsIn(before.getValue("product").mapNotNull { (it["photo_path"] as String?)?.let { p -> File(p).name } })
        assertThat(w.photoFiles().keys).containsExactlyElementsIn(realPhotos.listFiles()!!.map { it.name })
        val settings = after.getValue("app_setting").associate { it["key"] to it["value"] }
        assertThat(settings[BackupFormat.SETTING_LAST_BACKUP_AT]).isEqualTo("1800000000000")
    }
}

package gt.marcos.joyeria.data.backup

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import gt.marcos.joyeria.data.local.AppDatabase
import gt.marcos.joyeria.domain.backup.BackupActivity
import gt.marcos.joyeria.domain.backup.BackupException
import gt.marcos.joyeria.domain.backup.BackupFailure
import gt.marcos.joyeria.domain.backup.BackupFormat
import gt.marcos.joyeria.domain.backup.UidSequence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile
import javax.inject.Inject

/** Lo que se le muestra a la usuaria ANTES de confirmar: qué trae el respaldo y qué hay hoy en el teléfono. */
class ImportPreview(
    val backup: ReadBackup,
    val backupCounts: Map<String, Int>,
    val currentCounts: Map<String, Int>,
    /** `true` si en el teléfono hay algo registrado DESPUÉS de lo último que trae el respaldo: importar lo borraría. */
    val backupIsOlder: Boolean,
)

class SafetyCopy(val file: File, val createdAt: Long)

/**
 * Importar un respaldo (Fase 14, D-061): **reemplaza todo, no mezcla**.
 *
 * Orden, y por qué: (1) se lee y valida TODO el archivo sin tocar nada ([BackupReader]); (2) la usuaria ve la vista
 * previa y confirma; (3) se guarda una copia de lo que hoy hay, en `filesDir` y ya validada — si no se puede, no se
 * importa; (4) las fotos se extraen a una carpeta temporal y se verifican; (5) UNA transacción borra todo, inserta
 * el respaldo y comprueba las claves foráneas y las categorías — si algo falla, se deshace entera y la base queda
 * como estaba; (6) recién con la transacción cerrada se mueven las fotos y se limpian las huérfanas.
 */
class BackupImporter @Inject constructor(
    private val db: AppDatabase,
    private val paths: BackupPaths,
    private val exporter: BackupExporter,
    @param:ApplicationContext private val context: Context,
) {

    /** Copia el archivo elegido (de otra app) a una carpeta propia para poder leerlo como zip. */
    suspend fun copyToTemp(uri: Uri, now: Long): File = withContext(Dispatchers.IO) {
        val target = File(paths.importTmpDir, "elegido-$now.zip")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw BackupException(BackupFailure.NOT_A_BACKUP, "no se pudo abrir el archivo elegido")
            FileOutputStream(target).use { out ->
                BackupReader.streamBounded(input, BackupFormat.MAX_TOTAL_BYTES) { buf, n -> out.write(buf, 0, n) }
            }
        } catch (error: BackupException) {
            target.delete()
            throw error
        } catch (error: java.io.IOException) {
            target.delete()
            throw BackupException(BackupFailure.NOT_A_BACKUP, "no se pudo copiar el archivo elegido", error)
        }
        target
    }

    /** Lee y valida el archivo, sin tocar la base. */
    suspend fun prepare(file: File): ImportPreview = withContext(Dispatchers.IO) {
        val sdb = db.openHelper.writableDatabase
        val schema = liveSchema(sdb)
        val backup = BackupReader.read(file, schema)
        val current = counts(sdb, schema.keys)
        val currentLatest = latestActivity(sdb, schema)
        val backupLatest = BackupActivity.latest(backup.data)
        ImportPreview(
            backup = backup,
            backupCounts = backup.data.tables.mapValues { it.value.size },
            currentCounts = current,
            backupIsOlder = currentLatest != null && (backupLatest ?: Long.MIN_VALUE) < currentLatest,
        )
    }

    /**
     * Reemplaza la base por el respaldo. Antes guarda una copia de lo actual para poder deshacer.
     *
     * `NonCancellable`: si ella sale de la pantalla a mitad de camino, la importación NO se corta por la mitad. La
     * transacción ya se deshace sola, pero cortar entre «la base ya cambió» y «las fotos ya están en su lugar»
     * dejaría piezas sin foto. Una vez que empieza, termina (o falla entera).
     */
    suspend fun import(preview: ImportPreview, now: Long) = withContext(Dispatchers.IO + NonCancellable) {
        val safety = File(paths.safetyDir, "${BackupPaths.SAFETY_PREFIX}$now.zip")
        makeSafetyCopy(safety, now)
        try {
            restore(preview.backup, restoringSafetyCopy = false)
        } catch (error: Throwable) {
            // No se importó nada: la copia previa no tiene objeto y no debe ofrecerse como «Deshacer».
            safety.delete()
            throw error
        }
        preview.backup.file.delete()
    }

    /** La copia previa más reciente, si existe y todavía no venció. */
    fun latestSafetyCopy(): SafetyCopy? =
        paths.safetyDir.listFiles { f -> f.isFile && f.name.startsWith(BackupPaths.SAFETY_PREFIX) && f.name.endsWith(".zip") }
            ?.mapNotNull { f ->
                f.name.removePrefix(BackupPaths.SAFETY_PREFIX).removeSuffix(".zip").toLongOrNull()?.let { SafetyCopy(f, it) }
            }
            ?.maxByOrNull { it.createdAt }

    /** Vuelve a lo que había justo antes de la última importación. No se corta a la mitad, igual que [import]. */
    suspend fun undo() = withContext(Dispatchers.IO + NonCancellable) {
        val copy = latestSafetyCopy() ?: throw BackupException(BackupFailure.NOTHING_TO_UNDO)
        val schema = liveSchema(db.openHelper.writableDatabase)
        val backup = BackupReader.read(copy.file, schema)
        restore(backup, restoringSafetyCopy = true)
        copy.file.delete()
    }

    /** Las copias previas dejan de hacer falta cuando se hace un respaldo nuevo, o a los 30 días. */
    fun discardSafetyCopies(olderThan: Long? = null) {
        paths.safetyDir.listFiles()?.forEach { f ->
            val stamp = f.name.removePrefix(BackupPaths.SAFETY_PREFIX).removeSuffix(".zip").toLongOrNull()
            if (olderThan == null || stamp == null || stamp < olderThan) f.delete()
        }
    }

    fun discardExpiredSafetyCopies(now: Long) = discardSafetyCopies(olderThan = now - TimeUnit.DAYS.toMillis(SAFETY_DAYS))

    fun discardTemp(file: File) {
        file.delete()
    }

    private suspend fun makeSafetyCopy(target: File, now: Long) {
        try {
            exporter.export(target, now, keepLocalSettings = true)
            BackupReader.read(target, liveSchema(db.openHelper.writableDatabase))
        } catch (error: Exception) {
            target.delete()
            throw BackupException(BackupFailure.SAFETY_COPY_FAILED, "no se pudo guardar la copia previa", error)
        }
    }

    private suspend fun restore(backup: ReadBackup, restoringSafetyCopy: Boolean) {
        val staged = stagePhotos(backup)
        try {
            db.withTransaction { replaceAll(db.openHelper.writableDatabase, backup, restoringSafetyCopy) }
        } catch (error: android.database.SQLException) {
            // Una clave repetida, un NOT NULL o una foránea rota: el archivo trae datos imposibles. Ya se deshizo todo.
            staged.dir.deleteRecursively()
            throw BackupException(BackupFailure.INVALID_DATA, "la base rechazó los datos del respaldo", error)
        } catch (error: Throwable) {
            staged.dir.deleteRecursively()
            throw error
        }
        // Con la base ya confirmada: las fotos al lugar definitivo y fuera las que ninguna pieza usa.
        staged.names.forEach { name ->
            val target = File(paths.photosDir, name)
            val source = File(staged.dir, name)
            if (target.exists()) target.delete()
            if (!source.renameTo(target)) source.copyTo(target, overwrite = true)
        }
        staged.dir.deleteRecursively()
        deleteOrphanPhotos(backup)
    }

    private class Staged(val dir: File, val names: List<String>)

    /** Extrae las fotos a una carpeta temporal verificando la huella de cada una; todavía no toca `photos/`. */
    private fun stagePhotos(backup: ReadBackup): Staged {
        val dir = File(paths.importTmpDir, "fotos-${System.nanoTime()}").apply { mkdirs() }
        try {
            ZipFile(backup.file).use { zip ->
                for (photo in backup.manifest.photos) {
                    val entry = zip.getEntry(BackupFormat.PHOTO_DIR + photo.name)
                        ?: throw BackupException(BackupFailure.CORRUPT, "falta la foto ${photo.name}")
                    val digest = MessageDigest.getInstance("SHA-256")
                    FileOutputStream(File(dir, photo.name)).use { out ->
                        BackupReader.streamBounded(zip.getInputStream(entry), BackupFormat.MAX_PHOTO_BYTES) { buf, n ->
                            digest.update(buf, 0, n)
                            out.write(buf, 0, n)
                        }
                    }
                    if (BackupExporter.hex(digest.digest()) != photo.sha256) {
                        throw BackupException(BackupFailure.CORRUPT, "la foto ${photo.name} está dañada")
                    }
                }
            }
        } catch (error: Throwable) {
            dir.deleteRecursively()
            throw error
        }
        return Staged(dir, backup.manifest.photos.map { it.name })
    }

    /** Dentro de la transacción: todo o nada. */
    private suspend fun replaceAll(sdb: SupportSQLiteDatabase, backup: ReadBackup, restoringSafetyCopy: Boolean) {
        val schema = liveSchema(sdb)
        sdb.execSQL("PRAGMA defer_foreign_keys = ON")
        schema.keys.forEach { sdb.execSQL("DELETE FROM `$it`") }
        // Que los ids vuelvan a contarse desde lo que se inserta, igual que en la base de origen.
        if (tableExists(sdb, "sqlite_sequence")) sdb.execSQL("DELETE FROM sqlite_sequence")

        for ((table, columns) in schema) {
            for (row in backup.data.rows(table)) {
                val values = ContentValues()
                for (column in columns) {
                    val value = row[column.name]
                    when {
                        value == null -> values.putNull(column.name)
                        (table to column.name) in BackupFormat.PHOTO_COLUMNS ->
                            values.put(column.name, File(paths.photosDir, value as String).absolutePath)
                        value is Long -> values.put(column.name, value)
                        else -> values.put(column.name, value as String)
                    }
                }
                if (sdb.insert(table, SQLiteDatabase.CONFLICT_ABORT, values) == -1L) {
                    throw BackupException(BackupFailure.INVALID_DATA, "no se pudo insertar una fila de $table")
                }
            }
        }

        val fileSeq = backup.data.rows("app_setting")
            .firstOrNull { it["key"] == BackupFormat.SETTING_NEXT_UID_SEQ }?.get("value")?.let { (it as String).toLongOrNull() }
        val uids = backup.data.rows("product").mapNotNull { it["uid"] as? String }
        putSetting(sdb, BackupFormat.SETTING_NEXT_UID_SEQ, UidSequence.nextAfterImport(fileSeq, uids).toString())

        // `last_backup_at`: un respaldo normal deja «hecho al momento del archivo»; la copia previa devuelve lo que había.
        if (!restoringSafetyCopy) {
            sdb.execSQL("DELETE FROM app_setting WHERE `key` = ?", arrayOf(BackupFormat.SETTING_LAST_BACKUP_AT))
            putSetting(sdb, BackupFormat.SETTING_LAST_BACKUP_AT, backup.manifest.exportedAt.toString())
        }

        sdb.query("PRAGMA foreign_key_check").use { cursor ->
            if (cursor.moveToFirst()) {
                throw BackupException(BackupFailure.INVALID_DATA, "una fila apunta a otra que no existe (${cursor.getString(0)})")
            }
        }
        checkCategories(sdb)
    }

    /** La regla de categorías (D-057) la sigue definiendo `CategoryDao`: acá se pregunta lo mismo, con la misma consulta. */
    private suspend fun checkCategories(sdb: SupportSQLiteDatabase) {
        val active = sdb.query("SELECT id, name FROM category WHERE archived = 0").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getLong(0) to cursor.getString(1)) }
        }
        for ((id, name) in active) {
            if (db.categoryDao().countActiveWithName(name, excludeId = id) > 0) {
                throw BackupException(BackupFailure.CATEGORY_CONFLICT, "dos categorías activas se llaman «$name»")
            }
        }
    }

    private fun putSetting(sdb: SupportSQLiteDatabase, key: String, value: String) {
        // Primero se intenta actualizar la fila que ya entró del archivo: así conserva su lugar y la base queda igual
        // que la de origen. Solo si no estaba se inserta.
        val values = ContentValues().apply { put("value", value) }
        if (sdb.update("app_setting", SQLiteDatabase.CONFLICT_NONE, values, "`key` = ?", arrayOf(key)) == 0) {
            sdb.execSQL("INSERT INTO app_setting (`key`, value) VALUES (?, ?)", arrayOf(key, value))
        }
    }

    private fun deleteOrphanPhotos(backup: ReadBackup) {
        val used = backup.data.rows("product").mapNotNull { it["photo_path"] as? String }.toSet()
        paths.photosDir.listFiles()?.forEach { if (it.name !in used) it.delete() }
    }

    private fun liveSchema(sdb: SupportSQLiteDatabase): Map<String, List<ColumnInfo>> =
        SqlSchema.userTables(sdb).associateWith { SqlSchema.columns(sdb, it) }

    private fun tableExists(sdb: SupportSQLiteDatabase, name: String): Boolean =
        sdb.query("SELECT 1 FROM sqlite_master WHERE name = ?", arrayOf(name)).use { it.moveToFirst() }

    private fun counts(sdb: SupportSQLiteDatabase, tables: Collection<String>): Map<String, Int> =
        tables.associateWith { table -> sdb.query("SELECT COUNT(*) FROM `$table`").use { it.moveToFirst(); it.getInt(0) } }

    private fun latestActivity(sdb: SupportSQLiteDatabase, schema: Map<String, List<ColumnInfo>>): Long? =
        schema.flatMap { (table, columns) -> columns.filter { it.name.endsWith("_at") }.map { table to it.name } }
            .mapNotNull { (table, column) ->
                sdb.query("SELECT MAX(`$column`) FROM `$table`").use { if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else null }
            }
            .maxOrNull()

    private companion object {
        const val SAFETY_DAYS = 30L
    }
}

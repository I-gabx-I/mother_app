package gt.marcos.joyeria.data.backup

import android.content.Context
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import gt.marcos.joyeria.data.local.AppDatabase
import gt.marcos.joyeria.data.local.DATABASE_SCHEMA_VERSION
import gt.marcos.joyeria.domain.backup.BackupData
import gt.marcos.joyeria.domain.backup.BackupFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

/**
 * Arma el respaldo (Fase 14, D-061): un `.zip` con `manifest.json`, `data.json` y `photos/`.
 *
 * - La lectura de TODAS las tablas va en una sola transacción: un respaldo nunca ve la base a medias.
 * - **La ruta absoluta de cada foto no viaja**: `product.photo_path` se reescribe al nombre del archivo; la
 *   importación la reescribe a la carpeta del teléfono donde se importe.
 * - `last_backup_at` no viaja dentro del archivo (un respaldo no puede saber cuándo terminó de guardarse).
 */
class BackupExporter @Inject constructor(
    private val db: AppDatabase,
    private val paths: BackupPaths,
    @param:ApplicationContext private val context: Context,
) {
    data class Result(val file: File, val manifest: BackupManifest)

    /**
     * @param keepLocalSettings `true` solo para la copia previa a una importación: conserva `last_backup_at` para que
     * «Deshacer» devuelva el recordatorio al estado que tenía (un respaldo normal nunca lo lleva).
     */
    suspend fun export(target: File, now: Long, keepLocalSettings: Boolean = false): Result = withContext(Dispatchers.IO) {
        val snapshot = db.withTransaction { SqlSchema.dump(db.openHelper.writableDatabase) }
        val (data, photoFiles, missing) = relativizePhotos(if (keepLocalSettings) snapshot else withoutLocalSettings(snapshot))
        val dataBytes = BackupJson.encode(data)

        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, target.name + ".tmp")
        val photos = ArrayList<BackupPhoto>()
        val manifest: BackupManifest
        ZipOutputStream(FileOutputStream(temp)).use { zip ->
            zip.putNextEntry(ZipEntry(BackupFormat.ENTRY_DATA))
            zip.write(dataBytes)
            zip.closeEntry()

            // Un JPEG ya está comprimido: se guarda sin comprimir de nuevo (más rápido, mismo tamaño).
            zip.setLevel(Deflater.NO_COMPRESSION)
            photoFiles.forEach { (name, file) ->
                zip.putNextEntry(ZipEntry(BackupFormat.PHOTO_DIR + name))
                val digest = MessageDigest.getInstance("SHA-256")
                var bytes = 0L
                DigestInputStream(FileInputStream(file), digest).use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        zip.write(buffer, 0, n)
                        bytes += n
                    }
                }
                zip.closeEntry()
                photos.add(BackupPhoto(name, hex(digest.digest()), bytes))
            }
            zip.setLevel(Deflater.DEFAULT_COMPRESSION)

            manifest = BackupManifest(
                formatVersion = BackupFormat.FORMAT_VERSION,
                schemaVersion = DATABASE_SCHEMA_VERSION,
                appVersion = appVersion(),
                exportedAt = now,
                encryption = BackupFormat.ENCRYPTION_NONE,
                rowCounts = data.tables.mapValues { it.value.size },
                dataSha256 = hex(MessageDigest.getInstance("SHA-256").digest(dataBytes)),
                photos = photos,
                photosMissing = missing,
            )
            zip.putNextEntry(ZipEntry(BackupFormat.ENTRY_MANIFEST))
            zip.write(manifest.toJson())
            zip.closeEntry()
        }
        check(temp.renameTo(target) || (target.delete() && temp.renameTo(target))) { "No se pudo guardar el respaldo" }
        Result(target, manifest)
    }

    private fun withoutLocalSettings(data: BackupData): BackupData =
        BackupData(
            data.tables.mapValues { (table, rows) ->
                if (table == "app_setting") rows.filter { it["key"] != BackupFormat.SETTING_LAST_BACKUP_AT } else rows
            },
        )

    private data class Relativized(val data: BackupData, val photoFiles: Map<String, File>, val missing: Int)

    private fun relativizePhotos(data: BackupData): Relativized {
        val photoFiles = LinkedHashMap<String, File>()
        var missing = 0
        val tables = data.tables.mapValues { (table, rows) ->
            val photoColumns = BackupFormat.PHOTO_COLUMNS.filter { it.first == table }.map { it.second }
            if (photoColumns.isEmpty()) {
                rows
            } else {
                rows.map { row ->
                    val copy = LinkedHashMap(row)
                    for (column in photoColumns) {
                        val path = row[column] as? String ?: continue
                        val name = File(path).name
                        copy[column] = name
                        val original = File(path)
                        val file = if (original.isFile) original else File(paths.photosDir, name)
                        if (file.isFile) photoFiles.putIfAbsent(name, file) else missing++
                    }
                    copy
                }
            }
        }
        return Relativized(BackupData(tables), photoFiles, missing)
    }

    private fun appVersion(): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()

    companion object {
        // Sin String.format: es un valor que se guarda en el archivo (CLAUDE.md §5, Locale.ROOT).
        fun hex(bytes: ByteArray): String = bytes.joinToString("") { Integer.toHexString((it.toInt() and 0xff) or 0x100).substring(1) }
    }
}

package gt.marcos.joyeria.data.backup

import gt.marcos.joyeria.data.local.DATABASE_SCHEMA_VERSION
import gt.marcos.joyeria.domain.backup.BackupData
import gt.marcos.joyeria.domain.backup.BackupException
import gt.marcos.joyeria.domain.backup.BackupFailure
import gt.marcos.joyeria.domain.backup.BackupFormat
import gt.marcos.joyeria.domain.backup.BackupUpgraders
import gt.marcos.joyeria.domain.backup.UpgradePlan
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

/** Un respaldo ya leído y VALIDADO por completo; `data` ya está en el esquema de esta versión de la app. */
class ReadBackup(
    val file: File,
    val manifest: BackupManifest,
    val data: BackupData,
    /** La versión de esquema que traía el archivo si hubo que mejorarlo, o `null` si ya era la actual. */
    val upgradedFrom: Int?,
)

/**
 * Lee y valida un respaldo ANTES de que nada toque la base (Fase 14, D-061). Si algo está mal, lanza
 * [BackupException] con la causa y no deja nada a medias: acá no se escribe ni un byte fuera de la lectura.
 *
 * Orden: zip → entradas seguras y con tope → manifiesto → versión de formato → versión de esquema (antes de leer
 * los datos: un archivo más nuevo se rechaza sin interpretarlo) → huella de los datos → huella de cada foto →
 * forma de las filas contra el esquema VIVO de la base.
 */
object BackupReader {
    private const val MAX_MANIFEST_BYTES = 1L * 1024 * 1024

    fun read(
        file: File,
        liveSchema: Map<String, List<ColumnInfo>>,
        currentSchemaVersion: Int = DATABASE_SCHEMA_VERSION,
        upgraders: Map<Int, (BackupData) -> BackupData> = BackupUpgraders.all,
    ): ReadBackup {
        val zip = try {
            ZipFile(file)
        } catch (error: ZipException) {
            throw BackupException(BackupFailure.NOT_A_BACKUP, "no es un zip", error)
        } catch (error: IOException) {
            throw BackupException(BackupFailure.NOT_A_BACKUP, "no se pudo abrir", error)
        }
        zip.use { return readZip(file, it, liveSchema, currentSchemaVersion, upgraders) }
    }

    private fun readZip(
        file: File,
        zip: ZipFile,
        liveSchema: Map<String, List<ColumnInfo>>,
        currentSchemaVersion: Int,
        upgraders: Map<Int, (BackupData) -> BackupData>,
    ): ReadBackup {
        val entries = checkEntries(zip)
        val manifestEntry = entries[BackupFormat.ENTRY_MANIFEST]
            ?: throw BackupException(BackupFailure.NOT_A_BACKUP, "falta ${BackupFormat.ENTRY_MANIFEST}")
        val manifest = BackupManifest.fromJson(readBounded(zip, manifestEntry, MAX_MANIFEST_BYTES))

        if (manifest.formatVersion != BackupFormat.FORMAT_VERSION || manifest.encryption != BackupFormat.ENCRYPTION_NONE) {
            throw BackupException(BackupFailure.UNSUPPORTED_FORMAT, "formato ${manifest.formatVersion}, cifrado ${manifest.encryption}")
        }
        val plan = BackupUpgraders.plan(manifest.schemaVersion, currentSchemaVersion, upgraders)
        when (plan) {
            is UpgradePlan.TooNew -> throw BackupException(BackupFailure.SCHEMA_TOO_NEW, "esquema ${plan.backup}, esta app entiende hasta ${plan.current}")
            is UpgradePlan.MissingUpgrader -> throw BackupException(BackupFailure.SCHEMA_NO_UPGRADER, "falta mejorar desde el esquema ${plan.version}")
            else -> Unit
        }

        val dataEntry = entries[BackupFormat.ENTRY_DATA]
            ?: throw BackupException(BackupFailure.NOT_A_BACKUP, "falta ${BackupFormat.ENTRY_DATA}")
        val dataBytes = readBounded(zip, dataEntry, BackupFormat.MAX_DATA_BYTES)
        if (BackupExporter.hex(MessageDigest.getInstance("SHA-256").digest(dataBytes)) != manifest.dataSha256) {
            throw BackupException(BackupFailure.CORRUPT, "la huella de data.json no coincide")
        }
        verifyPhotos(zip, entries, manifest)

        val raw = BackupJson.decode(dataBytes)
        val counts = raw.tables.mapValues { it.value.size }
        if (counts != manifest.rowCounts) {
            throw BackupException(BackupFailure.CORRUPT, "las cantidades de filas no coinciden con el manifiesto")
        }
        val data = if (plan is UpgradePlan.Upgrade) BackupUpgraders.upgrade(raw, plan.from, plan.to, upgraders) else raw
        validateShape(data, liveSchema)
        return ReadBackup(file, manifest, data, (plan as? UpgradePlan.Upgrade)?.from)
    }

    private fun checkEntries(zip: ZipFile): Map<String, ZipEntry> {
        if (zip.size() > BackupFormat.MAX_ENTRIES) throw BackupException(BackupFailure.TOO_BIG, "demasiadas entradas")
        val result = LinkedHashMap<String, ZipEntry>()
        var declared = 0L
        for (entry in zip.entries()) {
            if (entry.isDirectory || !BackupFormat.isSafeEntryName(entry.name)) {
                throw BackupException(BackupFailure.UNSAFE_FILE, "entrada no permitida: ${entry.name}")
            }
            if (result.put(entry.name, entry) != null) throw BackupException(BackupFailure.UNSAFE_FILE, "entrada repetida: ${entry.name}")
            declared += maxOf(entry.size, 0L)
        }
        if (declared > BackupFormat.MAX_TOTAL_BYTES) throw BackupException(BackupFailure.TOO_BIG, "pesa más del tope")
        return result
    }

    private fun verifyPhotos(zip: ZipFile, entries: Map<String, ZipEntry>, manifest: BackupManifest) {
        for (photo in manifest.photos) {
            if (!BackupFormat.isSafePhotoName(photo.name)) throw BackupException(BackupFailure.UNSAFE_FILE, "foto con nombre no permitido")
            val entry = entries[BackupFormat.PHOTO_DIR + photo.name]
                ?: throw BackupException(BackupFailure.CORRUPT, "falta la foto ${photo.name}")
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = streamBounded(zip.getInputStream(entry), BackupFormat.MAX_PHOTO_BYTES) { buf, n -> digest.update(buf, 0, n) }
            if (bytes != photo.bytes || BackupExporter.hex(digest.digest()) != photo.sha256) {
                throw BackupException(BackupFailure.CORRUPT, "la foto ${photo.name} está dañada")
            }
        }
    }

    /** Las tablas y columnas del archivo tienen que ser EXACTAMENTE las de la base viva, con su tipo y sus enums. */
    internal fun validateShape(data: BackupData, liveSchema: Map<String, List<ColumnInfo>>) {
        if (data.tables.keys != liveSchema.keys) {
            val diff = (data.tables.keys - liveSchema.keys) + (liveSchema.keys - data.tables.keys)
            throw BackupException(BackupFailure.INVALID_DATA, "las tablas no son las de esta versión: $diff")
        }
        for ((table, columns) in liveSchema) {
            val names = columns.map { it.name }.toSet()
            for (row in data.rows(table)) {
                if (row.keys != names) throw BackupException(BackupFailure.INVALID_DATA, "$table: las columnas no coinciden")
                for (column in columns) {
                    val value = row[column.name]
                    if (value == null) {
                        if (column.notNull) throw BackupException(BackupFailure.INVALID_DATA, "$table.${column.name} no puede ser nulo")
                        continue
                    }
                    val typeOk = if (column.type == "INTEGER") value is Long else value is String
                    if (!typeOk) throw BackupException(BackupFailure.INVALID_DATA, "$table.${column.name} tiene un tipo equivocado")
                    BackupFormat.ENUM_COLUMNS[table to column.name]?.let { allowed ->
                        if (value !in allowed) throw BackupException(BackupFailure.INVALID_DATA, "$table.${column.name} tiene un valor desconocido")
                    }
                    if ((table to column.name) in BackupFormat.PHOTO_COLUMNS && !BackupFormat.isSafePhotoName(value as String)) {
                        throw BackupException(BackupFailure.UNSAFE_FILE, "$table.${column.name} no es un nombre de foto")
                    }
                }
            }
        }
    }

    private fun readBounded(zip: ZipFile, entry: ZipEntry, max: Long): ByteArray {
        val out = ByteArrayOutputStream()
        streamBounded(zip.getInputStream(entry), max) { buf, n -> out.write(buf, 0, n) }
        return out.toByteArray()
    }

    /** Lee todo el flujo sin pasarse de [max] bytes aunque el zip MIENTA en el tamaño que declara. Devuelve cuántos leyó. */
    internal fun streamBounded(input: InputStream, max: Long, onChunk: (ByteArray, Int) -> Unit): Long {
        var total = 0L
        input.use {
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = it.read(buffer)
                if (n < 0) break
                total += n
                if (total > max) throw BackupException(BackupFailure.TOO_BIG, "una entrada supera el tope")
                onChunk(buffer, n)
            }
        }
        return total
    }
}

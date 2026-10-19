package gt.marcos.joyeria.data.backup

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import gt.marcos.joyeria.domain.backup.BackupData
import gt.marcos.joyeria.domain.backup.BackupException
import gt.marcos.joyeria.domain.backup.BackupFailure
import gt.marcos.joyeria.domain.backup.BackupRow

data class ColumnInfo(val name: String, val type: String, val notNull: Boolean)

/**
 * El esquema VIVO de la base, leído de `sqlite_master`, y el volcado genérico (Fase 14, D-061). Nada acá nombra una
 * tabla ni una columna: cuando una versión futura agregue una, entra al respaldo sola.
 */
object SqlSchema {
    private val SYSTEM_TABLES = setOf("room_master_table", "android_metadata")

    /** Las tablas de la usuaria: todas menos las de sistema de SQLite y de Room. */
    fun userTables(db: SupportSQLiteDatabase): List<String> =
        db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }.filter { it !in SYSTEM_TABLES }.sorted()

    fun columns(db: SupportSQLiteDatabase, table: String): List<ColumnInfo> =
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val name = cursor.getColumnIndexOrThrow("name")
            val type = cursor.getColumnIndexOrThrow("type")
            val notNull = cursor.getColumnIndexOrThrow("notnull")
            buildList { while (cursor.moveToNext()) add(ColumnInfo(cursor.getString(name), cursor.getString(type).uppercase(), cursor.getInt(notNull) == 1)) }
        }

    /** Todas las filas de todas las tablas de usuario. Rechaza un decimal o un blob: la base no los usa (D-001). */
    fun dump(db: SupportSQLiteDatabase): BackupData =
        BackupData(
            userTables(db).associateWith { table ->
                db.query("SELECT * FROM `$table` ORDER BY rowid").use { cursor -> readRows(cursor, table) }
            },
        )

    private fun readRows(cursor: Cursor, table: String): List<BackupRow> {
        val names = cursor.columnNames
        val rows = ArrayList<BackupRow>(cursor.count)
        while (cursor.moveToNext()) {
            val row = LinkedHashMap<String, Any?>(names.size)
            for (i in names.indices) {
                row[names[i]] = when (cursor.getType(i)) {
                    Cursor.FIELD_TYPE_NULL -> null
                    Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i)
                    Cursor.FIELD_TYPE_STRING -> cursor.getString(i)
                    else -> throw BackupException(BackupFailure.INVALID_DATA, "$table.${names[i]} tiene un decimal o un blob")
                }
            }
            rows.add(row)
        }
        return rows
    }
}

package gt.marcos.joyeria.data.backup

import gt.marcos.joyeria.domain.backup.BackupData
import gt.marcos.joyeria.domain.backup.BackupException
import gt.marcos.joyeria.domain.backup.BackupFailure
import gt.marcos.joyeria.domain.backup.BackupRow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** `data.json`: `{ "tables": { "<tabla>": [ { "<columna>": valor, ... }, ... ] } }`. Solo enteros, textos y `null`. */
object BackupJson {

    fun encode(data: BackupData): ByteArray {
        val tables = JSONObject()
        data.tables.forEach { (table, rows) ->
            val array = JSONArray()
            rows.forEach { row ->
                val o = JSONObject()
                row.forEach { (column, value) -> o.put(column, value ?: JSONObject.NULL) }
                array.put(o)
            }
            tables.put(table, array)
        }
        return JSONObject().put("tables", tables).toString().toByteArray(Charsets.UTF_8)
    }

    fun decode(bytes: ByteArray): BackupData = try {
        val tables = JSONObject(String(bytes, Charsets.UTF_8)).getJSONObject("tables")
        BackupData(
            tables.keys().asSequence().associateWith { table ->
                val array = tables.getJSONArray(table)
                (0 until array.length()).map { index -> readRow(table, array.getJSONObject(index)) }
            },
        )
    } catch (error: JSONException) {
        throw BackupException(BackupFailure.INVALID_DATA, "data.json no es un JSON de respaldo válido", error)
    }

    private fun readRow(table: String, o: JSONObject): BackupRow {
        val row = LinkedHashMap<String, Any?>()
        for (column in o.keys()) {
            row[column] = when (val value = o.get(column)) {
                JSONObject.NULL -> null
                is String -> value
                is Int -> value.toLong()
                is Long -> value
                else -> throw BackupException(BackupFailure.INVALID_DATA, "$table.$column tiene un valor que no es entero ni texto")
            }
        }
        return row
    }
}

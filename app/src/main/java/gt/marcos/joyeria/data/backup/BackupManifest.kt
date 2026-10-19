package gt.marcos.joyeria.data.backup

import gt.marcos.joyeria.domain.backup.BackupException
import gt.marcos.joyeria.domain.backup.BackupFailure
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class BackupPhoto(val name: String, val sha256: String, val bytes: Long)

/**
 * `manifest.json`: qué trae el respaldo y cómo verificarlo. `photosMissing` son fotos que una fila nombraba y que ya
 * no estaban en el teléfono (no entran al respaldo; la fila conserva el nombre, igual que antes).
 */
data class BackupManifest(
    val formatVersion: Int,
    val schemaVersion: Int,
    val appVersion: String,
    val exportedAt: Long,
    val encryption: String,
    val rowCounts: Map<String, Int>,
    val dataSha256: String,
    val photos: List<BackupPhoto>,
    val photosMissing: Int,
) {
    fun toJson(): ByteArray {
        val counts = JSONObject().also { o -> rowCounts.forEach { (table, n) -> o.put(table, n) } }
        val photoArray = JSONArray().also { array ->
            photos.forEach { array.put(JSONObject().put("name", it.name).put("sha256", it.sha256).put("bytes", it.bytes)) }
        }
        return JSONObject()
            .put("formatVersion", formatVersion)
            .put("schemaVersion", schemaVersion)
            .put("appVersion", appVersion)
            .put("exportedAt", exportedAt)
            .put("encryption", encryption)
            .put("rowCounts", counts)
            .put("dataSha256", dataSha256)
            .put("photos", photoArray)
            .put("photosMissing", photosMissing)
            .toString(2)
            .toByteArray(Charsets.UTF_8)
    }

    companion object {
        fun fromJson(bytes: ByteArray): BackupManifest = try {
            val o = JSONObject(String(bytes, Charsets.UTF_8))
            val counts = o.getJSONObject("rowCounts")
            val photoArray = o.getJSONArray("photos")
            BackupManifest(
                formatVersion = o.getInt("formatVersion"),
                schemaVersion = o.getInt("schemaVersion"),
                appVersion = o.optString("appVersion", ""),
                exportedAt = o.getLong("exportedAt"),
                encryption = o.getString("encryption"),
                rowCounts = counts.keys().asSequence().associateWith { counts.getInt(it) },
                dataSha256 = o.getString("dataSha256"),
                photos = (0 until photoArray.length()).map {
                    val p = photoArray.getJSONObject(it)
                    BackupPhoto(p.getString("name"), p.getString("sha256"), p.getLong("bytes"))
                },
                photosMissing = o.optInt("photosMissing", 0),
            )
        } catch (error: JSONException) {
            throw BackupException(BackupFailure.NOT_A_BACKUP, "manifest.json ilegible", error)
        }
    }
}

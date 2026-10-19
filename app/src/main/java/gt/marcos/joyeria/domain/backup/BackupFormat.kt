package gt.marcos.joyeria.domain.backup

/** El contenedor del respaldo (Fase 14, D-061): un `.zip` con `manifest.json`, `data.json` y `photos/`. */
object BackupFormat {
    /** La versión del CONTENEDOR (qué hay en el zip y cómo se verifica), distinta de la versión del esquema de Room. */
    const val FORMAT_VERSION = 1
    const val ENTRY_MANIFEST = "manifest.json"
    const val ENTRY_DATA = "data.json"
    const val PHOTO_DIR = "photos/"

    /** Reservado para una contraseña opcional en una versión futura: hoy siempre es `none` (D-061). */
    const val ENCRYPTION_NONE = "none"

    // Topes contra un zip hecho a propósito para agotar el teléfono (zip bomb).
    const val MAX_ENTRIES = 20_000
    const val MAX_DATA_BYTES = 100L * 1024 * 1024
    const val MAX_PHOTO_BYTES = 25L * 1024 * 1024
    const val MAX_TOTAL_BYTES = 4L * 1024 * 1024 * 1024

    /**
     * Las columnas que guardan la ruta ABSOLUTA de una foto en ESTE teléfono. En el archivo viaja solo el nombre y la
     * importación la reescribe a la carpeta del teléfono donde se importa. Un test falla si aparece una columna con
     * «path» en el esquema que no esté acá.
     */
    val PHOTO_COLUMNS: Set<Pair<String, String>> = setOf("product" to "photo_path")

    /** Columnas de texto con valores fijos (ESQUEMA.md): un valor desconocido se rechaza, no se importa. */
    val ENUM_COLUMNS: Map<Pair<String, String>, Set<String>> = mapOf(
        ("sale" to "type") to setOf("CASH", "CREDIT"),
        ("sale" to "status") to setOf("PAID", "PENDING", "CANCELLED"),
        ("payment" to "method") to setOf("CASH", "TRANSFER", "OTHER"),
    )

    const val SETTING_NEXT_UID_SEQ = "next_product_uid_seq"
    const val SETTING_LAST_BACKUP_AT = "last_backup_at"

    private val SAFE_PHOTO_NAME = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")

    /** Un nombre de foto sin rutas: nada de `/`, `\\`, `..` ni nombres que empiecen con punto. */
    fun isSafePhotoName(name: String): Boolean = SAFE_PHOTO_NAME.matches(name) && !name.contains("..")

    /** Solo se aceptan las entradas del formato: el manifiesto, los datos y `photos/<nombre seguro>`. Lo demás (incluido `..`) es inseguro. */
    fun isSafeEntryName(entry: String): Boolean =
        entry == ENTRY_MANIFEST || entry == ENTRY_DATA ||
            (entry.startsWith(PHOTO_DIR) && isSafePhotoName(entry.removePrefix(PHOTO_DIR)))
}

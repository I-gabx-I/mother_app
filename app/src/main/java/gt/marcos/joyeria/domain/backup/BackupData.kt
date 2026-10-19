package gt.marcos.joyeria.domain.backup

/** Una fila de una tabla: nombre de columna -> `Long`, `String` o `null`. Nunca un decimal ni un blob (D-001, D-010). */
typealias BackupRow = Map<String, Any?>

/**
 * El contenido de la base como datos puros: tabla -> filas. Genérico a propósito (no una clase por entidad): cuando
 * una versión futura del esquema agregue una columna o una tabla, entra al respaldo sola. Sin Android, para poder
 * transformarlo con los mejoradores de versión ([BackupUpgraders]) y probarlo en la JVM.
 */
class BackupData(val tables: Map<String, List<BackupRow>>) {
    fun rows(table: String): List<BackupRow> = tables[table].orEmpty()
}

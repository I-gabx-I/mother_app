package gt.marcos.joyeria.domain.backup

/**
 * Cuándo fue lo último que pasó en unos datos: el mayor valor de cualquier columna cuyo nombre termina en `_at`
 * (`created_at`, `updated_at`, `sold_at`, `paid_at`, `changed_at`, ...). Es genérico a propósito: una tabla futura
 * con su `_at` entra sola, sin tocar esta función (D-061).
 *
 * Sirve para avisarle, antes de importar, que el respaldo es **más viejo** que lo que hoy hay en el teléfono:
 * importarlo haría desaparecer lo que se registró después.
 */
object BackupActivity {
    fun latest(data: BackupData): Long? =
        data.tables.values.asSequence()
            .flatMap { it.asSequence() }
            .flatMap { row -> row.entries.asSequence() }
            .filter { (column, value) -> column.endsWith("_at") && value is Long }
            .maxOfOrNull { it.value as Long }
}

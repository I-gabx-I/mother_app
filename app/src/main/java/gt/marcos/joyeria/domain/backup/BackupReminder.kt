package gt.marcos.joyeria.domain.backup

/**
 * Cuándo avisarle que hace falta un respaldo (FASES.md Fase 14: «más de 15 días»). Pura: `now` lo pasa quien llama
 * (D-055, ninguna función de dominio ni de repositorio lee el reloj).
 *
 * `lastBackupAt` es la última vez que armó y entregó un respaldo (o la fecha del respaldo que importó). Si nunca
 * hizo uno, se cuenta desde que cargó su primera pieza (`firstDataAt`): sin datos no hay nada que respaldar.
 */
object BackupReminder {
    const val DAYS = 15
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    data class Due(val days: Int, val neverBackedUp: Boolean)

    /** `null` si todavía no toca avisar. «Más de 15 días» es estrictamente más: a los 15 días justos no avisa. */
    fun due(lastBackupAt: Long?, firstDataAt: Long?, now: Long): Due? {
        val reference = lastBackupAt ?: firstDataAt ?: return null
        val elapsed = now - reference
        if (elapsed <= DAYS * DAY_MILLIS) return null
        return Due(days = (elapsed / DAY_MILLIS).toInt(), neverBackedUp = lastBackupAt == null)
    }
}

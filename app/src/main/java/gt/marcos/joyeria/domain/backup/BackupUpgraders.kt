package gt.marcos.joyeria.domain.backup

/** Qué hay que hacer para leer un respaldo de la versión [backup] del esquema con la app de la versión [current]. */
sealed interface UpgradePlan {
    /** Misma versión: se importa tal cual. */
    data object Current : UpgradePlan

    /** Más vieja, y hay un mejorador para cada paso: se transforma de [from] a [to]. */
    data class Upgrade(val from: Int, val to: Int) : UpgradePlan

    /** Más nueva que la app: se rechaza antes de tocar nada. */
    data class TooNew(val backup: Int, val current: Int) : UpgradePlan

    /** Más vieja y falta el mejorador de la versión [version]: se rechaza, nunca se importa un esquema que no se sabe interpretar. */
    data class MissingUpgrader(val version: Int) : UpgradePlan
}

/**
 * Los mejoradores de respaldos viejos (Fase 14, D-061). El mejorador `n` transforma los datos de la versión `n` del
 * esquema a la `n + 1` (agrega la columna nueva con su valor por defecto, etc.).
 *
 * **Hoy no hay ninguno: el primer respaldo que existe ya es de la versión 2.** El primero se escribe junto con la
 * migración 2 -> 3, y no antes. Dos tests lo fuerzan: `BackupVersionTest.everyPastSchemaVersion_hasAnUpgrader` falla
 * en cuanto `DATABASE_SCHEMA_VERSION` sube sin mejorador, y el respaldo dorado de la versión 2 sigue importándose.
 */
object BackupUpgraders {
    /**
     * La primera versión del esquema con la que existe un respaldo: la 2. La versión 1 de la base existió antes
     * de que hubiera respaldos, así que un archivo «de la versión 1» no puede ser de esta app y se rechaza.
     */
    const val FIRST_EXPORTED_SCHEMA = 2

    val all: Map<Int, (BackupData) -> BackupData> = emptyMap()

    fun plan(backupVersion: Int, currentVersion: Int, upgraders: Map<Int, (BackupData) -> BackupData> = all): UpgradePlan {
        if (backupVersion == currentVersion) return UpgradePlan.Current
        if (backupVersion > currentVersion) return UpgradePlan.TooNew(backupVersion, currentVersion)
        if (backupVersion < 1) return UpgradePlan.MissingUpgrader(backupVersion)
        for (version in backupVersion until currentVersion) {
            if (version !in upgraders) return UpgradePlan.MissingUpgrader(version)
        }
        return UpgradePlan.Upgrade(backupVersion, currentVersion)
    }

    fun upgrade(
        data: BackupData,
        from: Int,
        to: Int,
        upgraders: Map<Int, (BackupData) -> BackupData> = all,
    ): BackupData {
        var current = data
        for (version in from until to) {
            current = checkNotNull(upgraders[version]) { "Falta el mejorador del esquema $version" }(current)
        }
        return current
    }
}

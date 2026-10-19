package gt.marcos.joyeria.domain.backup

import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.data.local.DATABASE_SCHEMA_VERSION
import org.junit.Test

class BackupVersionTest {

    private val noUpgraders = emptyMap<Int, (BackupData) -> BackupData>()
    private val add = { data: BackupData -> BackupData(data.tables + ("v" to emptyList())) }

    @Test
    fun plan_sameVersion_isCurrent() {
        assertThat(BackupUpgraders.plan(2, 2, noUpgraders)).isEqualTo(UpgradePlan.Current)
    }

    @Test
    fun plan_newerBackupThanTheApp_isTooNew() {
        assertThat(BackupUpgraders.plan(3, 2, noUpgraders)).isEqualTo(UpgradePlan.TooNew(backup = 3, current = 2))
    }

    @Test
    fun plan_olderBackupWithoutUpgrader_isMissingThatUpgrader() {
        assertThat(BackupUpgraders.plan(1, 2, noUpgraders)).isEqualTo(UpgradePlan.MissingUpgrader(1))
    }

    @Test
    fun plan_olderBackupWithAnUpgraderForEveryStep_isUpgrade() {
        val upgraders = mapOf(1 to add, 2 to add)
        assertThat(BackupUpgraders.plan(1, 3, upgraders)).isEqualTo(UpgradePlan.Upgrade(from = 1, to = 3))
    }

    @Test
    fun plan_olderBackupMissingTheMiddleStep_reportsTheMissingOne() {
        assertThat(BackupUpgraders.plan(1, 3, mapOf(1 to add))).isEqualTo(UpgradePlan.MissingUpgrader(2))
    }

    @Test
    fun plan_versionZeroOrNegative_isNeverImported() {
        assertThat(BackupUpgraders.plan(0, 2, mapOf(0 to add, 1 to add))).isEqualTo(UpgradePlan.MissingUpgrader(0))
    }

    @Test
    fun upgrade_appliesEachStepInOrder() {
        val first = { d: BackupData -> BackupData(d.tables + ("first" to emptyList())) }
        val second = { d: BackupData -> BackupData(d.tables.filterKeys { it == "first" }.plus("second" to emptyList())) }
        val result = BackupUpgraders.upgrade(BackupData(mapOf("x" to emptyList())), 1, 3, mapOf(1 to first, 2 to second))
        assertThat(result.tables.keys).containsExactly("first", "second")
    }

    // La guarda de D-061: subir la versión del esquema sin escribir el mejorador del respaldo de la versión anterior
    // rompe ESTE test, y el respaldo de hoy deja de poder importarse en silencio si nadie lo nota.
    @Test
    fun everyPastSchemaVersion_hasAnUpgrader() {
        // Desde el primer esquema que tuvo respaldos: con la base en la versión 2 no hace falta ninguno.
        for (version in BackupUpgraders.FIRST_EXPORTED_SCHEMA until DATABASE_SCHEMA_VERSION) {
            assertThat(BackupUpgraders.all.keys).contains(version)
        }
    }

    @Test
    fun plan_aBackupOfSchemaOne_isRejectedBecauseNoBackupOfThatVersionCanExist() {
        assertThat(BackupUpgraders.plan(1, 2)).isEqualTo(UpgradePlan.MissingUpgrader(1))
    }

    @Test
    fun currentSchemaVersion_isTwoUntilTheFirstMigrationAfterPhase05() {
        // Si cambia, hay que escribir el mejorador 2 -> 3 y regenerar el respaldo dorado de la versión 3.
        assertThat(DATABASE_SCHEMA_VERSION).isEqualTo(2)
    }
}

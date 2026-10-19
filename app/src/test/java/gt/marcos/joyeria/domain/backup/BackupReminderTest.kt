package gt.marcos.joyeria.domain.backup

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackupReminderTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_000 * day

    @Test
    fun due_exactlyFifteenDaysAfterTheLastBackup_doesNotRemindYet() {
        assertThat(BackupReminder.due(lastBackupAt = now - 15 * day, firstDataAt = 0, now = now)).isNull()
    }

    @Test
    fun due_oneMillisecondOverFifteenDays_reminds() {
        val due = BackupReminder.due(lastBackupAt = now - 15 * day - 1, firstDataAt = 0, now = now)
        assertThat(due).isEqualTo(BackupReminder.Due(days = 15, neverBackedUp = false))
    }

    @Test
    fun due_twentyThreeDaysAfterTheLastBackup_remindsWithThatCount() {
        assertThat(BackupReminder.due(now - 23 * day, null, now)?.days).isEqualTo(23)
    }

    @Test
    fun due_aRecentBackup_doesNotRemind() {
        assertThat(BackupReminder.due(now - 3 * day, 0, now)).isNull()
    }

    @Test
    fun due_neverBackedUpWithDataOlderThanFifteenDays_remindsAsNeverBackedUp() {
        val due = BackupReminder.due(lastBackupAt = null, firstDataAt = now - 40 * day, now = now)
        assertThat(due).isEqualTo(BackupReminder.Due(days = 40, neverBackedUp = true))
    }

    @Test
    fun due_neverBackedUpWithRecentData_doesNotRemind() {
        assertThat(BackupReminder.due(null, now - 5 * day, now)).isNull()
    }

    @Test
    fun due_neverBackedUpAndNoDataAtAll_doesNotRemind() {
        assertThat(BackupReminder.due(null, null, now)).isNull()
    }
}

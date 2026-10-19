package gt.marcos.joyeria.domain.backup

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackupFormatTest {

    @Test
    fun isSafeEntryName_acceptsTheManifestTheDataAndAPhotoWithASimpleName() {
        assertThat(BackupFormat.isSafeEntryName("manifest.json")).isTrue()
        assertThat(BackupFormat.isSafeEntryName("data.json")).isTrue()
        assertThat(BackupFormat.isSafeEntryName("photos/3f2a9c.jpg")).isTrue()
    }

    @Test
    fun isSafeEntryName_rejectsAPathThatTriesToEscapeTheFolder() {
        assertThat(BackupFormat.isSafeEntryName("photos/../../databases/joyeria.db")).isFalse()
        assertThat(BackupFormat.isSafeEntryName("../manifest.json")).isFalse()
        assertThat(BackupFormat.isSafeEntryName("photos/..jpg")).isFalse()
    }

    @Test
    fun isSafeEntryName_rejectsAbsoluteAndBackslashPaths() {
        assertThat(BackupFormat.isSafeEntryName("/data/data/gt.marcos.joyeria/x.jpg")).isFalse()
        assertThat(BackupFormat.isSafeEntryName("photos\\x.jpg")).isFalse()
        assertThat(BackupFormat.isSafeEntryName("photos/sub/x.jpg")).isFalse()
    }

    @Test
    fun isSafeEntryName_rejectsAnEntryTheFormatDoesNotDefine() {
        assertThat(BackupFormat.isSafeEntryName("extra.txt")).isFalse()
        assertThat(BackupFormat.isSafeEntryName("photos/")).isFalse()
        assertThat(BackupFormat.isSafeEntryName("")).isFalse()
    }

    @Test
    fun isSafePhotoName_rejectsANameStartingWithADot() {
        assertThat(BackupFormat.isSafePhotoName(".hidden.jpg")).isFalse()
    }
}

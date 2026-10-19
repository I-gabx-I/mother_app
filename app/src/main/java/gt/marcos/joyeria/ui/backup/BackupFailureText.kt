package gt.marcos.joyeria.ui.backup

import androidx.annotation.StringRes
import gt.marcos.joyeria.R
import gt.marcos.joyeria.domain.backup.BackupFailure

/**
 * Cada causa de fallo con su mensaje en español, sin jerga y diciendo qué hacer. Sin `else`: una causa nueva en
 * [BackupFailure] no compila hasta que tenga su texto.
 */
@StringRes
fun BackupFailure?.messageRes(): Int = when (this) {
    BackupFailure.NOT_A_BACKUP -> R.string.backup_error_not_a_backup
    BackupFailure.CORRUPT -> R.string.backup_error_corrupt
    BackupFailure.UNSUPPORTED_FORMAT -> R.string.backup_error_unsupported
    BackupFailure.SCHEMA_TOO_NEW -> R.string.backup_error_too_new
    BackupFailure.SCHEMA_NO_UPGRADER -> R.string.backup_error_too_old
    BackupFailure.UNSAFE_FILE -> R.string.backup_error_unsafe
    BackupFailure.TOO_BIG -> R.string.backup_error_too_big
    BackupFailure.INVALID_DATA -> R.string.backup_error_invalid
    BackupFailure.CATEGORY_CONFLICT -> R.string.backup_error_categories
    BackupFailure.SAFETY_COPY_FAILED -> R.string.backup_error_safety_copy
    BackupFailure.NOTHING_TO_UNDO -> R.string.backup_error_nothing_to_undo
    null -> R.string.backup_error_generic
}

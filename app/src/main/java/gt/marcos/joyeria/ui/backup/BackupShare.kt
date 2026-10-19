package gt.marcos.joyeria.ui.backup

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Entrega el archivo al selector de Android (WhatsApp, Drive, correo...). No pide ningún permiso: el archivo vive en
 * `cacheDir/backups/` (`res/xml/file_paths.xml`) y la app elegida recibe un permiso temporal de lectura.
 */
object BackupShareIntent {
    fun share(context: Context, file: File, mime: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, chooserTitle))
    }
}

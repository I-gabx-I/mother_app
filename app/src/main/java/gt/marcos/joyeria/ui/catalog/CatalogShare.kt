package gt.marcos.joyeria.ui.catalog

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Comparte las imágenes del catálogo por el selector de Android (FASES.md
 * Fase 12), no por una app en particular: ella elige WhatsApp ahí. Una página
 * va con `ACTION_SEND`; varias, con `ACTION_SEND_MULTIPLE`, que WhatsApp
 * recibe como un álbum.
 *
 * Los archivos salen por `FileProvider` (solo `cache/catalog/`, ver
 * `res/xml/file_paths.xml`) y no hace falta ningún permiso de Android.
 *
 * Además del extra, se arma un `ClipData` con las mismas URIs: sin él, el
 * selector no le transfiere el permiso temporal de lectura a la app destino en
 * las versiones recientes de Android, y la app elegida recibe la imagen pero
 * no puede abrirla.
 */
fun shareCatalog(context: Context, pages: List<String>, chooserTitle: String) {
    val authority = "${context.packageName}.fileprovider"
    val uris = pages.map { FileProvider.getUriForFile(context, authority, File(it)) }
    if (uris.isEmpty()) return

    val sendIntent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply { putExtra(Intent.EXTRA_STREAM, uris.first()) }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(uris))
        }
    }
    sendIntent.apply {
        type = "image/jpeg"
        clipData = ClipData.newRawUri(null, uris.first()).also { clip ->
            uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
}

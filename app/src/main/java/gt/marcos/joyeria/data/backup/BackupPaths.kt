package gt.marcos.joyeria.data.backup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * Las carpetas del respaldo (Fase 14). **La copia previa a una importación va en `filesDir`, no en la caché**:
 * Android puede vaciar la caché solo, y un «Deshacer» sin archivo detrás es peor que no ofrecerlo (D-061).
 */
class BackupPaths @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    /** Las fotos de las piezas (`ImageStorage` las guarda acá). */
    val photosDir: File get() = File(context.filesDir, "photos").apply { mkdirs() }

    /** Lo que se comparte (respaldos y CSV): caché, expuesta por `FileProvider` (`res/xml/file_paths.xml`). Se puede vaciar. */
    val exportDir: File get() = File(context.cacheDir, EXPORT_DIR_NAME).apply { mkdirs() }

    /** La copia previa a importar: **`filesDir`**, que Android no vacía. */
    val safetyDir: File get() = File(context.filesDir, "backups").apply { mkdirs() }

    /** Donde se copia el archivo elegido (de otra app) para leerlo como `ZipFile`. */
    val importTmpDir: File get() = File(context.cacheDir, "import-tmp").apply { mkdirs() }

    companion object {
        /** Debe coincidir con `res/xml/file_paths.xml`. */
        const val EXPORT_DIR_NAME = "backups"
        const val SAFETY_PREFIX = "pre-import-"
    }
}

package gt.marcos.joyeria.ui.catalog.render

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import gt.marcos.joyeria.R
import gt.marcos.joyeria.domain.catalog.CatalogCaption
import gt.marcos.joyeria.domain.catalog.CatalogItem
import gt.marcos.joyeria.domain.catalog.CatalogPaging
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Arma las imágenes del catálogo (Fase 12) y las guarda en la caché de la app
 * (`cacheDir/catalog/`, el único directorio que expone `file_paths.xml`): una
 * JPEG por página, para compartirlas todas juntas.
 *
 * Trabajo de CPU y de disco: quien llama lo corre fuera del hilo principal.
 * Cancelable entre páginas (`ensureActive`), así que "Cancelar" corta de verdad.
 *
 * Cada generación usa **nombres nuevos** (con la hora) y borra los de la
 * anterior: Coil guarda en memoria por ruta, y si se reusara el nombre la
 * vista previa mostraría la imagen vieja.
 */
class CatalogGenerator @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val labels = CatalogLabels(
        placeholderName = context.getString(R.string.add_product_default_name),
        noPhoto = context.getString(R.string.catalog_no_photo),
        pageOfFormat = context.getString(R.string.catalog_page_of),
    )
    private val renderer = CatalogRenderer(labels)

    private val outputDir: File
        get() = File(context.cacheDir, OUTPUT_DIR_NAME).apply { mkdirs() }

    /** El texto de nombre que va a salir debajo de la foto: lo usa también la pantalla de elegir. */
    fun captionOf(item: CatalogItem): String = CatalogCaption.of(item, labels.placeholderName)

    /**
     * @param onProgress se llama con (páginas listas, páginas totales), la
     * primera vez con 0 para poder mostrar «0 de N» desde el inicio.
     * @return las imágenes en orden de página.
     */
    suspend fun generate(
        items: List<CatalogItem>,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): List<File> {
        val pages = CatalogPaging.paginate(items)
        val dir = outputDir
        dir.listFiles()?.forEach { it.delete() }
        val stamp = System.currentTimeMillis()
        onProgress(0, pages.size)
        val files = ArrayList<File>(pages.size)
        pages.forEachIndexed { index, pieces ->
            currentCoroutineContext().ensureActive()
            val bitmap = renderer.renderPage(pieces, pageNumber = index + 1, pageCount = pages.size)
            val file = File(dir, "catalog-$stamp-${index + 1}.jpg")
            try {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, CatalogStyle.JPEG_QUALITY, out)
                }
            } finally {
                bitmap.recycle()
            }
            files.add(file)
            onProgress(index + 1, pages.size)
        }
        return files
    }

    companion object {
        /** Debe coincidir con `res/xml/file_paths.xml`. */
        const val OUTPUT_DIR_NAME = "catalog"
    }
}

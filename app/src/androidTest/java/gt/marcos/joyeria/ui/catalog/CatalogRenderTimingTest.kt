package gt.marcos.joyeria.ui.catalog

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import gt.marcos.joyeria.data.local.AppDatabase
import gt.marcos.joyeria.data.local.entity.ProductEntity
import gt.marcos.joyeria.data.repository.CatalogRepository
import gt.marcos.joyeria.ui.catalog.render.CatalogGenerator
import gt.marcos.joyeria.util.ImageStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Random

/**
 * Criterio 3 de la Fase 12: un catálogo de 20 piezas se genera en menos de 5
 * segundos. Medido en el emulador, de punta a punta: leer de la base +
 * decodificar las 20 fotos + dibujar + comprimir las 5 imágenes.
 *
 * Las fotos las guarda el mismo `ImageStorage` que usa el alta de piezas (lado
 * mayor 1600px, hasta 1MB), con ruido aleatorio para que pesen lo que pesa una
 * foto real: es el caso pesado, no uno amable. Lo que mide imprime con la
 * etiqueta `CatalogTiming` (`adb logcat -d -s CatalogTiming`).
 *
 * Corre con el emulador en reposo (CLAUDE.md sección 7.11): sin un build de
 * Gradle en paralelo. Una medición con contención no vale.
 */
@RunWith(AndroidJUnit4::class)
class CatalogRenderTimingTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private val photoPaths = mutableListOf<String>()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val storage = ImageStorage(context)
        val random = Random(42)
        repeat(PIECES) { index ->
            photoPaths += storage.save(noisyPhoto(random, index), rotationDegrees = 0)
        }
        runBlocking {
            photoPaths.forEachIndexed { index, path ->
                db.productDao().insert(
                    ProductEntity(
                        uid = "XP-%06d".format(java.util.Locale.ROOT, index + 1),
                        name = if (index % 3 == 0) "Pieza sin nombre" else "Anillo con piedra número $index",
                        categoryId = null,
                        costCents = 77777,
                        salePriceCents = 12500L + index,
                        stockQty = 2,
                        photoPath = path,
                        supplier = null,
                        notes = null,
                        createdAt = 0L,
                        updatedAt = 0L,
                    ),
                )
            }
        }
    }

    @After
    fun tearDown() {
        db.close()
        photoPaths.forEach { File(it).delete() }
        File(context.cacheDir, CatalogGenerator.OUTPUT_DIR_NAME).listFiles()?.forEach { it.delete() }
    }

    @Test
    fun render_twentyPieces_finishesUnderFiveSeconds() = runBlocking {
        val repository = CatalogRepository(db.productDao())
        val generator = CatalogGenerator(context)
        val photoBytes = photoPaths.sumOf { File(it).length() }
        Log.i(TAG, "fotos de entrada: $PIECES, ${photoBytes / 1024} KB en total (${photoBytes / PIECES / 1024} KB c/u)")

        val elapsedMillis = mutableListOf<Long>()
        repeat(RUNS) { run ->
            val start = System.nanoTime()
            val items = repository.observeItems().first()
            val files = generator.generate(items)
            val millis = (System.nanoTime() - start) / 1_000_000
            elapsedMillis += millis

            val totalBytes = files.sumOf { it.length() }
            Log.i(
                TAG,
                "corrida ${run + 1}: ${items.size} piezas -> ${files.size} imágenes en $millis ms, " +
                    "${totalBytes / 1024} KB en total (${files.map { it.length() / 1024 }} KB)",
            )
            assertThat(items).hasSize(PIECES)
            assertThat(files).hasSize(5)
            files.forEach { file ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                assertThat(bounds.outWidth).isEqualTo(1080)
            }
        }
        Log.i(TAG, "RESUMEN tiempos (ms): $elapsedMillis")
        assertThat(elapsedMillis.max()).isLessThan(5_000L)
    }

    /** Una foto de 1600x1200 con ruido por bloques, que obliga al JPEG a pesar como una foto real. */
    private fun noisyPhoto(random: Random, seed: Int): Bitmap {
        val width = 1600
        val height = 1200
        val pixels = IntArray(width * height)
        val base = 0xFF000000.toInt() or (random.nextInt(0xFFFFFF))
        for (y in 0 until height) {
            for (x in 0 until width) {
                val noise = random.nextInt(48) - 24
                val r = (((base shr 16) and 0xFF) + noise + (x * 255 / width) / 4 + seed).coerceIn(0, 255)
                val g = (((base shr 8) and 0xFF) + noise + (y * 255 / height) / 4).coerceIn(0, 255)
                val b = ((base and 0xFF) + noise).coerceIn(0, 255)
                pixels[y * width + x] = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private companion object {
        const val TAG = "CatalogTiming"
        const val PIECES = 20
        const val RUNS = 3
    }
}

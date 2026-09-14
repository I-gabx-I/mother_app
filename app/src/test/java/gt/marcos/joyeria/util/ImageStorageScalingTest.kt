package gt.marcos.joyeria.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

// JUnit4 puro, sin Robolectric: solo se testea la función pura de cálculo
// de dimensiones (sin Bitmap), pedido explícito del humano (FASES.md
// Fase 03). La parte de ImageStorage que sí toca Bitmap/archivos se
// verifica a mano con una captura real.
class ImageStorageScalingTest {

    @Test
    fun scaledDimensions_landscape4to3_longestSideBecomesExactly1600() {
        val (width, height) = ImageStorage.scaledDimensions(width = 4000, height = 3000, maxSide = 1600)

        assertThat(width).isEqualTo(1600)
        assertThat(height).isEqualTo(1200)
    }

    @Test
    fun scaledDimensions_portrait3to4_longestSideBecomesExactly1600() {
        val (width, height) = ImageStorage.scaledDimensions(width = 3000, height = 4000, maxSide = 1600)

        assertThat(width).isEqualTo(1200)
        assertThat(height).isEqualTo(1600)
    }

    @Test
    fun scaledDimensions_square_bothSidesCapped() {
        val (width, height) = ImageStorage.scaledDimensions(width = 2000, height = 2000, maxSide = 1600)

        assertThat(width).isEqualTo(1600)
        assertThat(height).isEqualTo(1600)
    }

    @Test
    fun scaledDimensions_alreadyUnderTheLimit_isNotUpscaled() {
        val (width, height) = ImageStorage.scaledDimensions(width = 800, height = 600, maxSide = 1600)

        assertThat(width).isEqualTo(800)
        assertThat(height).isEqualTo(600)
    }

    @Test
    fun scaledDimensions_exactlyAtTheLimit_isUnchanged() {
        val (width, height) = ImageStorage.scaledDimensions(width = 1600, height = 1200, maxSide = 1600)

        assertThat(width).isEqualTo(1600)
        assertThat(height).isEqualTo(1200)
    }
}

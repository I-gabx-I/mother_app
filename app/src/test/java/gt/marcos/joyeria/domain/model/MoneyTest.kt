package gt.marcos.joyeria.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

// JUnit4 puro, sin Robolectric: aritmética de domain, nada de Android
// (CLAUDE.md sección 8 / D-012).
class MoneyTest {

    @Test
    fun plus_addsCentsCorrectly() {
        val result = Money(4000) + Money(6000)

        assertThat(result).isEqualTo(Money(10000))
    }

    @Test
    fun minus_subtractsCentsCorrectly() {
        val result = Money(10000) - Money(4000)

        assertThat(result).isEqualTo(Money(6000))
    }

    @Test
    fun minus_canProduceNegativeMoney() {
        val result = Money(4000) - Money(10000)

        assertThat(result.cents).isEqualTo(-6000L)
    }

    @Test
    fun times_multipliesByPositiveInt() {
        val result = Money(500) * 3

        assertThat(result).isEqualTo(Money(1500))
    }

    @Test
    fun times_byZero_isZero() {
        val result = Money(500) * 0

        assertThat(result).isEqualTo(Money.ZERO)
    }

    @Test
    fun compareTo_ordersByCents() {
        assertThat(Money(100) < Money(200)).isTrue()
        assertThat(Money(200) < Money(100)).isFalse()
        assertThat(Money(100) < Money(100)).isFalse()
    }

    @Test
    fun equals_sameCents_areEqual() {
        assertThat(Money(500)).isEqualTo(Money(500))
    }

    @Test
    fun zero_hasZeroCents() {
        assertThat(Money.ZERO.cents).isEqualTo(0L)
    }
}

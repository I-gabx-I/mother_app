package gt.marcos.joyeria.domain.model

/**
 * Dinero como Long en centavos de quetzal (CLAUDE.md 3.1/3.2, D-001).
 * Prohibido pasar un Long pelado entre funciones de negocio: este tipo
 * evita que alguien sume centavos con unidades o con cantidades de stock.
 *
 * Sin format(): CLAUDE.md 3.3 prohíbe formatear moneda fuera de `ui`.
 * El formateo llega en la Fase 03, en `ui/format/MoneyFormat.kt`.
 */
@JvmInline
value class Money(val cents: Long) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(cents + other.cents)

    operator fun minus(other: Money): Money = Money(cents - other.cents)

    operator fun times(factor: Int): Money = Money(cents * factor)

    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    companion object {
        val ZERO = Money(0)
    }
}

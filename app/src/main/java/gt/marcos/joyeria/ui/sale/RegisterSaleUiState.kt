package gt.marcos.joyeria.ui.sale

import gt.marcos.joyeria.data.repository.Customer
import gt.marcos.joyeria.data.repository.ProductSummary
import gt.marcos.joyeria.data.repository.RegisterSaleResult
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.domain.pricing.PricingCalculator
import gt.marcos.joyeria.ui.format.parseMoneyToCents

/** Contado o crédito (D-042) -- selector arriba de "Vender", contado preseleccionado siempre. */
enum class SaleTypeChoice { CASH, CREDIT }

/**
 * Una línea del carrito.
 *
 * `unitPriceText` es el precio de **esta venta**, editable (D-045): llega
 * precargado con el precio de lista de la pieza y ella lo mueve hacia
 * arriba o hacia abajo. Reemplazó al descuento general de la venta, que
 * solo podía restar -- y a veces ella vende más caro que la lista.
 *
 * `unitCost`/`availableStock` son una **vista previa** tomada del catálogo
 * en el momento de agregar la pieza -- el snapshot real que se persiste en
 * `sale_item` se toma de nuevo, fresco, dentro de la transacción de
 * `SaleRepository.register()` (ver ESTADO.md "Fase 06 -- Plan", punto 1);
 * si algo cambiara entre medio, lo que se guarda es lo real, no esta vista
 * previa. El **precio** es la excepción, y es el punto de todo el cambio:
 * ese sí es el que ella tecleó acá, y el repositorio lo recibe como dato
 * de entrada en vez de releerlo del producto (D-045).
 */
data class SaleLineUiState(
    val productId: Long,
    val productName: String,
    val productUid: String,
    val unitPriceText: String,
    val unitCost: Money,
    val availableStock: Int,
    val qtyText: String = "1",
) {
    val qty: Int? get() = qtyText.toIntOrNull()

    /**
     * `null` mientras el precio no sea un monto completo -- campo vacío, o a
     * mitad de escribir (ej. "20." recién tecleado el separador). Mismo
     * criterio que cualquier otro campo de dinero (D-030).
     *
     * A diferencia del descuento y del abono inicial, un precio **vacío no
     * es cero**: una venta sin precio no es una venta regalada, es una venta
     * a medio escribir. Por eso vacío cae en `null` y bloquea el guardado,
     * en vez de asumir `Money.ZERO`.
     */
    val unitPrice: Money? get() = parseMoneyToCents(unitPriceText)?.let(::Money)

    // Cantidad vacía o 0: incompleta, sin mensaje (mismo criterio que un
    // campo de dinero vacío). Cantidad > stock: incompleta CON mensaje
    // (ver ESTADO.md punto 2) -- distinción a propósito, no un descuido.
    val exceedsStock: Boolean get() = (qty ?: 0) > availableStock
    // Sin `!!` (CLAUDE.md sección 5): la versión anterior de esta línea usaba
    // `qty != null && qty!! in 1..availableStock`, que lo violaba.
    val isValid: Boolean get() = (qty?.let { it in 1..availableStock } == true) && unitPrice != null

    /** `null` si el precio o la cantidad todavía están incompletos. */
    val subtotal: Money? get() = unitPrice?.let { price -> price * (qty ?: 0) }

    val totalCost: Money get() = unitCost * (qty ?: 0)

    /**
     * Ganancia de **esta** línea: (precio - costo) * cantidad. `null`
     * mientras el precio esté incompleto.
     *
     * D-046: el aviso de pérdida se evalúa acá, por línea, y no sobre el
     * total de la venta como decidió D-035 en su momento. Con precio por
     * pieza ella puede rematar un collar bajo costo y no enterarse porque
     * el total da positivo -- y ahora sí puede hacer algo distinto al
     * respecto: subirle el precio a esa línea sola.
     */
    val profit: Money?
        get() = unitPrice?.let { price -> PricingCalculator.profit(unitCost, price) * (qty ?: 0) }

    /**
     * `true` cuando esta línea no deja ninguna ganancia o deja pérdida.
     * Con cantidad incompleta (`qty == null`) la ganancia da `Money.ZERO`
     * por multiplicar por cero, y eso **no** es una pérdida real: es una
     * línea a medio escribir. Se excluye explícitamente para no avisarle de
     * una pérdida que no existe mientras todavía está tecleando.
     */
    val isLoss: Boolean get() = qty != null && profit?.let { it <= Money.ZERO } == true
}

/** Estado único de la pantalla "Vender" (CLAUDE.md sección 5). */
data class RegisterSaleUiState(
    val products: List<ProductSummary> = emptyList(),
    val query: String = "",
    val lines: List<SaleLineUiState> = emptyList(),
    val isSaving: Boolean = false,
    val pendingLossConfirmation: Boolean = false,
    val result: RegisterSaleResult? = null,
    val saleType: SaleTypeChoice = SaleTypeChoice.CASH,
    val customers: List<Customer> = emptyList(),
    val selectedCustomerId: Long? = null,
    val initialPaymentText: String = "",
    /**
     * Se entró desde el perfil de una clienta (Fase 08). Solo cambia una
     * cosa: en **contado** se muestra de quién es la venta, con un
     * "Cambiar" -- si no, ella no tendría forma de ver ni corregir a quién
     * se la están cargando. En crédito el selector de clienta ya existía.
     */
    val cameFromCustomerProfile: Boolean = false,
) {
    val isCredit: Boolean get() = saleType == SaleTypeChoice.CREDIT

    /** La clienta de la venta, ya resuelta a su nombre para mostrarla. */
    val selectedCustomer: Customer? get() = customers.find { it.id == selectedCustomerId }

    /** El encabezado "Venta para X" solo tiene sentido en contado: en crédito ya está el selector. */
    val showsCashCustomerHeader: Boolean get() = !isCredit && (cameFromCustomerProfile || selectedCustomerId != null)

    /** Solo piezas con stock: vender algo agotado no tiene sentido (punto 3 del plan). */
    val availableProducts: List<ProductSummary>
        get() = products.filter { it.stockQty > 0 }.let { active ->
            if (query.isBlank()) {
                active
            } else {
                active.filter { it.name.contains(query, ignoreCase = true) || it.uid.contains(query, ignoreCase = true) }
            }
        }

    /**
     * `null` mientras **alguna** línea tenga el precio o la cantidad
     * incompletos: no hay un subtotal real que mostrar, y afirmar uno
     * parcial sería un número que no corresponde a nada que ella haya
     * terminado de elegir (mismo criterio que D-015/D-030).
     */
    val subtotal: Money?
        get() {
            if (lines.isEmpty()) return null
            return lines.fold(Money.ZERO) { acc, line -> acc + (line.subtotal ?: return null) }
        }

    val totalCost: Money get() = lines.fold(Money.ZERO) { acc, line -> acc + line.totalCost }

    /**
     * Ganancia de toda la venta. `null` mientras el subtotal esté
     * incompleto.
     *
     * Sigue pasando por `PricingCalculator.saleProfit` con descuento
     * `Money.ZERO` en vez de restar a mano: esa función es la única fuente
     * de esta cuenta en toda la app (incluye las ventas viejas, que sí
     * tienen descuento real guardado -- ver la advertencia de
     * `discount_cents` en `ESQUEMA.md`), y la vista previa en pantalla
     * tiene que usar exactamente la misma que el valor persistido.
     */
    val profit: Money?
        get() = subtotal?.let { PricingCalculator.saleProfit(it, Money.ZERO, totalCost) }

    /**
     * Las líneas que no dejan ganancia, para el aviso de D-046. La pantalla
     * las nombra una por una con su monto: "en qué pieza pierde y cuánto".
     */
    val lossLines: List<SaleLineUiState> get() = lines.filter { it.isLoss }

    /** Lo que ella cobraría de esta venta si fuera al contado -- el tope del abono inicial. */
    val net: Money? get() = subtotal

    // Punto 5 del pedido de Fase 07: vacío = sin abono inicial, nunca exige un monto.
    // Mismo tratamiento que cualquier campo de dinero opcional (D-030): `null`
    // mientras el texto esté incompleto, sin filtrar al tipear -- el tope (net) es
    // un número que cambia con el carrito, ella no lo puede anticipar (mismo
    // criterio que exceedsStock en una línea).
    val initialPayment: Money?
        get() = if (initialPaymentText.isBlank()) Money.ZERO else parseMoneyToCents(initialPaymentText)?.let(::Money)

    val initialPaymentExceedsNet: Boolean
        get() {
            val net = net ?: return false
            return (initialPayment ?: Money.ZERO) > net
        }

    val canSave: Boolean
        get() = !isSaving &&
            lines.isNotEmpty() &&
            lines.all { it.isValid } &&
            profit != null &&
            (!isCredit || (selectedCustomerId != null && initialPayment != null && !initialPaymentExceedsNet))
}

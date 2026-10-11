package gt.marcos.joyeria.data.repository

import androidx.room.withTransaction
import gt.marcos.joyeria.data.local.AppDatabase
import gt.marcos.joyeria.data.local.dao.CustomerDao
import gt.marcos.joyeria.data.local.dao.PaymentDao
import gt.marcos.joyeria.data.local.dao.PendingSaleRow
import gt.marcos.joyeria.data.local.dao.ProductDao
import gt.marcos.joyeria.data.local.dao.SaleDao
import gt.marcos.joyeria.data.local.dao.SaleItemDao
import gt.marcos.joyeria.data.local.entity.PaymentEntity
import gt.marcos.joyeria.data.local.entity.SaleEntity
import gt.marcos.joyeria.data.local.entity.SaleItemEntity
import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.domain.pricing.PricingCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Registra una venta (al contado o a crédito, Fase 07) y la anula (Fase 06).
 *
 * **Snapshots de `sale_item` -- dos orígenes distintos, a propósito (D-045):**
 *
 * - `product_uid_snapshot`, `product_name_snapshot` y `unit_cost_cents` se
 *   toman leyendo cada producto **fresco desde `ProductDao`, dentro de la
 *   misma transacción** que inserta la venta -- nunca desde un valor que
 *   venía cacheado en el `ViewModel` -- para que nada escrito después pueda
 *   cambiar lo que quedó guardado (D-002). Mismo patrón que
 *   `PurchaseRepository` (Fase 05) ya usa para leer el costo/stock real
 *   antes de recalcular.
 * - `unit_price_cents` **NO**: es el precio que la usuaria tecleó en esa
 *   línea (`SaleLineInput.unitPrice`), y por eso llega como dato de entrada.
 *   Leerlo fresco del producto sería exactamente el bug: le pisaría el
 *   precio que ella eligió para esta venta con el precio de lista del
 *   inventario. El precio de lista (`product.sale_price_cents`) **no se
 *   toca** acá: lo cambia solamente la pantalla de editar pieza, que es la
 *   que alimenta `price_history`.
 *
 * El costo nunca es editable en la pantalla de vender: es el dato que hace
 * confiable la ganancia histórica, y tiene que salir del inventario.
 *
 * `payment` es una entidad hija de `sale` (una venta tiene muchos abonos,
 * el saldo/estado de una venta es lo que decide si un abono entra o no) --
 * por eso su DAO se maneja acá, no en un `PaymentRepository` aparte: sigue
 * el mismo criterio que ya usa `SaleItemDao`, inyectado directo en este
 * repositorio sin su propia clase repositorio (Fase 07 plan, arquitectura).
 */
class SaleRepository @Inject constructor(
    private val db: AppDatabase,
    private val saleDao: SaleDao,
    private val saleItemDao: SaleItemDao,
    private val productDao: ProductDao,
    private val customerDao: CustomerDao,
    private val paymentDao: PaymentDao,
) {
    suspend fun register(input: RegisterSaleInput): RegisterSaleResult = db.withTransaction {
        // D-036: dos líneas del mismo producto leerían y escribirían la
        // misma fila de `product` por separado dentro de esta transacción
        // -- el segundo `productDao.update` pisaría al primero en vez de
        // sumarse, vendiendo de más sin que el chequeo de stock lo note
        // (los dos checks leen el mismo stockQty original). Se rechaza en
        // vez de fusionar en silencio: el carrito ya agrupa por producto,
        // así que un duplicado acá es un error del llamador, no un caso de
        // uso real a resolver.
        val duplicateProductIds = input.lines
            .groupingBy { it.productId }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        require(duplicateProductIds.isEmpty()) {
            "La venta trae el mismo producto repetido en más de una línea " +
                "($duplicateProductIds) -- agrupá la cantidad en una sola línea " +
                "por producto antes de vender."
        }

        val items = input.lines.map { line ->
            val product = checkNotNull(productDao.getById(line.productId)) {
                "Producto ${line.productId} no existe -- no se puede vender algo que no está"
            }
            check(line.qty <= product.stockQty) {
                "No hay suficiente stock de ${product.uid}: pidió ${line.qty}, hay ${product.stockQty}"
            }
            // Un precio de cero es legítimo (regalar una pieza, y el aviso de
            // pérdida ya se lo dice antes de confirmar); uno negativo no
            // existe como caso de uso y significaría cobrar al revés. El
            // campo de la pantalla no puede producirlo -- su filtro no acepta
            // el signo -- así que si llega acá es un error de quien llama y
            // hay que enterarse, no persistirlo (mismo criterio que D-015).
            require(line.unitPrice.cents >= 0L) {
                "El precio de la línea de ${product.uid} es negativo " +
                    "(${line.unitPrice.cents} centavos) -- no se puede vender a un precio bajo cero"
            }
            product to line
        }

        // El total sale de los precios **tecleados**, no del precio de lista
        // (D-045). El costo sigue saliendo del producto fresco.
        val totalCents = items.sumOf { (_, line) -> line.unitPrice.cents * line.qty }
        val totalCostCents = items.sumOf { (product, line) -> product.costCents * line.qty }

        // D-042: `credit == null` es el camino de Fase 06 (CASH/PAID).
        // `credit != null` nace SIEMPRE PENDING (FASES.md: "con o sin abono
        // inicial") -- si trae un abono inicial, se procesa después de
        // insertar, por el mismo camino que cualquier otro abono
        // (registerPaymentInternal), no un `if` especial.
        //
        // Fase 08: la clienta puede venir por dos lados. En crédito sale de
        // `credit` (es la que debe); en contado, de `input.customerId` (la
        // venta se le asocia pero no es deuda). `credit` gana si están los
        // dos: la que debe no se puede cambiar por otra.
        val credit = input.credit
        val customerId = credit?.customerId ?: input.customerId

        // La validación corre para CUALQUIERA de los dos caminos, no solo
        // para el crédito como hasta Fase 07. La clave foránea de
        // `sale.customer_id` es NO_ACTION (ESQUEMA.md), así que la base no
        // frena una venta apuntando a una clienta que no existe; y una
        // clienta archivada no debería recibir ventas nuevas por ningún
        // camino. Sin esto, el contado habría sido la puerta de atrás a las
        // dos cosas.
        if (customerId != null) {
            val customer = checkNotNull(customerDao.getById(customerId)) {
                "Cliente $customerId no existe -- no se puede venderle a alguien que no está"
            }
            check(!customer.archived) {
                "Cliente $customerId está archivada -- no se puede registrar una venta nueva a su nombre"
            }
        }

        val saleId = saleDao.insert(
            SaleEntity(
                soldAt = input.soldAt,
                customerId = customerId,
                type = if (credit != null) "CREDIT" else "CASH",
                status = if (credit != null) "PENDING" else "PAID",
                totalCents = totalCents,
                totalCostCents = totalCostCents,
                discountCents = input.discount.cents,
                notes = input.notes,
                cancelledAt = null,
                cancelReason = null,
            ),
        )

        saleItemDao.insertAll(
            items.map { (product, line) ->
                SaleItemEntity(
                    saleId = saleId,
                    productId = product.id,
                    productUidSnapshot = product.uid,
                    productNameSnapshot = product.name,
                    qty = line.qty,
                    unitPriceCents = line.unitPrice.cents,
                    unitCostCents = product.costCents,
                )
            },
        )

        items.forEach { (product, line) ->
            productDao.update(product.copy(stockQty = product.stockQty - line.qty))
        }

        val initialPayment = credit?.initialPayment
        if (initialPayment != null && initialPayment.cents > 0L) {
            registerPaymentInternal(
                RegisterPaymentInput(
                    saleId = saleId,
                    amount = initialPayment,
                    paidAt = input.soldAt,
                    method = "CASH",
                    notes = null,
                ),
            )
        }

        RegisterSaleResult(saleId)
    }

    /**
     * Anula la venta y devuelve exactamente el stock que había descontado
     * (D-006: nunca se borra, siempre con motivo y fecha). `cancelledAt` se
     * recibe como parámetro -- igual que `soldAt` en `register()` -- para
     * que el repositorio sea determinístico y testeable sin depender del
     * reloj real; quien llama (el `ViewModel`) es quien decide "ahora".
     */
    suspend fun cancel(saleId: Long, cancelledAt: Long, cancelReason: String?): Unit = db.withTransaction {
        val sale = checkNotNull(saleDao.getById(saleId)) { "Venta $saleId no existe" }
        check(sale.status != "CANCELLED") { "La venta $saleId ya estaba anulada" }

        // Encontrado al escribir el resto de Fase 07, no pedido en la ronda
        // de bloqueos -- ver DECISIONES.md D-043. Anular una venta a crédito
        // que ya tiene abonos dejaría esos `payment` reales apuntando a una
        // venta `CANCELLED`: el dinero que ya cobró de esa clienta quedaría
        // sin ninguna venta viva a la que corresponder. No hay en esta fase
        // ninguna forma de anular un abono, así que se bloquea la anulación
        // entera en vez de dejar el dato a medio consistente.
        val paidSoFar = paymentDao.sumForSale(saleId)
        check(paidSoFar == 0L) {
            "La venta $saleId ya tiene abonos registrados ($paidSoFar centavos) -- no se puede anular " +
                "sin antes resolver esos abonos"
        }

        saleItemDao.getForSale(saleId).forEach { item ->
            val productId = item.productId ?: return@forEach
            productDao.getById(productId)?.let { product ->
                productDao.update(product.copy(stockQty = product.stockQty + item.qty))
            }
        }

        saleDao.updateStatus(
            id = saleId,
            status = "CANCELLED",
            cancelledAt = cancelledAt,
            cancelReason = cancelReason,
        )
    }

    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<SaleSummary>> =
        saleDao.observeBetween(startMillis, endMillis).map { sales -> sales.map { it.toSummary() } }

    suspend fun getDetail(saleId: Long): SaleDetail? {
        val sale = saleDao.getById(saleId) ?: return null
        val items = saleItemDao.getForSale(saleId)
        return SaleDetail(
            id = sale.id,
            soldAt = sale.soldAt,
            total = Money(sale.totalCents),
            discount = Money(sale.discountCents),
            profit = sale.toProfit(),
            items = items.map { item ->
                SaleItemDetail(
                    productUidSnapshot = item.productUidSnapshot,
                    productNameSnapshot = item.productNameSnapshot,
                    qty = item.qty,
                    unitPrice = Money(item.unitPriceCents),
                    unitCost = Money(item.unitCostCents),
                )
            },
            type = sale.type,
            status = sale.status,
            hasPayments = paymentDao.sumForSale(saleId) > 0L,
        )
    }

    /**
     * Registra un abono sobre una venta a crédito (Fase 07). Transacción
     * propia -- Room anida `withTransaction` sobre la misma conexión sin
     * problema, así que cuando `register()` llama a `registerPaymentInternal`
     * para el abono inicial, todo queda en la transacción externa de esa
     * venta; llamado desde acá afuera, abre y cierra la suya propia.
     */
    suspend fun registerPayment(input: RegisterPaymentInput): RegisterPaymentResult = db.withTransaction {
        registerPaymentInternal(input)
    }

    /**
     * Valida y aplica un abono: rechaza cero/negativo y cualquier monto que
     * supere el saldo (criterio 3 de FASES.md, con mensaje claro -- nunca un
     * `Money.ZERO`/centinela silencioso, D-015), y si el saldo nuevo llega a
     * exactamente `Money.ZERO` pasa la venta a `PAID` (criterio 4). Un
     * centavo menos y `status` sigue `PENDING` -- no hay redondeo de por
     * medio, es una resta exacta de `Long`.
     */
    private suspend fun registerPaymentInternal(input: RegisterPaymentInput): RegisterPaymentResult {
        val sale = checkNotNull(saleDao.getById(input.saleId)) { "Venta ${input.saleId} no existe" }
        check(sale.status == "PENDING") {
            "La venta ${input.saleId} no admite abonos (status=${sale.status}) -- solo se abona una venta PENDING"
        }

        val paidSoFar = Money(paymentDao.sumForSale(input.saleId))
        val balanceBefore = PricingCalculator.saleBalance(Money(sale.totalCents), Money(sale.discountCents), paidSoFar)
        require(input.amount.cents in 1..balanceBefore.cents) {
            "El abono (${input.amount.cents} centavos) tiene que ser mayor a cero y no puede superar " +
                "el saldo pendiente (${balanceBefore.cents} centavos)"
        }

        val paymentId = paymentDao.insert(
            PaymentEntity(
                saleId = input.saleId,
                paidAt = input.paidAt,
                amountCents = input.amount.cents,
                method = input.method,
                notes = input.notes,
            ),
        )

        val newBalance = balanceBefore - input.amount
        val saleNowPaid = newBalance == Money.ZERO
        if (saleNowPaid) {
            saleDao.updateStatus(id = input.saleId, status = "PAID", cancelledAt = null, cancelReason = null)
        }

        return RegisterPaymentResult(paymentId = paymentId, newBalance = newBalance, saleNowPaid = saleNowPaid)
    }

    /**
     * "¿Quién me debe?" (D-038): agrupa por clienta las ventas `PENDING`,
     * suma el saldo de cada una (`PricingCalculator.saleBalance`, nunca en
     * SQL) y ordena descendente por ese total. Toda fila que aparece acá
     * tiene saldo > 0 por construcción -- una venta con saldo cero ya pasó
     * a `PAID` en `registerPaymentInternal` y deja de ser `PENDING`.
     */
    fun observeCustomerDebts(): Flow<List<CustomerDebtSummary>> =
        saleDao.observePendingCreditSales().map { rows ->
            rows.groupBy { it.customerId }
                .map { (_, customerRows) ->
                    val first = customerRows.first()
                    CustomerDebtSummary(
                        customerId = first.customerId,
                        customerName = first.customerName,
                        customerPhone = first.customerPhone,
                        totalBalance = customerRows.fold(Money.ZERO) { acc, row -> acc + row.toBalance() },
                        oldestPendingSaleAt = customerRows.minOf { it.soldAt },
                    )
                }
                .sortedByDescending { it.totalBalance.cents }
        }

    /**
     * Estado de cuenta de una clienta: todas sus ventas `PENDING`, cada una
     * con su lista de abonos (para poder calcular su saldo, D-041/Fase 07
     * plan, punto 2). Reactivo de punta a punta -- registrar un abono nuevo
     * actualiza esta lista sola, sin releer nada a mano.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeAccountStatements(customerId: Long): Flow<List<SaleAccountStatement>> =
        saleDao.observePendingCreditSalesForCustomer(customerId).flatMapLatest { rows ->
            if (rows.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(rows.map { row -> paymentDao.observeForSale(row.saleId) }) { paymentLists ->
                    rows.zip(paymentLists.toList()) { row, payments -> row.toStatement(payments) }
                }
            }
        }

    /**
     * Historial completo de compras de una clienta para su perfil
     * (Fase 08): contado y crédito, incluidas las anuladas (B-06).
     *
     * `mayBeMissingCashSales` sale de comparar sus ventas contra la
     * **primera venta de contado registrada con clienta en toda la base**
     * (B-10). Si tiene alguna venta anterior a esa fecha, estuvo comprando
     * en la época en que la app no guardaba a quién se le vendía, así que
     * su historial **puede** tener huecos de contado y la pantalla lo
     * avisa. Si todo su historial es posterior, no hay nada que explicar y
     * no se muestra ningún aviso.
     *
     * Si todavía no existe **ninguna** venta de contado con clienta
     * (`firstNamed == null`), el registro por nombre no empezó: cualquier
     * clienta con historial previo puede tener huecos, así que el aviso se
     * muestra. Se apaga solo en cuanto exista la primera.
     *
     * Limitación conocida, a propósito: esto detecta "era clienta cuando el
     * contado no se registraba", no "le faltan compras de verdad". Una
     * clienta de esa época que nunca compró de contado va a ver el aviso
     * igual. Por eso el texto dice "puede que falten" y no "faltan": lo que
     * se afirma es que no se puede saber, que es exactamente lo cierto.
     */
    fun observeCustomerHistory(customerId: Long): Flow<CustomerPurchaseHistory> =
        saleDao.observeSalesForCustomer(customerId)
            .combine(saleDao.observeFirstNamedCashSaleDate()) { sales, firstNamed ->
                CustomerPurchaseHistory(
                    sales = sales.map { it.toSummary() },
                    mayBeMissingCashSales = sales.any { firstNamed == null || it.soldAt < firstNamed },
                )
            }

    private fun SaleEntity.toProfit(): Money =
        PricingCalculator.saleProfit(Money(totalCents), Money(discountCents), Money(totalCostCents))

    private fun SaleEntity.toSummary() = SaleSummary(
        id = id,
        soldAt = soldAt,
        total = Money(totalCents),
        discount = Money(discountCents),
        profit = toProfit(),
        type = type,
        status = status,
    )

    private fun PendingSaleRow.toBalance(): Money =
        PricingCalculator.saleBalance(Money(totalCents), Money(discountCents), Money(paidCents))

    private fun PendingSaleRow.toStatement(payments: List<PaymentEntity>) = SaleAccountStatement(
        saleId = saleId,
        soldAt = soldAt,
        total = Money(totalCents),
        discount = Money(discountCents),
        status = "PENDING",
        payments = payments.map { it.toSummary() },
    )

    private fun PaymentEntity.toSummary() = PaymentSummary(
        id = id,
        paidAt = paidAt,
        amount = Money(amountCents),
        method = method,
        notes = notes,
    )
}

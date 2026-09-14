package gt.marcos.joyeria.domain.usecase

import gt.marcos.joyeria.data.repository.ProductRepository
import gt.marcos.joyeria.domain.model.Money
import javax.inject.Inject

/**
 * Datos ya resueltos para dar de alta una pieza: el nombre y la cantidad
 * llegan con su valor por defecto ya aplicado (lo resuelve `ui`, porque
 * necesita `stringResource` para el nombre por defecto, y `domain` no
 * puede tocar recursos de Android — CLAUDE.md sección 5). `photoPath` es
 * obligatorio: sin foto no hay alta rápida posible (FASES.md Fase 03).
 */
data class AddProductInput(
    val name: String,
    val categoryId: Long?,
    val cost: Money,
    val salePrice: Money,
    val stockQty: Int,
    val photoPath: String,
    val notes: String?,
)

/**
 * Orquesta el alta de una pieza nueva. Hoy es un paso directo a
 * `ProductRepository` (que genera el `uid` y guarda todo en una sola
 * transacción) — se mantiene como caso de uso separado, no llamado
 * directo desde el ViewModel, porque es el punto donde CLAUDE.md/FASES.md
 * esperan que viva cualquier regla de negocio futura del alta (Fase 04+),
 * sin tener que mover la llamada de lugar cuando aparezca.
 */
class AddProductUseCase @Inject constructor(
    private val repository: ProductRepository,
) {
    suspend operator fun invoke(input: AddProductInput): String =
        repository.insert(input)
}

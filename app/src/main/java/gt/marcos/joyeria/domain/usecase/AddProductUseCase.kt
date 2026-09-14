package gt.marcos.joyeria.domain.usecase

import gt.marcos.joyeria.data.repository.AddProductInput
import gt.marcos.joyeria.data.repository.ProductRepository
import javax.inject.Inject

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

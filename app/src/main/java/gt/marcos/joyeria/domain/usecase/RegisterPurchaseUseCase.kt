package gt.marcos.joyeria.domain.usecase

import gt.marcos.joyeria.data.repository.PurchaseRepository
import gt.marcos.joyeria.data.repository.RegisterPurchaseInput
import gt.marcos.joyeria.data.repository.RegisterPurchaseResult
import javax.inject.Inject

/**
 * Paso directo a `PurchaseRepository.register()` (mismo patrón que
 * `AddProductUseCase`): la regla de negocio ya vive en el repositorio,
 * este caso de uso es el punto estable donde `ui` engancha, sin tener
 * que mover la llamada de lugar si la regla crece en una fase futura.
 */
class RegisterPurchaseUseCase @Inject constructor(
    private val repository: PurchaseRepository,
) {
    suspend operator fun invoke(input: RegisterPurchaseInput): RegisterPurchaseResult =
        repository.register(input)
}

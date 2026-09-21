package gt.marcos.joyeria.domain.usecase

import gt.marcos.joyeria.data.repository.RegisterPaymentInput
import gt.marcos.joyeria.data.repository.RegisterPaymentResult
import gt.marcos.joyeria.data.repository.SaleRepository
import javax.inject.Inject

/** Paso directo a `SaleRepository.registerPayment()`, mismo patrón que `RegisterSaleUseCase`. */
class RegisterPaymentUseCase @Inject constructor(
    private val repository: SaleRepository,
) {
    suspend operator fun invoke(input: RegisterPaymentInput): RegisterPaymentResult =
        repository.registerPayment(input)
}

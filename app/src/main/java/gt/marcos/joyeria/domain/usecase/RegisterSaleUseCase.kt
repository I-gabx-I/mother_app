package gt.marcos.joyeria.domain.usecase

import gt.marcos.joyeria.data.repository.RegisterSaleInput
import gt.marcos.joyeria.data.repository.RegisterSaleResult
import gt.marcos.joyeria.data.repository.SaleRepository
import javax.inject.Inject

/** Paso directo a `SaleRepository.register()`, mismo patrón que `AddProductUseCase`. */
class RegisterSaleUseCase @Inject constructor(
    private val repository: SaleRepository,
) {
    suspend operator fun invoke(input: RegisterSaleInput): RegisterSaleResult =
        repository.register(input)
}

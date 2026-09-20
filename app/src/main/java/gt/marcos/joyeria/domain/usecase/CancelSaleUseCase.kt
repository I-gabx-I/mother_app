package gt.marcos.joyeria.domain.usecase

import gt.marcos.joyeria.data.repository.SaleRepository
import javax.inject.Inject

/** Paso directo a `SaleRepository.cancel()`, mismo patrón que `ArchiveProductUseCase`. */
class CancelSaleUseCase @Inject constructor(
    private val repository: SaleRepository,
) {
    suspend operator fun invoke(saleId: Long, cancelledAt: Long, cancelReason: String?) {
        repository.cancel(saleId, cancelledAt, cancelReason)
    }
}

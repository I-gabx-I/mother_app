package gt.marcos.joyeria.domain.usecase

import gt.marcos.joyeria.data.repository.ProductRepository
import javax.inject.Inject

/** Archiva una pieza (CLAUDE.md D-006: nunca se borra de verdad). */
class ArchiveProductUseCase @Inject constructor(
    private val repository: ProductRepository,
) {
    suspend operator fun invoke(productId: Long) = repository.archive(productId)
}

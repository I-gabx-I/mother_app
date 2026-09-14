package gt.marcos.joyeria.domain.usecase

import gt.marcos.joyeria.data.repository.EditProductInput
import gt.marcos.joyeria.data.repository.ProductRepository
import javax.inject.Inject

class EditProductUseCase @Inject constructor(
    private val repository: ProductRepository,
) {
    suspend operator fun invoke(input: EditProductInput) = repository.update(input)
}

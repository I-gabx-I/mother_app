package gt.marcos.joyeria.data.repository

import gt.marcos.joyeria.domain.model.Money

data class AddProductInput(
    val name: String,
    val categoryId: Long?,
    val cost: Money,
    val salePrice: Money,
    val stockQty: Int,
    val photoPath: String,
    val notes: String?,
)

data class EditProductInput(
    val id: Long,
    val name: String,
    val categoryId: Long?,
    val cost: Money,
    val salePrice: Money,
    val stockQty: Int,
    val supplier: String?,
    val notes: String?,
)

data class ProductSummary(
    val id: Long,
    val uid: String,
    val name: String,
    val photoPath: String?,
    val stockQty: Int,
    val cost: Money,
    val salePrice: Money,
)

data class ProductDetail(
    val id: Long,
    val uid: String,
    val name: String,
    val categoryId: Long?,
    val cost: Money,
    val salePrice: Money,
    val stockQty: Int,
    val photoPath: String?,
    val supplier: String?,
    val notes: String?,
)

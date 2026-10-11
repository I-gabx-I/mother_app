package gt.marcos.joyeria.ui.product.category

import gt.marcos.joyeria.data.repository.Category

/**
 * Único estado de la pantalla de administración de categorías (Fase 09,
 * CLAUDE.md sección 5). `duplicateNameAttempt` no distingue si vino de
 * "Agregar" o de un renombre porque nunca hay los dos a la vez: solo se
 * llena con la acción que está en curso (`renamingCategoryId != null`
 * dice cuál es), y se limpia en el próximo tecleo.
 */
data class CategoryManageUiState(
    val categories: List<Category> = emptyList(),
    val newCategoryName: String = "",
    val renamingCategoryId: Long? = null,
    val renamingName: String = "",
    val archiveTarget: Category? = null,
    val duplicateNameAttempt: String? = null,
) {
    val activeCategories: List<Category> get() = categories.filter { !it.archived }
    val archivedCategories: List<Category> get() = categories.filter { it.archived }
    val canAddCategory: Boolean get() = newCategoryName.isNotBlank()
    val canConfirmRename: Boolean get() = renamingName.isNotBlank()
}

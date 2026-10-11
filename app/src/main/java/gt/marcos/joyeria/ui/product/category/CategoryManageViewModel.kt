package gt.marcos.joyeria.ui.product.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gt.marcos.joyeria.data.repository.Category
import gt.marcos.joyeria.data.repository.CategoryNameAlreadyExistsException
import gt.marcos.joyeria.data.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Único `StateFlow<UiState>` de la pantalla (CLAUDE.md sección 5). */
@HiltViewModel
class CategoryManageViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryManageUiState())
    val uiState: StateFlow<CategoryManageUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.observeAll().collectLatest { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun onNewCategoryNameChanged(value: String) {
        _uiState.update { it.copy(newCategoryName = value, duplicateNameAttempt = null) }
    }

    /** D-057: la unicidad la garantiza este repositorio, no la base. */
    fun onAddClick() {
        val state = _uiState.value
        if (!state.canAddCategory) return
        val name = state.newCategoryName.trim()
        viewModelScope.launch {
            try {
                categoryRepository.add(name)
                _uiState.update { it.copy(newCategoryName = "", duplicateNameAttempt = null) }
            } catch (e: CategoryNameAlreadyExistsException) {
                _uiState.update { it.copy(duplicateNameAttempt = e.name) }
            }
        }
    }

    fun onRenameClick(category: Category) {
        _uiState.update {
            it.copy(renamingCategoryId = category.id, renamingName = category.name, duplicateNameAttempt = null)
        }
    }

    fun onRenamingNameChanged(value: String) {
        _uiState.update { it.copy(renamingName = value, duplicateNameAttempt = null) }
    }

    fun onRenameDismiss() {
        _uiState.update { it.copy(renamingCategoryId = null, renamingName = "", duplicateNameAttempt = null) }
    }

    fun onRenameConfirm() {
        val state = _uiState.value
        val id = state.renamingCategoryId ?: return
        if (!state.canConfirmRename) return
        val name = state.renamingName.trim()
        viewModelScope.launch {
            try {
                categoryRepository.rename(id, name)
                _uiState.update { it.copy(renamingCategoryId = null, renamingName = "", duplicateNameAttempt = null) }
            } catch (e: CategoryNameAlreadyExistsException) {
                _uiState.update { it.copy(duplicateNameAttempt = e.name) }
            }
        }
    }

    fun onArchiveClick(category: Category) {
        _uiState.update { it.copy(archiveTarget = category) }
    }

    fun onArchiveDismiss() {
        _uiState.update { it.copy(archiveTarget = null) }
    }

    /** No hay `DELETE`: se archiva (D-006). Las piezas que la tengan asignada la siguen mostrando (Fase 09, Opción A). */
    fun onArchiveConfirm() {
        val target = _uiState.value.archiveTarget ?: return
        viewModelScope.launch {
            categoryRepository.archive(target.id)
            _uiState.update { it.copy(archiveTarget = null) }
        }
    }
}

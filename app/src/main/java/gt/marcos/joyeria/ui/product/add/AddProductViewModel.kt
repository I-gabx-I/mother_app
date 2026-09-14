package gt.marcos.joyeria.ui.product.add

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gt.marcos.joyeria.data.repository.AddProductInput
import gt.marcos.joyeria.data.repository.AppSettingRepository
import gt.marcos.joyeria.data.repository.CategoryRepository
import gt.marcos.joyeria.domain.usecase.AddProductUseCase
import gt.marcos.joyeria.util.ImageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Único `StateFlow<UiState>` de la pantalla (CLAUDE.md sección 5). No
 * conoce Compose ni Composables: solo estado y funciones que la UI llama.
 */
@HiltViewModel
class AddProductViewModel @Inject constructor(
    private val addProductUseCase: AddProductUseCase,
    private val appSettingRepository: AppSettingRepository,
    private val categoryRepository: CategoryRepository,
    private val imageStorage: ImageStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddProductUiState())
    val uiState: StateFlow<AddProductUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val bp = appSettingRepository.getDefaultMarkupBp()
            val step = appSettingRepository.getPriceRoundingStep()
            _uiState.update { it.copy(defaultMarkupBp = bp, roundingStep = step) }
        }
        viewModelScope.launch {
            categoryRepository.observeActive().collectLatest { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    /**
     * `ImageStorage.save` comprime y escribe a disco: se corre en
     * `Dispatchers.IO`, nunca en el hilo principal (el callback de CameraX
     * que la llama corre en el executor principal).
     */
    fun onPhotoCaptured(bitmap: Bitmap, rotationDegrees: Int) {
        viewModelScope.launch {
            val previousPath = _uiState.value.photoPath
            val newPath = withContext(Dispatchers.IO) { imageStorage.save(bitmap, rotationDegrees) }
            // Retomó la foto antes de guardar: la anterior queda huérfana si no se borra.
            if (previousPath != null) {
                withContext(Dispatchers.IO) { imageStorage.delete(previousPath) }
            }
            _uiState.update { it.copy(photoPath = newPath) }
        }
    }

    fun onCostDigitsChanged(raw: String) {
        _uiState.update { state ->
            val withNewCost = state.copy(costDigits = sanitizeMoneyDigits(raw))
            if (state.salePriceManuallyEdited) {
                withNewCost
            } else {
                // Mientras ella no haya tocado el precio a mano, se pre-llena
                // con el sugerido -- FASES.md: "mientras escribe el costo, la
                // app muestra en vivo el precio sugerido".
                val suggested = withNewCost.suggestedPrice
                if (suggested != null) withNewCost.copy(salePriceDigits = suggested.cents.toString()) else withNewCost
            }
        }
    }

    fun onSalePriceDigitsChanged(raw: String) {
        _uiState.update {
            it.copy(salePriceDigits = sanitizeMoneyDigits(raw), salePriceManuallyEdited = true)
        }
    }

    fun onNameChanged(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    fun onCategorySelected(categoryId: Long?) {
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun onQuantityChanged(raw: String) {
        _uiState.update { it.copy(quantityText = raw.filter(Char::isDigit)) }
    }

    fun onNotesChanged(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    /**
     * `defaultName` llega ya resuelto desde `ui` (`stringResource`), porque
     * `domain` no puede leer recursos de Android (CLAUDE.md sección 5).
     */
    fun onSaveClick(defaultName: String) {
        val state = _uiState.value
        val photoPath = state.photoPath
        if (!state.canSave || photoPath == null) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val input = AddProductInput(
                name = state.name.trim().ifBlank { defaultName },
                categoryId = state.categoryId,
                cost = state.cost,
                salePrice = state.salePrice,
                stockQty = state.stockQty,
                photoPath = photoPath,
                notes = state.notes.trim().ifBlank { null },
            )
            try {
                val uid = addProductUseCase(input)
                // Ya se guardó: la foto pasa a estar referenciada por el
                // producto, no queda "pendiente" -- por eso el estado nuevo
                // no la vuelve a traer, para que onCleared() no la borre.
                _uiState.value = AddProductUiState(
                    defaultMarkupBp = state.defaultMarkupBp,
                    roundingStep = state.roundingStep,
                    categories = state.categories,
                    savedUid = uid,
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun onSavedConfirmationDismissed() {
        _uiState.update { it.copy(savedUid = null) }
    }

    /**
     * Deuda técnica documentada (ver ESTADO.md, "Fotos huérfanas"): esto
     * cubre el caso de "tomó la foto y salió sin guardar" (el `ViewModel`
     * se limpia cuando la Activity termina). No cubre que el proceso
     * muera sin pasar por acá (el sistema mata la app, o un crash) -- ese
     * caso queda para una barrida de arranque en una fase futura.
     */
    override fun onCleared() {
        super.onCleared()
        val pendingPhoto = _uiState.value.photoPath
        if (pendingPhoto != null) {
            imageStorage.delete(pendingPhoto)
        }
    }
}

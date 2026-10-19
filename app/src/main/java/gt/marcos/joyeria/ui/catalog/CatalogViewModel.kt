package gt.marcos.joyeria.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gt.marcos.joyeria.data.repository.CatalogRepository
import gt.marcos.joyeria.domain.catalog.CatalogPaging
import gt.marcos.joyeria.ui.catalog.render.CatalogGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Único `StateFlow<UiState>` de la pantalla (CLAUDE.md sección 5). Arma las
 * imágenes fuera del hilo principal y se puede cancelar. La selección vive
 * acá, así que sobrevive a girar el teléfono y a volver de la vista previa a
 * «Elegir otras piezas» sin perder lo elegido.
 */
@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val generator: CatalogGenerator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null

    init {
        viewModelScope.launch {
            catalogRepository.observeItems().collectLatest { items ->
                val pieces = items.map { CatalogPiece(item = it, caption = generator.captionOf(it)) }
                _uiState.update { it.withPieces(pieces) }
            }
        }
    }

    fun onQueryChange(value: String) = _uiState.update { it.withQuery(value) }

    fun onPieceToggled(productId: Long) = _uiState.update { it.toggled(productId) }

    fun onSelectAllVisibleClick() = _uiState.update { it.withAllVisibleSelected() }

    fun onDeselectVisibleClick() = _uiState.update { it.withVisibleDeselected() }

    fun onDeselectAllClick() = _uiState.update { it.withNothingSelected() }

    fun onCreateClick() {
        val state = _uiState.value
        if (!state.canCreate || state.step is CatalogStep.Generating) return
        val items = state.selectedItems
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            // El total se sabe desde el principio: sin esto se vería «0 de 0» hasta el primer aviso.
            val total = CatalogPaging.paginate(items).size
            _uiState.update { it.copy(step = CatalogStep.Generating(done = 0, total = total)) }
            try {
                val files = withContext(Dispatchers.Default) {
                    generator.generate(items) { done, total ->
                        _uiState.updateIfGenerating { it.copy(step = CatalogStep.Generating(done = done, total = total)) }
                    }
                }
                _uiState.updateIfGenerating { it.copy(step = CatalogStep.Ready(pages = files.map { file -> file.absolutePath })) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(step = CatalogStep.Failed) }
            } catch (error: OutOfMemoryError) {
                _uiState.update { it.copy(step = CatalogStep.Failed) }
            }
        }
    }

    /**
     * Si ella cancela justo cuando termina una página, el aviso de progreso o
     * el resultado llegan tarde y pisarían el paso «Elegir» con «Armando…» o
     * con una vista previa que ya no pidió: solo cuentan si todavía se está armando.
     */
    private fun MutableStateFlow<CatalogUiState>.updateIfGenerating(transform: (CatalogUiState) -> CatalogUiState) =
        update { if (it.step is CatalogStep.Generating) transform(it) else it }

    /** «Cancelar» mientras arma, «Elegir otras piezas» al terminar y la tecla de volver: todos vuelven a elegir, con lo elegido intacto. */
    fun onBackToPicking() {
        generationJob?.cancel()
        generationJob = null
        _uiState.update { it.copy(step = CatalogStep.Picking) }
    }
}

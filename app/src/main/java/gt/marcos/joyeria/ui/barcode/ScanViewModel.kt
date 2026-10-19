package gt.marcos.joyeria.ui.barcode

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import gt.marcos.joyeria.data.repository.BarcodeRepository
import gt.marcos.joyeria.domain.barcode.ScanResolver
import gt.marcos.joyeria.domain.barcode.ScanResult
import javax.inject.Inject

/**
 * Qué hay detrás de un código escaneado. Sin estado propio: el escáner pregunta y
 * la pantalla que lo abrió decide qué hacer con la respuesta (Inventario abre la
 * pieza, Vender la agrega al carrito). La lógica de decisión está en
 * `ScanResolver`, pura y probada.
 */
@HiltViewModel
class ScanViewModel @Inject constructor(
    private val repository: BarcodeRepository,
) : ViewModel() {
    suspend fun resolve(raw: String): ScanResult = ScanResolver.resolve(raw) { repository.find(it) }
}

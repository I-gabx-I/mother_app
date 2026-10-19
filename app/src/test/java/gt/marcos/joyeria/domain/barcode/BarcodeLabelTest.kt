package gt.marcos.joyeria.domain.barcode

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * El cerrojo de la garantía estructural de la Fase 13: la etiqueta solo lleva el
 * código de la pieza, nunca un monto (D-003, D-060).
 */
class BarcodeLabelTest {

    private fun fieldNames(type: Class<*>): Set<String> =
        type.declaredFields.filter { !it.isSynthetic && !Modifier.isStatic(it.modifiers) }.map { it.name }.toSet()

    @Test
    fun barcodeLabel_fieldsAreExactlyTheUid() {
        assertWithMessage(
            "BarcodeLabel es lo único que recibe el generador de etiquetas y NO puede tener por dónde " +
                "recibir un precio ni ningún monto (D-003). Si agregaste un campo, un código de barras " +
                "pegado a una pieza no es el lugar para ese dato.",
        )
            .that(fieldNames(BarcodeLabel::class.java))
            .containsExactly("uid")
    }

    @Test
    fun labelPiece_fieldsHaveNoMoneyAtAll() {
        assertWithMessage("LabelPiece (la pantalla de etiquetas) no puede cargar ningún monto.")
            .that(fieldNames(LabelPiece::class.java))
            .containsExactly("uid", "name", "categoryName", "photoPath")
    }
}

package gt.marcos.joyeria.ui.catalog.render

import gt.marcos.joyeria.domain.model.Money
import gt.marcos.joyeria.ui.format.format
import java.util.Locale

/**
 * El precio tal como se dibuja en la imagen del catálogo: `Q1,250.00`, con
 * `Locale.ROOT`.
 *
 * **Esto no contradice la regla de formato de la app** (`Money.format()` usa
 * el idioma del teléfono para lo que ELLA lee en pantalla). La razón es
 * distinta a la del uid o a la de lo impreso: este texto viaja al teléfono de
 * otra persona. Una imagen no se vuelve a formatear al llegar, así que tiene
 * que verse igual sin importar el idioma de quien la genera: con un teléfono
 * en `es-ES` la clienta vería `Q1.250,00`, que en Guatemala se lee mal. Ver
 * D-058.
 */
internal fun catalogPriceText(price: Money): String = price.format(Locale.ROOT)

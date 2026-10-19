package gt.marcos.joyeria.domain.catalog

import gt.marcos.joyeria.domain.model.Money

/**
 * Una pieza tal como la ve una clienta en el catálogo compartible (Fase 12).
 *
 * **Este tipo no tiene por dónde cargar un costo, una ganancia, un margen ni
 * un recargo, y eso es a propósito:** el catálogo nunca los muestra, y es un
 * requisito de seguridad del negocio. Es la misma idea que `SaleLineInput`
 * (D-045): el tipo no puede representar lo que no debe viajar, y eso es más
 * fuerte que un test que verifica valores. Todo lo que llega a la pantalla
 * del catálogo y al generador de imágenes es un `CatalogItem`, y se arma
 * desde `CatalogRow`, que tampoco tiene costo (la consulta ni siquiera lo
 * lee de la base). Ver D-058 en `DECISIONES.md`.
 *
 * Agregarle un campo a este tipo rompe `CatalogItemTest`, que fija el
 * conjunto exacto de campos: es el cerrojo para que no se afloje en silencio.
 *
 * `uid` va en la tarjeta (chico y discreto) para que la clienta pueda decir
 * "quiero la XP-000012" aunque la pieza no tenga nombre. A diferencia del
 * costo, no es un dato sensible.
 *
 * @property name el nombre guardado; puede ser el texto de relleno de una
 * pieza sin nombre (ver [CatalogCaption]).
 */
data class CatalogItem(
    val productId: Long,
    val uid: String,
    val name: String,
    val categoryName: String?,
    val photoPath: String?,
    val price: Money,
)

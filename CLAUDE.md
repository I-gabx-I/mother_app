# CLAUDE.md — Reglas del proyecto

Este archivo es autoridad. Si algo aquí contradice una idea tuya, gana este archivo.
Si algo aquí contradice `FASES.md`, gana este archivo y me avisás del conflicto antes de codificar.

---

## 1. Qué estamos construyendo

App Android para que una vendedora de joyería Xuping (acero/latón con baño de oro)
lleve su inventario, sus ventas y sus cobros a plazos, reemplazando un cuaderno físico.

**La usuaria final no es técnica.** Es la mamá del dueño del proyecto. No lee inglés,
no entiende "SKU", "margen bruto" ni "sincronizar". El éxito del proyecto no es que el
código compile: es que ella deje el cuaderno.

Compra el producto **localmente en Guatemala** (mayoristas del país) y lo revende
en su trabajo. **No importa, no hay aduana, no hay flete internacional.**
No modeles nada relacionado con importación.

Moneda: Quetzal guatemalteco (GTQ), símbolo `Q`.

---

## 2. Stack fijo

No cambies, no sustituyas, no agregues alternativas sin autorización explícita del humano.

| Capa | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Arquitectura | MVVM + Clean Architecture (`data` / `domain` / `ui`) |
| Persistencia | Room |
| DI | Hilt |
| Async | Coroutines + Flow |
| Imágenes | CameraX (captura) + Coil (carga) |
| Códigos de barras | ML Kit Barcode Scanning (lectura), ZXing core (generación) |
| Tests | JUnit4 + Truth + Turbine + Room in-memory |
| Build | Gradle Kotlin DSL + version catalog (`libs.versions.toml`) |

- `minSdk = 24`, `targetSdk` y `compileSdk` = el estable más reciente disponible en el entorno.
  Razón de `minSdk 24`: la usuaria puede tener un teléfono viejo; 24 cubre prácticamente
  todo el parque instalado y no nos obliga a desugaring agresivo.
- **Todas las dependencias van en `libs.versions.toml` con versión pineada explícita.**
  Prohibido `+`, prohibido `latest.release`.
- Prohibido agregar una dependencia nueva sin escribir primero una entrada en `DECISIONES.md`
  justificándola. Si no podés justificarla en 3 líneas, no la necesitás.

---
## 2.1. Reglas de dependencias y versiones (crítico)

Los errores de versiones son la causa número uno de fases fallidas en Android.

- El `gradle/libs.versions.toml` que generó el asistente de Android Studio es
  **la línea base y se asume correcta**. Prohibido cambiar las versiones de AGP,
  Gradle, Kotlin o compileSdk que ya vinieron ahí. Si creés que hay que subir
  alguna, parás y lo anotás en ESTADO.md.
- **Nunca inventes un número de versión.** Si no estás seguro de que existe, no
  la escribas. Usá la que el proyecto ya tiene, o un BOM.
- **Que una versión exista no significa que sea compatible.** Antes de fijar
  la última versión de una librería, verificá que no exija un Kotlin, AGP o
  `compileSdk` más nuevos que los de la línea base (`gradle/libs.versions.toml`
  ya generado). Si los exige, bajá a la última versión de esa librería que sí
  sea compatible, y registralo como decisión en `DECISIONES.md`. Cumplir la
  regla anterior (versión verificada contra `maven-metadata.xml`) no alcanza
  por sí solo: Coil `3.6.2` estaba verificada así y aun así rompió la
  compilación de todo el proyecto porque exigía `kotlin-stdlib` más nuevo que
  el Kotlin pineado (D-018 en `DECISIONES.md`, Fase 03).
- **Prohibido subir Kotlin, AGP o Gradle para satisfacer a una librería.** Si
  una dependencia solo funciona con una línea base más nueva que la del
  proyecto, parás y preguntás — no se sube la versión de Kotlin/AGP/Gradle
  por tu cuenta para acomodarla. La línea base no se mueve por una librería.
- Usá BOM siempre que exista (`androidx.compose:compose-bom`). Las librerías
  cubiertas por el BOM se declaran **sin versión**. Esto elimina de raíz una
  familia entera de incompatibilidades.
- **KSP, desde la versión 2.3.0, tiene versionado independiente de Kotlin.**
  Ya no usa el formato compuesto `<kotlin>-<ksp>` (ese formato solo aplica a
  versiones de KSP anteriores a 2.3.0). Usá la última versión estable 2.3.x
  verificada en `github.com/google/ksp/releases` o en el `maven-metadata.xml`
  de `com.google.devtools.ksp`. No apliques la regla vieja de "debe coincidir
  con Kotlin" a versiones de KSP ≥ 2.3.0.
- **Un mensaje de KSP/Hilt de "tipo no se pudo resolver" no implica que la
  causa sea una versión.** Antes de tocar KSP/Hilt/Kotlin, descartá primero
  un comentario roto: ver la regla nueva de la sección 5 sobre `/*` dentro
  de un comentario KDoc. Ver D-025 en `DECISIONES.md` (Fase 04): dos
  versiones distintas de KSP, elegidas por fecha de publicación y hasta por
  coincidencia exacta con la dependencia declarada en el POM de Dagger,
  reprodujeron el error idéntico — la causa real no tenía nada que ver con
  versiones.
- **Si de verdad hay sospecha de incompatibilidad entre un procesador de
  anotaciones y su motor (Hilt/Dagger sobre KSP, Room sobre KSP), el
  método es leer el POM real de esa versión exacta del procesador, no
  comparar fechas de publicación.** Comparar fechas de publicación es una
  aproximación débil: dice qué versión del motor *existía* cuando se
  publicó el procesador, no cuál declaró usar. El POM sí lo dice de forma
  explícita — buscá la dependencia declarada en
  `repo1.maven.org/maven2/<grupo>/<artefacto>/<versión>/<artefacto>-<versión>.pom`
  (o el `dependencyInsight`/`dependencies --configuration
  kspDebugKotlinProcessorClasspath` de Gradle, que resuelve el árbol real)
  y fijá el motor exactamente en esa versión, no en la más nueva que exista
  para esa fecha. D-025 en `DECISIONES.md` (Fase 04) usó este método para
  encontrar que `dagger-compiler:2.60.1` declara
  `com.google.devtools.ksp:symbol-processing-api:2.3.7` — un dato que la
  fecha de publicación de Hilt no permitía deducir por sí sola. (En este
  caso puntual la causa real terminó siendo otra cosa completamente
  distinta, pero el método de leer el POM sigue siendo el correcto para
  esta clase de sospecha, y es más preciso que D-018, que solo comparaba
  la versión de `kotlin-stdlib` que cada candidata declaraba sin ir a
  buscar la versión exacta que el proyecto necesitaba igualar.)
- Con Kotlin 2.0+, Compose se configura con el plugin
  `org.jetbrains.kotlin.plugin.compose`, **no** con
  `composeOptions { kotlinCompilerExtensionVersion }`. Si escribís lo segundo,
  está desactualizado y va a fallar.
- Room y Hilt se configuran con KSP, no con KAPT.
- **Una dependencia a la vez.** Agregás una, corrés `./gradlew assembleDebug`,
  confirmás que compila, y solo entonces agregás la siguiente. Prohibido meter
  cinco de un solo y compilar al final: si truena no vas a saber cuál fue.
- Si un build falla, leé el error completo antes de tocar nada. Prohibido
  adivinar. Después de **2 intentos fallidos**, parás, pegás el error textual
  en ESTADO.md bajo Bloqueos, y esperás al humano.
- Prohibido "arreglar" un build bajando minSdk, corriendo con `--offline`,
  borrando caches de Gradle o desactivando checks.

## 3. Reglas de dinero (las más importantes)

**3.1. El dinero se guarda y se opera SIEMPRE en centavos, como `Long`.**
`Q75.90` se persiste como `7590`.

**Prohibido `Float`, `Double` o `BigDecimal` para dinero en cualquier capa.**
Razón: los flotantes binarios no representan decimales exactos. Con `Double`, sumar
cien ventas produce descuadres de centavos en el reporte mensual y es imposible
rastrear de dónde salieron. `BigDecimal` es correcto pero lento y ruidoso en Room.
Enteros en centavos es exacto, rápido y trivial de testear.

**3.2. Existe un tipo `Money` en `domain` que envuelve el `Long`.**
Nada de pasar `Long` pelado entre funciones de negocio. El tipo evita que alguien
sume centavos con unidades o con cantidades de stock.

**3.3. Formateo de moneda solo en la capa `ui`, nunca en `domain` ni `data`.**
Una sola función `Money.format()` en un único archivo. Si aparece un
`String.format("Q%.2f", ...)` regado por las pantallas, está mal.

**3.4. Redondeo: definido, documentado y testeado.**
El precio sugerido se redondea hacia arriba al múltiplo configurable (por defecto Q5).
Cualquier función que redondee lleva test con casos de borde.

**3.5. Ganancia, margen y markup son tres cosas distintas y se nombran distinto.**

```
ganancia = precioVenta - costo
markup   = ganancia / costo          // compré a 40, vendo a 100 -> 150%
margen   = ganancia / precioVenta    // el mismo caso -> 60%
```

En la UI nunca se muestra un porcentaje sin decir cuál es. Etiquetas en español:
"Ganancia", "Margen sobre venta", "Recargo sobre costo".

**3.6. Un porcentaje calculado (margen, recargo o cualquier otro) se
representa como `Int` en puntos básicos, nunca `Double`.**
1 punto básico = 0.01%, es decir `valor / 100` = el porcentaje con dos
decimales: `6000` = `60.00%`, `15000` = `150.00%`. Mismo razonamiento que
los centavos para dinero: un entero exacto, no un flotante que redondea
donde no se lo pediste. El formateo a texto (dividir por 100, mostrar el
`%` y los decimales) es de `ui`, igual que `Money.format()`; `domain` y
`data` nunca formatean. Ver `ESQUEMA.md` y D-013 en `DECISIONES.md`.

**3.7. Las ventas guardan snapshot de costo y precio.**
Cuando se registra una venta, se copian a la línea de venta: el nombre del producto,
su UID, su costo unitario y su precio de venta **en ese momento**.
Razón: si mañana ella sube el precio o corrige el costo, el historial de ganancias
del mes pasado NO debe cambiar. Un reporte que se reescribe solo es un reporte inútil.
Jamás calcules ganancias históricas haciendo JOIN al precio actual del producto.

---

## 4. Reglas de datos

- **Offline-first sin excepciones.** Room es la única fuente de verdad. Ninguna pantalla
  puede quedar bloqueada esperando red. La app debe funcionar completa en modo avión.
- **Sin cuentas, sin login, sin backend.** Fase 0 a 12 son 100% locales.
- **Nada se borra de verdad.** Los productos se archivan (`archived = true`), las ventas
  se anulan (`status = CANCELLED`). Razón: es un registro contable; borrar destruye
  el historial de ganancias y la usuaria no puede deshacerlo.
- **Migraciones Room: aditivas y versionadas.** Prohibido editar una migración ya
  commiteada. Prohibido `fallbackToDestructiveMigration()` incluso en debug.
  Si el esquema cambia, se agrega una migración nueva y su test.
- **`exportSchema = true`** y los JSON de esquema se commitean. Son la evidencia
  para auditar que nadie rompió la base.
- **Una consulta tiene que expresar TODA su condición, aunque parte sea
  hoy redundante.** Si una consulta responde "las ventas a crédito
  pendientes", pide `status = 'PENDING' AND type = 'CREDIT'` — aunque hoy
  la segunda condición no descarte ni una fila, porque ninguna venta de
  contado puede estar pendiente. Razón: **un invariante que se cumple en
  otro archivo no es una garantía, es una coincidencia con fecha de
  vencimiento.** Quien cambia el otro archivo no está leyendo esta
  consulta, y la consulta no se queja: sigue devolviendo filas, solo que
  las equivocadas. No hay error de compilación ni excepción que marque el
  momento en que dejó de ser correcta — aparece meses después como "a Doña
  María le figura una deuda que ya pagó". El costo de cumplir la regla es
  una condición de más en un `WHERE`. Visto en Fase 08 (D-050): las dos
  consultas de ventas pendientes filtraban solo por `status`, y al permitir
  que una venta de contado tenga `customer_id` eso habría puesto ventas ya
  cobradas en "¿Quién me debe?" **y** habría impedido archivar a cualquier
  clienta con ventas de contado (D-039 usa la misma consulta).
  **Corolario para los tests:** para probar que una consulta se defiende
  sola, el test tiene que construir el estado que el resto del sistema hoy
  impide — no el que el sistema produce. Un test del camino real pasa
  igual con la consulta rota.
- El esquema completo vive en `ESQUEMA.md`. **No inventes tablas ni columnas.**
  Si necesitás una que no está ahí, parás, lo proponés en `ESTADO.md` y esperás.

---

## 5. Reglas de código

- **Identificadores, nombres de archivo y mensajes de commit: en inglés.**
  **Los comentarios de código: en español**, con tildes correctas — los
  archivos son UTF-8, no hay problema de codificación. Razón: los
  comentarios explican decisiones del negocio (por qué el redondeo es
  hacia arriba, por qué una división puede devolver `null`, por qué un
  campo es opcional) para un equipo hispanohablante; forzarlos a inglés
  no ayuda a nadie acá y solo agrega fricción a releerlos. Ver D-016 en
  `DECISIONES.md`.
  **Todo texto visible por la usuaria: en español, y solo en `strings.xml`.**
  Prohibido hardcodear strings en Composables. Razón: permite revisar de un solo
  vistazo todo lo que ella va a leer, sin cazarlo por el código.
  **Las respuestas de Claude Code al humano, en esta conversación: en
  español.** No estaba definido y por defecto se contestó en inglés — el
  equipo (el humano y quien audite `ESTADO.md`) es hispanohablante, mismo
  motivo que los comentarios de código. Esto es sobre el chat, no sobre
  el código: identificadores, nombres de archivo y mensajes de commit
  siguen en inglés, sin cambios.
- `domain` no importa nada de Android. Ni `Context`, ni Room, ni Compose.
  Si `domain` necesita Android, el diseño está mal.
- Los ViewModels exponen un único `StateFlow<UiState>`. Nada de `LiveData`.
- Los Composables son `@Preview`-ables y no reciben ViewModels: reciben estado y lambdas.
- Un archivo por pantalla. Si un archivo pasa de ~300 líneas, se divide.
- **Todo formateo de un dato que se persiste o se imprime usa
  `Locale.ROOT`** (`String.format(Locale.ROOT, ...)`, nunca la sobrecarga sin
  locale). Ejemplo: el `uid` de producto (`XP-%06d`), cualquier código que
  termine en una etiqueta de barras, cualquier valor que se guarda en Room o
  se compara como clave. Razón: sin `Locale.ROOT`, en un teléfono con locale
  de dígitos no arábigos (ej. algunos locales árabes o de Asia) el mismo
  `String.format("XP-%06d", n)` puede producir dígitos no ASCII — y ese
  string es una clave única que además se imprime en un código de barras. El
  formateo pensado para que la usuaria lo lea (fechas, montos en pantalla) sí
  usa su locale — la distinción es persistido/impreso vs. mostrado en UI.
- **Prohibido escribir la secuencia literal `/*` dentro del texto de un
  comentario de bloque (`/** ... */` o `/* ... */`).** Kotlin permite
  comentarios de bloque **anidados** — a diferencia de Java/C. Un `/*`
  suelto dentro del texto de un KDoc (por ejemplo, mencionando un glob de
  archivos como `usecase/*.kt`) abre un segundo nivel de anidamiento que el
  `*/` de cierre normal del comentario no cierra del todo, así que el
  comentario sigue "abierto" y se traga código real después de él sin que
  el compilador lo señale con un error de sintaxis obvio. Visto en Fase 04
  (D-025 en `DECISIONES.md`): un `/*` así en el KDoc de `ProductRepository`
  hizo que esa clase, desde la perspectiva del frontend, dejara de existir
  como declaración real, y KSP/Hilt reportaba `'ProductRepository' could
  not be resolved` como si fuera un problema de dependencias o de versión
  de KSP — dos rondas de cambiar la versión de KSP no arreglaron nada,
  porque el código nunca fue el problema de versiones que parecía ser. Si
  necesitás mencionar un patrón de archivos en un comentario, escribilo sin
  que quede un `/` seguido de `*` (ejemplo: "los archivos de
  `domain/usecase`" en vez de "`domain/usecase/*.kt`").
- **Todo campo que capture dinero usa `MoneyTextField` y todo campo que
  capture una cantidad usa `QuantityTextField`, los dos en `ui/format/`.**
  Presentes y futuros. Prohibido construir un campo numérico local con
  `OutlinedTextField` + `KeyboardType.Number` o `KeyboardType.Decimal`,
  aunque sea "un solo campo simple": el mecanismo de esos componentes
  (`TextFieldValue`, y rechazar la tecla inválida sin nunca presentar un
  texto "corregido") es lo que arregló D-030, y un campo local no lo
  tiene. Si hace falta capturar un tipo numérico nuevo (un porcentaje,
  por ejemplo), se crea **un** componente compartido en `ui/format/` con
  el mismo mecanismo y se usa en todos lados, no uno por pantalla.
  Razón: este bug ya reapareció dos veces — la primera vez (D-030)
  porque el campo reformateaba lo que ella tecleaba, la segunda porque
  el arreglo se aplicó a los campos de dinero y quedaron tres campos de
  cantidad reimplementados a mano (`AddProductScreen`,
  `ProductEditScreen`, `RegisterPurchaseScreen`). Un componente
  compartido no puede quedarse a medio aplicar sin que se note. Ver
  D-048 en `DECISIONES.md`.
- **Toda lista de Compose (`items`, `itemsIndexed`) cuyas filas tengan
  estado de entrada propio lleva `key` estable, derivada del dato y no
  de la posición.** "Estado de entrada propio" es cualquier fila que
  contenga un campo de texto, un `remember` interno, un check, un
  desplegable abierto o un foco — en la práctica, toda fila con un
  `MoneyTextField` o un `QuantityTextField` adentro.
  **Qué pasa sin `key`:** Compose identifica cada fila por su
  **posición** en la lista, no por el dato que muestra. El `remember`
  interno del campo (su `TextFieldValue`: el texto *y* la posición del
  cursor) queda atado a esa posición. Al borrar o reordenar una fila,
  las que estaban más abajo se corren un lugar y **heredan el estado
  interno de la fila que ocupaba antes ese lugar**: el cursor salta, una
  tecla se pierde, y con dos campos por fila el valor de una pieza
  aparece en la pieza equivocada. El texto se re-sincroniza solo cuando
  el estado está izado al ViewModel, así que **el síntoma no es un
  número visiblemente mal, es un campo que "se porta raro" mientras
  ella escribe** — y eso es exactamente lo que no se puede reproducir
  leyendo el diff. Es un bug invisible en revisión de código: solo
  aparece usando la app, con las manos, borrando una línea del medio.
  Visto en Fase 06 y encontrado recién en la ronda de corrección de
  uso real (`itemsIndexed(state.lines)` en `RegisterSaleScreen` era la
  única lista de la app sin `key`). Ver D-048 en `DECISIONES.md`.
- **Dos `items`/`itemsIndexed` distintos dentro del MISMO `LazyColumn` o
  `LazyRow` comparten un solo espacio de claves.** No son dos listas con
  claves independientes: para Compose es una sola lista perezosa con
  varias secciones, y **una clave repetida entre secciones es un crash**
  (`IllegalArgumentException: Key "<x>" was already used`), no un aviso.
  Esto no es obvio y va a volver a aparecer apenas una pantalla tenga dos
  secciones sobre el mismo tipo de dato — un carrito y un catálogo, una
  lista de pendientes y una de hechas, "recientes" y "todas" — porque el
  caso natural es justo ese: **la misma entidad aparece en las dos
  secciones, y su `id` es la clave obvia en ambas.** Por eso la clave
  lleva **prefijo por sección**, siempre, aunque hoy haya una sola:
  `"cart-${line.productId}"` y `"product-${it.id}"`, no `line.productId`
  e `it.id` pelados. Agregar la segunda sección después no debería
  obligar a acordarse de esto. Lo mismo vale para un `item { }` suelto si
  alguna vez lleva `key`. Encontrado así: el `key` que se agregó para
  cumplir la regla anterior rompió la app al agregar la primera pieza al
  carrito — el compilador y los 188 tests unitarios lo dieron por bueno.
  Ver D-048 en `DECISIONES.md`.
- Nada de `!!`. Nada de `runBlocking` fuera de tests. Nada de `GlobalScope`.
- Nada de `TODO()` ni stubs vacíos que compilen y mientan. Si algo no se implementó
  en esta fase, no existe en el código todavía.

---

## 6. Reglas de UI para la usuaria final

Estas son requisitos, no sugerencias estéticas.

- **Registrar una pieza nueva: máximo 4 taps y menos de 20 segundos.**
  Pantalla de alta rápida: foto, costo, precio y categoría (D-026). Todo lo
  demás (nombre, cantidad, notas) es opcional, visible sin necesidad de
  expandir nada, y se edita después. Si el formulario de alta pide más de
  4 campos obligatorios, está mal.
- La pantalla de inicio tiene dos acciones grandes y obvias: **Vender** y **Agregar pieza**.
  No es un dashboard de métricas.
- Tamaño mínimo de texto de cuerpo: 18sp. Área táctil mínima: 56dp.
- **El piso de 56dp está fijado en el tema, en `ui/theme/TouchTargetFloor.kt`,
  y `JoyeriaTheme` lo aplica a toda la app.** Son dos valores y los dos hacen
  falta: `LocalMinimumInteractiveComponentSize` (el espacio que ocupa el
  componente; Material 3 trae 48dp) y `minimumTouchTargetSize` de
  `ViewConfiguration` (hasta dónde Compose extiende la zona que de verdad
  responde al toque; también 48dp). Con solo el primero, el componente se ve
  de 56dp pero el anillo de afuera de los 48dp no responde. Reglas:
  - **Nadie lo pisa con un `Modifier.size`, `height` ni `width` local
    pensando que ayuda.** El piso ya está; un tamaño fijo local solo lo
    deshace con letra grande (el texto se recorta en vez de crecer) o lo
    esconde. Si un elemento con texto necesita un alto, es `heightIn(min = ...)`.
    Tampoco se vuelve a proveer `LocalMinimumInteractiveComponentSize` con otro
    valor en una pantalla.
  - **Un diálogo y un menú desplegable son otra ventana** y reinician
    `ViewConfiguration`: pierden el segundo valor. Por eso se usa
    `JoyeriaAlertDialog` y `JoyeriaDropdownMenuItem` (en `TouchTargetFloor.kt`),
    **nunca `AlertDialog` ni `DropdownMenuItem` directo**. Un diálogo nuevo de
    otro tipo (`DatePickerDialog`, por ejemplo) envuelve sus botones en
    `TouchTargetFloor { }`.
  - **Componentes de Material con alto fijo que el tema no sube:** `Tab`
    (48dp). Se le pasa `Modifier.heightIn(min = MinTouchTarget)`. Si aparece
    otro componente así, se mide primero (ver abajo) y se hace lo mismo.
  - **Única excepción: el calendario** (`DatePicker`, grilla y encabezado, en
    `RegisterPurchaseScreen`). La grilla tiene 7 columnas; 7 celdas de 56dp
    son 392dp y no caben en un teléfono de 360dp (la columna del sábado
    quedaba fuera de la pantalla). El `DatePicker` vuelve a los 48dp de
    Material; sus botones «Cancelar» y «Aceptar» sí miden 56dp. Cualquier otro
    componente de varias columnas con el mismo problema se trata igual, y se
    anota acá.
  - **Se verifica midiendo, no a ojo:** `uiautomator dump` y ningún nodo
    `clickable` por debajo de 56dp (en px: 56dp x la densidad del emulador).
- **El piso de 18sp para texto también está fijado en el tema** (`Type.kt`):
  `bodyLarge`, `bodyMedium`, `labelLarge` (texto de todos los botones) y
  `titleSmall` (texto de las pestañas). Un componente de Material que escribe
  en otro estilo hereda su tamaño por defecto, que puede ser menor: si se ve
  texto chico, se mide el nodo de texto y se corrige el estilo en el tema, no
  en la pantalla. **Excepción decidida:** las etiquetas flotantes de los
  campos (`labelSmall`/`labelMedium`, ~11-12sp) quedan como están: son texto
  secundario junto a un campo cuyo contenido sí está en 18sp, y tocarlas puede
  afectar a los campos de dinero.
- La app respeta el tamaño de fuente del sistema (nada de `sp` fijos que ignoren accesibilidad).
- Toda acción destructiva o irreversible pide confirmación con texto claro en español,
  diciendo exactamente qué va a pasar.
- Cero jerga. "Inventario" sí, "SKU" no. "Código de la pieza" sí, "UID" no.
- Los montos siempre con `Q` visible.

---

## 7. Flujo de trabajo obligatorio

1. Trabajás **una fase a la vez**, en el orden de `FASES.md`.
2. Antes de escribir código: releés la fase completa y confirmás en `ESTADO.md`
   qué vas a hacer.
3. Solo tocás los archivos listados en "Archivos permitidos" de la fase.
   Si necesitás tocar otro, parás y lo anotás en `ESTADO.md` como bloqueo.
4. Cumplís **todos** los criterios de aceptación y los verificás con los comandos indicados.
5. Actualizás `ESTADO.md` (qué hiciste, supuestos, pendientes).
6. Agregás una entrada a `DECISIONES.md` si tomaste alguna decisión de diseño.
7. Commit con el mensaje exacto de la fase y tag.
8. **Parás. No empezás la siguiente fase sin autorización del humano.**
9. **Todo análisis, resumen o hallazgo que me pidas lo escribís en un `.md`
   del repo antes de imprimirlo en consola. La consola se pierde, el archivo
   se audita.** El destino por defecto es **`ESTADO.md`**, que es la bitácora
   única: abrir un `.md` nuevo por cada análisis dispersa el registro y
   vuelve imposible la auditoría descrita al final de esta sección (un modelo
   leyendo `ESTADO.md` y el diff del tag).
   **Única excepción: un documento que sea una política permanente, que se
   consulta sola y es independiente de la fase en que se escribió.** Esos van
   en `docs/` (ej. `docs/PERMISOS.md`), no en la raíz y no embutidos en
   `ESTADO.md`. La prueba para distinguir: si dentro de tres meses alguien lo
   va a abrir a propósito para saber **cómo se decide** algo, es política y va
   en `docs/`; si es el registro de **qué pasó** en una fase, va en `ESTADO.md`.
   Un análisis puntual que no pasa esa prueba no estrena archivo.
10. **Logs y capturas de una verificación manual (emulador, dispositivo)
    se guardan en `verification/screenshots/` y `verification/logs/`, en la
    raíz del proyecto — nunca dentro de `build/`, nunca en `%TEMP%` ni en un
    directorio fuera del árbol de trabajo.** Quedan fuera del repo porque
    `.gitignore` tiene su propia entrada (`/verification/`) y no chocan con el
    bloqueo de lecturas fuera del árbol de trabajo. `ESTADO.md` referencia la
    ruta relativa de cada archivo, no la pega entera ni la deja solo en la
    consola.
    **Por qué nunca dentro de `build/`:** que algo viva ahí es lo que lo hace
    «ignorado por git», y es exactamente la misma propiedad que lo hace
    borrable — `./gradlew clean` borra todo `build/` y git no tiene copia que
    devolver. Pasó el 2026-10-19: un `clean` se llevó todas las capturas y
    logs de las fases 03 a 11 y dejó 38 rutas de `ESTADO.md` apuntando a
    nada. `clean` es un comando normal y se puede correr; lo que no puede
    pasar es que borre evidencia. Como una carpeta ignorada por git no existe
    en un clon nuevo, quien verifique la crea con
    `mkdir -p verification/screenshots verification/logs`.
    **Antes de cerrar una fase hay que abrir cada pantalla nueva que
    tenga una lista y ejercitarla de verdad: agregar un elemento, quitar
    uno, salir y volver a entrar.** No alcanza con que compile y con que
    la suite pase. Razón: dos fases seguidas tiraron la app por una clave
    de `LazyColumn`, con causas **distintas** — una clave repetida entre
    dos secciones del mismo `LazyColumn` (D-048) y una clave que quedó
    como texto literal en vez de interpolarse (Fase 08) — y las dos veces
    el compilador y la suite completa dieron verde. No es casualidad:
    una clave es un `String` cualquiera, así que ningún tipo la valida, y
    el error solo existe en tiempo de composición, con datos reales, al
    dibujar la segunda fila. La única forma de verlas es tocar la
    pantalla.
11. **Una medición de tiempo o performance tomada en un entorno con
    contención de recursos real (build corriendo en paralelo, ANRs de
    otras apps en el emulador, CPU/host saturados) no es una medición
    válida — se descarta y se repite, aunque el resultado haya sido
    favorable.** Antes de cronometrar algo, confirmá que el entorno está
    en reposo (ej. `adb shell top` mostrando el sistema ocioso, no un
    build de Gradle corriendo al mismo tiempo). Ver `ESTADO.md`, fix de
    usabilidad de alta rápida (2026-09-17): una corrida de 16.60s
    coincidió con un ANR real del emulador por contención de CPU y se
    descartó; esa corrida habría pasado el criterio de 20 segundos de
    `FASES.md` sin que nadie se enterara de que el número no significaba
    nada. Un resultado que "pasa" el criterio por casualidad, tomado en
    condiciones degradadas, no es evidencia — es ruido que coincidió con
    lo esperado.
12. **Todo build se corre con la herramienta `Bash` y `./gradlew`. No con
    PowerShell.** Razón: las reglas de permisos de
    `.claude/settings.local.json` usan el prefijo `Bash(./gradlew:*)`, que
    aplica únicamente a esa herramienta. El mismo comando lanzado por
    PowerShell pide aprobación manual una por una, y la sintaxis del prefijo
    `PowerShell(...)` **no está verificada en este entorno**. Si algún día un
    build necesita PowerShell de verdad, se verifica primero con
    `/permissions`, se registra la decisión en `DECISIONES.md` y recién
    entonces se usa — no se asume que el prefijo funciona. Esto deja de
    depender de la costumbre: antes solo estaba implícito en que los comandos
    de `FASES.md` se escriben como `./gradlew`. Ver `docs/PERMISOS.md`.
13. **Toda verificación manual va en el emulador (`Medium_Phone_API_35`),
    nunca en el teléfono físico de la usuaria**, que tiene datos reales.
    Verificar es instalar, abrir, tocar, agregar, archivar y anular: todo eso
    escribe en la base. Si `adb devices` lista un teléfono físico además del
    emulador, **todo `adb` lleva `-s emulator-5554`** (o el serial del
    emulador); nunca `adb` sin `-s` cuando hay más de un dispositivo, y nunca
    se instala, se limpia ni se abre la app en el físico. **Lo mismo vale para
    Gradle:** `./gradlew connectedDebugAndroidTest` corre en TODOS los
    dispositivos conectados y, al terminar, **desinstala la app, lo que borra
    sus datos** en cada uno. Con un teléfono físico conectado se corre siempre
    con `ANDROID_SERIAL=emulator-5554` (el emulador, nunca el de ella).
    Razón de todo esto: una prueba que agrega y anula ventas en el teléfono de
    ella deja datos de prueba mezclados con su cuaderno, y no hay forma de
    separarlos después — nada se borra de verdad (sección 4). Si hace falta
    ver algo en el teléfono real, lo pide el humano y lo hace él.
    **Antes de cualquier tarea instrumentada** (`connected*AndroidTest`, o
    cualquier cosa que instale o desinstale la app) **se corre `adb devices`.
    Si aparece un dispositivo físico (cualquier serial que no empiece con
    `emulator-`), se para y se le avisa al humano, aunque `ANDROID_SERIAL`
    esté puesto.** La variable es un candado que se puede no heredar (un shell
    nuevo, otro proceso, un `export` olvidado) y fallar en silencio, y lo que
    protege no tiene vuelta atrás. Mirar la lista de dispositivos es lo único
    que no depende de acordarse de nada.

### Git

- Rama por fase: `fase/NN-slug`
- Commit: `fase-NN: descripción corta en inglés`
- Merge a `main` y tag: `fase-NN-ok`
- Un commit por fase. Si necesitás más, que cada uno compile y pase tests.

Razón de este ritual: el humano audita con un modelo más caro leyendo únicamente
`ESTADO.md` y el diff del tag. Si mezclás fases o hacés commits gigantes, la auditoría
se vuelve imposible y el proyecto se cae.

---

## 8. Tests

- Fases marcadas **[TESTS OBLIGATORIOS]** en `FASES.md` no se cierran sin tests que pasen.
- Toda función que toque dinero lleva test unitario con casos de borde:
  costo cero, precio menor al costo, cantidades grandes, redondeo justo en el límite.
- Los DAOs se testean con Room in-memory.
- **Robolectric es solo para tests de Room/DAO** (necesitan un `Context` de
  Android que la JVM pura no tiene). Prohibido usarlo en `domain/` — los
  tests de `Money`/`PricingCalculator` (Fase 02) y cualquier otro test de
  lógica pura son aritmética sin Android y tienen que correr en milisegundos,
  como JUnit4 normal. Si un test de `domain` necesita Robolectric o cualquier
  runner que no sea `JUnit4` puro, el diseño está mal: `domain` no importa
  Android (sección 5).
- `app/build.gradle.kts` con Robolectric lleva
  `testOptions { unitTests { isIncludeAndroidResources = true } }`. No es
  opcional: sin eso Robolectric no resuelve el manifest ni los recursos y
  falla con errores confusos en vez de un mensaje claro.
- Comando de verificación: `./gradlew testDebugUnitTest`
- Un test que no puede fallar no es un test. Prohibido `assertTrue(true)` y similares.
- **El nombre de un test es una afirmación sobre lo que cubre, y vale
  tanto como el cuerpo.** Quien audita, y quien decide si hace falta un
  test nuevo, lee la lista de nombres — no los cuerpos uno por uno. Un
  nombre que promete más de lo que el cuerpo ejercita es **peor que no
  tener el test**: aparenta una red que no existe, y hace que nadie
  escriba la que falta.
  - **Si el nombre menciona dos ramas, el cuerpo tiene que ejercitar las
    dos.** `zeroOrNegative` prueba el cero **y** el negativo;
    `nameOrUid` prueba el nombre **y** el código; `xAndY` asegura `x`
    **y** `y`. Si solo se prueba una, el nombre lleva solo esa.
  - **Si el nombre afirma una propiedad general** (`siempre`, `nunca`,
    `cualquiera`, `todos`, `son inversas`, `ignora X`), un solo ejemplo
    no alcanza: o el cuerpo cubre los casos que hacen general a la
    afirmación, o el nombre se baja al caso concreto que sí prueba.
  - **Si el nombre dice que algo se ignora, el cuerpo tiene que poner ese
    algo presente** y mostrar que no cambia el resultado. Dejarlo en su
    valor por defecto no prueba que se ignore: prueba que funciona
    cuando no está.
  Razón: en la ronda de corrección de uso real (2026-10-10) apareció
  `availableProducts_filtersByNameOrUid_caseInsensitive`, que ejercitaba
  **solo la rama del nombre**. La búsqueda por código — la que la usuaria
  de verdad usa — estaba sin ninguna red, y el nombre del test era
  exactamente la razón por la que nadie lo había notado. La auditoría que
  siguió encontró que no era un caso aislado (ver `ESTADO.md`, auditoría
  de nombres de test). **Al renombrar o al partir un test, preferí partir
  en dos tests antes que acortar el nombre:** el nombre acortado es
  honesto, pero la rama que se cae del nombre se cae también de la
  cobertura y nadie se entera.

---

## 9. Prohibiciones globales

- ❌ `Float`/`Double` para dinero
- ❌ Borrado físico de registros
- ❌ Strings de UI fuera de `strings.xml`
- ❌ Dependencias sin pinear o sin justificar en `DECISIONES.md`
- ❌ `fallbackToDestructiveMigration()`
- ❌ Editar migraciones ya commiteadas
- ❌ Adelantar trabajo de fases futuras "porque ya que estamos"
- ❌ Inventar campos o tablas que no están en `ESQUEMA.md`
- ❌ Backend, red, analytics, crash reporting, publicidad o telemetría de cualquier tipo
- ❌ Pedir permisos de Android que no se usen en la fase actual

---

## 10. Cuando tengas dudas

No adivinés. Escribí la duda en `ESTADO.md` bajo `## Bloqueos` y detené la fase.
Una fase detenida con una pregunta clara vale más que una fase terminada con una
suposición equivocada enterrada en el código.

**El silencio nunca es aprobación.** Si dejás un bloqueo escrito y no recibís
respuesta a ese bloqueo puntual, no avanzás asumiendo que "no decir nada" es un
sí — volvés a preguntar y esperás. Un bloqueo que asumiste aprobado porque no
hubo respuesta es indistinguible, para quien audite después, de un bloqueo que
nunca se leyó. Esto aplica bloqueo por bloqueo: si de varios que dejaste
planteados el humano responde algunos y otros no, los respondidos se
resuelven con esa respuesta y los demás siguen deteniendo la fase igual.
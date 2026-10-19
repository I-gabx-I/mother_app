# FASES.md — Plan de ejecución

Una fase a la vez. Al cerrar cada fase: actualizar `ESTADO.md`, commit, tag, **parar**.
No se empieza la siguiente sin autorización humana explícita.

Comandos de verificación estándar:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Leyenda:
- **[TESTS OBLIGATORIOS]** — la fase no se cierra sin tests nuevos que pasen.
- **Archivos permitidos** — lista cerrada. Tocar algo fuera de ella es motivo de parada.

MVP = fases 00 a 06. Con eso la usuaria ya puede dejar el cuaderno.

---

## Fase 00 — Andamiaje del proyecto

**Objetivo:** proyecto Android compilable, con estructura de paquetes, DI y tema, sin ninguna funcionalidad.

**Archivos permitidos:** `settings.gradle.kts`, `build.gradle.kts` (raíz y `app`), `gradle/libs.versions.toml`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/**/JoyeriaApp.kt`, `app/src/main/java/**/ui/theme/**`, `app/src/main/java/**/MainActivity.kt`, `app/src/main/res/values/strings.xml`, `.gitignore`, `README.md`

**Entregable:**
- Estructura `data/` `domain/` `ui/` creada (vacía pero con los paquetes).
- Hilt configurado con `@HiltAndroidApp`.
- Tema Material 3 con tipografía de cuerpo a 18sp mínimo.
- `MainActivity` muestra una pantalla vacía con el nombre de la app.

**Criterios de aceptación:**
1. `./gradlew assembleDebug` termina sin error ni warning nuevo.
2. `gradle/libs.versions.toml` existe y **ninguna** versión usa `+` o `latest`.
3. `grep -r "Double\|Float" app/src/main` no devuelve nada.
4. La app instala y abre sin crash.

**Prohibido:** crear entidades, DAOs, pantallas o lógica de negocio.

**Commit:** `fase-00: project scaffolding` → tag `fase-00-ok`

---

## Fase 01 — Capa de datos núcleo **[TESTS OBLIGATORIOS]**

**Objetivo:** base de datos Room versión 1 con **todas** las tablas de
`ESQUEMA.md` (`category`, `product`, `price_history`, `purchase`,
`purchase_item`, `customer`, `sale`, `sale_item`, `payment`, `app_setting`).
Ver D-011 en `DECISIONES.md`: todas las tablas se crean acá aunque su DAO y su
UI lleguen en fases posteriores; esto reemplaza el reparto original de tablas
fase por fase.

**Archivos permitidos:** `app/src/main/java/**/data/local/**`, `app/src/main/java/**/di/DatabaseModule.kt`, `app/src/test/java/**/data/**`, `app/schemas/**`, `gradle/libs.versions.toml`, `app/build.gradle.kts`

**Entregable:**
- Entidades de **todas** las tablas de `ESQUEMA.md`, y `AppDatabase` con todas
  ellas registradas, exactamente como los define `ESQUEMA.md`.
- DAOs solo para `category`, `product` y `app_setting` (las que usa esta
  fase). Las demás tablas quedan con su entidad declarada, sin DAO, hasta la
  fase que las use.
- `exportSchema = true`, JSON de esquema commiteado.
- Semilla de las 5 categorías y de las claves de `app_setting` en la primera apertura.
- Generador de `uid` basado en `next_product_uid_seq`, transaccional.

**Criterios de aceptación:**
1. `./gradlew testDebugUnitTest` pasa.
2. Existen tests de DAO con Room in-memory para insertar, actualizar, archivar y consultar producto.
3. Existe test que verifica que dos productos creados en paralelo **nunca** reciben el mismo `uid`.
4. Existe `app/schemas/gt.marcos.joyeria.data.local.AppDatabase/1.json`
   commiteado (esa es la ruta real que genera Room, no `app/schemas/1.json`),
   con las diez tablas.
5. `grep -r "fallbackToDestructiveMigration" app/src` no devuelve nada.

**Prohibido:** cualquier Composable. Esta fase no tiene UI. Prohibido escribir
DAO o lógica de negocio para las tablas que no usa esta fase (`price_history`,
`purchase`, `purchase_item`, `customer`, `sale`, `sale_item`, `payment`) —
solo se declara su entidad.

**Commit:** `fase-01: room core schema` → tag `fase-01-ok`

---

## Fase 02 — Motor de dinero y precios **[TESTS OBLIGATORIOS]**

**Objetivo:** toda la aritmética del negocio, pura, sin Android, sin UI.

**Archivos permitidos:** `app/src/main/java/**/domain/model/Money.kt`, `app/src/main/java/**/domain/pricing/**`, `app/src/test/java/**/domain/**`

**Entregable:**
- `Money` (value class sobre `Long` en centavos) con suma, resta, multiplicación por `Int` y comparación.
  **Sin `format()`** — CLAUDE.md 3.3 prohíbe formatear moneda fuera de `ui`; `Money` en esta fase es
  aritmética pura. El formateo llega en la Fase 03, en `ui`.
- `PricingCalculator` con: `profit`, `marginOnSale`, `markupOnCost`,
  `suggestedPrice(cost, markupBp, roundingStep) = cost + cost * markupBp / 10000`.
  `markupBp: Int` en puntos básicos, la misma unidad y el mismo significado que devuelve
  `markupOnCost` (recargo real sobre costo, D-014) y que usa `default_markup_bp` de
  `app_setting` (`10000` = recargo del 100% = precio costo × 2). `marginOnSale` y
  `markupOnCost` devuelven puntos básicos (`Int`), nunca `Double` — ver `ESQUEMA.md`
  y D-013/D-014 en `DECISIONES.md`.
- Manejo explícito del caso `cost = 0` (resultado definido, sin división entre cero).

**Criterios de aceptación:**
1. `./gradlew testDebugUnitTest` pasa con al menos 20 tests nuevos.
2. Test verifica el ejemplo canónico: costo Q40, venta Q100 → ganancia Q60, margen 6000 (60.00%), recargo 15000 (150.00%).
3. Tests de redondeo en los límites: Q71 → Q75, Q75 → Q75, Q76 → Q80 con paso de Q5.
4. Test de costo cero, de precio de venta cero, y de precio menor al costo
   (ganancia negativa permitida y correcta). Las dos divisiones entre cero
   posibles (markupOnCost divide entre el costo, marginOnSale divide entre el
   precio de venta) devuelven un resultado explícito, sin crash ni NaN.
5. `suggestedPrice` y `markupOnCost` son inversas para un par (costo, precio): pasarle a
   `suggestedPrice` el `markupBp` que devolvió `markupOnCost(costo, precio)` reproduce ese
   mismo precio, sin redondeo de por medio (`roundingStep` que no cambie el resultado, ej. 1).
   Ejemplo mínimo: costo Q40, precio Q100 → `markupOnCost` da `15000` → `suggestedPrice(4000, 15000, 1)`
   da `10000` centavos (Q100) de vuelta.
6. `grep -rn "Double\|Float\|BigDecimal" app/src/main/java/gt/marcos/joyeria/domain | grep -vE "^[^:]+:[0-9]+: *(\*|//|/\*)"` no devuelve nada.
   (Ruta literal, no `**/domain`: sin `globstar` un shell no expande `**` de forma recursiva, y el
   comando queda o vacío o roto — el criterio "pasa" sin haber revisado nada. El segundo `grep`
   descarta líneas de comentario — bloque KDoc multilínea con continuación ` * `, KDoc de una
   sola línea que empieza con `/**`, y comentario `//` de línea completa — para que una mención
   de estas palabras explicando por qué no se usan, dentro de un comentario, no cuente como una
   violación; los comentarios de código van en español desde D-016, y "nunca uses `Double` acá"
   es exactamente ese caso. No cubre un comentario al final de una línea de código real, ej.
   `val x = 1 // menciona Double acá`: si la palabra aparece solo ahí, el criterio la marca
   igual. No es un caso usado hoy en `domain/`.)
7. Ningún archivo de `domain/` importa `android.*`.

**Prohibido:** UI, Room, Hilt.

**Commit:** `fase-02: money and pricing engine` → tag `fase-02-ok`

---

## Fase 03 — Alta rápida de pieza

**Objetivo:** la pantalla que decide si la app se usa o no. Registrar una pieza en 3 taps.

**Archivos permitidos:** `app/src/main/java/**/ui/product/add/**`, `app/src/main/java/**/ui/format/MoneyFormat.kt`, `app/src/main/java/**/domain/usecase/AddProduct*.kt`, `app/src/main/java/**/data/repository/ProductRepository*.kt`, `app/src/main/java/**/data/repository/AppSettingRepository.kt`, `app/src/main/java/**/util/ImageStorage.kt`, `app/src/main/java/**/MainActivity.kt` (solo para conectar esta pantalla: anotarla `@AndroidEntryPoint`, obtener el ViewModel con `by viewModels()`, y mostrar la pantalla de esta fase en vez de `HomePlaceholder` — nada más, ninguna otra lógica), `app/src/test/java/**/ui/product/add/MoneyDigitsInputTest.kt`, `app/src/test/java/**/util/ImageStorageScalingTest.kt` (las dos únicas excepciones a que esta fase no lleva tests: son funciones puras, sin Robolectric — ver más abajo), `app/src/main/res/values/strings.xml`, `AndroidManifest.xml` (solo permiso de cámara)

**Entregable:**
- `Money.format()` (única función de formateo de moneda, en `ui/format/MoneyFormat.kt`, per CLAUDE.md 3.3).
  `domain` y `data` siguen sin formatear nada.
- Captura de foto con CameraX, comprimida a JPEG ≤ 1MB y lado mayor ≤ 1600px, guardada en almacenamiento interno.
- Formulario con **solo 3 campos obligatorios**: foto, costo, precio de venta. Nombre, categoría, cantidad y notas son opcionales con valores por defecto sensatos.
- Mientras escribe el costo, la app muestra en vivo el precio sugerido y la ganancia.
- El `uid` se genera y se muestra al guardar.
- El campo de costo/precio interpreta lo que ella escribe como centavos
  (dígito a dígito, sin punto decimal que tipear ni ambigüedad de locale)
  y la lógica de esa interpretación vive en una función pura, separada del
  Composable, testeable sin Robolectric.
  **Reemplazado — ver D-030 en `DECISIONES.md`:** este patrón de "buffer
  de dígitos" (`MoneyDigitsField`) se probó con la usuaria real y causó
  un error de captura silencioso (el cursor caía en posiciones que no
  correspondían a lo que ella veía en pantalla). Desde el fix
  `money-field-plain-decimal`, el campo es un texto decimal normal con
  filtro de entrada (`MoneyTextField`, `ui/format/`), no un buffer de
  dígitos. No se reescribe el resto de esta fase, ya cerrada y tageada.
- La rotación de la foto (`imageInfo.rotationDegrees` de CameraX) se aplica
  antes de comprimir. Se confirma **visualmente** con una captura real
  (no alcanza con el tamaño del archivo): una foto tomada en vertical se
  guarda en vertical.

**Criterios de aceptación:**
1. `./gradlew assembleDebug` y `testDebugUnitTest` pasan.
2. Contar los campos `required` del formulario: exactamente 3.
3. Ningún string literal en los Composables (`grep` de comillas dobles en `ui/product/add` solo debe dar recursos, logs o claves técnicas).
4. Prueba manual documentada en `ESTADO.md`, hecha por **la usuaria final**
   (no el equipo de desarrollo — quien construyó la pantalla no puede medir
   su propia curva de aprendizaje) en un teléfono real, con cronómetro.
   Dos mediciones, las dos documentadas: la primera vez que ve la pantalla,
   y una segunda después de registrar 2-3 piezas más. **El criterio se
   evalúa sobre la segunda medición** (el uso real es repetido, no una vez);
   si esa segunda medición pasa de 20s, la fase no se cierra. Si la primera
   medición es mucho peor que la segunda, se anota igual — es una señal de
   que la pantalla no se explica sola, aunque no bloquee el cierre.
5. La foto guardada pesa menos de 1MB (verificado y anotado). Test de la
   función pura de redimensión: un lado mayor de 4000px con aspecto 4:3 da
   exactamente 1600 y su proporción correcta.

**Prohibido:** listados, edición, ventas.

**Commit:** `fase-03: quick product capture` → tag `fase-03-ok`

---

## Fase 04 — Inventario y edición

**Objetivo:** ver, buscar y editar el inventario existente.
La tabla `price_history` ya existe desde la Fase 01 (D-011); esta fase
agrega su DAO, repositorio y pantallas. `purchase`/`purchase_item`
(compras a mayorista) se separaron a la Fase 05 — ver **D-022** en
`DECISIONES.md`: el único criterio `[TESTS OBLIGATORIOS]` de la fase
original combinada estaba en el prorrateo, señal de que compras pesaba
como fase aparte.

**Archivos permitidos:** `app/src/main/java/**/ui/product/**`,
`app/src/main/java/**/ui/navigation/**`,
`app/src/main/java/**/domain/usecase/**`, `app/src/main/java/**/data/**`,
`app/src/main/java/**/data/repository/CategoryRepository.kt`,
`app/src/main/java/**/di/DatabaseModule.kt`,
`app/src/main/java/**/MainActivity.kt`, `gradle/libs.versions.toml`,
`app/build.gradle.kts`, `strings.xml`

**Entregable:**
- Listado con foto, nombre, `uid`, stock, precio y ganancia; búsqueda por nombre o `uid`; filtro por categoría.
- Pantalla de detalle/edición. Al cambiar costo o precio se inserta fila en `price_history`.
- Archivar pieza (no borrar).
- **Selector de categoría también en la pantalla de alta rápida** (Fase 03,
  `ui/product/add/**`), no solo en la de edición — Fase 03 lo dejó afuera a
  propósito (D-021 en `DECISIONES.md`) porque necesitaba `CategoryRepository`,
  que no estaba en sus archivos permitidos. Se puede diferir sin costo: la
  usuaria no empieza a usar la app hasta después de la Fase 06 (fin del
  MVP), y esta fase (04) llega antes, así que nunca va a existir un
  inventario cargado sin categorías por esta demora.
- Navegación entre pantallas (listado ↔ detalle/edición ↔ alta rápida) y
  una pantalla de inicio provisoria con las dos acciones que existen hoy
  ("Agregar pieza", "Inventario") — desviación temporal explícita de
  CLAUDE.md sección 6 (que pide "Vender" y "Agregar pieza"), registrada
  con fecha de vencimiento en `DECISIONES.md`: se corrige en la Fase 06
  (Venta de contado), cuando "Vender" exista de verdad.

**Criterios de aceptación:**
1. Build y tests pasan.
2. Test de que editar el precio de un producto inserta en `price_history`.
3. No existe ningún `DELETE FROM product` en el código.
4. Navegar listado → detalle → volver, y alta rápida → volver, sin crash (verificado a mano).

**Prohibido:** registrar compras, registrar ventas.

**Commit:** `fase-04: inventory management and editing` → tag `fase-04-ok`

---

## Fase 05 — Compras a mayorista **[TESTS OBLIGATORIOS]**

**Objetivo:** registrar compras a mayorista local, con líneas y prorrateo
opcional de transporte. Las tablas `purchase` y `purchase_item` ya
existen desde la Fase 01 (D-011); esta fase agrega su DAO, repositorio y
pantallas. Separada de Inventario (Fase 04) — ver **D-022** en
`DECISIONES.md`.

**Efecto de una compra sobre `product.cost_cents` — D-029, ya
decidido:** regla híbrida, una sola fórmula de promedio ponderado por
cantidad:

```
totalActual = stock_qty_actual × cost_cents_actual
totalLinea  = qty_comprada × unit_cost_cents + allocated_extra_cents   // costo real total de la línea, ya prorrateada, sin redondeos intermedios
nuevoCosto  = ceilDiv(totalActual + totalLinea, stock_qty_actual + qty_comprada)
```

Cuando `stock_qty_actual = 0`, esta misma fórmula ya da exactamente el
costo de la compra nueva (no es una rama de código aparte, ver D-029)
— pero igual lleva un test dedicado que confirme ese caso explícitamente
(criterio 4 abajo). `ceilDiv` redondea hacia arriba cuando la división
no es exacta (mismo espíritu conservador que el redondeo de
`suggestedPrice`, D-015): un centavo de más en el costo nunca hace que
se muestre más ganancia de la real, un centavo de menos sí podría.

**Archivos permitidos:** `app/src/main/java/**/ui/purchase/**`,
`app/src/main/java/**/domain/usecase/*Purchase*.kt`,
`app/src/main/java/**/domain/pricing/PricingCalculator.kt` (agregado:
la fórmula de arriba es aritmética pura de dinero, mismo tipo de
función que `profit`/`suggestedPrice` — no le corresponde a
`usecase/*Purchase*.kt`, que es orquestación, no cálculo puro),
`app/src/main/java/**/data/**` (incluye `data/local/AppDatabase.kt`
para `MIGRATION_1_2`, y `data/local/dao/SaleDao.kt` con el único
método que necesita D-032 — ver esa decisión sobre por qué esto no
adelanta el resto de Fase 06), `app/src/main/java/**/ui/navigation/**`
(faltaba en la lista original: hace falta un destino nuevo en el
`NavHost` y un punto de entrada real desde `HomeScreen` para poder
llegar a la pantalla de compra — corrección de alcance, no una
decisión de diseño, mismo criterio que los dos ítems de test de abajo),
`app/schemas/**` (nuevo: `2.json`, primera migración real — D-033),
`app/src/test/java/**/domain/**`, `app/src/test/java/**/data/**` (los
dos últimos faltaban en la lista original — sin ellos no hay dónde
escribir los tests que esta misma fase exige con
`[TESTS OBLIGATORIOS]`; corrección de alcance, no una decisión de
diseño), `app/src/androidTest/java/**/data/**` (nuevo: el test de
`MIGRATION_1_2` no puede correr bajo Robolectric — D-034 — así que vive
acá, no en `app/src/test`), `gradle/libs.versions.toml`,
`app/build.gradle.kts` (nuevo: `androidx.room:room-testing` para
`MigrationTestHelper`, D-033; fuerza de versión de
`kotlinx-serialization` acotada a `androidTest`, D-034), `strings.xml`

**Entregable:**
- Registro de compra con líneas, y prorrateo opcional de transporte
  local según `ESQUEMA.md`.
- Al confirmar una compra, en una sola transacción: inserta `purchase` +
  `purchase_item`(s), recalcula `product.cost_cents` con la fórmula de
  arriba, suma `qty` a `product.stock_qty`, e inserta una fila en
  `price_history` con `purchase_id` (la compra recién insertada) y
  `cycle_start = true` únicamente si `stock_qty` era `0` antes de esta
  compra (D-033; mismo mecanismo de `price_history` que la edición
  manual de Fase 04, con las dos columnas nuevas de la versión 2).
- Una compra **nunca** cambia `product.sale_price_cents` de forma
  automática — decide ella, no el sistema. Dos avisos distintos, no
  uno (D-031), evaluados sobre `profit`/`marginOnSale` sobre el precio
  de venta **actual**:
  - **ADVERTENCIA:** `marginOnSale(nuevoCosto, precioActual) < min_margin_bp`
    (clave nueva de `app_setting`, default `2500`).
  - **ALERTA** (más grave, se ve más fuerte que la ADVERTENCIA):
    `profit(nuevoCosto, precioActual).cents <= 0`.
- Si `purchased_at` de la compra es anterior a la última venta no
  `CANCELLED` de ese producto, aviso informativo sin bloquear: el
  costo nuevo aplica desde ahora, no corrige ventas ya hechas (D-032).
  El cálculo de costo/stock nunca depende de `purchased_at` — siempre
  usa el `stock_qty` real del momento de registrar la compra.

**Criterios de aceptación:**
1. Build y tests pasan.
2. **[TESTS OBLIGATORIOS]** El prorrateo tiene test: la suma de
   `allocated_extra_cents` es exactamente igual a `extra_cost_cents`,
   incluyendo un caso con residuo de redondeo (ej. Q10 entre 3 líneas).
3. **[TESTS OBLIGATORIOS]** El promedio ponderado tiene test con los
   números del ejemplo de D-029 (10 u. a Q40 + 5 u. a Q55 → Q45 exacto)
   y con un caso que no divide exacto, confirmando la dirección de
   redondeo hacia arriba (ver `ESTADO.md`, "Fase 05 — Plan", para los
   valores concretos).
4. **[TESTS OBLIGATORIOS]** Test dedicado que confirme que una compra
   con `stock_qty = 0` deja `cost_cents` en el costo real de la compra
   nueva, no en un promedio con el valor viejo (D-029), y que la fila
   de `price_history` resultante tiene `cycle_start = true`.
5. Test de que registrar una compra inserta una fila en `price_history`
   con el `cost_cents` nuevo, `purchase_id` apuntando a la compra, y
   `cycle_start = false` cuando había stock antes de la compra.
6. Test de que la compra, el recálculo de costo/stock y la fila de
   `price_history` se escriben atómicamente (si algo falla a mitad de
   camino, no queda una compra huérfana ni un costo a medio actualizar)
   — mismo patrón de `db.withTransaction` que `ProductUidGenerator`
   (Fase 01).
7. **[TESTS OBLIGATORIOS]** Test de las dos condiciones de aviso
   (D-031) por separado: margen por debajo de `min_margin_bp` dispara
   ADVERTENCIA sin disparar ALERTA; ganancia cero o negativa dispara
   ALERTA; margen sano no dispara ninguna.
8. **[TESTS OBLIGATORIOS]** `MIGRATION_1_2` tiene test con
   `MigrationTestHelper`, en `app/src/androidTest` (D-034: Robolectric
   tiene un defecto real contra Room 2.8.5 para este caso puntual,
   verificado, no adivinado): crea una base v1 con una fila de
   `price_history` real, corre la migración, y confirma que la fila
   sobrevive con `purchase_id = NULL` y `cycle_start = false`, que
   `app_setting` tiene `min_margin_bp = 2500`. Además, verificación
   manual documentada en `ESTADO.md`: instalar la versión anterior en
   el emulador, cargar una pieza real, instalar la versión nueva
   encima (sin desinstalar) y confirmar que los datos sobreviven — un
   emulador limpio no ejercita nunca el camino de migración real.
9. `app/schemas/gt.marcos.joyeria.data.local.AppDatabase/2.json`
   commiteado, sin tocar `1.json`.

**Commit:** `fase-05: bulk purchases` → tag `fase-05-ok`

---

## Fase 06 — Venta de contado **[TESTS OBLIGATORIOS]**

**Objetivo:** registrar una venta al contado con snapshots y descuento de stock.

**Archivos permitidos:** `app/src/main/java/**/ui/sale/**`,
`app/src/main/java/**/domain/usecase/*Sale*.kt` (corrección de
alcance: el glob original, `RegisterSale*.kt`, no matchea
`CancelSaleUseCase.kt` — mismo tipo de corrección que `*Purchase*.kt`
tuvo en Fase 05), `app/src/main/java/**/domain/pricing/PricingCalculator.kt`
(agregado: `saleProfit`, aritmética pura de dinero, no le corresponde a
`usecase/*Sale*.kt` que es orquestación), `app/src/main/java/**/ui/navigation/**`
(agregado: destinos nuevos en el `NavHost` y `HomeScreen` deja de ser
provisoria, D-024), `app/src/main/java/**/data/**`,
`app/src/test/java/**/domain/**`, `app/src/test/java/**/data/**`,
`app/src/test/java/**/ui/sale/**` (agregados: la lista original no
tenía ningún directorio de test, y la fase exige
`[TESTS OBLIGATORIOS]` — mismo olvido que tuvo Fase 05; el último es
para probar la validación pura de `RegisterSaleUiState`, sin
Robolectric, mismo criterio que `MoneyInputTest` en
`ui/format`), `strings.xml`

**Entregable:**
- Flujo de venta: elegir piezas (lista con fotos, no un buscador de
  texto como camino principal — ver `ESTADO.md`, "Fase 06 — Plan",
  punto 3), cantidades, descuento opcional, confirmar.
- Al confirmar, en una sola transacción: crea `sale` + `sale_item` con
  snapshots (tomados leyendo el producto fresco del DAO dentro de la
  misma transacción — ver `ESTADO.md` punto 1), descuenta `stock_qty`.
- Si el descuento deja `gananciaDeVenta <= 0`, aviso con el monto
  exacto antes de confirmar, sin bloquear — D-035.
- Anulación de venta (`CANCELLED`) que devuelve el stock.
- Pantalla "Ventas de hoy" con total vendido y ganancia del día.
- `HomeScreen` deja de ser provisoria (D-024, cumplida): "Vender" y
  "Agregar pieza" son las dos acciones grandes.

**Criterios de aceptación:**
1. Build y tests pasan.
2. Test: registrar venta, luego **cambiar el precio del producto**, y verificar que la ganancia de esa venta **no cambió**.
3. Test: no se puede vender más unidades de las que hay en stock.
4. Test: anular una venta devuelve exactamente el stock descontado.
5. Test: la venta y sus líneas se crean atómicamente (si falla una línea, no queda venta huérfana).
6. Verificación manual (documentada en `ESTADO.md`, no automatizable):
   escribir una cantidad inválida, una cantidad mayor al stock, un
   descuento con coma, y un descuento mayor al subtotal — con captura.
   **La medición de tiempo de "alta rápida" con cronómetro real queda
   pendiente para la usuaria**, no se resuelve en este cierre (D-024
   cambia dónde está el botón en Home).

**Commit:** `fase-06: cash sales` → tag `fase-06-ok`

> **Fin del MVP.** Aquí se hace la primera prueba real con la usuaria antes de continuar.

---

## Fase 07 — Clientes, crédito y abonos **[TESTS OBLIGATORIOS]**

**Objetivo:** el módulo que más valor da. Ella vende con "te pago después" y necesita saber quién le debe.
Las tablas `customer` y `payment` ya existen desde la Fase 01 (D-011); esta
fase agrega su DAO, repositorio y pantallas.

**Archivos permitidos:** `app/src/main/java/**/ui/customer/**`, `app/src/main/java/**/ui/credit/**`, `app/src/main/java/**/ui/sale/**` (corrección de alcance: sin esto no hay pantalla real desde la que registrar una venta `CREDIT`, ni forma de distinguir una venta pendiente de una cobrada en "Ventas de hoy" — D-042 en `DECISIONES.md`), `app/src/main/java/**/domain/usecase/*Payment*.kt`, `app/src/main/java/**/domain/pricing/PricingCalculator.kt` (agregado: `saleBalance`, aritmética pura de dinero, mismo criterio que las adiciones de Fase 05/06 a este archivo), `app/src/main/java/**/data/**`, `app/src/main/java/**/ui/navigation/**` (agregado: destinos nuevos y botón "Clientes" en `HomeScreen`, mismo tipo de corrección que ya tuvieron Fase 05 y Fase 06), `app/src/test/java/**/domain/**`, `app/src/test/java/**/data/**`, `app/src/test/java/**/ui/**` (agregados: la lista original no tenía ningún directorio de test pese al `[TESTS OBLIGATORIOS]` de esta fase — mismo olvido que tuvieron Fase 05 y Fase 06), `strings.xml`

**Entregable:**
- CRUD de clientes (archivar, no borrar).
- Venta tipo `CREDIT` que nace `PENDING`, con o sin abono inicial.
- Registro de abonos con fecha, monto y método.
- Pantalla **"¿Quién me debe?"**: lista de clientes con saldo, ordenada por monto, con la deuda más vieja marcada.
- Estado de cuenta por venta: total, abonos, saldo.
- Al completarse el saldo, la venta pasa a `PAID` automáticamente.

**Criterios de aceptación:**
1. Build y tests pasan.
2. Test: el saldo es siempre `total - descuento - Σ abonos`, y **no existe columna persistida de saldo** en el esquema.
3. Test: un abono que excede el saldo es rechazado con error claro.
4. Test: al cubrir el saldo exacto, el estado cambia a `PAID`; un centavo menos y sigue `PENDING`.

**Commit:** `fase-07: customers, credit sales and installments` → tag `fase-07-ok`

---

## Fase 08 — Perfil de clienta y venta desde ahí

**Origen:** ronda de corrección de uso real (2026-10-10), punto 5. No salió
de la planificación original: salió de tres semanas de uso con datos reales.
Pedido textual de la usuaria: para venderle varias piezas a la misma persona
hoy hay que ir pieza por pieza desde "Vender"; quiere entrar al perfil de la
clienta y armar ahí la venta completa, de contado o a crédito.

**Archivos permitidos:**
`app/src/main/java/**/ui/credit/**`,
`app/src/main/java/**/ui/navigation/JoyeriaDestination.kt`,
`app/src/main/java/**/ui/navigation/JoyeriaNavHost.kt`,
`app/src/main/java/**/ui/sale/RegisterSaleRoute.kt`,
`app/src/main/java/**/ui/sale/RegisterSaleViewModel.kt`,
`app/src/main/java/**/ui/sale/RegisterSaleScreen.kt` (agregado al aprobar
B-07: **únicamente** para mostrar y permitir corregir de quién es la venta
en el camino de contado -- nada más en esa pantalla),
`app/src/main/java/**/data/repository/SaleRepository.kt`,
`app/src/main/java/**/data/repository/SaleModels.kt`,
`app/src/main/java/**/data/local/dao/SaleDao.kt`,
`app/src/test/java/**` (`[TESTS OBLIGATORIOS]`), `strings.xml`

**Entregable:**
- **El perfil de clienta existe y se abre para cualquier clienta**, no solo
  para las que deben. Hoy `AccountStatementScreen` es lo más parecido a un
  perfil y solo se llega desde la pestaña "¿Quién me debe?"; en "Todas las
  clientas" tocar una fila abre el diálogo de editar. Además
  `observeAccountStatements` devuelve solo ventas `PENDING`, así que una
  clienta al día tiene un perfil vacío.
- **Vender desde el perfil reutilizando la pantalla de venta existente**, sin
  una segunda pantalla ni un segundo ViewModel: la ruta `RegisterSale` acepta
  un argumento opcional `customerId`, el ViewModel lo lee del
  `SavedStateHandle` y preselecciona esa clienta. El selector
  Contado/Crédito sigue intacto — ella dijo "de contado o a crédito", así
  que entrar desde el perfil no fuerza crédito.
- **Una venta de contado puede quedar asociada a una clienta.** Hoy
  `SaleRepository` graba `customer_id = credit?.customerId`, o sea `null`
  siempre que no sea crédito, así que venderle de contado desde su perfil no
  quedaría asociado a ella — justo lo que quiere ver. El esquema ya lo
  permite (`sale.customer_id` es nullable y tiene índice): no hace falta
  migración.

**Criterios de aceptación:**
1. Build y tests pasan.
2. El perfil de una clienta sin ninguna deuda se abre y muestra su historial,
   no una pantalla vacía.
3. Una venta de contado hecha desde el perfil queda con `customer_id` de esa
   clienta, y aparece en su perfil.
4. **"¿Quién me debe?" sigue filtrando por `status = PENDING` y por `type =
   CREDIT`, nunca por la presencia de `customer_id`.** Test explícito: una
   venta de contado con clienta asociada **no** aparece como deuda. Sin esto,
   el cambio del punto anterior convertiría toda venta de contado a una
   clienta conocida en una deuda inventada.
5. Las ventas de contado sin clienta (el caso más común) siguen funcionando
   exactamente igual, con `customer_id = null`.

**Commit:** `fase-08: customer profile and selling from it` → tag `fase-08-ok`

---

## Fase 09 — Categorías propias

**Origen:** ronda de corrección de uso real (2026-10-10), punto 6. Las 5
categorías de la semilla no le alcanzan y no tiene forma de agregar ni
corregir ninguna.

**Archivos permitidos:**
`app/src/main/java/**/data/local/dao/CategoryDao.kt`,
`app/src/main/java/**/data/repository/CategoryRepository.kt`,
`app/src/main/java/**/ui/product/**`,
`app/src/main/java/**/ui/navigation/JoyeriaDestination.kt`,
`app/src/main/java/**/ui/navigation/JoyeriaNavHost.kt`,
`app/src/test/java/**`, `strings.xml`

**Entregable:**
- Las 5 categorías actuales **se mantienen** como semilla
  (`Anillos, Cadenas, Aretes, Pulseras, Juegos`), sin marca especial: una vez
  creadas son categorías normales.
- Agregar categorías nuevas y renombrar las existentes. `CategoryDao` necesita
  un `rename`; hoy no lo tiene. `CategoryRepository` hoy expone únicamente
  `observeActive`.
- **Nunca borrar: archivar** (D-006). `CategoryEntity` ya tiene la columna
  `archived`, así que **no hace falta migración**.
- Se administran desde Inventario, en el menú de la barra superior — no como
  un botón nuevo en el inicio: la sección 6 de `CLAUDE.md` dice que el inicio
  tiene dos acciones grandes y obvias y que no es un tablero.

**A decidir antes de codificar (sin esto la fase no arranca):**
- **Qué pasa con los productos de una categoría archivada.** El problema:
  `product.category_id` sigue apuntando a ella pero `observeActive()` la
  esconde, así que el desplegable de editar pieza queda vacío para esos
  productos y el filtro de Inventario no los puede volver a encontrar. Tres
  opciones, con su costo, en `ESTADO.md` (ronda de corrección, punto 6).
- **Regla de nombres duplicados.** Hoy `category.name` no tiene índice único:
  nada impide dos categorías con el mismo nombre, ni renombrar una al nombre
  de otra.

**Criterios de aceptación:**
1. Build y tests pasan.
2. Agregar, renombrar y archivar una categoría funciona desde la app, con
   confirmación en el archivado (texto claro en español).
3. Ninguna ruta borra físicamente una categoría.
4. Las piezas de una categoría archivada siguen siendo encontrables y
   editables, según la opción aprobada.
5. Test de DAO con Room in-memory para `rename` y `archive`.

**Commit:** `fase-09: custom categories` → tag `fase-09-ok`

---

## Fase 10 — Recordatorios y estado de cuenta por WhatsApp

**Objetivo:** cobrar sin escribir el mensaje a mano. Sin backend.

**Archivos permitidos:** `app/src/main/java/**/ui/credit/**`, `app/src/main/java/**/util/WhatsAppLink.kt`, `strings.xml`

**Entregable:**
- Botón "Recordar por WhatsApp" que abre `https://wa.me/<numero>?text=<mensaje>` con el texto ya armado y respetuoso, en español guatemalteco.
- El teléfono se normaliza a formato internacional (código 502 si viene sin código).
- Si el cliente no tiene teléfono, el botón no aparece.
- Compartir estado de cuenta como texto plano por cualquier app.

**Criterios de aceptación:**
1. Build y tests pasan.
2. **[TESTS OBLIGATORIOS]** Test de normalización de teléfonos: `5555-1234`, `55551234`, `+502 5555 1234`, `50255551234` producen todos el mismo resultado.
3. Test de que el mensaje se URL-encodea correctamente (tildes, ñ, saltos de línea).
4. La app **no** pide permiso de contactos ni de teléfono.

**Commit:** `fase-10: whatsapp reminders` → tag `fase-10-ok`

---

## Fase 11 — Reportes **[TESTS OBLIGATORIOS]**

**Objetivo:** responder las cuatro preguntas del negocio.

**Entregable:**
- Ganancia del mes (y comparación con el anterior).
- Capital invertido en inventario vs capital ya recuperado.
- Top 5 piezas más vendidas y top 5 por ganancia.
- **Capital estancado:** piezas sin vender en más de `stale_stock_days`, con el dinero que representan.
- Alerta de bajo stock.

**Criterios de aceptación:**
1. Build y tests pasan.
2. Test de agregación mensual con dataset fijo y resultado esperado calculado a mano.
3. Test de que las ganancias se calculan **solo** desde snapshots de `sale_item`.
4. Test de que las ventas `CANCELLED` se excluyen de todos los reportes.
5. Test de frontera de mes: una venta a las 23:59 del día 31 cuenta en ese mes y no en el siguiente (zona horaria de Guatemala).

**Commit:** `fase-11: reports` → tag `fase-11-ok`

---

## Fase 12 — Catálogo compartible

**Objetivo:** vender sin que la clienta vaya a la casa.

**Archivos permitidos (aprobados por el humano el 2026-10-19):** `app/src/main/java/**/domain/catalog/**`, `app/src/main/java/**/data/repository/CatalogRepository.kt`, `app/src/main/java/**/data/local/dao/ProductDao.kt` (solo la consulta y la proyección del catálogo), `app/src/main/java/**/ui/catalog/**`, `app/src/main/java/**/ui/navigation/**` (destino nuevo y enlace "Catálogo" en `HomeScreen`), `app/src/main/AndroidManifest.xml` (**solo** un `<provider>` de `FileProvider` para compartir las imágenes: ningún `<uses-permission>`), `app/src/main/res/xml/file_paths.xml` (solo `cache/catalog/`), `strings.xml`, `app/src/test/java/**/domain/**`, `app/src/test/java/**/data/**`, `app/src/test/java/**/ui/**`, `app/src/androidTest/java/**/ui/catalog/**` (la medición del criterio 3 corre en el emulador). Sin dependencias nuevas, sin cambio de esquema de Room.

**Entregable:**
- Seleccionar piezas y generar imágenes con foto, nombre, **código de la pieza** (chico y discreto, la foto es la protagonista) y **precio de venta**. El código se agregó el 2026-10-19: con la mayoría de las piezas sin nombre, sin él la clienta ve tarjetas iguales y no puede decir cuál quiere.
- **Nunca se muestra el costo ni la ganancia en el catálogo.** Requisito de seguridad del negocio. La garantía es estructural: el tipo que viaja a la pantalla y al generador no tiene campo de costo (ver D-058).
- Compartir por el share sheet de Android.

**Criterios de aceptación:**
1. Build y tests pasan.
2. **[TESTS OBLIGATORIOS]** Test que verifica que el modelo de datos del catálogo no contiene campos de costo ni ganancia.
3. Catálogo de 20 piezas se genera en menos de 5 segundos (medido y anotado en `ESTADO.md`).

**Commit:** `fase-12: shareable catalog` → tag `fase-12-ok`

---

## Fase 13 — Códigos de barras

**Objetivo:** encontrar una pieza al instante escaneando su etiqueta.

**Archivos permitidos (aprobados por el humano el 2026-10-19):** `app/src/main/java/**/domain/barcode/**`, `app/src/main/java/**/data/repository/BarcodeRepository.kt`, `app/src/main/java/**/data/local/dao/ProductDao.kt` (solo las consultas y proyecciones de códigos), `app/src/main/java/**/ui/barcode/**`, `app/src/main/java/**/ui/permission/**`, `app/src/main/java/**/ui/product/add/CameraCaptureView.kt` (pasa a usar el componente compartido de permiso: arregla el callejón sin salida), `app/src/main/java/**/ui/product/list/**` y `app/src/main/java/**/ui/sale/**` (solo el botón de escanear en la búsqueda), `app/src/main/java/**/ui/credit/**` (solo el formato fijo de las fechas de los textos que salen), `app/src/main/java/**/ui/navigation/**`, `app/src/main/res/xml/file_paths.xml`, `strings.xml`, `gradle/libs.versions.toml` y `app/build.gradle.kts` (solo `zxing`), `app/src/test/java/**`, `app/src/androidTest/java/**`. Sin cambio de esquema de Room. **ML Kit se descartó** (D-059): la lectura es con ZXing.

**Entregable:**
- Generar el Code128 que codifica **el `uid`, nunca el precio**; el tipo que lo genera no tiene por dónde recibir un monto (D-060).
- Hoja de etiquetas imprimible (varias por página): PDF vectorial en hoja Carta, etiquetas de 38 × 16 mm, desde el menú de Inventario; más una hoja de prueba para comprobar el tamaño con la impresora.
- Escaneo con ZXing y CameraX `ImageAnalysis`, en la búsqueda de Inventario (abre la pieza) y en el selector de piezas de Vender (la agrega al carrito).
- Si se escanea un código desconocido, mensaje claro, sin crash.
- Las fechas de los textos que viajan a otro teléfono quedan fijas en `dd/MM/yyyy`.

**Criterios de aceptación:**
1. Build y tests pasan.
2. **[TESTS OBLIGATORIOS]** Test: el contenido codificado es exactamente el `uid` y no contiene ningún monto.
3. Test de round-trip: generar el código de `XP-000042` y decodificarlo devuelve `XP-000042`.
4. Permiso de cámara solicitado solo al entrar al escáner, con explicación en español.
5. **[TESTS OBLIGATORIOS]** `NetworkPermissionTest` (`androidTest`): el paquete instalado no pide `INTERNET`. Guardián de la sección 9 de `CLAUDE.md` para cualquier dependencia, no solo para esta.

**Commit:** `fase-13: barcode labels and scanning` → tag `fase-13-ok`

---

## Fase 14 — Respaldo, exportar e importar **[TESTS OBLIGATORIOS]**

**Objetivo:** que perder el teléfono no sea peor que perder el cuaderno.

**Entregable:**
- Exportar todo a un `.zip` (JSON + fotos) vía share sheet, para guardarlo en Drive o donde sea.
- Exportar inventario y ventas a CSV legible en Excel.
- Importar un respaldo, con vista previa de qué se va a restaurar y confirmación explícita.
- Recordatorio en la app si pasaron más de 15 días desde el último respaldo.

**Criterios de aceptación:**
1. Build y tests pasan.
2. Test round-trip: exportar una base con datos, importarla en una base vacía, y verificar igualdad campo por campo.
3. Test: importar un archivo corrupto o de otra versión falla con mensaje claro y **deja la base intacta**.
4. El CSV abre correctamente con tildes (UTF-8 con BOM).

**Commit:** `fase-14: backup and export` → tag `fase-14-ok`

---

## Fase 15 — Pulido para la usuaria real

**Objetivo:** cerrar la brecha entre "funciona" y "ella lo usa".

**Entregable:**
- Revisión completa de textos: cero jerga, todo en español claro.
- Tamaños de fuente y áreas táctiles auditados contra la regla de `CLAUDE.md`.
- Estados vacíos con instrucciones, no pantallas en blanco.
- Confirmaciones en toda acción irreversible.
- Modo de carga inicial rápida, pensado para pasar el cuaderno a la app de un solo.
- Ícono y nombre de la app.
- **Cerrar los cuatro tests pendientes de la auditoría de nombres de test**
  (ver abajo). No es trabajo nuevo: ya están identificados, con el problema
  de cada uno escrito.

**Deuda arrastrada — auditoría de nombres de test (2026-10-10):**

La ronda de corrección de uso real auditó las 207 funciones de test del
proyecto contra la regla de `CLAUDE.md` sección 8 ("el nombre de un test es
una afirmación sobre lo que cubre"). Encontró 6 nombres que prometían más
de lo que probaban. **Los dos de prioridad alta se arreglaron en esa misma
ronda** (commit `fix: user feedback round`); estos cuatro quedaron
explícitamente diferidos acá:

| Test | Qué promete de más |
|---|---|
| `MoneyInputTest.toEditableText_and_parseMoneyToCents_areInverses` | "Son inversas" probado con **un** valor y **una** dirección |
| `MoneyTest.compareTo_ordersByCents` | Ejercita solo `<`; el operador que usa producción es `>` |
| `SaleRepositoryTest.observeCustomerDebts_everyRowHasPositiveBalance` | `all { }` sobre una lista de **una** fila |
| `PricingCalculatorTest.weightedAverageCost_totalQtyZeroOrLess_...` | "Cero o menos": prueba solo el cero |

Se agrega uno más, detectado en la misma auditoría pero no contado entre
los 6 porque apareció como contexto de otro hallazgo:
`RegisterSaleUiStateTest.availableProducts_filtersByName_caseInsensitive`
prueba una sola dirección de caso. Conviene arreglarlo junto con los
cuatro: su test hermano (`..._filtersByUid_regardlessOfQueryCase`) ya se
corrigió y hoy los dos usan criterios de nombre distintos.

**El detalle completo de cada uno — por qué importa, qué caso falta y el
orden sugerido — está en `ESTADO.md`, sección "Auditoría de nombres de test
(2026-10-10)".** No repetir el análisis acá: leer esa sección antes de
tocar nada. Se anota en esta fase, y no se deja solo en `ESTADO.md`, porque
entre la ronda de corrección y el pulido final hay siete fases de distancia
y una deuda que solo vive en la bitácora es una deuda que nadie encuentra.

Criterio al arreglarlos (sección 8): **partir en dos tests antes que
acortar el nombre.** El nombre acortado es honesto, pero la rama que se cae
del nombre se cae también de la cobertura. Y verificar por mutación que
cada test nuevo detecta la regresión que dice proteger, como se hizo con
los dos de prioridad alta.

**Deuda arrastrada — auditoría de relojes en repositorios (2026-10-10):**

La misma ronda de corrección encontró `System.currentTimeMillis()` dentro
de `CustomerRepository.add()` y lo arregló ahí mismo (D-055). Al revisar el
resto de `data/`/`domain/` contra la misma regla ("un repositorio no lee el
reloj; toda marca de tiempo que se persiste entra como parámetro desde
`ui`"), quedaron cuatro más, **sin tocar**:

| # | Lugar | Qué marca | Costo de arreglarlo |
|---|---|---|---|
| 1 | `ProductRepository.insert` | `created_at` y `updated_at` de una pieza nueva | `AddProductInput` gana `createdAt`. Toca `AddProductViewModel` y los tests de alta |
| 2 | `ProductRepository.archive` | `updated_at` al archivar | `archive(id, updatedAt)`, igual que `SaleRepository.cancel(id, cancelledAt, ...)`. Toca `ProductEditViewModel` |
| 3 | `ProductRepository.update` | `updated_at`, y el `changed_at` de la fila de `price_history` | `EditProductInput` gana `updatedAt`. El de mayor valor de los cuatro: `price_history` es el historial de precios, y hoy no hay forma de testear con qué fecha se inserta una fila sin depender de la hora real |
| 4 | `PurchaseRepository.register` | `updated_at` de los productos y `changed_at` de `price_history` | `RegisterPurchaseInput` ya recibe `purchasedAt` de quien llama; le falta el `now` de las escrituras derivadas |

**Nota de riesgo sobre el caso 4, a leer antes de tocar
`PurchaseRepository.register`:** `register()` ya acepta `purchasedAt` como
parámetro (D-032, porque la usuaria puede anotar una compra con fecha
anterior) y hoy además llama al reloj para las escrituras derivadas. Las
dos fechas son distintas **a propósito** — la compra pudo ser la semana
pasada, pero el costo nuevo rige desde hoy — pero nada en el código lo dice.
Agregar el parámetro que falta deja una firma con **dos fechas
`Long`/epoch millis, una al lado de la otra, que significan cosas
distintas**: `purchasedAt` (cuándo pasó la compra) y el `now` nuevo
(cuándo se registró, para `updated_at`/`changed_at`). Eso es exactamente la
clase de firma donde alguien, meses después, invierte los dos argumentos
sin que el compilador se queje — los dos son `Long`, ninguna de las dos
fechas es inherentemente "la primera". El nombre del parámetro nuevo tiene
que distinguirlas sin ambigüedad (`recordedAt`, no `now` ni `timestamp`) y
llevar un comentario que diga por qué son dos y no una. Quien cierre este
punto lee esta nota antes de escribir la firma, no después.

**El detalle completo — tabla con rutas de archivo y línea, y el resto de
las observaciones — está en `ESTADO.md`, sección "Auditoría de relojes en
repositorios (2026-10-10)".** Orden sugerido ahí: 3 y 4 juntos primero (los
que habilitan tests reales y los que hoy confunden), 1 y 2 después o nunca.

**Deuda arrastrada — alturas fijas con texto (2026-10-19):**

La regla de `CLAUDE.md` sección 6 (el piso de 56dp vive en el tema) prohíbe
`.height(Ndp)` fijo en algo con texto: con letra grande del sistema el texto
se recorta en vez de crecer, y un alto fijo local además deshace el piso. Hoy
quedan **15 sitios** que ya miden ≥ 56dp y no se recortaron en las pruebas
de letra 2.0× de `fix/design-debt`, pero siguen siendo el riesgo conocido.
No se tocaron en esa rama (decisión del humano). Pasar cada uno a
`heightIn(min = ...)`, y verificar con letra 1.0×, 1.3× y 2.0× que el texto
crece en vez de recortarse:

| Archivo | Líneas (a 2026-10-19) |
|---|---|
| `ui/navigation/HomeScreen.kt` | 70, 82, 94, 103, 112, 121, 130 |
| `ui/credit/CustomerProfileScreen.kt` | 130, 386 |
| `ui/product/edit/ProductEditScreen.kt` | 156, 165 |
| `ui/product/add/AddProductScreen.kt` | 143 |
| `ui/product/category/CategoryManageScreen.kt` | 100 |
| `ui/purchase/RegisterPurchaseScreen.kt` | 156 |
| `ui/sale/RegisterSaleScreen.kt` | 343 |

Ojo con `HomeScreen`: el orden de los modificadores importa (`padding` antes de
`height`, ver `ESTADO.md`, sección `fix/design-debt`). Dos botones de Inicio
medían 48dp por ese orden y ya se corrigió.

**Criterios de aceptación:**
1. Build y tests pasan.
2. Checklist de accesibilidad completo en `ESTADO.md`, pantalla por pantalla.
3. Lista completa de strings de UI revisada y pegada en `ESTADO.md` para aprobación humana.
4. Prueba con la usuaria real documentada: qué logró sola, dónde se trabó.
5. Los cinco tests de la deuda de arriba, cerrados: cada uno ejercita lo que
   su nombre afirma, o lleva un nombre que afirma solo lo que ejercita.
6. **Verificado con la usuaria: el perfil de una clienta vieja muestra menos
   compras de las que ella recuerda.** Las ventas de contado anteriores a la
   Fase 08 tienen `customer_id = null` y ese dato no existe; el perfil lo
   explica con una línea al pie del historial. Esta fase es la **primera vez
   que ella va a ver un perfil con compras faltantes**, así que no alcanza
   con que el texto esté: hay que mirarla usarlo y anotar si entiende por
   qué faltan o si cree que la app perdió el dato. Si no se entiende, el
   texto cambia -- el problema no es de datos, es de que la app parezca
   rota. Ver `ESTADO.md`, "Fase 08 -- Plan", sección 6.
7. Los 15 `.height(Ndp)` fijos de la deuda de alturas, resueltos con
   `heightIn(min = ...)` o justificados uno por uno en `ESTADO.md`.

**Commit:** `fase-15: usability polish` → tag `fase-15-ok`

---

## Ideas para después (no implementar sin autorización)

- Múltiples fotos por pieza.
- Apartados con fecha límite y recordatorio automático.
- Precios por mayoreo (si empieza a venderle a otras revendedoras).
- Sincronización entre dos teléfonos.
- Registro de gastos del negocio (bolsitas, cajitas, pulidor) para ganancia neta real.
- **Devoluciones con reembolso de abonos.** Escenario concreto que hoy no tiene
  solución (Fase 07, D-043): una clienta compra a crédito, abona una o más
  veces, y después devuelve la pieza. Hoy no se puede anular esa venta (tiene
  abonos), no existe manera de devolver o anular un abono ya registrado, y la
  pieza queda fuera del stock aunque vuelva a la mano de la usuaria. Necesita
  su propia decisión de diseño (¿se anula la venta y se "devuelve" cada abono
  por separado? ¿se registra como una venta nueva en negativo? ¿algo más?)
  antes de tocar código.
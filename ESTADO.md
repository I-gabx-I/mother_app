# ESTADO.md — Bitácora del agente

Claude Code actualiza este archivo **al inicio y al cierre de cada fase**, antes del commit.

Este archivo existe para una sola cosa: que el humano pueda auditar el proyecto leyendo
esto más el diff del tag, sin tener que leer el repo completo. Escribí pensando en
alguien que no vio nada de lo que hiciste.

Reglas:
- No borrés entradas viejas. Se agregan hacia abajo.
- Sé literal sobre lo que **no** hiciste. Una fase con pendientes declarados vale más
  que una fase que aparenta estar completa.
- Si tomaste una suposición que no estaba escrita en `CLAUDE.md`, `ESQUEMA.md` o
  `FASES.md`, **tenés que anotarla**. Las suposiciones no declaradas son el error más caro.

---

## Estado actual

**Actualizado 2026-09-17.**

- **Fases 00-04: cerradas, tageadas y mergeadas a `main`** —
  `fase-00-ok`, `fase-01-ok`, `fase-02-ok`, `fase-03-ok` (criterio 4
  cumplido con medición real, ver "Fase 03 — Cierre" más abajo) y
  `fase-04-ok`. `fix/quick-add-usability` (fix de usabilidad post
  03/04: teclado tapando el campo de dinero, categoría obligatoria con
  chips, campos visibles) también tageado (`fix-quick-add-usability`) y
  mergeado a `main`.
- **Versión de base de datos:** 1
- **Próximo paso, antes de escribir código de Fase 05 (Compras):** el
  humano tiene que elegir qué efecto tiene una compra sobre
  `product.cost_cents` (Opción A/B/C). Ver "Compras y el costo del
  producto — decisión pendiente" y su resumen/recomendación más abajo
  (2026-09-17). **No se empieza Fase 05 hasta esa decisión.**
- **Bloqueos abiertos:** ninguno salvo la decisión de arriba.

---

## Plantilla por fase

```markdown
## Fase NN — <nombre>

**Inicio:** YYYY-MM-DD
**Cierre:** YYYY-MM-DD
**Commit:** <hash>  **Tag:** fase-NN-ok

### Qué se hizo
-

### Archivos tocados
- (si tocaste algo fuera de "Archivos permitidos", explicá por qué y marcalo con ⚠️)

### Criterios de aceptación
| # | Criterio | Cómo se verificó | Resultado |
|---|---|---|---|
| 1 | | `./gradlew ...` | ✅ / ❌ |

### Tests agregados
- `NombreDelTest` — qué cubre

### Suposiciones que tomé
- (o "ninguna")

### Lo que NO hice
-

### Deuda técnica que dejé
-

### Bloqueos / preguntas para el humano
- (si hay algo aquí, la fase NO se cierra)
```

---

## Bloqueos

Preguntas abiertas que detienen el trabajo. El humano responde aquí mismo y el agente
continúa solo después de ver la respuesta.

| Fecha | Fase | Pregunta | Respuesta del humano |
|---|---|---|---|
| | | | |

---

## Bitácora

<!-- Las entradas de cada fase van aquí abajo, en orden -->

## Corrección: `default_markup_percent` no era solo una unidad distinta, era un significado distinto

**Fecha:** 2026-09-13. Mismo commit de `docs/fix-fase-02` (amend, por pedido
explícito del humano — no es un commit nuevo).

El humano encontró el problema real detrás de lo que yo había dejado como
"dos convenciones distintas conviviendo": no eran solo dos unidades (entero
simple vs. puntos básicos), eran **dos fórmulas distintas** para lo mismo.
`default_markup_percent = 200` con `costo * percent / 100` da precio =
costo × 2, que según la propia definición de CLAUDE.md 3.5
(`markup = ganancia / costo`) es un recargo del **100%**, no del 200% — y
`markupOnCost` (Fase 02) sí calculaba el recargo real (Q40→Q100 = 150%).
Mismo nombre, dos números distintos para el mismo concepto.

### Qué se hizo

- `ESQUEMA.md`: `default_markup_percent` → `default_markup_bp`, valor `200`
  → `10000`. Nota de la inconsistencia reemplazada por la explicación de la
  unificación, con referencia a D-014.
- `FASES.md` Fase 02: `suggestedPrice(cost, markupPercent, roundingStep)` →
  `suggestedPrice(cost, markupBp, roundingStep) = cost + cost * markupBp / 10000`.
  Agregado un criterio de aceptación nuevo (ahora criterio 5, y renumerados
  los dos `grep` que quedaron después): `suggestedPrice` y `markupOnCost`
  son inversas para un par (costo, precio) de ejemplo.
- `DECISIONES.md`: **D-014**, que reemplaza explícitamente a D-010 (D-010
  no se edita ni se borra, sigue en el archivo tal como quedó aprobada en su
  momento) y ajusta la nota de D-013 (edité esa nota puntual porque D-013 es
  parte de este mismo commit sin mergear todavía, no una decisión ya
  cerrada de una fase anterior — no toqué D-010 por la misma razón inversa:
  esa sí ya está cerrada).

### Aclaración que dejo explícita sobre el criterio de la inversa

El criterio nuevo dice "para un par (costo, precio)", en singular — lo
tomé literal, no como "para cualquier par". Es matemáticamente relevante:
`markupOnCost` usa división entera (`gananciaUnitaria * 10000 / cost_cents`),
que trunca. Si esa división no cae exacta, recomponer el precio con
`suggestedPrice` puede no reproducir el centavo exacto original — no es una
propiedad de round-trip garantizada para *cualquier* (costo, precio), sí
lo es para el ejemplo canónico (Q40 → Q100, ambas divisiones caen exactas:
`60*10000/40=15000`, `40*15000/10000=60`) y para los pares que se elijan
como casos de test. Lo dejo anotado acá para que quien implemente Fase 02
no se sorprenda si elige un par al azar donde no cierra exacto, y para que
el criterio se pruebe con casos elegidos a propósito, no con valores
arbitrarios.

### Archivos tocados

`ESQUEMA.md`, `FASES.md`, `DECISIONES.md`, `ESTADO.md`. Sin código, mismo
commit de la corrección anterior en esta rama.

### Bloqueos / preguntas para el humano

Ninguno nuevo.

---

## Corrección de FASES.md (Fase 02) y ruta de schema (Fase 01)

**Fecha:** 2026-09-13. Rama `docs/fix-fase-02`, desde `main` (ya con
`fase-01-ok` y `docs-fix-01` mergeados). Un solo commit, sin tag — pausa
pedida explícitamente para revisión.

### A) Representación de porcentajes: puntos básicos en `Int`

`ESQUEMA.md` no definía cómo representar `margenSobreVenta`/
`recargoSobreCosto` sin `Double`. Documenté la convención junto a
"Cálculos derivados": `Int` en puntos básicos (1 punto básico = 0.01%,
`valor/100` = porcentaje con dos decimales; `6000` = `60.00%`, `15000` =
`150.00%`), con las fórmulas actualizadas
(`gananciaUnitaria * 10000 / salePriceCents`, etc.). Agregada la misma
regla en `CLAUDE.md` como **3.6** (renumeré la vieja 3.6 "snapshot de venta"
a 3.7 — no hay otra referencia numérica a la sección vieja en el repo,
verificado con grep). Registrado como **D-013** en `DECISIONES.md`.

**Inconsistencia que señalo, sin resolverla por mi cuenta:**
`default_markup_percent` (D-010, `app_setting`, valor `200` = 200%) usa
porcentaje entero simple, **no** puntos básicos — son dos convenciones
distintas para "porcentaje" conviviendo en el mismo esquema. Lo dejé
anotado explícitamente en `ESQUEMA.md` y en D-013 como una duda para
revisar, no lo unifiqué: D-010 ya estaba aprobado con ese valor exacto y
cambiarlo no estaba entre lo pedido.

### B) `Money.format()` sale de Fase 02, entra en Fase 03

Quité `format()` del entregable de `Money` en Fase 02 (queda aritmética
pura: suma, resta, multiplicación por `Int`, comparación). Agregué
`app/src/main/java/**/ui/format/MoneyFormat.kt` a "Archivos permitidos" de
Fase 03 y un bullet en su entregable señalando que ahí vive la única
función de formateo de moneda (CLAUDE.md 3.3).

### C) `suggestedPrice`: `multiplier` → `markupPercent: Int`

Cambiado en el entregable de Fase 02, con la nota de que es coherente con
`default_markup_percent` de `app_setting` (200 = recargo del 100%). También
agregué que `marginOnSale`/`markupOnCost` devuelven puntos básicos (`Int`),
no `Double` — consecuencia directa del punto A que había que dejar escrita
en el mismo lugar donde se define la función, no solo en `ESQUEMA.md`.

### D) `grep` con `**` no expande sin `globstar`

Revisé **todos** los criterios de aceptación de **todas** las fases
(`grep -n "grep.*\*\*" FASES.md`, y después `grep -n "\*\*" FASES.md`
completo para no confiar en un solo patrón). El único caso real es el que
señalaste: Fase 02, criterio 5
(`app/src/main/java/**/domain`). El resto de las apariciones de `**` en el
archivo son dentro de "Archivos permitidos" — esas las interpreto yo
directamente como glob conceptual al decidir qué tocar, nunca pasan por un
shell, así que no tienen el mismo problema y las dejé como están. Corregí
la ruta del criterio 5 a la literal `app/src/main/java/gt/marcos/joyeria/domain`
y dejé una nota corta explicando el motivo, para que no se repita el mismo
error si alguien copia el patrón `**/domain` a una fase futura.

### E) Ruta real del schema de Fase 01

Corregido el criterio 4 de Fase 01: `app/schemas/1.json` →
`app/schemas/gt.marcos.joyeria.data.local.AppDatabase/1.json` (la ruta que
Room genera de verdad, confirmada en la Fase 01 ya cerrada).

### Archivos tocados

`ESQUEMA.md`, `CLAUDE.md`, `DECISIONES.md`, `FASES.md`, `ESTADO.md`. Ninguno
tiene código; es una rama de solo documentación, igual que
`docs/fix-fase-01-scope`.

### Bloqueos / preguntas para el humano

- ~~La inconsistencia entre `default_markup_percent` (porcentaje simple) y
  los puntos básicos de `margenSobreVenta`/`recargoSobreCosto`.~~ **Resuelto
  en el mismo commit:** ver la entrada de arriba, "`default_markup_percent`
  no era solo una unidad distinta, era un significado distinto" — el humano
  encontró que el problema real era más profundo que la unidad, y quedó
  cerrado con D-014.

---

## Fase 01 — Capa de datos núcleo

**Inicio:** 2026-09-13
**Cierre:** 2026-09-13
**Commit:** ver hash en `git log` (commit único de esta entrada, rama `fase/01-room-core`, ramificada desde `docs/fix-fase-01-scope`)
**Tag:** *(pendiente — parada antes del tag, como se pidió)*

**Nota sobre la base de la rama:** `fase/01-room-core` no arranca desde
`main` sino desde `docs/fix-fase-01-scope` (que a su vez sale de
`fase-00-ok`). Es deliberado: `ESQUEMA.md`/`FASES.md`/`CLAUDE.md` corregidos
en esa rama (todas las tablas en v1, `default_markup_percent`, archivos
permitidos de Fase 01, regla de Robolectric) son la base real contra la que
se implementó esta fase. Implementar contra el `main` viejo (que todavía
tiene `default_markup_multiplier` y el esquema partido) habría sido
implementar contra reglas ya reemplazadas.

### Qué se hizo

- Agregadas las dependencias nuevas, **una a la vez, compilando entre cada
  una** (CLAUDE.md §2.1): plugin KSP (ya estaba de Fase 00) → Room
  (`room-runtime` + `room-ktx` + `room-compiler` vía KSP) →
  `kotlinx-coroutines-core`/`-test` → Robolectric + `androidx.test:core` →
  Truth + Turbine. Cada paso con su propio `./gradlew assembleDebug`/
  `testDebugUnitTest` en verde antes de seguir.
- `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` en
  `app/build.gradle.kts` para que Room exporte el schema.
- `testOptions { unitTests { isIncludeAndroidResources = true } }`
  (obligatorio, D-012).
- Las **diez entidades** de `ESQUEMA.md` en `data/local/entity/`
  (`CategoryEntity`, `ProductEntity`, `PriceHistoryEntity`, `PurchaseEntity`,
  `PurchaseItemEntity`, `CustomerEntity`, `SaleEntity`, `SaleItemEntity`,
  `PaymentEntity`, `AppSettingEntity`), con sus foreign keys, `onDelete` y
  índices tal como los describe `ESQUEMA.md`.
- `AppDatabase` (`data/local/AppDatabase.kt`) con las diez entidades
  registradas, `version = 1`, `exportSchema = true`, y un
  `RoomDatabase.Callback.onCreate` (`SeedCallback`) que siembra las 5
  categorías y las 6 claves de `app_setting` con `execSQL` parametrizado
  (síncrono: termina antes de que `onCreate` devuelva el control, sin
  ninguna corrutina de por medio que pueda dejar la semilla a medias).
- DAOs solo para `category`, `product` y `app_setting`
  (`data/local/dao/*.kt`), como exige la fase.
- `AppSettingKeys.kt` con las 6 claves como constantes (evita strings
  mágicos repetidos entre el seed, el DAO y el generador de `uid`).
- `ProductUidGenerator` (`data/local/ProductUidGenerator.kt`): lee e
  incrementa `next_product_uid_seq` dentro de un solo
  `db.withTransaction { }`, formatea con `String.format(Locale.ROOT,
  "XP-%06d", seq)` (regla nueva de CLAUDE.md sección 5). Ver más abajo la
  verificación de que esto es realmente atómico, no solo "debería serlo".
- `di/DatabaseModule.kt`: Hilt provee `AppDatabase` (con el `SeedCallback`
  enganchado) y los tres DAOs.
- Tests: `ProductDaoTest` (insertar/actualizar/archivar/consultar, con
  Robolectric), `SeedDataTest` (las 5 categorías y las 6 claves de
  `app_setting` quedan sembradas y son parseables a entero),
  `ProductUidGeneratorTest` (secuencial + concurrencia real, Robolectric) y
  su copia exacta `ProductUidGeneratorInstrumentedTest` en `androidTest`,
  corrida una vez en el emulador `Medium_Phone_API_35`.
- Se agregó `--add-opens` de JVM a `tasks.withType<Test>` en
  `app/build.gradle.kts` — ver "Bloqueo resuelto" más abajo.

### Verificación de la atomicidad del generador de `uid` (no solo el diseño — la corrida real)

Tal como me comprometí en la sección anterior de este mismo archivo, antes
de dar por buena la implementación:

1. Escribí a propósito la versión **rota** de `ProductUidGenerator.next()`
   (el `SELECT` y el `UPDATE` como dos llamadas sueltas al DAO, sin
   `withTransaction`).
2. Corrí `concurrent_calls_never_produce_duplicate_uids` (100 llamadas
   concurrentes reales, `Dispatchers.IO` + `async` + `awaitAll`) **3 veces**
   contra esa versión rota. **Falló las 3 veces**, y de la forma más
   contundente posible: `iterable was: [XP-000001]` con tamaño **1**, no
   100 — las 100 corrutinas leyeron el contador antes de que ninguna lo
   escribiera, así que las 100 mintieron el mismo `uid`.
3. Restauré la versión correcta (`db.withTransaction { }`) y corrí el mismo
   test **2 veces más** (más las 3 corridas previas a hacer el ejercicio,
   total 5). **Pasó las 5 veces**, sin excepción.
4. Corrí la copia idéntica en `androidTest`
   (`ProductUidGeneratorInstrumentedTest`) una vez sobre SQLite real en el
   emulador `Medium_Phone_API_35`: **pasó** (`concurrent_calls_never_produce_duplicate_uids`
   en 11.296s, `sequential_calls_produce_increasing_uids` en 0.684s, 2/2,
   0 fallos).

Esto no es "el test debería fallar con la versión rota" — falló de hecho,
documentado acá, y dejó de fallar de hecho al arreglar la implementación.

### Bloqueo resuelto: Robolectric + JDK 24

**Error textual** (los 9 tests nuevos fallaban todos con el mismo error al
primer intento):

```
java.lang.RuntimeException: Failed to interact with raw FileDescriptor internals; perhaps JRE has changed?
	at org.robolectric.interceptors.AndroidInterceptors$FileDescriptorInterceptor.setInt(AndroidInterceptors.java:88)
Caused by: java.lang.IllegalAccessException: class org.robolectric.interceptors.AndroidInterceptors$FileDescriptorInterceptor
cannot access class jdk.internal.access.SharedSecrets (in module java.base)
because module java.base does not export jdk.internal.access to unnamed module
```

**Causa:** el JDK de este entorno es Java 24 (`java -version` →
`24.0.1`). Robolectric hace reflexión profunda sobre internals de la JDK
(`jdk.internal.access.SharedSecrets`, etc.) para simular el runtime de
Android; el module system de JDK 17+ bloquea ese acceso por defecto.

**Arreglo:** agregar los flags `--add-opens` correspondientes a la JVM que
corre los tests, vía `tasks.withType<Test>().configureEach { jvmArgs(...) }`
en `app/build.gradle.kts`. No es una dependencia nueva ni un cambio de
versión — es una bandera de arranque de la JVM de test. Confirmado con el
error completo leído antes de tocar nada (CLAUDE.md §2.1); no hizo falta un
segundo intento fallido.

### Archivos tocados

Dentro de "Archivos permitidos" de Fase 01 (ya corregido para incluir
`gradle/libs.versions.toml` y `app/build.gradle.kts`):
- `gradle/libs.versions.toml`, `app/build.gradle.kts`
- `app/src/main/java/gt/marcos/joyeria/data/local/**` (entidades, DAOs,
  `AppDatabase`, `AppSettingKeys`, `ProductUidGenerator`)
- `app/src/main/java/gt/marcos/joyeria/di/DatabaseModule.kt`
- `app/src/test/java/gt/marcos/joyeria/data/**`
- `app/schemas/**`

⚠️ Fuera de lo permitido, tocados igual, con motivo:
- **`CLAUDE.md`, `ESTADO.md`** — igual que en Fase 00: son los archivos de
  proceso que la sección 7 obliga a mantener, y las ediciones de `CLAUDE.md`
  en esta ronda (Locale.ROOT, regla de Robolectric) fueron pedidas
  explícitamente por el humano antes de autorizar la fase.
- **`app/src/androidTest/java/gt/marcos/joyeria/data/local/ProductUidGeneratorInstrumentedTest.kt`**
  — no está en "Archivos permitidos" de Fase 01 (que solo lista
  `app/src/test/java/**/data/**`). Se agregó por pedido explícito del
  humano: correr el mismo test de concurrencia una vez sobre SQLite real,
  no solo sobre Robolectric.

### Criterios de aceptación

| # | Criterio | Cómo se verificó | Resultado |
|---|---|---|---|
| 1 | `./gradlew testDebugUnitTest` pasa | Corrido repetidamente durante la fase; corrida final: 10 tests, 0 fallos (`ProductDaoTest` 5, `ProductUidGeneratorTest` 2, `SeedDataTest` 2, `ExampleUnitTest` 1) | ✅ |
| 2 | Tests de DAO con Room in-memory: insertar, actualizar, archivar, consultar producto | `ProductDaoTest`: `insert_and_getById_returns_the_stored_product`, `update_changes_the_stored_fields`, `archive_sets_archived_true_and_removes_it_from_the_active_query`, `getByUid_finds_the_product_by_its_uid`, `getByUid_returns_null_for_an_unknown_uid` | ✅ |
| 3 | Dos productos creados en paralelo nunca reciben el mismo `uid` | `ProductUidGeneratorTest.concurrent_calls_never_produce_duplicate_uids` (100 llamadas reales concurrentes) + copia en `androidTest` corrida en emulador. Verificado además que el test falla de verdad contra una implementación rota (ver sección de arriba) | ✅ |
| 4 | Existe `app/schemas/1.json` commiteado, con las diez tablas | Generado en `app/schemas/gt.marcos.joyeria.data.local.AppDatabase/1.json` (ruta estándar de Room: `app/schemas/<paquete>.<Clase>/<versión>.json`, no literalmente `app/schemas/1.json` — así es como Room nombra el archivo siempre, no fue una decisión mía). Contiene las 10 `entities` | ✅ |
| 5 | `grep -r "fallbackToDestructiveMigration" app/src` no devuelve nada | `Grep` sobre `app/src` → 0 resultados | ✅ |

Verificación adicional (no numerada): `./gradlew assembleDebug` →
`BUILD SUCCESSFUL`, sin warnings nuevos (el único warning presente ya
existía desde el scaffold de Fase 00). `grep -r "Double\|Float"
app/src/main` → 0 resultados (ajusté un comentario que mencionaba la
palabra "Double" para que ni siquiera el comentario diera un falso
positivo).

Prohibido de la fase, verificado: ningún Composable agregado; ningún DAO ni
lógica de negocio para `price_history`, `purchase`, `purchase_item`,
`customer`, `sale`, `sale_item` o `payment` (solo sus entidades existen).

### Tests agregados

- `ProductDaoTest` (5) — insertar, actualizar, archivar (y que desaparece de
  la consulta de activos), buscar por `uid` (encontrado y no encontrado).
- `SeedDataTest` (2) — las 5 categorías y las 6 claves de `app_setting`
  quedan sembradas en la primera apertura, con los valores exactos de
  `ESQUEMA.md` (incluido `default_markup_percent = 200`), parseables a
  entero.
- `ProductUidGeneratorTest` (2, `app/src/test`, Robolectric) —
  `sequential_calls_produce_increasing_uids`,
  `concurrent_calls_never_produce_duplicate_uids` (100 llamadas paralelas
  reales, `Set` completo sin duplicados).
- `ProductUidGeneratorInstrumentedTest` (2, `app/src/androidTest`) — copia
  exacta de la anterior, corrida una vez sobre SQLite real en el emulador.

### Suposiciones que tomé

- Índices en columnas FK que `ESQUEMA.md` no listó explícitamente
  (`price_history.product_id`, `purchase_item.purchase_id`,
  `purchase_item.product_id`, `sale_item.sale_id`, `sale_item.product_id`):
  los agregué porque Room los pide para no hacer table scan completo en
  cada operación sobre la tabla padre. No son columnas nuevas, son índices;
  `ESQUEMA.md` no los prohíbe, solo no los mencionó para estas tablas en
  particular (sí los menciona para `product`, `sale` y `payment`).
- Semilla por `execSQL` síncrono en `onCreate`, no por corrutina +
  `Provider<AppDatabase>` (patrón que también es común en Hilt+Room): elegí
  la versión síncrona porque es determinística y no necesita ningún
  mecanismo de sincronización para que el test no se adelante a la semilla.
- El nombre del archivo de base de datos es `joyeria.db`. No está definido
  en ningún documento; es una decisión menor sin impacto en ningún criterio.

### Lo que NO hice

- No taguée `fase-01-ok` ni mergeé la rama — como se pidió.
- No escribí DAO, repositorio ni pantalla para `price_history`, `purchase`,
  `purchase_item`, `customer`, `sale`, `sale_item` ni `payment` — solo sus
  entidades, como exige la fase.
- No agregué `kotlinx-coroutines-android` (da `Dispatchers.Main`): no hace
  falta todavía, no hay ningún `ViewModel` en esta fase. Se agrega cuando
  la fase que lo necesite lo pida.
- No mergeé ni tagueé `docs/fix-fase-01-scope` — sigue como rama aparte,
  a la espera de que el humano decida cuándo integrarla a `main` (Fase 01
  depende de su contenido pero no la reemplaza).

### Deuda técnica que dejé

- Ninguna deliberada.

### Bloqueos / preguntas para el humano

- Ninguno abierto. El único bloqueo que hubo (Robolectric + JDK 24) se
  resolvió en el mismo intento, documentado arriba.

---

## Fase 00 — Andamiaje del proyecto

**Inicio:** 2026-09-13
**Cierre:** 2026-09-13
**Commit:** `3cfca31`, `95abb15`, `9231a2e`, `1880be6`, y el commit final de esta entrada (ver hash en `git log`) — todos en la rama `fase/00-scaffolding`, abierta desde el commit baseline `d2fa181` (pre-fase-00, scaffold de Android Studio).
**Tag:** *(pendiente — el humano pidió no taguear todavía)*

### Qué se hizo

- Corregida la regla de KSP en `CLAUDE.md` §2.1 (independiente de Kotlin desde 2.3.0) y registrada como D-007 en `DECISIONES.md`, por instrucción del humano.
- Movido el análisis previo a Fase 00 (antes en un archivo aparte `ANALISIS-FASE-00.md`, ya borrado) a esta bitácora, junto con las respuestas del humano que autorizaron la fase.
- Abierta la rama `fase/00-scaffolding` desde el commit baseline `d2fa181`.
- Renombrado `namespace`/`applicationId` de `com.example.motherapp` a `gt.marcos.joyeria` (D-008). Movidos con `git mv` y reescrito el `package` de: `MainActivity.kt`, `ui/theme/{Color,Theme,Type}.kt`, `ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt` (y su aserción de `packageName`).
- Renombrado el Composable de tema `MotherAppTheme` → `JoyeriaTheme` para que no quede una referencia al nombre de trabajo "Mother App" dentro del paquete `gt.marcos.joyeria`.
- Creada la estructura de paquetes vacía `data/` y `domain/` (cada una con un `.gitkeep`, ya que git no versiona directorios vacíos), además de la `ui/` ya existente.
- Agregado el plugin KSP `2.3.12` (`gradle/libs.versions.toml`, `build.gradle.kts` raíz y `app/build.gradle.kts`). Versión verificada contra `github.com/google/ksp/releases/tag/2.3.12` y el `maven-metadata.xml` real de Maven Central, no inventada (D-007).
- Agregado Hilt `2.60.1`: plugin `com.google.dagger.hilt.android`, dependencia `hilt-android` (`implementation`) y `hilt-compiler` (`ksp(...)`, no KAPT). Versión verificada contra el `maven-metadata.xml` de `com.google.dagger:hilt-android` en Maven Central, confirmando además que los tres artefactos existen publicados en esa versión exacta (D-009).
- Creado `JoyeriaApp.kt` con `@HiltAndroidApp` y registrado `android:name=".JoyeriaApp"` en `AndroidManifest.xml`.
- `Type.kt`: `bodyLarge` y `bodyMedium` subidos de 16sp a **18sp** (mínimo de CLAUDE.md sección 6), con `lineHeight` ajustado a 26sp.
- `MainActivity.kt`: reemplazado el boilerplate "Hello Android" del wizard por una pantalla que solo muestra `stringResource(R.string.app_name)` centrado (ningún string hardcodeado en el Composable).
- Agregado `README.md` corto, apuntando a los cinco archivos de reglas del proyecto.
- Cada paso de arriba se compiló y verificó por separado (una dependencia a la vez, según CLAUDE.md §2.1) antes de seguir con el siguiente.

### Archivos tocados

Dentro de "Archivos permitidos" de Fase 00:
- `build.gradle.kts` (raíz), `app/build.gradle.kts`, `gradle/libs.versions.toml`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/gt/marcos/joyeria/JoyeriaApp.kt`
- `app/src/main/java/gt/marcos/joyeria/ui/theme/{Color,Theme,Type}.kt`
- `app/src/main/java/gt/marcos/joyeria/MainActivity.kt`
- `README.md`

⚠️ Fuera de "Archivos permitidos" de Fase 00, tocados de todos modos:
- **`CLAUDE.md`, `DECISIONES.md`, `ESTADO.md`** — no están en la lista de la fase (que es una lista de archivos de código), pero son los archivos de proceso que la sección 7 del propio `CLAUDE.md` obliga a mantener en cada fase, y el cambio en `CLAUDE.md` fue una corrección pedida explícitamente por el humano antes de empezar a escribir código. No lo anoto como bloqueo porque ya estaba autorizado en el mensaje que dio inicio a la fase.
- **`app/src/test/java/gt/marcos/joyeria/ExampleUnitTest.kt`** y **`app/src/androidTest/java/gt/marcos/joyeria/ExampleInstrumentedTest.kt`** (movidos y con su `package`/aserción de `packageName` actualizados) — no están en la lista de archivos permitidos de Fase 00. Los toqué porque son consecuencia directa y obligatoria del cambio de `namespace` (D-008): dejarlos en `com.example.motherapp` habría roto la compilación (paquete inexistente) o dejado un test que afirma un `packageName` que ya no es cierto. No agregué contenido de test nuevo, solo moví y corregí los que ya existían.
- **`app/src/main/java/gt/marcos/joyeria/data/.gitkeep`, `.../domain/.gitkeep`** — el glob de "Archivos permitidos" no menciona `data/**` ni `domain/**` bajo `java/`, pero el "Entregable" de la fase exige explícitamente "Estructura `data/` `domain/` `ui/` creada (vacía pero con los paquetes)". Son placeholders vacíos, no código.

No debería haber tocado estos archivos sin pausar primero según la letra de la regla de "archivos permitidos"; lo anoto acá en vez de esconderlo. Ninguno de los cuatro casos agrega funcionalidad de negocio ni se sale del objetivo de "andamiaje sin funcionalidad" de la fase.

### Criterios de aceptación

| # | Criterio | Cómo se verificó | Resultado |
|---|---|---|---|
| 1 | `./gradlew assembleDebug` termina sin error ni warning nuevo | Corrido tras cada cambio (6 builds en total). El único warning presente (`Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so`) ya estaba en el build baseline del wizard, **antes** de cualquier cambio mío (verificado releyendo el log del primer build) — no es un warning nuevo. | ✅ |
| 2 | `gradle/libs.versions.toml` existe y ninguna versión usa `+` o `latest` | `Grep` con el patrón `version\s*=\s*"[^"]*\+"\|latest\.release` sobre el archivo → 0 resultados | ✅ |
| 3 | `grep -r "Double\|Float" app/src/main` no devuelve nada | `Grep` con patrón `Double\|Float` sobre `app/src/main` → 0 resultados | ✅ |
| 4 | La app instala y abre sin crash | Emulador `Medium_Phone_API_35` (API 35, x86_64) arrancado, `adb install -r app-debug.apk` → `Success`, `adb shell am start -n gt.marcos.joyeria/.MainActivity` → `Starting: Intent {...}` sin error, `adb shell pidof gt.marcos.joyeria` devuelve un PID vivo, y `adb logcat -d \| grep -iE "gt\.marcos\.joyeria\|FATAL\|AndroidRuntime"` no muestra ningún `FATAL EXCEPTION` ni `AndroidRuntime` de crash. No se tomó captura de pantalla (no hacía falta para este criterio; la evidencia de logcat + proceso vivo es suficiente). | ✅ |

Verificación adicional (no es un criterio numerado de Fase 00, pero es el segundo comando estándar de `FASES.md`): `./gradlew testDebugUnitTest` → `BUILD SUCCESSFUL` (el único test es el `ExampleUnitTest` boilerplate del wizard, movido de paquete; Fase 00 no tiene `[TESTS OBLIGATORIOS]`).

Prohibido de la fase, verificado: no se creó ninguna entidad, DAO, pantalla nueva ni lógica de negocio.

### Tests agregados

- Ninguno nuevo. Fase 00 no está marcada `[TESTS OBLIGATORIOS]`. El único test existente (`ExampleUnitTest`, boilerplate del wizard) se movió de paquete sin cambiar su contenido; `ExampleInstrumentedTest` se movió y se le corrigió la aserción de `packageName` para que siga siendo verdadera con el namespace nuevo.

### Suposiciones que tomé

- Que renombrar `MotherAppTheme` a `JoyeriaTheme` (no pedido explícitamente) era preferible a dejar un identificador con el nombre de trabajo "Mother App" adentro del paquete `gt.marcos.joyeria` recién definido. Es un cambio cosmético de una función interna, no afecta ningún criterio de aceptación.
- Que un `.gitkeep` vacío es la forma correcta de dejar versionados `data/` y `domain/` vacíos, ya que git no trackea directorios vacíos y `ESQUEMA.md`/`CLAUDE.md` no dicen cómo hacerlo.
- Que el commit final de cierre de fase debía llevar el mensaje exacto `fase-00: project scaffolding` (tal como dice `FASES.md`), aunque los commits intermedios de esta rama tienen mensajes descriptivos distintos (permitido por la regla "un commit por fase, si necesitás más, que cada uno compile y pase tests").

### Lo que NO hice

- No taguée `fase-00-ok` — el humano pidió explícitamente pausar antes del tag.
- No mergeé `fase/00-scaffolding` a `main`.
- No tomé captura de pantalla del emulador (ver aclaración arriba: no era necesaria para el criterio, y el humano confirmó que la evidencia de logcat alcanza).
- No toqué `settings.gradle.kts` ni `.gitignore` — no hizo falta ningún cambio ahí.
- No agregué ningún string nuevo a `strings.xml` más allá del `app_name` que ya existía — la pantalla de Fase 00 no necesitó texto adicional.

### Deuda técnica que dejé

- Ninguna deliberada. El `Application` (`JoyeriaApp`) queda sin módulos de Hilt porque todavía no hay nada que inyectar; eso es esperado hasta Fase 01 (`DatabaseModule`).

### Bloqueos / preguntas para el humano

- Ninguno. La fase queda lista para revisión y, si la aprobás, para el tag `fase-00-ok` y el merge a `main`.

---

## Análisis previo a Fase 00

**Fecha:** 2026-09-13

Antes de escribir código, leí completos `CLAUDE.md`, `FASES.md`, `ESQUEMA.md`, `DECISIONES.md` y `ESTADO.md`, e inspeccioné el scaffold que ya había generado el asistente de Android Studio. No toqué código en este paso.

### 1. Reglas que entendí

**Dinero (sección 3 de CLAUDE.md, la más crítica):**
- Todo monto es `Long` en **centavos**, nunca `Float`/`Double`/`BigDecimal`, en ninguna capa.
- Existe un tipo `Money` en `domain` que envuelve el `Long`; nada de `Long` pelado circulando.
- El formateo (`Money.format()`) vive únicamente en `ui`, en un solo archivo.
- El redondeo del precio sugerido es hacia arriba, a múltiplo configurable (Q5 por defecto), y va testeado en los bordes.
- `ganancia`, `markup` y `margen` son tres fórmulas distintas y no se confunden en la UI.
- Cada `sale_item` guarda **snapshot** de nombre, uid, costo y precio al momento de vender. Los reportes históricos jamás hacen JOIN al producto actual.

**Versiones (sección 2.1, "causa número uno de fases fallidas"):**
- El `libs.versions.toml` que ya generó Android Studio (AGP, Gradle, Kotlin, compileSdk) es línea base intocable. Si creo que hay que subir algo, lo anoto acá y paro, no lo cambio.
- Prohibido inventar un número de versión. Solo uso lo que ya está en el proyecto o un BOM.
- Todo lo cubierto por `compose-bom` va **sin versión** propia.
- (Regla original leída en ese momento, luego corregida por el humano — ver "Respuestas del humano" abajo): "la versión de KSP debe coincidir exactamente con la de Kotlin, formato `<kotlin>-<ksp>`".
- Compose con Kotlin 2.0+ se configura con el plugin `org.jetbrains.kotlin.plugin.compose` (esto ya está bien hecho en el scaffold), no con `composeOptions`.
- Room y Hilt van con KSP, no KAPT.
- Una dependencia a la vez, compilar entre cada una, prohibido meter varias y compilar al final.
- Build roto → leer el error completo, prohibido adivinar; a los 2 intentos fallidos paro y pego el error textual acá bajo Bloqueos.

**Otras reglas que van a condicionar todo lo que haga:** offline-first sin excepciones, sin cuentas/backend/red/analytics; nada se borra físico (archivado / `CANCELLED`); migraciones Room aditivas, nunca `fallbackToDestructiveMigration`; esquema cerrado en `ESQUEMA.md`, prohibido inventar tablas/columnas; identificadores y comentarios en inglés, todo texto visible por la usuaria en español y solo en `strings.xml`; `domain` no importa nada de Android; una fase a la vez, rama `fase/NN-slug`, un commit, tag `fase-NN-ok`, y **paro** sin excepción hasta autorización.

### 2. Fase 00 — qué ya estaba y qué faltaba

**Ya hecho por el asistente de Android Studio** (todo en `main`, un solo commit `d2fa181`, sin rama `fase/00-*`):
- Proyecto Gradle Kotlin DSL con version catalog, ninguna versión con `+` ni `latest`.
- `compose-bom` declarado y las libs de Compose sin versión propia (cumple la regla del BOM).
- Compose configurado con el plugin `org.jetbrains.kotlin.plugin.compose` (correcto para Kotlin 2.0+).
- `minSdk 24` (coincide con la regla), tema Material 3 base (`Color.kt`, `Theme.kt`, `Type.kt`) generado por el wizard.
- `MainActivity` con Compose, arranca y muestra el boilerplate "Hello Android".
- `strings.xml` solo con `app_name`.
- Repo git inicializado.

**Faltaba para poder cerrar Fase 00:**
- Hilt no existía en absoluto: sin plugin, sin dependencia, sin `@HiltAndroidApp`, sin `JoyeriaApp.kt`, sin registrar `android:name` en el manifest.
- KSP no estaba declarado en `libs.versions.toml`.
- Estructura de paquetes `data/`, `domain/`, `ui/` — solo existía `ui/theme`.
- Tipografía Material 3 con cuerpo mínimo 18sp — `Type.kt` tenía `bodyLarge` en 16sp.
- `MainActivity` mostraba "Hello Android" en vez del nombre de la app.
- No había rama `fase/00-slug`.
- Namespace/applicationId en `com.example.motherapp` (placeholder del wizard).

### 3. Versiones exactas encontradas (archivo y línea)

| Herramienta | Versión | Archivo | Línea |
|---|---|---|---|
| AGP | `9.3.2` | `gradle/libs.versions.toml` | línea 2: `agp = "9.3.2"` |
| Kotlin | `2.2.10` | `gradle/libs.versions.toml` | línea 9: `kotlin = "2.2.10"` |
| Gradle (wrapper) | `9.5.0` | `gradle/wrapper/gradle-wrapper.properties` | línea 5: `distributionUrl=...gradle-9.5.0-bin.zip` |
| compileSdk | `37` | `app/build.gradle.kts` | líneas 8-10: `compileSdk { version = release(37) }` |
| targetSdk | `37` | `app/build.gradle.kts` | línea 15: `targetSdk = 37` |
| minSdk | `24` | `app/build.gradle.kts` | línea 14: `minSdk = 24` |
| KSP | no existía | — | ninguna entrada en `libs.versions.toml` ni plugin aplicado |

También `compose-bom = "2026.02.01"` en `gradle/libs.versions.toml` línea 10.

### 4. Contradicciones, ambigüedades y dudas planteadas al humano

1. KSP faltaba por completo y CLAUDE.md (en ese momento) exigía que coincidiera exacto con Kotlin en formato `<kotlin>-<ksp>`; no tenía forma de saber el sufijo sin inventarlo.
2. `namespace`/`applicationId` = `com.example.motherapp`, placeholder del wizard — ¿se mantiene o se cambia antes de escribir código?
3. AGP 9.3.2 / compileSdk 37 / Gradle 9.5.0 son más nuevos que lo que yo tengo como referencia — lo doy por bueno por la regla de línea base intocable, pero lo señalo.
4. No había rama `fase/00-slug`; todo el scaffold está en un commit directo en `main`. ¿Ese commit cuenta como baseline pre-fase-00?
5. `README.md` está en "Archivos permitidos" de Fase 00 pero no es un entregable explícito.

### Respuestas del humano (autorización para empezar Fase 00, 2026-09-13)

1. **KSP:** corrigió la regla de CLAUDE.md §2.1. Desde KSP 2.3.0 la versión es independiente de Kotlin, sin formato compuesto. Usar la última 2.3.x estable verificada en `github.com/google/ksp/releases`. CLAUDE.md ya quedó actualizado con esto.
2. **Namespace/applicationId:** cambiar a `gt.marcos.joyeria` en la Fase 00.
3. **AGP, Gradle, compileSdk:** se dejan como están.
4. **Rama:** el commit baseline (`d2fa181`) es pre-fase-00. Se abre `fase/00-scaffolding` desde ahí.
5. **README:** corto, sí.
6. **Proceso:** de ahora en adelante todo análisis se escribe primero acá, en `ESTADO.md`, antes de imprimirlo en la conversación. Este mismo análisis (que originalmente se había volcado a un archivo aparte, `ANALISIS-FASE-00.md`) se movió a esta bitácora y el archivo aparte se borró.

### Verificación de versiones para Fase 00 (fuentes primarias, no inventadas)

- **KSP `2.3.12`** — confirmado en `https://github.com/google/ksp/releases/tag/2.3.12` (publicado 2026-09-09) y en `maven-metadata.xml` de `com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin` en Maven Central (última versión listada, `lastUpdated` 20260909175426). El release notes de la versión `2.3.0` confirma el cambio de esquema: "KSP version is no longer tied to the Kotlin compiler version" — por lo tanto es compatible con Kotlin 2.2.10 sin necesitar sufijo.
- **Hilt `2.60.1`** — confirmado en `maven-metadata.xml` de `com.google.dagger:hilt-android` en Maven Central (`lastUpdated` 20260706203408), y verificado que existen en Maven Central los tres artefactos necesarios (`hilt-android`, `hilt-compiler`, `hilt-android-gradle-plugin`) en esa versión exacta (HTTP 200 en cada `.pom`). No usé el buscador `search.maven.org` como fuente única porque su índice mostró una versión vieja (`2.56.2`) desactualizada respecto al `maven-metadata.xml` real.

---

## Correcciones de proceso y de reglas previas a Fase 01

**Fecha:** 2026-09-13

El humano aprobó, tagueó (`fase-00-ok`) y mergeó la Fase 00 a `main` (colapsando
la rama a un solo commit con `git reset --soft`, deshaciendo mis commits
intermedios). Antes de arrancar Fase 01, dio dos correcciones de proceso y tres
correcciones de contenido sobre `ESQUEMA.md`/`FASES.md`. Este análisis se
escribe acá, en `ESTADO.md`, antes de imprimirse en la conversación — es la
primera vez que aplico esa regla nueva.

### Correcciones de proceso (para mí, sin tocar archivos)

1. **Un solo commit por fase** (CLAUDE.md §7). En Fase 00 hice 5 commits
   intermedios (uno por sub-paso verificado); el humano los colapsó a mano.
   De ahora en adelante hago un solo commit al cerrar cada fase — sigo
   verificando build/tests en cada paso intermedio antes de escribir código
   nuevo, pero no commiteo hasta que la fase completa esté lista, salvo que
   necesite parar por un bloqueo real.
2. **Todo análisis se escribe primero en `ESTADO.md`**, después se imprime en
   la conversación. Ya lo venía haciendo desde el cierre de Fase 00; esta
   entrada es la primera vez que se aplica también a un pedido de corrección
   a mitad de proyecto, no solo al análisis inicial de una fase.

### A) `default_markup_multiplier` → `default_markup_percent` (Int)

Aplicado en `ESQUEMA.md`: la clave de `app_setting` pasa de
`default_markup_multiplier` (`"2.0"`) a `default_markup_percent` (`200`,
entero). Agregada nota explícita: los valores de `app_setting` se parsean
siempre a `Long` o `Int`, nunca a `Double`. Registrado como **D-010** en
`DECISIONES.md`.

### B) Todas las tablas del esquema se crean en la versión 1 (Fase 01)

Antes de tocar nada, confirmé con `grep -i "migraci\|schemas?/"` sobre
`FASES.md` que **solo la Fase 06** tenía criterio de migración y referencia a
`app/schemas/2.json** (línea 186 original) — las Fases 04 y 05 no
mencionaban ninguna migración, así que no había nada que quitarles ahí más
allá de la consistencia general del esquema.

Aplicado:
- `ESQUEMA.md`: nota en la sección de versión de base de datos explicando que
  todas las tablas se crean en la v1/Fase 01. Los headers de `price_history`,
  `purchase`, `purchase_item`, `customer` y `payment` (los únicos que tenían
  una anotación `(fase 0X)`) ahora dicen "tabla en Fase 01; DAO y UI en
  Fase 0X".
- `FASES.md` Fase 01: objetivo y entregable reescritos para declarar las diez
  tablas completas (antes decía solo `category`, `product`, `app_setting`);
  los DAOs siguen siendo solo de esas tres en esta fase. Criterio 4 ahora dice
  "con las diez tablas". Se agregó una prohibición explícita de escribir DAO o
  lógica de negocio para las tablas que no usa esta fase.
- `FASES.md` Fase 04 y Fase 06: una línea aclarando que sus tablas ya existen
  desde la Fase 01. En Fase 06 se quitó el criterio 5 (migración +
  `schemas/2.json`) y la mención "migración Room nueva" de "Archivos
  permitidos".

Registrado como **D-011** en `DECISIONES.md`.

### C1) Fase 01, archivos permitidos: agregados `gradle/libs.versions.toml` y `app/build.gradle.kts`

Aplicado directamente en `FASES.md` — hacían falta para declarar la versión
de Room y `room.schemaLocation` (vía `ksp { arg(...) }` o
`javaCompileOptions` en `app/build.gradle.kts`, según cómo se termine
configurando en la fase misma). No es una decisión de diseño nueva, es
corregir un olvido de la lista de archivos permitidos, así que no le puse
entrada en `DECISIONES.md`.

### C2) Propuesta: cómo testear DAOs de Room con un Context, en un solo `testDebugUnitTest`

**El problema:** Room in-memory (`Room.inMemoryDatabaseBuilder`) necesita un
`android.content.Context` para construirse. Un test unitario JVM puro
(`app/src/test/**`, corrido por `testDebugUnitTest`) no tiene ningún runtime
de Android disponible — no hay `Context`, no hay SQLite nativo de Android.
La solución obvia (moverlos a `app/src/androidTest/**`) rompe el criterio 1
de la Fase 01 tal como está escrito ("`./gradlew testDebugUnitTest` pasa"),
porque `androidTest` corre con `connectedAndroidTest` sobre un
emulador/dispositivo, no con `testDebugUnitTest`.

**Propuesta:** usar **Robolectric** para correr los tests de DAO como tests
unitarios JVM con un `Context` de Android simulado (incluye su propio SQLite,
por eso Room funciona adentro). Es el patrón estándar y documentado para
testear Room sin emulador.

Dependencias nuevas propuestas para `app/build.gradle.kts` (`testImplementation`),
verificadas contra `maven-metadata.xml` real, no inventadas:

| Dependencia | Versión propuesta | Verificación |
|---|---|---|
| `org.robolectric:robolectric` | `4.17` | última versión estable (no beta) en `maven-metadata.xml` de Maven Central, `lastUpdated` 20260910204321; existencia confirmada con HTTP 200 en `robolectric-4.17.pom` |
| `androidx.test:core` | `1.7.0` | última versión estable en `maven-metadata.xml` de Google Maven (`dl.google.com`), `lastUpdated` 20250730230830; existencia confirmada con HTTP 200 en `core-1.7.0.pom`. Da `ApplicationProvider.getApplicationContext()`, el `Context` que necesita `Room.inMemoryDatabaseBuilder`. |

`androidx.test.ext:junit` (`androidx-junit`, ya pineado en `1.3.0`) no
necesita cambio: es la misma versión más reciente disponible según su propio
`maven-metadata.xml`.

Los tests de DAO quedarían en `app/src/test/java/**/data/**` (JVM, tal como
dice "Archivos permitidos" de la Fase 01), anotados
`@RunWith(RobolectricTestRunner::class)` en vez de `AndroidJUnit4`, obteniendo
el contexto con `ApplicationProvider.getApplicationContext<Context>()` y
construyendo la base con
`Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()`. Con
eso, `./gradlew testDebugUnitTest` los ejecuta igual que cualquier otro test
unitario — el criterio 1 de la Fase 01 se mantiene sin cambios.

Nota aparte, no una dependencia: Robolectric recomienda
`android { testOptions { unitTests { isIncludeAndroidResources = true } } }`
en `app/build.gradle.kts` para que pueda resolver el manifest/recursos
mergeados. Para un test que solo abre una base Room in-memory puede no hacer
falta, pero lo dejo propuesto porque es la configuración estándar que
recomienda la documentación de Robolectric, para evitar sorpresas.

**Esto todavía no está aplicado** — es la propuesta pedida en el punto C,
pendiente de aprobación. No toqué `libs.versions.toml` ni `app/build.gradle.kts`
para esto.

---

## Corrección de proceso (2026-09-13, antes de escribir código de Fase 01)

El humano aprobó C2 con dos ajustes (`isIncludeAndroidResources = true`
obligatorio, y Robolectric restringido solo a tests de DAO — registrado como
**D-012** en `DECISIONES.md`, regla anotada en `CLAUDE.md` §8) y movió el
commit `94b4625` de `main` a una rama nueva, `docs/fix-fase-01-scope`, con la
instrucción de que **los cambios de reglas también van en rama, nunca directo
a `main`**. Lo aplico desde ahora: este mismo commit de documentación (regla
de Robolectric + D-012 + esta entrada) va en `docs/fix-fase-01-scope`, no en
`main`.

Además, antes de esta entrada, empecé a buscar versiones de Room y de
`kotlinx-coroutines` para implementar toda la Fase 01 de una sola vez. El
humano me paró: eso viola CLAUDE.md §2.1 ("una dependencia a la vez,
compilás y confirmás, y solo entonces la siguiente") y además ignoraba que
en el mismo mensaje donde dio luz verde a la fase, pidió explícitamente que
primero explicara la atomicidad del generador de `uid` acá, en `ESTADO.md`,
antes de escribir nada. "Luz verde para la fase" no reemplaza una instrucción
puntual dada en el mismo mensaje. No voy a repetir ese error: lo que sigue es
**solo** la explicación pedida. No abrí la rama `fase/01-room-core`, no toqué
`libs.versions.toml` ni ningún archivo de código, y no voy a hacerlo hasta
que el humano confirme que esta explicación le sirve.

---

## Fase 01 — Diseño de la atomicidad del generador de `uid` (criterio 3)

**Contexto:** `ESQUEMA.md` dice que el `uid` de `product` se genera con un
contador persistido en `app_setting` (`next_product_uid_seq`), no con
`MAX(id)+1`, justamente para que dos productos nunca puedan terminar con el
mismo código. El criterio 3 de la Fase 01 exige un test que pruebe que dos
productos creados **en paralelo** nunca reciben el mismo `uid`. La pregunta
del humano es válida: leer el contador y escribirlo incrementado son, a
simple vista, dos operaciones — si no están atadas entre sí, dos corrutinas
pueden leer el mismo valor antes de que ninguna de las dos escriba, y las dos
generan el mismo `uid` (una carrera clásica de "lost update").

### Qué operación de Room garantiza la atomicidad

La operación es **`RoomDatabase.withTransaction { ... }`** (extensión suspend
de Kotlin sobre `RoomDatabase`), envolviendo la lectura y la escritura del
contador en un solo bloque:

```kotlin
// data/local — no en domain, porque toca Room/Context.
class ProductUidGenerator(
    private val db: AppDatabase,
    private val appSettingDao: AppSettingDao,
) {
    suspend fun next(): String = db.withTransaction {
        val seq = appSettingDao.getLong(KEY_NEXT_PRODUCT_UID_SEQ)
        appSettingDao.setLong(KEY_NEXT_PRODUCT_UID_SEQ, seq + 1)
        "XP-%06d".format(seq)
    }
}
```

Lo que hace atómico esto **no es que esté "adentro de una función de
Kotlin"** — una función normal no protege nada por sí sola. Lo que lo hace
atómico es que `withTransaction` abre una única transacción de SQLite
(`BEGIN` ... `COMMIT`) que envuelve el `SELECT` y el `UPDATE` juntos, y
SQLite (y el framework de Android sobre el que corre Room) **solo permite una
transacción de escritura activa a la vez sobre el mismo archivo de base de
datos**. Cualquier transacción que va a escribir tiene que tomar esa
exclusividad hasta que termina (`commit` o `rollback`); una segunda
transacción que también quiere escribir queda **bloqueada** hasta que la
primera libera.

Compará con la versión rota (dos llamadas sueltas, sin transacción):

```kotlin
// ROTO — no usar. Cada línea es una llamada suspend separada a Room.
suspend fun next(): String {
    val seq = appSettingDao.getLong(KEY_NEXT_PRODUCT_UID_SEQ)   // (1) SELECT suelto
    appSettingDao.setLong(KEY_NEXT_PRODUCT_UID_SEQ, seq + 1)    // (2) UPDATE suelto
    return "XP-%06d".format(seq)
}
```

Acá un `SELECT` no toma ningún bloqueo de escritura. Entre el paso (1) y el
paso (2) de una corrutina A, SQLite no está protegiendo nada — es una ventana
abierta. Si la corrutina B ejecuta su propio paso (1) justo en esa ventana,
lee el mismo `seq` que A ya leyó, porque A todavía no escribió el valor
incrementado. Las dos calculan el mismo `uid` y las dos terminan escribiendo
`seq + 1` (una de las dos escrituras además se pierde, el contador solo
avanza una vez en vez de dos).

### Qué pasa con dos corrutinas llamando al generador "al mismo tiempo"

Con la versión de `withTransaction`: sea cual sea la corrutina que logra
arrancar su transacción primero, ejecuta **todo** su bloque (lectura +
escritura + commit) de punta a punta antes de que la otra pueda siquiera
empezar la suya — porque la otra transacción no puede tomar el lock de
escritura hasta que la primera lo libera. No existe ningún punto en el que
las dos puedan tener el contador "leído pero no escrito todavía" al mismo
tiempo. El resultado: las dos llamadas quedan serializadas a nivel de base de
datos aunque a nivel de Kotlin se hayan lanzado juntas, cada una ve un `seq`
distinto y estrictamente creciente, y los dos `uid` son distintos siempre.

### El test del criterio 3, y por qué uno "de mentira" no sirve

Un test como este **no prueba nada**, y el humano tiene razón en desconfiar
de él:

```kotlin
// NO ALCANZA — no hay concurrencia real acá.
val uid1 = generator.next()
val uid2 = generator.next()
assertThat(uid1).isNotEqualTo(uid2)
```

Esto pasaría **incluso con la versión rota**: llamado dos veces en secuencia
(sin overlap real), el contador avanza bien igual — la carrera solo aparece
cuando dos llamadas están *en vuelo* al mismo tiempo, y acá nunca lo están.
Un test que espera a que termine la primera llamada antes de lanzar la
segunda no le da ninguna chance a la implementación rota de fallar.

El test que sí tiene sentido:

```kotlin
@Test
fun `concurrent uid generation never repeats`() = runTest {
    val n = 100
    val uids = (1..n)
        .map { async(Dispatchers.IO) { generator.next() } }
        .awaitAll()

    assertThat(uids.toSet()).hasSize(n)
}
```

Lo que hace que este test tenga dientes de verdad, no solo forma:

- **`Dispatchers.IO`, no el dispatcher de test.** Si corrieran todas las
  corrutinas en el dispatcher cooperativo de `runTest` (un solo hilo lógico),
  nunca habría dos hilos de verdad tocando Room al mismo tiempo y el test
  volvería a no probar nada, aunque "parezca" concurrente por tener `async`.
  `Dispatchers.IO` reparte las 100 llamadas en un pool de hilos reales del
  sistema operativo.
- **`async` + `awaitAll`, no un loop secuencial con `await()` uno por uno.**
  Lanzar las 100 antes de esperar ninguna maximiza cuántas están realmente
  en vuelo al mismo tiempo.
- **Se compara el `Set` completo (`hasSize(n)`), no pares sueltos.** El
  criterio real es que **ninguno** de los 100 se repita con **ningún** otro,
  no que dos elegidos al azar sean distintos.
- **La implementación rota tiene una ventana real de suspensión entre el
  `SELECT` y el `UPDATE`** — no es solo una entrelínea de código, es una
  llamada `suspend` separada que pasa por el executor de Room cada vez. Con
  100 llamadas en paralelo sobre un pool de hilos reales, esa ventana se
  atraviesa muchísimas veces por corrida; en la práctica, la probabilidad de
  que la versión rota **no** choque ninguna de las 100 veces es
  despreciable.

**La parte honesta que quiero dejar clara:** esto es "prácticamente
determinístico bajo carga real" (con 100 llamadas concurrentes de verdad, una
implementación rota va a fallar en la enorme mayoría de las corridas), pero
no es una prueba matemática de que fallaría el 100% de las veces en
cualquier hardware — un test 100% determinístico requeriría instrumentar la
implementación rota con un punto de sincronización artificial (un
`Mutex`/`CountDownLatch` que fuerce a la corrutina B a leer exactamente
mientras A está entre su lectura y su escritura), lo cual complica el código
de producción solo para el test y no lo voy a hacer sin que se apruebe
explícitamente.

En cambio, lo que sí voy a hacer cuando implemente la Fase 01, y voy a dejar
documentado acá con el resultado real (no solo la promesa): **voy a
implementar primero a propósito la versión rota** (dos llamadas sueltas, sin
`withTransaction`), correr este mismo test de concurrencia, y confirmar que
**falla** (que aparecen `uid` repetidos, o que `uids.toSet()` tiene menos de
`n` elementos). Recién después cambio a la versión con `withTransaction` y
confirmo que el mismo test **pasa**, corriéndolo varias veces para
descartar que haya pasado por suerte. Esa es la prueba de que el test tiene
dientes: no que "debería" fallar con la versión rota, sino que **de hecho
falló** cuando la corrí.

**No implementé nada de esto todavía.** Esta sección es la explicación
pedida; sigo esperando confirmación antes de abrir `fase/01-room-core` o
tocar cualquier archivo de código.

---

## Corrección: por qué la transacción es atómica (el "por qué" de arriba estaba mal)

**Fecha:** 2026-09-13. El humano aprobó el diseño y la conclusión, pero
corrigió el mecanismo que expliqué arriba. Dejo la sección original tal como
está (no se borran entradas viejas) y agrego acá la corrección, como se hace
en `DECISIONES.md`.

**Lo que dije mal:** "SQLite solo permite una transacción de escritura
activa a la vez sobre el archivo... una segunda transacción que también
quiere escribir queda bloqueada." Esto da a entender que un **lock de
archivo de SQLite** es lo que impide que la segunda corrutina lea el valor
viejo. Es engañoso por una razón concreta que el humano señaló: un `BEGIN`
normal en SQLite es una transacción **deferred** — no toma ningún lock de
escritura en el momento de abrirse, solo lo toma recién en la **primera
sentencia de escritura**. Un `SELECT` dentro de una transacción deferred no
pide más que un lock compartido (`SHARED`), que **no** bloquea a otro
`SELECT` de otra transacción deferred concurrente. Si la única protección
fuera "el lock de escritura de SQLite", dos transacciones podrían hacer su
`SELECT` en paralelo sin ningún problema, leer las dos el mismo `seq`, y
recién chocar (o ni eso) en el momento del `UPDATE` — y para entonces cada
una ya calculó su `uid` en memoria con el valor viejo. El lock de escritura
de SQLite protege la integridad del archivo, pero no evita por sí solo esta
carrera de "leer viejo, escribir basado en lo viejo".

**El mecanismo real:** en Android, `SQLiteDatabase` no habla directo con el
archivo — administra un **pool de conexiones** (`SQLiteConnectionPool`).
Aunque haya varias conexiones de lectura (más con WAL), hay **una sola
conexión designada para escribir** a la vez. Para ejecutar una transacción
que va a escribir, primero hay que **adquirir esa conexión** del pool —y esa
adquisición es una operación bloqueante a nivel del framework de Android/
Java, no una sentencia SQL—: si otra transacción ya la tiene, la que llega
después queda bloqueada ahí, **antes de mandar siquiera su `BEGIN`**, hasta
que la primera termina y la libera. `RoomDatabase.withTransaction` corre
todo el bloque (lectura y escritura juntas) dentro de esa única adquisición
de conexión: mientras la corrutina A tiene la conexión de escritura tomada
para su transacción, la corrutina B no puede ni empezar la suya — su
`SELECT` incluido — hasta que A suelta la conexión (commit o rollback). Por
eso no importa que el `BEGIN` de SQLite sea deferred y que el `SELECT` por sí
solo no tome ningún lock fuerte: **la serialización pasa un nivel más
arriba, en cómo Android/Room reparten la única conexión de escritura**, no
en el lock SQL del `SELECT` ni del `UPDATE`.

**La conclusión no cambia:** con `withTransaction`, la corrutina B nunca
puede leer el contador mientras A todavía no escribió el suyo, porque B ni
siquiera puede arrancar su transacción hasta que A termine la suya
completa. Los `uid` generados siguen siendo siempre distintos. Lo que
cambia es el motivo correcto: no es un lock de fila ni de archivo a nivel
SQL, es la adquisición serializada de la conexión de escritura de Android
que Room usa para correr el bloque de `withTransaction` de punta a punta.

---

## Fase 02 — Plan (antes de escribir código)

**Fecha:** 2026-09-13. Rama a abrir: `fase/02-pricing`, desde `main`
(que ya tiene `fase-01-ok` y la corrección `docs: fix fase-02 scope and
money rules` mergeadas — confirmado con `git log --oneline main`).

Releí completos `CLAUDE.md`, `FASES.md` (Fase 02), `ESQUEMA.md`
("Cálculos derivados") y `DECISIONES.md` (D-013, D-014) antes de escribir
esto. No toqué ningún archivo de código todavía.

### Confirmaciones explícitas pedidas por el humano

1. **Los porcentajes son `Int` en puntos básicos, nunca `Double`** (D-013).
   Confirmado: `marginOnSale` y `markupOnCost` devuelven `Int`, calculados
   en `Long` (`gananciaCents * 10000 / denominadorCents`) y convertidos a
   `Int` solo al final, para no perder precisión por un `Int` intermedio
   antes de dividir.
2. **`default_markup_bp` y `markupOnCost` usan la MISMA fórmula de
   recargo** (D-014). Confirmado: `suggestedPrice(cost, markupBp,
   roundingStep) = cost + cost * markupBp / 10000` es la operación inversa
   exacta de `markupOnCost = gananciaUnitaria * 10000 / costCents`. No hay
   una segunda fórmula de "recargo" en ningún lado de esta fase.
3. **Robolectric prohibido en `domain`; estos tests son JUnit4 puro y
   corren en milisegundos** (D-012). Confirmado: `MoneyTest` y
   `PricingCalculatorTest` van en `app/src/test/java/gt/marcos/joyeria/domain/**`,
   sin `@RunWith(RobolectricTestRunner::class)`, sin `Context`, sin ningún
   import de `android.*` ni de Robolectric. Son aritmética sobre `Long`
   envuelta en `Money`, nada más.

### Archivos que voy a crear (todos dentro de "Archivos permitidos" de Fase 02)

- `app/src/main/java/gt/marcos/joyeria/domain/model/Money.kt`
- `app/src/main/java/gt/marcos/joyeria/domain/pricing/PricingCalculator.kt`
- `app/src/test/java/gt/marcos/joyeria/domain/model/MoneyTest.kt`
- `app/src/test/java/gt/marcos/joyeria/domain/pricing/PricingCalculatorTest.kt`

No toco `gradle/libs.versions.toml` ni ningún `build.gradle.kts`: no hace
falta ninguna dependencia nueva para esta fase (aritmética pura de Kotlin,
JUnit4 + Truth ya están desde Fase 01).

### `Money` — firma exacta

```kotlin
// domain/model/Money.kt
@JvmInline
value class Money(val cents: Long) : Comparable<Money> {
    operator fun plus(other: Money): Money = Money(cents + other.cents)
    operator fun minus(other: Money): Money = Money(cents - other.cents)
    operator fun times(factor: Int): Money = Money(cents * factor)
    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    companion object {
        val ZERO = Money(0)
    }
}
```

- Sin `format()` (sale en Fase 03, `ui`, per corrección ya aplicada a
  `FASES.md`).
- `equals`/`hashCode`/`toString` no se escriben a mano: un `value class`
  los deriva automáticamente a partir de `cents`.
- Permite `cents` negativo (una venta con pérdida es una `Money` negativa
  válida — no hay ninguna regla que prohíba costo > precio, CLAUDE.md 3.5
  dice explícitamente que la ganancia negativa es un resultado correcto).
- `times(factor: Int)` cubre "multiplicación por `Int`" del entregable
  (cantidad × precio unitario). No agrego `div` ni `times(Money)`: no están
  en el entregable de esta fase y multiplicar dinero por dinero no tiene
  sentido de negocio (violaría exactamente lo que D-002/3.2 quieren evitar:
  mezclar unidades).

### `PricingCalculator` — firma exacta de cada función

```kotlin
// domain/pricing/PricingCalculator.kt
object PricingCalculator {

    fun profit(cost: Money, salePrice: Money): Money =
        salePrice - cost

    fun marginOnSale(cost: Money, salePrice: Money): Int? {
        if (salePrice.cents == 0L) return null
        return (profit(cost, salePrice).cents * 10_000L / salePrice.cents).toInt()
    }

    fun markupOnCost(cost: Money, salePrice: Money): Int? {
        if (cost.cents == 0L) return null
        return (profit(cost, salePrice).cents * 10_000L / cost.cents).toInt()
    }

    fun suggestedPrice(cost: Money, markupBp: Int, roundingStep: Money): Money {
        val raw = cost.cents + (cost.cents * markupBp / 10_000L)
        return Money(roundUpToMultiple(raw, roundingStep.cents))
    }

    private fun roundUpToMultiple(value: Long, step: Long): Long {
        if (step <= 0L) return value
        val remainder = value % step
        return if (remainder == 0L) value else value + (step - remainder)
    }
}
```

**ACTUALIZACIÓN — el humano rechazó la propuesta de `0` como centinela**
(ver más abajo, sección "Fase 02 — Ajuste al plan tras la respuesta del
humano"): `marginOnSale` y `markupOnCost` devuelven `Int?`, `null` en el
caso indefinido, no `0`. El bloque de arriba queda como registro de la
primera propuesta (no se borran entradas viejas); la firma real que se
implementó es la de la actualización.

Notas sobre las firmas:
- Todas reciben y devuelven `Money`/`Int`, nunca `Long` pelado ni `Double`
  (CLAUDE.md 3.2, 3.6).
- `roundingStep` es `Money`, no `Long` ni `Int`: es un valor en centavos
  (Q5 = `Money(500)`), y pasar un `Long` suelto ahí sería exactamente el
  "centavos sin unidad" que `Money` existe para prohibir. El ejemplo de
  `FASES.md` criterio 5 (`suggestedPrice(4000, 15000, 1)`) lo leo como
  abreviatura de `suggestedPrice(Money(4000), 15000, Money(1))` — `Money(1)`
  = paso de redondeo de 1 centavo, que en la práctica no redondea nada
  (cualquier valor es múltiplo de 1), tal como pide el criterio ("sin
  redondeo de por medio").
- `markupBp` es el nombre exacto que usa `FASES.md`/D-014 para el parámetro
  de `suggestedPrice`, la misma unidad que devuelve `markupOnCost`.

### Las dos divisiones entre cero — propuesta concreta, pendiente de tu OK (RECHAZADA — ver ajuste más abajo)

`ESQUEMA.md` y `FASES.md` exigen que el resultado sea "explícito, sin
crash ni NaN", pero no dicen **qué** valor devolver. Es una decisión de
diseño que no está escrita en ningún `.md` todavía, así que la propongo
acá y la registro como **D-015** en `DECISIONES.md` recién cuando me
confirmes (no antes, para no anotar algo que todavía puede cambiar).

1. **`marginOnSale(cost, salePrice)` cuando `salePrice.cents == 0`**
   (vender regalado o a precio cero): la fórmula divide entre el precio de
   venta. Propuesta: **devolver `0`**.
2. **`markupOnCost(cost, salePrice)` cuando `cost.cents == 0`** (pieza con
   costo cero, ej. una que le regalaron): la fórmula divide entre el
   costo. Propuesta: **devolver `0`**.

**Por qué `0` y no, por ejemplo, `Int.MAX_VALUE`:**
- Matemáticamente el recargo real en el caso 2 (costo 0, precio > 0) es
  infinito, no cero — así que `0` **no** es "la respuesta correcta
  redondeada", es un valor centinela que dice "este porcentaje no tiene
  sentido con estos datos entrá". Uso `0` en vez de `Int.MAX_VALUE` porque:
  (a) es el mismo valor para los dos casos, fácil de recordar y de testear;
  (b) no arriesga overflow ni un `%` sin sentido en la UI (`214748364700%`
    se ve como un bug, `0%` como "no hay dato" es más leíble, aunque
    tampoco sea perfecto); (c) `profit()` (que sí es matemáticamente
    correcto siempre, no divide nada) sigue devolviendo el valor real —
    quien necesite saber "ganó Q60 con costo Q0" lo lee de `profit()`, no
    de `markupOnCost()`. La UI (fases posteriores) puede decidir mostrar
    "—" en vez de "0%" cuando el costo o el precio sean cero; eso es
    decisión de `ui`, no de esta fase.
- Alternativa que descarto: hacer que la función lance excepción — viola
  "sin crash" explícitamente pedido.
- Esto es una decisión mía, no una derivación obvia de lo ya escrito. La
  marco para tu confirmación explícita antes de escribirla en código y en
  `DECISIONES.md`.

**Efecto colateral relacionado, también propuesto:** `suggestedPrice` con
`roundingStep.cents <= 0` (paso de redondeo cero o negativo, que no debería
pasar en la práctica porque `price_rounding_step_cents` de `app_setting`
por defecto es `500`, pero la función no controla lo que le pasen) no
redondea nada y devuelve el precio crudo sin dividir entre cero en el
`%`. Mismo criterio: explícito, sin crash, y lo pruebo con un test.

### Plan de tests (≥ 20 nuevos, criterio 1 de la fase)

`MoneyTest.kt` (JUnit4 puro, sin Robolectric):
1. `plus_addsCentsCorrectly`
2. `minus_subtractsCentsCorrectly`
3. `minus_canProduceNegativeMoney`
4. `times_multipliesByPositiveInt`
5. `times_byZero_isZero`
6. `compareTo_ordersByCents`
7. `equals_sameCents_areEqual`
8. `zero_hasZeroCents`

`PricingCalculatorTest.kt` (JUnit4 puro, sin Robolectric):
9. `profit_canonicalExample_cost40_price100_isProfit60`
10. `profit_zeroCost`
11. `profit_zeroSalePrice_isNegativeCost`
12. `profit_salePriceLessThanCost_isNegative`
13. `profit_bothZero_isZero`
14. `marginOnSale_canonicalExample_is6000Bp`
15. `marginOnSale_zeroSalePrice_returnsZeroExplicitly`
16. `marginOnSale_zeroCostPositivePrice_is10000Bp`
17. `marginOnSale_priceLessThanCost_isNegativeBp`
18. `markupOnCost_canonicalExample_is15000Bp`
19. `markupOnCost_zeroCost_returnsZeroExplicitly`
20. `markupOnCost_zeroCostAndZeroPrice_returnsZeroExplicitly`
21. `markupOnCost_priceLessThanCost_isNegativeBp`
22. `suggestedPrice_defaultMarkupBp10000_doublesTheCost`
23. `suggestedPrice_roundingExactMultiple_q75StaysQ75`
24. `suggestedPrice_roundingRoundsUp_q71ToQ75`
25. `suggestedPrice_roundingRoundsUp_q76ToQ80`
26. `suggestedPrice_zeroCost_isZero`
27. `suggestedPrice_roundingStepOfOneCent_isEffectivelyNoRounding`
28. `suggestedPrice_roundingStepZeroOrNegative_returnsRawPriceWithoutCrash`
29. `suggestedPrice_isInverseOfMarkupOnCost_forCanonicalPair` (criterio 5:
    `markupOnCost(Money(4000), Money(10000))` → `15000`;
    `suggestedPrice(Money(4000), 15000, Money(1))` → `Money(10000)`)

29 tests en total (9 + 20), por encima del mínimo de 20 del criterio 1.

### Verificación planeada de los criterios de aceptación

| # | Criterio | Cómo lo verifico |
|---|---|---|
| 1 | ≥20 tests nuevos, todos pasan | `./gradlew testDebugUnitTest` |
| 2 | Ejemplo canónico Q40/Q100 → ganancia 60, margen 6000, recargo 15000 | tests 9, 14, 18 |
| 3 | Redondeo Q71→Q75, Q75→Q75, Q76→Q80 con paso Q5 | tests 23-25 |
| 4 | Costo cero, precio cero, precio < costo; las dos divisiones entre cero explícitas | tests 10-13, 15-17, 19-21 |
| 5 | `suggestedPrice` y `markupOnCost` inversas (par canónico, sin redondeo) | test 29 |
| 6 | `grep -rn "Double\|Float\|BigDecimal" app/src/main/java/gt/marcos/joyeria/domain` vacío | corrido al cerrar, antes del commit |
| 7 | Ningún archivo de `domain/` importa `android.*` | `grep -rn "^import android" app/src/main/java/gt/marcos/joyeria/domain`, corrido al cerrar |

### Lo que NO voy a hacer en esta fase

- Nada de UI, Room ni Hilt (prohibido explícito de la fase).
- Nada de `Money.format()` (es de Fase 03).
- No voy a tocar `app_setting` ni sus DAOs — `PricingCalculator` recibe
  `markupBp`/`roundingStep` como parámetros, no los lee él mismo de la
  base de datos (eso es trabajo de un caso de uso en `domain/usecase` de
  una fase futura, que si hace falta se propone explícitamente).

### Commit y rama

Un solo commit: `fase-02: money and pricing engine`, en rama
`fase/02-pricing` desde `main`. Paro antes del tag `fase-02-ok`, como pide
el flujo de trabajo.

### Bloqueos / preguntas para el humano (resuelto)

- ~~Confirmar la propuesta de "las dos divisiones entre cero devuelven
  `0`"~~ **Resuelto, ver ajuste abajo:** el humano la rechazó con un
  contraejemplo concreto (Q50/Q50 tiene margen y recargo real de `0`, que
  con un centinela `0` queda indistinguible de "no se puede calcular").

---

## Fase 02 — Ajuste al plan tras la respuesta del humano

**Fecha:** 2026-09-13. El humano rechazó la propuesta de `0` como
centinela para las dos divisiones entre cero y pidió `Int?`/`null` en su
lugar, con la razón concreta arriba resumida. Aprobó el resto del plan
(archivos, firmas de `Money`, `profit`, `suggestedPrice`, plan de tests,
rama, commit) y autorizó seguir adelante con la fase. Registrado como
**D-015** en `DECISIONES.md` (razonamiento completo ahí, no lo repito
acá).

### Firmas finales (reemplazan a las "propuesta" de la sección anterior)

```kotlin
fun marginOnSale(cost: Money, salePrice: Money): Int?   // null si salePrice.cents == 0
fun markupOnCost(cost: Money, salePrice: Money): Int?    // null si cost.cents == 0
```

`suggestedPrice` no cambia de tipo de retorno (sigue devolviendo `Money`,
no `Money?`): con `roundingStep.cents <= 0` no hay ninguna operación
indefinida, solo "no hay nada que redondear" — el precio crudo sigue
siendo un valor real y usable. Ver D-015 para el razonamiento completo
de por qué este caso es distinto de los otros dos.

### Plan de tests — ajustado

Se renombran los tests 15, 19 y 20 del plan original (devolvían "cero
explícito", ahora devuelven `null`) y se agregan dos tests nuevos que
existen específicamente para que nadie vuelva a colapsar esto a un
centinela más adelante. Lista final de `PricingCalculatorTest.kt`:

9. `profit_canonicalExample_cost40_price100_isProfit60`
10. `profit_zeroCost`
11. `profit_zeroSalePrice_isNegativeCost`
12. `profit_salePriceLessThanCost_isNegative`
13. `profit_bothZero_isZero`
14. `marginOnSale_canonicalExample_is6000Bp`
15. `marginOnSale_zeroSalePrice_returnsNull` *(renombrado: ya no
    "...returnsZeroExplicitly")*
16. `marginOnSale_zeroCostPositivePrice_is10000Bp`
17. `marginOnSale_priceLessThanCost_isNegativeBp`
18. `markupOnCost_canonicalExample_is15000Bp`
19. `markupOnCost_zeroCost_returnsNull` *(renombrado)*
20. `markupOnCost_zeroCostAndZeroPrice_returnsNull` *(renombrado)*
21. `markupOnCost_priceLessThanCost_isNegativeBp`
22. `marginOnSale_equalCostAndPrice_isZeroBpNotNull` **(nuevo)** — Q50/Q50:
    margen real `0`, distinto de `null`. Protege contra que alguien
    "simplifique" esto de vuelta a un centinela.
23. `markupOnCost_equalCostAndPrice_isZeroBpNotNull` **(nuevo)** — mismo
    caso, para `markupOnCost`.
24. `suggestedPrice_defaultMarkupBp10000_doublesTheCost`
25. `suggestedPrice_roundingExactMultiple_q75StaysQ75`
26. `suggestedPrice_roundingRoundsUp_q71ToQ75`
27. `suggestedPrice_roundingRoundsUp_q76ToQ80`
28. `suggestedPrice_zeroCost_isZero`
29. `suggestedPrice_roundingStepOfOneCent_isEffectivelyNoRounding`
30. `suggestedPrice_roundingStepZeroOrNegative_returnsRawPriceWithoutCrash`
31. `suggestedPrice_isInverseOfMarkupOnCost_forCanonicalPair` — ajustado:
    `markupOnCost(...)` devuelve `Int?`; el test usa `checkNotNull(...)`
    (o equivalente) para desenvolverlo, así que **falla** si por algún
    motivo diera `null` para el par canónico, en vez de tratar `null`
    como un resultado válido y saltearse la aserción.

`MoneyTest.kt` no cambia (tests 1-8 del plan original, sin ajustes).

Total: 8 (`Money`) + 23 (`PricingCalculator`) = **31 tests**, por encima
del mínimo de 20.

### Confirmación de que sigo, no que ya terminé

Con esto el plan quedó cerrado y autorizado. Lo que sigue en esta entrada
de la bitácora es la implementación real: abrir `fase/02-pricing`,
escribir `Money.kt` y `PricingCalculator.kt`, escribir los tests de
arriba, correr `./gradlew testDebugUnitTest` y los `grep` de los
criterios 6 y 7, y solo entonces el commit único `fase-02: money and
pricing engine`. El cierre de fase, con resultados reales, está en la
sección de abajo.

---

## Fase 02 — Motor de dinero y precios

**Inicio:** 2026-09-13
**Cierre:** 2026-09-13
**Commit:** ver hash en `git log` (commit único de esta entrada, rama `fase/02-pricing`, desde `main`)
**Tag:** *(pendiente — parada antes del tag, como pide el flujo de trabajo)*

### Qué se hizo

Implementado exactamente lo planeado en las dos secciones de arriba
("Fase 02 — Plan" y "Fase 02 — Ajuste al plan tras la respuesta del
humano"), sin desvíos:

- `domain/model/Money.kt`: `value class Money(val cents: Long) :
  Comparable<Money>` con `plus`, `minus`, `times(Int)`, `compareTo` y
  `ZERO`. Sin `format()` (CLAUDE.md 3.3, llega en Fase 03).
- `domain/pricing/PricingCalculator.kt`: `profit`, `marginOnSale`,
  `markupOnCost` (estas dos devuelven `Int?`, D-015), `suggestedPrice`,
  con la función privada `roundUpToMultiple` para el redondeo hacia
  arriba (CLAUDE.md 3.4).
- `test/domain/model/MoneyTest.kt` (8 tests) y
  `test/domain/pricing/PricingCalculatorTest.kt` (23 tests): 31 tests
  nuevos en total, JUnit4 puro, **sin** `@RunWith`, sin Robolectric, sin
  ningún import de `android.*` (D-012).
- **D-015** registrada en `DECISIONES.md`: `marginOnSale`/`markupOnCost`
  devuelven `Int?` (`null` cuando la división no está definida), no `0`
  como centinela — con el razonamiento completo de por qué (Q50/Q50 tiene
  margen y recargo real de `0`, que con un centinela quedaría
  indistinguible de "no se pudo calcular").
- Ajuste menor durante la verificación del criterio 6: el primer borrador
  de un comentario en `PricingCalculator.kt` contenía literalmente la
  palabra "Double" (explicando la regla de no usarlo), lo que hacía que
  el propio `grep -rn "Double\|Float\|BigDecimal"` diera un falso
  positivo contra su propio comentario. Reescrito como "nunca un
  flotante" — mismo caso que ya había pasado en Fase 01 (ver esa entrada
  de la bitácora), lo dejo anotado para que no se repita una tercera vez.

### Archivos tocados

Dentro de "Archivos permitidos" de Fase 02:
- `app/src/main/java/gt/marcos/joyeria/domain/model/Money.kt`
- `app/src/main/java/gt/marcos/joyeria/domain/pricing/PricingCalculator.kt`
- `app/src/test/java/gt/marcos/joyeria/domain/model/MoneyTest.kt`
- `app/src/test/java/gt/marcos/joyeria/domain/pricing/PricingCalculatorTest.kt`

⚠️ Fuera de lo permitido, tocados igual, con motivo (mismo patrón que
Fases 00 y 01): `DECISIONES.md` (D-015) y `ESTADO.md` — archivos de
proceso que la sección 7 obliga a mantener en cada fase.

### Criterios de aceptación

| # | Criterio | Cómo se verificó | Resultado |
|---|---|---|---|
| 1 | `./gradlew testDebugUnitTest` pasa, ≥20 tests nuevos | 31 tests nuevos (8 `MoneyTest` + 23 `PricingCalculatorTest`), `BUILD SUCCESSFUL`, 0 fallos, 0 errores (ver XML de resultados) | ✅ |
| 2 | Ejemplo canónico Q40/Q100 → ganancia 60, margen 6000, recargo 15000 | `profit_canonicalExample_cost40_price100_isProfit60`, `marginOnSale_canonicalExample_is6000Bp`, `markupOnCost_canonicalExample_is15000Bp` | ✅ |
| 3 | Redondeo Q71→Q75, Q75→Q75, Q76→Q80 con paso Q5 | `suggestedPrice_roundingRoundsUp_q71ToQ75`, `suggestedPrice_roundingExactMultiple_q75StaysQ75`, `suggestedPrice_roundingRoundsUp_q76ToQ80` | ✅ |
| 4 | Costo cero, precio cero, precio < costo; las dos divisiones entre cero explícitas (sin crash, sin NaN) | `profit_zeroCost`, `profit_zeroSalePrice_isNegativeCost`, `profit_salePriceLessThanCost_isNegative`, `marginOnSale_zeroSalePrice_returnsNull`, `markupOnCost_zeroCost_returnsNull`, `markupOnCost_zeroCostAndZeroPrice_returnsNull`, más `marginOnSale_priceLessThanCost_isNegativeBp`/`markupOnCost_priceLessThanCost_isNegativeBp` | ✅ |
| 5 | `suggestedPrice` y `markupOnCost` inversas (par canónico, sin redondeo) | `suggestedPrice_isInverseOfMarkupOnCost_forCanonicalPair`: `markupOnCost(Money(4000), Money(10000))` = `15000`, `suggestedPrice(Money(4000), 15000, Money(1))` = `Money(10000)` | ✅ |
| 6 | `grep -rn "Double\|Float\|BigDecimal" app/src/main/java/gt/marcos/joyeria/domain` vacío | `Grep` sobre la ruta literal → 0 resultados (tras corregir el comentario que se auto-detectaba, ver arriba) | ✅ |
| 7 | Ningún archivo de `domain/` importa `android.*` | `grep -rn "^import android" app/src/main/java/gt/marcos/joyeria/domain` → 0 resultados | ✅ |

Verificación adicional: `./gradlew assembleDebug` → `BUILD SUCCESSFUL`
(no es criterio de esta fase, pero es uno de los dos comandos estándar de
`FASES.md`). `./gradlew testDebugUnitTest` completo (no solo `domain`) →
también verde, los tests de `data/` de Fase 01 siguen pasando sin
tocarlos.

Prohibido de la fase, verificado: no se tocó ninguna UI, ni Room, ni
Hilt.

### Tests agregados

- `MoneyTest` (8) — suma, resta (incluye resultado negativo),
  multiplicación por `Int` (incluye por cero), comparación, igualdad,
  `ZERO`.
- `PricingCalculatorTest` (23) — `profit` (5, incluye ambos ceros y
  precio menor al costo), `marginOnSale` (4, incluye `null` explícito),
  `markupOnCost` (4, incluye `null` explícito y el caso doble-cero), el
  par de tests de D-015 que distingue `0` real de `null` (2), y
  `suggestedPrice` (8, incluye redondeo en los tres bordes, costo cero,
  paso de 1 centavo, paso cero/negativo, y la inversa con `markupOnCost`).

### Suposiciones que tomé

- Ninguna nueva más allá de lo ya discutido y aprobado explícitamente en
  el plan (D-015: `Int?`/`null` en vez de `0`; `roundingStep <= 0` no
  redondea en vez de crashear). Ambas están documentadas en `DECISIONES.md`.

### Lo que NO hice

- No taguée `fase-02-ok` ni mergeé la rama — como pide el flujo estándar.
- No escribí `Money.format()` (es de Fase 03).
- No toqué `app_setting`, sus DAOs, ni ningún caso de uso que lea
  `default_markup_bp`/`price_rounding_step_cents` — `PricingCalculator`
  solo recibe esos valores como parámetros, no los busca él mismo.
- No agregué ninguna dependencia nueva a `libs.versions.toml` — no hacía
  falta ninguna para esta fase.

### Deuda técnica que dejé

- Ninguna deliberada.

### Bloqueos / preguntas para el humano

- Ninguno abierto.

---

## Fase 02 — Tres ajustes pedidos antes del tag (mismo commit, amend)

**Fecha:** 2026-09-13. El humano casi aprobó la fase, con tres ajustes.
Los tres van en el mismo commit de Fase 02 (amend, no un commit nuevo),
igual que las correcciones ya hechas así en Fases 00/01.

### 1) Comentarios de código en español, no en inglés

`CLAUDE.md` §5 decía "identificadores, nombres de archivo, comentarios y
commits: en inglés". El humano pidió cambiar la regla (no el código):
identificadores/nombres de archivo/commits en inglés, **comentarios en
español**, con tildes correctas — los archivos son UTF-8. Aplicado:

- `CLAUDE.md` §5 reescrita, con referencia a **D-016** (`DECISIONES.md`).
- Acentos unificados en los 4 archivos de Fase 02
  (`Money.kt`, `PricingCalculator.kt`, `MoneyTest.kt`,
  `PricingCalculatorTest.kt`), en la misma línea de estilo que
  `data/local/AppDatabase.kt` (referencia que dio el humano, ya escrito
  así desde Fase 01 sin que la regla existiera todavía explícita).
  Correcciones puntuales: "prohibe"→"prohíbe", "Aritmetica"→"Aritmética",
  "seccion"→"sección", "basicos"→"básicos", "formula"→"fórmula",
  "division"→"división", "esta definida"→"está definida",
  "legitimo"→"legítimo", "multiplo"→"múltiplo", "proximo"→"próximo",
  "produccion"→"producción", "generico"→"genérico", "rapida"→"rápida",
  "maximo"→"máximo", "entre si"→"entre sí", "canonico"→"canónico".
- **No toqué** comentarios de `data/` ni de ningún archivo fuera de
  "Archivos permitidos" de Fase 02 (Fases 00/01 ya cerradas y tageadas):
  si hay acentos faltantes ahí, queda pendiente para quien toque esos
  archivos en su propia fase, no se corrige de paso acá.
- **Hallazgo colateral, no corregido (fuera de alcance de esta fase):**
  al leer `AppDatabase.kt` para tomarlo de referencia, noté que
  `SeedCallback` todavía siembra la clave `default_markup_percent` con
  valor `"200"` — el nombre y valor **previos** a D-014, que la renombró
  a `default_markup_bp` con valor `10000`. Es un relicto de Fase 01
  (escrita antes de que D-014 existiera), tageada y mergeada a `main`.
  No lo toco: `data/local/**` no está en "Archivos permitidos" de Fase
  02. Lo anoto acá como bloqueo/aviso para que se corrija explícitamente
  (probablemente al abrir Fase 04, que es la primera que toca
  `app_setting` de verdad) — hoy es un dato sembrado con un nombre de
  clave que ningún código todavía lee, así que no rompe nada en este
  momento, pero va a romper el primer caso de uso que busque
  `default_markup_bp` y no lo encuentre.

### 2) El criterio 6 forzó a degradar un comentario — propuesta de comando nuevo, sin aplicar todavía

El `grep -rn "Double\|Float\|BigDecimal" app/src/main/java/gt/marcos/joyeria/domain`
original no distingue una mención en un comentario ("nunca Double") de
un uso real en código, así que me había obligado a escribir "flotante"
en vez de la palabra técnica correcta. Ya restauré el comentario a decir
`Double` (ver `PricingCalculator.kt` línea 11). El comando de
verificación de `FASES.md` criterio 6 sigue **sin cambiar todavía** —
tal como está hoy, contra el comentario restaurado, da un falso
positivo a propósito, porque el humano pidió ver el comando exacto antes
de que lo aplique.

**Comando propuesto** (reemplaza al actual en `FASES.md`, criterio 6 de
Fase 02):

```
grep -rn "Double\|Float\|BigDecimal" app/src/main/java/gt/marcos/joyeria/domain | grep -vE "^[^:]+:[0-9]+: *(\*|//)"
```

Cómo funciona: la primera parte es el `grep` original, sin cambios. La
segunda descarta las líneas cuyo contenido (después de `ruta:línea:`)
empieza con `*` (línea de continuación de un bloque KDoc, con el estilo
`/** ... * ... */` que ya usamos en todo el proyecto) o con `//`
(comentario de una sola línea). Si la palabra aparece únicamente en una
línea de comentario, la segunda parte la descarta y el resultado final
queda vacío; si aparece en una línea de código real, la segunda parte no
la toca y el `grep` sigue fallando como debe.

**Verificado, no solo propuesto** (antes de escribir esto):
- Contra el estado actual del repo (con `Double` restaurado en el
  comentario de `PricingCalculator.kt`), el comando propuesto da **0
  resultados** — confirmado con `Bash`/`Grep`.
- Contra un archivo temporal descartable con `val x: Double = 1.0` y
  `fun y(): Float = 2.0f` (creado y borrado en el mismo paso, nunca
  commiteado), el comando propuesto **sí** los detecta — confirmado que
  no se volvió un cheque que nunca falla.

**Limitación que dejo documentada, para que quede escrita si se aprueba
este comando:** el filtro solo reconoce comentario de bloque en el
estilo que ya usamos (`/**` en su propia línea, cada línea siguiente con
` * `) y comentario de línea completa (`//` al principio de la línea).
No reconoce un comentario **al final** de una línea de código real
(`val x = 1 // menciona Double acá`) — en ese caso, si la palabra
apareciera solo en esa cola de comentario, el filtro no la descartaría y
el `grep` fallaría igual. No es un problema hoy (no usamos comentarios
al final de línea en `domain/`), pero lo anoto como límite conocido del
cheque, igual que ya existe una nota similar para el criterio 5 de esta
misma fase (glob `**` sin `globstar`).

**No apliqué este cambio a `FASES.md` todavía.** Espero confirmación
explícita del comando antes de tocar ese archivo.

### 3) Dos tests nuevos

**a) Inversa con pérdida** (el criterio 5 original solo cubre el par con
ganancia): agregado
`suggestedPrice_isInverseOfMarkupOnCost_forLossPair` en
`PricingCalculatorTest.kt` — `markupOnCost(Money(10000), Money(8000))` =
`-2000`, `suggestedPrice(Money(10000), -2000, Money(1))` reconstruye
`Money(8000)`. Pasa.

**b) `roundUpToMultiple` con `value` negativo** — el humano señaló que
con `-4100`/paso `500` la implementación original devolvía `-3500`
cuando el múltiplo superior correcto es `-4000`, y me dejó elegir entre
arreglarlo o documentarlo como límite conocido. **Elegí arreglarlo**, no
documentarlo como límite. Razón: la fórmula corregida
(`value.floorDiv(step) * step`, más un `step` si no cae exacto) no es
más compleja que la rota — es una sola expresión sin casos especiales
por signo, y sigue pasando los mismos tres tests de redondeo positivo
que ya existían (`q71→q75`, `q75→q75`, `q76→q80`). Dejar a propósito una
función de dinero que es "correcta salvo para ciertas entradas" es
exactamente lo que la sección 3 de `CLAUDE.md` existe para evitar,
incluso si hoy esa entrada no es alcanzable con los rangos de negocio
reales (`cost >= 0`, `markupBp >= -10000` en la práctica). Documentado
en el KDoc de `roundUpToMultiple` y como adenda a **D-015** en
`DECISIONES.md` (no un número de decisión nuevo: es una corrección de
implementación dentro de la misma decisión ya registrada, en el mismo
commit sin mergear). Test agregado:
`suggestedPrice_roundingNegativeRawPrice_minus4100RoundsUpToMinus4000`
(cost `10000`, `markupBp = -14100` para llegar al precio crudo `-4100`,
paso `500`, resultado esperado `Money(-4000)`). Pasa.

### Verificación tras los tres ajustes

- `./gradlew testDebugUnitTest --tests "gt.marcos.joyeria.domain.*"` →
  `BUILD SUCCESSFUL`. `MoneyTest`: 8 tests, 0 fallos. `PricingCalculatorTest`:
  **25** tests (23 + los 2 nuevos), 0 fallos. Total `domain`: 33 tests.
- Los tres tests de redondeo positivo que ya existían (`q71→q75`,
  `q75→q75`, `q76→q80`) siguen pasando con la nueva implementación de
  `roundUpToMultiple` — no se rompió nada al arreglar el caso negativo.
- Pendiente de correr de nuevo tras el commit final: `./gradlew
  assembleDebug`, y el `grep` del criterio 6 (con el comando viejo o el
  nuevo, según qué se apruebe) y el del criterio 7.

### Qué falta para tagear

- ~~Tu confirmación del comando de `grep` del punto 2~~ **Resuelta.** El
  humano aprobó el comando con un agregado: mi filtro no cubría una
  línea que empieza directamente con `/**` (KDoc de una sola línea,
  como el de `profit` en `PricingCalculator.kt`), solo el caso
  multilínea con continuación ` * `. Comando final:

  ```
  grep -rn "Double\|Float\|BigDecimal" app/src/main/java/gt/marcos/joyeria/domain | grep -vE "^[^:]+:[0-9]+: *(\*|//|/\*)"
  ```

  Verificado en los dos sentidos, igual que la vez anterior:
  - Contra el repo actual → `0` resultados (`exit=1`).
  - Contra un archivo temporal descartable (creado y borrado en el
    mismo paso, nunca commiteado) con un KDoc de una sola línea
    (`/** ... Double ... */`) **y** un uso real (`val x: Double`,
    `fun y(): Float`) → el KDoc de una sola línea queda descartado, el
    uso real se sigue detectando.
  - Limitación documentada (igual que antes): no cubre un comentario al
    final de una línea de código real. Anotada en `FASES.md` junto al
    criterio 6.
  - Aplicado a `FASES.md`, criterio 6 de Fase 02.
- Con esto, los tres ajustes están aplicados y verificados. Quedo lista
  para `fase-02-ok` y el merge a `main` — paro acá, como se pidió.

### Bug encontrado en `AppDatabase.kt`, fuera de esta fase — corrección para después del merge

El humano confirmó que el hallazgo de la sección anterior no es solo un
aviso: es un bug activo. `SeedCallback` siembra `default_markup_percent`
(clave que ya no existe en `ESQUEMA.md` desde D-014) y nunca crea
`default_markup_bp` (la que sí existe). Fase 03 va a necesitar esa clave
para el precio sugerido en vivo, así que hay que corregirlo antes de esa
fase.

**No lo corrijo en esta rama** (`data/local/**` sigue fuera de "Archivos
permitidos" de Fase 02, y el arreglo depende de que `main` ya tenga
Fase 02 mergeada). Instrucción del humano, para ejecutar **después** de
que mergee `fase-02-ok` a `main`:

1. Abrir rama `fix/seed-markup-bp` desde `main` (ya con Fase 02
   mergeada).
2. Corregir `AppSettingKeys` (constante de la clave) y `SeedCallback` en
   `AppDatabase.kt`: `default_markup_percent`/`"200"` →
   `default_markup_bp`/`"10000"`.
3. Sin migración: la tabla `app_setting` no cambia de forma, solo el
   contenido de la semilla, y no hay ninguna instalación con datos
   reales todavía.
4. Agregar un test que verifique que la semilla crea **exactamente las
   seis claves que lista `ESQUEMA.md`**, con sus valores exactos
   (`default_markup_bp=10000`, `price_rounding_step_cents=500`,
   `low_stock_threshold=2`, `stale_stock_days=90`,
   `next_product_uid_seq=1`, `owner_name=""`). Ese es el test que habría
   atrapado este bug antes de que llegara a `main`.

Esto queda pendiente para la próxima rama, no para esta.

---

## fix/seed-markup-bp — La semilla sembraba `default_markup_percent`, una clave que ya no existe

**Fecha:** 2026-09-13. Rama `fix/seed-markup-bp`, desde `main` (ya con
`fase-02-ok` tageada y mergeada). No es una fase de `FASES.md`: es la
corrección del bug encontrado durante la revisión de Fase 02 (ver la
sección anterior de esta bitácora), con instrucciones explícitas del
humano. Un solo commit, sin tag — paro antes, como se pidió.

### El bug

`AppDatabase.SeedCallback` sembraba la clave `default_markup_percent`
con valor `"200"`. Esa clave **no existe** en `ESQUEMA.md` desde D-014
(que la renombró a `default_markup_bp`, valor `10000`, con una fórmula
distinta — ver D-014 en `DECISIONES.md`). El código de Fase 01 se
escribió antes de que D-014 existiera y nadie lo actualizó cuando la
Fase 02 corrigió el esquema. Resultado: la clave que el esquema real
pide (`default_markup_bp`) nunca se creaba, y la que sí se creaba
(`default_markup_percent`) no la lee ningún código del esquema vigente.
Cualquier caso de uso futuro que pida `default_markup_bp` (Fase 03, para
el precio sugerido en vivo) la iba a encontrar vacía.

### Qué se hizo

- `AppSettingKeys.kt`: constante renombrada de `DEFAULT_MARKUP_PERCENT`
  (`"default_markup_percent"`) a `DEFAULT_MARKUP_BP`
  (`"default_markup_bp"`). Comentario actualizado citando D-014.
- `AppDatabase.kt` (`SeedCallback.SEED_SETTINGS`): la fila sembrada pasa
  de `AppSettingKeys.DEFAULT_MARKUP_PERCENT to "200"` a
  `AppSettingKeys.DEFAULT_MARKUP_BP to "10000"` — la clave y el valor
  que `ESQUEMA.md` pide de verdad.
- **Sin migración**, como indicó el humano: la tabla `app_setting` no
  cambia de forma (sigue siendo `key`/`value` genérica), solo cambia el
  contenido de la fila que se inserta en `onCreate`. Sigue en versión 1,
  `app/schemas/.../1.json` no se toca. No hay ninguna instalación real
  con datos que migrar todavía (D-005/D-011).
- `AppSettingDao.kt`: agregado `getAll(): List<AppSettingEntity>`
  (`SELECT * FROM app_setting`), necesario para el test de la semilla
  completo que pidió el humano — no existía ninguna consulta que trajera
  todas las filas a la vez, solo `getValue(key)` por clave.
- `SeedDataTest.kt`: el test
  `first_open_seeds_all_app_setting_keys_as_parseable_integers` (seis
  `assertThat` sueltos, uno por clave, clave por clave) se reemplazó por
  `first_open_seeds_exactly_the_six_esquema_keys_with_their_values`: una
  sola aserción (`containsExactly`) contra la lista completa de las seis
  `AppSettingEntity` de `ESQUEMA.md`, con sus valores reales
  (`default_markup_bp=10000` en vez de `default_markup_percent=200`).
  Con `containsExactly` una clave de más o de menos hace fallar el test
  tan explícitamente como un valor distinto — los seis `assertThat`
  sueltos que reemplaza solo detectaban un valor incorrecto en una clave
  que el test ya conocía, nunca una clave de más o de menos.

### Prueba de que el test tiene dientes (no solo "debería fallar")

Mismo estándar que `ProductUidGeneratorTest` en Fase 01: antes de dar el
test por bueno, reproduje el bug real a propósito (seedeando la fila
literal `"default_markup_percent" to "200"` en vez de la constante
nueva) y corrí `SeedDataTest`. **Falló**, con el mensaje exacto:

```
missing (1)   : AppSettingEntity(key=default_markup_bp, value=10000)
unexpected (1): AppSettingEntity(key=default_markup_percent, value=200)
```

Restauré el fix real (`AppSettingKeys.DEFAULT_MARKUP_BP to "10000"`) y
corrí de nuevo: pasa, junto con el resto de la suite completa.

### Archivos tocados

- `app/src/main/java/gt/marcos/joyeria/data/local/AppSettingKeys.kt`
- `app/src/main/java/gt/marcos/joyeria/data/local/AppDatabase.kt`
- `app/src/main/java/gt/marcos/joyeria/data/local/dao/AppSettingDao.kt`
- `app/src/test/java/gt/marcos/joyeria/data/local/SeedDataTest.kt`

No hay "Archivos permitidos" formales para esta rama (no es una fase de
`FASES.md`); me ceñí a los cuatro archivos que el bug y el test piden,
todos dentro de `data/local/**`, consistente con dónde vive el resto de
la capa de datos.

### Verificación

- `./gradlew assembleDebug testDebugUnitTest` → `BUILD SUCCESSFUL`.
- `SeedDataTest`: 2 tests, 0 fallos (`first_open_seeds_the_five_categories`
  sin cambios, `first_open_seeds_exactly_the_six_esquema_keys_with_their_values`
  nuevo, verde).
- Resto de la suite (`ProductDaoTest`, `ProductUidGeneratorTest`,
  `MoneyTest`, `PricingCalculatorTest`) sin cambios, sigue verde.
- No quedó ninguna referencia a `DEFAULT_MARKUP_PERCENT` ni
  `default_markup_percent` en `app/src` (`Grep` sobre el árbol → solo el
  comentario de `AppSettingKeys.kt` que explica el renombre, mencionado
  como texto, no como identificador).

### Lo que NO hice

- No taguée ni mergeé esta rama — paro antes, como se pidió.
- No toqué ningún caso de uso ni ViewModel que lea `default_markup_bp`:
  todavía no existe ninguno (llega con Fase 03/04).
- No agregué migración: no hace falta, ver arriba.

### Bloqueos / preguntas para el humano

- Ninguno.

---

## Fase 03 — Plan (antes de escribir código)

**Fecha:** 2026-09-13. Releí completos `CLAUDE.md`, `FASES.md` (Fase 03),
`ESQUEMA.md` (tabla `product`) y `DECISIONES.md` antes de escribir esto.
No toqué ningún archivo de código todavía. Rama a abrir: `fase/03-quick-add`,
desde `main` (ya con `fase-02-ok` y `fix-seed-markup-bp` mergeadas).

Esta fase es distinta a las anteriores en un sentido concreto que quiero
dejar explícito desde acá, no descubierto al final: **el criterio 4
(cronómetro, <20s) mide desempeño humano real — tiempo de reacción, de
mirar la pantalla, de encontrar el campo, de tipear con el dedo.** Yo no
tengo manos ni ojos para operar un teléfono como una persona; puedo
scriptear `adb shell input tap/text` contra el emulador, pero eso mide la
latencia de un script, no el tiempo que tarda una persona en decidir qué
tocar y leerlo — sería un número falso, más rápido que cualquier uso real,
y lo prohibido en este proyecto es justamente inventar o maquillar una
medición (CLAUDE.md, "Report outcomes faithfully"). Lo explico en detalle
en la sección 6 de este plan, con lo que sí puedo hacer yo mismo y lo que
necesito que hagas vos (o alguien) con el dedo y un cronómetro de verdad.

### 1) Archivos que voy a crear (dentro de "Archivos permitidos" de Fase 03)

- `app/src/main/java/gt/marcos/joyeria/ui/format/MoneyFormat.kt` —
  `Money.format()`, la única función de formateo de moneda (CLAUDE.md 3.3),
  pendiente desde que Fase 02 la excluyó a propósito.
- `app/src/main/java/gt/marcos/joyeria/domain/usecase/AddProductUseCase.kt`
  — incluye también `AddProductInput` (data class de entrada, sin tipos de
  Android/Room) en el mismo archivo; el nombre matchea el glob
  `AddProduct*.kt`.
- `app/src/main/java/gt/marcos/joyeria/data/repository/ProductRepository.kt`
  — clase concreta (no interfaz + impl separadas; ver nota de arquitectura
  en la sección 4).
- `app/src/main/java/gt/marcos/joyeria/util/ImageStorage.kt` — redimensión,
  compresión y guardado de la foto (sección 5).
- `app/src/main/java/gt/marcos/joyeria/ui/product/add/AddProductUiState.kt`
- `app/src/main/java/gt/marcos/joyeria/ui/product/add/AddProductViewModel.kt`
- `app/src/main/java/gt/marcos/joyeria/ui/product/add/AddProductScreen.kt`
  (Composable stateless, `@Preview`-able)
- `app/src/main/java/gt/marcos/joyeria/ui/product/add/AddProductRoute.kt`
  (wrapper stateful, conecta el ViewModel con `AddProductScreen`; ver
  sección 4 sobre por qué este archivo sí "recibe" el ViewModel)
- `app/src/main/java/gt/marcos/joyeria/ui/product/add/MoneyDigitsField.kt`
  (campo de costo/precio, patrón "buffer de dígitos", sección 3)
- `app/src/main/java/gt/marcos/joyeria/ui/product/add/CameraCaptureView.kt`
  (envoltorio de CameraX: permiso + `PreviewView` + botón de disparo)
- `app/src/main/res/values/strings.xml` — todos los strings nuevos
  (editar el existente, no crear uno nuevo).
- `AndroidManifest.xml` — **una sola línea nueva**:
  `<uses-permission android:name="android.permission.CAMERA" />`. Nada de
  almacenamiento: la foto se guarda en almacenamiento interno de la app
  (`filesDir`), no en galería/MediaStore, así que no hace falta ningún
  permiso de almacenamiento (CLAUDE.md sección 9: "prohibido pedir
  permisos que no se usen").

**No planeo ningún archivo de test.** `app/src/test/**` no está en
"Archivos permitidos" de Fase 03 (a diferencia de Fases 01 y 02), y la
fase no está marcada `[TESTS OBLIGATORIOS]` en `FASES.md`. Lo leo como
intencional: esta fase se verifica corriendo la app, no con JUnit. Si
preferís que igual agregue un par de tests de lógica pura (por ejemplo,
el parseo del buffer de dígitos, o el cálculo de dimensiones al
redimensionar la foto), decímelo y los agrego — pero por defecto no
voy a tocar `app/src/test/**` sin que me lo pidas, porque no está en la
lista permitida.

### 1.1) Un archivo que necesito que agregues a "Archivos permitidos": `MainActivity.kt`

Hoy `MainActivity` no tiene `@AndroidEntryPoint` (Hilt nunca se conectó
ahí, porque hasta ahora no había nada que inyectar en una pantalla) y
muestra un `HomePlaceholder` fijo. Para que la fase se pueda *usar* de
verdad — que es literalmente el criterio 4 — necesito:

1. Anotar `MainActivity` con `@AndroidEntryPoint`.
2. Obtener `AddProductViewModel` con `by viewModels()` (Hilt se lo provee
   automáticamente gracias al punto 1).
3. Reemplazar `HomePlaceholder` por `AddProductRoute(viewModel)`.

`MainActivity.kt` no está en "Archivos permitidos" de Fase 03. Es un
cambio mecánico de tres líneas, no agrega lógica de negocio ni pantallas
nuevas más allá de mostrar la que esta fase ya construye, pero como la
regla del proyecto es "si necesitás tocar otro archivo, parás y lo
anotás" — te lo pregunto en vez de tocarlo por mi cuenta. Si preferís
otro mecanismo (por ejemplo, dejar `MainActivity` como está y no
verificar el flujo real hasta una fase futura con navegación), decímelo
y ajusto el plan.

### 2) Los tres campos obligatorios, y los opcionales con su valor por defecto

**Obligatorios (3, ni uno más):**
1. **Foto** — sin foto, no hay `photoPath` que guardar; `Guardar` queda
   deshabilitado hasta que exista una captura.
2. **Costo** — campo de dígitos (sección 3). Vacío = no habilitado.
3. **Precio de venta** — mismo campo, pre-llenado con el precio sugerido
   apenas hay costo (sección 3), pero editable; ella puede aceptarlo tal
   cual o escribir el suyo.

**Opcionales, con default sensato (se editan después, Fase 04):**
- **Nombre** — si lo deja vacío, se guarda con un texto por defecto en
  español (`strings.xml`, ej. "Pieza sin nombre"), **no** con el `uid`:
  descarté usar el `uid` como nombre por defecto porque el nombre tiene
  que quedar resuelto *antes* de guardar (la UI lo resuelve con
  `stringResource`, ya que `domain` no puede leer `strings.xml`), y el
  `uid` recién se genera *durante* el guardado — usar el `uid` como
  default habría significado generar el `uid` en dos pasos separados
  (uno para el nombre, otro para el insert), perdiendo el `db.withTransaction`
  único que hace atómica la operación completa (sección 4). Un texto fijo
  evita esa complicación y es igual de claro para ella.
- **Categoría** — sin elegir = sin categoría (`category_id = null`, ya
  válido en el esquema, `ON DELETE SET NULL`).
- **Cantidad** — sin escribir = `1` (el caso más común: está cargando la
  pieza que tiene en la mano).
- **Notas** — sin escribir = `null`.

**Lo que dejo explícitamente fuera del formulario de esta fase, aunque
`ProductEntity` tiene el campo:** `supplier` (mayorista). `FASES.md`
Fase 03 solo menciona "Nombre, categoría, cantidad y notas" como
opcionales — no menciona `supplier`. Lo leo como que ese campo entra en
Fase 04 (edición), no en la alta rápida. Se guarda `null` y se completa
después. Si querés que esté también en esta pantalla, decímelo.

### 3) Cómo se escriben costo y precio: buffer de dígitos, no un campo decimal

Diseño (no está escrito en ningún `.md`, lo dejo explícito para tu OK):
el campo de costo/precio funciona como una caja registradora, no como un
campo de texto con punto decimal. Cada tecla que ella toca es un dígito;
el campo guarda un `String` de puros dígitos y lo **interpreta
directamente como centavos** (los últimos dos dígitos son siempre los
centavos): escribe "7", "5", "9", "0" y el campo muestra en vivo
`Q75.90`, sin que ella tenga que encontrar ni tocar el punto. Ventajas
sobre un campo decimal:
- Cero ambigüedad de separador decimal por locale (no hay que decidir si
  "." o "," separa los centavos).
- El valor que produce el campo **ya es** `Money.cents`, sin ningún
  parseo de texto decimal — nada que redondear ni que pueda fallar.
- Es el patrón estándar en apps de cobro/punto de venta, así que aunque
  ella no lo haya visto en esta app, es un patrón que probablemente
  conoce de otras apps de pago.

`MoneyDigitsField` (en `ui/product/add/`) hace el filtrado de dígitos y
el tope de longitud (9 dígitos, tope de `Q9,999,999.99` — de sobra para
joyería); `Money.format()` (en `ui/format/MoneyFormat.kt`) se usa para
mostrar el valor ya interpretado. Este archivo no necesita una función
de "parseo" separada: los dígitos crudos **son** los centavos.

**Precio sugerido en vivo:** mientras escribe el costo, si el campo de
precio todavía no fue tocado a mano, se **pre-llena** con
`PricingCalculator.suggestedPrice(cost, defaultMarkupBp, roundingStep)`
(leyendo `default_markup_bp` y `price_rounding_step_cents` de
`app_setting` una vez al abrir la pantalla). Si ella edita el precio a
mano, deja de auto-completarse (no le pisa lo que ya escribió). Debajo
del precio se muestra siempre "Ganancia: Qxx.xx" en vivo, con
`PricingCalculator.profit(cost, salePrice)` sobre los valores actuales
de los dos campos — esto cubre literalmente el entregable ("mientras
escribe el costo, la app muestra en vivo el precio sugerido y la
ganancia").

`AddProductViewModel` inyecta `AppSettingDao` directamente (interfaz que
ya existe desde Fase 01) para esa única lectura de dos claves; no creo
un `AppSettingRepository` nuevo porque no está en "Archivos permitidos"
de esta fase, y `ProductRepository` leyendo configuración de precios se
sentía peor (una clase llamada "Product" haciendo algo que no es de
productos). Lo anoto como nota de arquitectura, no como bloqueo — si
preferís que lo haga distinto, decímelo.

### 4) Arquitectura: dónde vive cada cosa y una aclaración sobre "los Composables no reciben ViewModels"

```
AddProductRoute (stateful)          <- ui/product/add, obtiene el ViewModel
  └─ collectAsStateWithLifecycle()
  └─ AddProductScreen(state, onXChanged, onSaveClick, ...)   (stateless, @Preview-able)

AddProductViewModel                 <- ui/product/add, StateFlow<AddProductUiState>
  └─ AddProductUseCase               <- domain/usecase, sin imports de Android
       └─ ProductRepository          <- data/repository, concreta (Room adentro)
            ├─ ProductDao (ya existe)
            ├─ ProductUidGenerator (ya existe, Fase 01)
            └─ AppDatabase.withTransaction { } envolviendo generar uid + insertar
```

- `MainActivity` (si se aprueba 1.1) instancia el ViewModel con
  `by viewModels()` (Hilt, gracias a `@AndroidEntryPoint`) y se lo pasa a
  `AddProductRoute`. **No** agrego `hilt-navigation-compose`: no hay
  `NavHost` en la app todavía, y `by viewModels()` en la Activity ya
  resuelve la inyección sin esa dependencia extra.
- `AddProductScreen` (el Composable de verdad, el que sería
  `@Preview`-able) recibe **estado y lambdas**, nunca el ViewModel — eso
  cumple la letra de CLAUDE.md sección 5. `AddProductRoute` es el
  wrapper delgado que sí conoce el ViewModel; es la excepción esperada
  (el "borde" entre Activity/DI y la UI pura), no una violación — lo
  dejo explícito porque la regla no lo aclara y prefiero que quede
  escrito antes de que alguien lo lea como una contradicción.
- `AddProductUseCase` recibe un `AddProductInput` ya resuelto (nombre y
  cantidad ya con su default aplicado por el ViewModel, que sí puede
  tocar `stringResource`/recursos porque vive en `ui`) y delega en
  `ProductRepository.insert(input)`, que hace TODO en un solo
  `db.withTransaction { }`: llama a `ProductUidGenerator.next()`
  (que ya es atómico por sí mismo, Fase 01) e inserta el `ProductEntity`,
  como una sola operación — más fuerte que dejarlas sueltas: si el
  insert fallara, el `uid` consumido también se revierte, no queda un
  hueco en la secuencia.
- `ProductRepository` no tiene una interfaz separada en `domain`
  (`domain/repository/ProductRepository.kt` no está en "Archivos
  permitidos" de esta fase). `AddProductUseCase` depende directo de la
  clase concreta de `data`. Es una simplificación respecto a Clean
  Architecture "de libro" (que pondría la interfaz en `domain`), pero
  respeta la única regla dura que exige `CLAUDE.md` sección 5: *"`domain`
  no importa nada de Android"* — `ProductRepository` expone solo tipos
  planos (`Money`, `String`, `Long`) en su firma pública, nunca
  `ProductEntity` ni nada de `androidx.room`, así que `AddProductUseCase.kt`
  no termina importando Room de forma indirecta. Si preferís la interfaz
  en `domain` desde ya, decímelo y la agrego (pediría sumar
  `domain/repository/**` a "Archivos permitidos").

### 5) Captura y compresión de la foto

1. **Permiso:** al tocar "Tomar foto" por primera vez,
   `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`
   pide `CAMERA`. Si lo niega, mensaje claro en español explicando para
   qué se usa (CLAUDE.md sección 6), sin crash, y puede reintentar
   tocando de nuevo. Si ya está concedido, va directo a la cámara.
2. **Vista previa y disparo:** `CameraCaptureView` arma un `ImageCapture`
   + `Preview` de CameraX, ligados al ciclo de vida
   (`ProcessCameraProvider` + `LifecycleCameraController` o
   `bindToLifecycle`, a confirmar cuál es más simple al implementar) y
   muestra un `PreviewView`. Un botón de disparo grande (56dp mínimo,
   CLAUDE.md sección 6) llama a `imageCapture.takePicture(executor,
   OnImageCapturedCallback)`, que entrega un `ImageProxy` en memoria (no
   escribo un archivo temporal sin comprimir primero).
3. **Redimensión + compresión** (`ImageStorage`, `util/`):
   - `ImageProxy` → `Bitmap` (vía su `planes`/`toBitmap()` o
     `ImageDecoder` según el formato que entregue CameraX — JPEG directo
     desde el sensor si está disponible, para no perder un paso de
     decodificación).
   - Si el lado mayor supera 1600px, se escala manteniendo el aspecto
     (regla de tres simple sobre ancho/alto) — esta función de "calcular
     el tamaño destino" es matemática pura (`Int`/`Int` sobre ancho y
     alto), la única parte de `ImageStorage` que sería trivial de
     testear sin Robolectric si en algún momento se habilita esa
     carpeta de test para esta fase.
   - Codifica a JPEG (`Bitmap.compress(JPEG, calidad, stream)`) empezando
     en una calidad alta (ej. 90) y bajando en pasos si el resultado
     supera 1MB, hasta que quede debajo o se llegue a un piso de calidad
     razonable (ej. 50) — documentado en el propio código, con el motivo
     de por qué existe un piso (no seguir degradando la imagen hasta que
     no se reconozca la pieza).
   - Guarda el JPEG final en `context.filesDir/photos/<nombre único>.jpg`
     (almacenamiento interno de la app, no galería — nadie más que la
     app lee estos archivos, coherente con "sin backend, sin compartir"
     de momento) y devuelve la ruta absoluta como `String` para
     `photoPath`.
   - Si ella retoma la foto antes de guardar, se borra el archivo de la
     captura anterior antes de escribir la nueva (para no acumular
     huérfanos en el almacenamiento interno con el tiempo).
4. **Verificación del criterio 5** ("la foto guardada pesa menos de
   1MB"): la hago con `adb shell run-as gt.marcos.joyeria ls -la
   files/photos/` (o `adb pull` del archivo) después de una captura real
   en el emulador, y anoto el tamaño exacto en `ESTADO.md` al cerrar la
   fase — es un criterio que el propio `FASES.md` pide "verificado y
   anotado", no un test automatizado.

### 6) Cómo pienso medir el criterio 4 (cronómetro, <20s) — leételo, es la parte que más quiero que confirmes

Lo que **yo sí puedo hacer** y voy a hacer al terminar de escribir el
código, antes de pedirte nada:
- Compilar (`assembleDebug`), instalar en el emulador
  (`Medium_Phone_API_35`, el mismo de Fases 00/01) y confirmar que abre
  sin crash (`adb logcat` limpio de `FATAL EXCEPTION`).
- Ejercitar el flujo completo con `adb shell input tap/text` (conceder
  el permiso de cámara, tocar "Tomar foto", disparar, escribir un costo,
  confirmar que el precio sugerido y la ganancia cambian en pantalla,
  tocar "Guardar", confirmar que aparece el `uid`) para probar que
  **funciona de punta a punta sin bugs** — cero crashes, cero pantallas
  colgadas, el dato se guarda de verdad en la base.
- Medir el tamaño real del archivo de foto guardado (criterio 5, sección
  5 de este plan).
- Contar los campos `required` del formulario y grepear comillas en los
  Composables (criterios 2 y 3 — esos sí son mecánicos, los hago yo).

Lo que **no puedo hacer** con esas herramientas: un script de `adb`
manda un tap o un texto en milisegundos, sin el tiempo real que le toma
a una persona mirar la pantalla, decidir qué tocar, encuadrar la foto
con la cámara y tipear con el dedo en un teclado táctil. Cronometrar eso
con `adb` daría un número falso — más rápido que cualquier uso real — y
el criterio 4 es explícito: *"Prueba manual documentada... tiempo real
de registro de una pieza, medido con cronómetro. Si pasa de 20s, la fase
no se cierra."* No voy a inventar ni estimar ese número.

**Lo que necesito de vos (o de quien haga la prueba):** después de que
yo confirme que el flujo funciona sin bugs (el paso anterior), voy a
pedir que alguien lo use de verdad — idealmente en un teléfono real, con
cronómetro en mano — registrando una pieza desde cero (cámara ya con
permiso concedido, que es el caso repetido real: el permiso se pide una
sola vez en la vida de la app, no en cada alta) y me pases el tiempo
que salió. Yo lo documento tal cual en `ESTADO.md`, sin redondear para
abajo. Si sale arriba de 20s, la fase se queda abierta hasta ajustar la
pantalla (menos campos visibles, atajos, lo que haga falta) y probar de
nuevo — no la cierro con un número que no se midió de verdad.

Decime si esto te sirve así, o si preferís otro método de medición (por
ejemplo, que vos mismo la corras en el emulador con mouse/teclado en vez
de un teléfono real).

### 7) Dependencias nuevas (una por vez, con `assembleDebug` entre cada una, versión verificada, nunca inventada)

Verificadas recién contra `maven-metadata.xml` real (Google Maven /
Maven Central), no inventadas:

| Dependencia | Versión | Fuente verificada |
|---|---|---|
| `androidx.camera:camera-core` | `1.6.2` | `dl.google.com/.../camera-core/maven-metadata.xml`, últimas en el listado antes de `1.7.0-alpha01` |
| `androidx.camera:camera-camera2` | `1.6.2` | ídem, mismo tren de versión (lockstep) |
| `androidx.camera:camera-lifecycle` | `1.6.2` | ídem |
| `androidx.camera:camera-view` | `1.6.2` | ídem (trae `PreviewView`) |
| `io.coil-kt.coil3:coil-compose` | `3.6.2` | `repo1.maven.org/.../coil-compose/maven-metadata.xml`, `<release>3.6.2</release>`, `lastUpdated` 2026-09-04 |
| `androidx.lifecycle:lifecycle-viewmodel-ktx` | `2.11.0` | `dl.google.com/.../lifecycle-viewmodel-ktx/maven-metadata.xml` — **reutiliza** el `version.ref` de `lifecycleRuntimeKtx` que ya existe en el catálogo (mismo tren de versión), no agrego un número nuevo |
| `androidx.lifecycle:lifecycle-runtime-compose` | `2.11.0` | ídem, mismo `version.ref` reutilizado |

Posibles, a confirmar recién al implementar (no comprometo versión
todavía porque depende de si hacen falta de verdad):
- `androidx.activity:activity-ktx` — solo si `by viewModels()` no
  resuelve ya de forma transitiva desde `androidx.activity:activity-compose`
  (lo confirmo compilando; si hace falta, reviso su versión real antes
  de agregarla, no asumo que coincide con `activityCompose`).
- `androidx.compose.material:material-icons-core` — **sin versión
  propia**, cubierta por `compose-bom` ya presente. La quiero para un
  ícono de cámara reconocible en el botón "Tomar foto" (más claro que
  solo texto para una usuaria no técnica, CLAUDE.md sección 6). Es
  de bajo riesgo (versión la resuelve el BOM), pero lo dejo anotado por
  si preferís que use solo texto y me ahorre esta dependencia.

Cada una se agrega sola, se corre `./gradlew assembleDebug`, se confirma
verde, y recién ahí la siguiente (CLAUDE.md §2.1). Cada una lleva su
entrada corta en `DECISIONES.md` antes de agregarla.

### 8) Verificación planeada de los criterios de aceptación

| # | Criterio | Cómo lo verifico |
|---|---|---|
| 1 | `assembleDebug` y `testDebugUnitTest` pasan | Corridos tras cada dependencia y al cerrar; la suite existente (`domain`, `data`) no debe romperse |
| 2 | Exactamente 3 campos `required` | Cuento a mano los campos marcados obligatorios en `AddProductScreen`/`AddProductUiState` (foto, costo, precio) |
| 3 | Sin strings literales en los Composables | `grep -n '"' app/src/main/java/gt/marcos/joyeria/ui/product/add/*.kt` — solo deben aparecer recursos (`R.string.*`), claves técnicas (rutas, formatos de fecha) o logs, nunca texto que ella lea |
| 4 | <20s con cronómetro real | Sección 6 — pendiente de una medición humana real, no la doy por cerrada sin ese número |
| 5 | Foto <1MB, verificado y anotado | `adb shell run-as`/`adb pull` sobre una captura real, tamaño anotado en `ESTADO.md` |

### Bloqueos / preguntas para el humano

1. **`MainActivity.kt` fuera de "Archivos permitidos"** (sección 1.1) —
   necesito agregarlo para poder mostrar la pantalla de verdad y medir
   el criterio 4. ¿Lo agrego a la lista?
2. **Metodología del criterio 4** (sección 6) — confirmame que el plan
   (yo verifico que funciona sin bugs + mido tamaño de foto; una persona
   real cronometra el uso) te sirve, o decime otra forma de medirlo.
3. **`supplier` fuera del formulario de esta fase** (sección 2) — ¿de
   acuerdo, o lo incluyo también?
4. **Patrón de "buffer de dígitos" para costo/precio** (sección 3) — ¿de
   acuerdo con ese diseño en vez de un campo decimal con punto?
5. **`AppSettingDao` inyectado directo en el ViewModel** en vez de un
   repositorio nuevo (sección 3) — ¿de acuerdo, o preferís otra forma
   dentro de los archivos permitidos?
6. **Sin archivos de test esta fase** (sección 1) — ¿de acuerdo, o
   agrego alguno igual (te pido agregar `app/src/test/**` a la lista)?

---

## Fase 03 — Respuestas del humano y ajustes finales al plan

**Fecha:** 2026-09-13. El humano respondió las 6 preguntas y agregó tres
cosas que faltaban (rotación de foto, fotos huérfanas, ícono+texto).
`FASES.md` ya se corrigió en un commit aparte (rama `docs/fix-fase-03-scope`,
commit `docs: fix fase-03 scope and stopwatch methodology`) con los
archivos permitidos nuevos y la metodología del cronómetro. Esta rama
(`fase/03-quick-add`) arranca desde ahí, no desde `main` directo — mismo
patrón que Fase 01 con `docs/fix-fase-01-scope`.

### Respuestas 1-6

1. **`MainActivity.kt`: aprobado**, limitado a las tres líneas descritas
   (`@AndroidEntryPoint`, `by viewModels()`, mostrar la pantalla nueva).
   Ya está en "Archivos permitidos" de `FASES.md`.
2. **Metodología del cronómetro: corregida.** Quien cronometra es **la
   usuaria final** (la mamá), en un teléfono real — no yo, ni el
   desarrollador, porque ya conocemos el diseño y sacaríamos un tiempo
   irreal. Dos mediciones: la primera vez que ve la pantalla, y una
   segunda después de registrar 2-3 piezas más. La segunda es la que
   cuenta para el criterio (uso repetido = caso real); si la primera es
   mucho peor, se anota como señal de diseño aunque no bloquee el cierre.
   Ya está en `FASES.md` criterio 4. Al terminar la implementación y mi
   propia verificación funcional, le voy a pedir al desarrollador que le
   pase estos pasos a su mamá y me traiga los dos tiempos.
3. **`supplier` fuera del formulario: confirmado.** Sin cambios al plan.
4. **Buffer de dígitos: confirmado.** Sin cambios al plan.
5. **`AppSettingDao` directo en el ViewModel: RECHAZADO.** El humano
   señaló correctamente que eso hace que `ui` importe `data/local`
   (Room), saltándose la capa `data/repository` — y que las fases 04+
   copiarían el atajo. Se agrega **`data/repository/AppSettingRepository.kt`**
   (ya sumado a "Archivos permitidos"): envuelve `AppSettingDao` y expone
   `getDefaultMarkupBp(): Int` y `getPriceRoundingStep(): Money`, ambos
   tipos planos. `AddProductViewModel` depende de este repositorio, nunca
   del DAO. `ProductRepository` no lo necesita (no lee configuración de
   precios).
6. **Tests: dos excepciones aprobadas**, ya en "Archivos permitidos":
   - `app/src/test/java/.../ui/product/add/MoneyDigitsInputTest.kt` —
     que `"7590"` interprete como `7590` centavos (Q75.90), no `Q7,590`
     ni `75900`. Es el punto exacto donde el dinero entra al sistema
     desde el dedo de la usuaria; un error de un factor de 10 ahí
     arruina todos los precios de la app sin que nada lo note. Para que
     sea testeable sin Robolectric, la lógica de interpretación vive en
     una función pura (`digitsToCents`), **separada** del Composable del
     campo — no adentro de él.
   - `app/src/test/java/.../util/ImageStorageScalingTest.kt` — un lado
     mayor de 4000px con aspecto 4:3 tiene que dar exactamente 1600 y su
     proporción correcta. También función pura, separada de la parte de
     `ImageStorage` que toca `Bitmap`/archivos (esa sigue sin test
     automatizado, se verifica a mano per sección 5 del plan original).

### A) Rotación de la foto — agregado al plan

CameraX entrega `imageInfo.rotationDegrees` (0/90/180/270) aparte de los
píxeles del `ImageProxy`; si no se aplica antes de comprimir, la foto
sale de costado aunque el archivo pese lo correcto. Plan: `ImageStorage`
recibe el `rotationDegrees` junto con el `Bitmap` y aplica un
`Matrix.postRotate(rotationDegrees)` antes de redimensionar/comprimir.
Verificación: **visual**, no solo de tamaño de archivo — voy a hacer una
captura real en el emulador, traerla con `adb pull` y mirarla (puedo ver
imágenes directamente) para confirmar que una foto tomada "en vertical"
se guarda en vertical, no de costado. Esto queda además como bullet
nuevo en el entregable de `FASES.md` (ya aplicado).

### B) Fotos huérfanas — decisión: mitigar el caso común ahora, documentar el resto como deuda

El caso que sí resuelvo en esta fase: ella toma la foto y sale de la
pantalla sin guardar (botón atrás, que hoy cierra la app porque no hay
otra pantalla a la que navegar). `AddProductViewModel.onCleared()`
borra el archivo de la foto pendiente si nunca se llegó a guardar un
producto con ella. Esto cubre el flujo normal de "me arrepentí y salí".

Lo que **no** resuelvo acá, y documento como deuda técnica explícita:
si el proceso muere sin pasar por `onCleared()` (el sistema operativo
mata la app por memoria, o un crash entre la captura y el guardado), el
archivo queda huérfano en `filesDir/photos/` para siempre. Propuesta
para una fase futura (Fase 04 u 11, donde ya existe una pantalla de
inventario o de respaldo que recorre todos los productos): una barrida
de arranque que liste `filesDir/photos/`, la compare contra
`SELECT photo_path FROM product` (incluyendo archivados, **no**
anulados/cancelados no aplica acá porque son productos, no ventas) y
borre los archivos que no estén referenciados por ningún producto. Es
más robusta que intentar atrapar cada camino posible de abandono
(incluido este mismo `onCleared()`, que sería redundante si existiera
la barrida) porque cubre todos los casos de una sola vez, pero es
trabajo de otra fase — no lo hago ahora para no ampliar el alcance de
"Archivos permitidos" de Fase 03 con una tarea de mantenimiento que no
tiene relación con el alta rápida.

### C) Ícono de cámara — ajustado

El botón "Tomar foto" muestra **ícono y texto juntos** (`Row` con
`Icon(Icons.Default.PhotoCamera)` + `Text("Tomar foto")`), no el ícono
solo. Confirmado: un ícono suelto es una adivinanza para una usuaria no
técnica (CLAUDE.md sección 6).

### Con esto, arranco la implementación

Sin más preguntas pendientes. Lo que sigue en esta misma entrada es el
trabajo real (dependencias, código, verificación), documentado a medida
que avanza — no antes de terminarlo, como en las fases anteriores.

---

## Fase 03 — Implementación, bugs encontrados y verificación

**Fecha:** 2026-09-13. Rama `fase/03-quick-add`, desde `docs/fix-fase-03-scope`
(que a su vez sale de `main`, con `fase-02-ok` y `fix-seed-markup-bp`
mergeadas) — mismo patrón que Fase 01 con `docs/fix-fase-01-scope`.

### Archivos creados/tocados

Dentro de "Archivos permitidos" (ya corregido en `docs/fix-fase-03-scope`):
- `domain/usecase/AddProductUseCase.kt` (+ `AddProductInput`)
- `data/repository/ProductRepository.kt`
- `data/repository/AppSettingRepository.kt`
- `util/ImageStorage.kt`
- `ui/format/MoneyFormat.kt` (`Money.format()`, pendiente desde Fase 02)
- `ui/product/add/AddProductUiState.kt`
- `ui/product/add/AddProductViewModel.kt`
- `ui/product/add/AddProductScreen.kt`
- `ui/product/add/AddProductRoute.kt`
- `ui/product/add/MoneyDigitsInput.kt` (funciones puras)
- `ui/product/add/MoneyDigitsField.kt` (Composable)
- `ui/product/add/CameraCaptureView.kt`
- `MainActivity.kt` (las tres líneas acordadas: `@AndroidEntryPoint`,
  `by viewModels()`, mostrar `AddProductRoute`)
- `AndroidManifest.xml` (una línea: permiso de cámara)
- `strings.xml` (strings nuevos)
- `test/.../ui/product/add/MoneyDigitsInputTest.kt`
- `test/.../util/ImageStorageScalingTest.kt`
- `gradle/libs.versions.toml`, `app/build.gradle.kts` (dependencias)
- `DECISIONES.md`, `ESTADO.md` (proceso)

No se tocó nada fuera de esta lista.

### Dependencias agregadas (una por vez, con `assembleDebug` entre cada una)

CameraX `1.6.2` (4 artefactos), Coil `3.3.0` (no `3.6.2`, ver más abajo),
`lifecycle-viewmodel-ktx`/`lifecycle-runtime-compose` `2.11.0`
(reutilizando el `version.ref` existente), `material-icons-extended` (no
`-core`, ver más abajo). Cada una con su entrada en `DECISIONES.md`
(D-017 a D-021) **antes** de agregarla, como pide CLAUDE.md.

### Bugs reales encontrados y corregidos durante la implementación

**1) Coil 3.6.2 rompía la compilación de todo el proyecto, no solo de
Coil.** Al escribir el primer código que usa Coil de verdad,
`assembleDebug` falló con `Class 'kotlin.Unit' was compiled with an
incompatible version of Kotlin` y "Unresolved reference" sobre `apply`,
`filter`, `to`... en archivos sin relación con Coil. Diagnóstico con
`dependencyInsight`: `coil-android:3.6.2` fuerza `kotlin-stdlib:2.4.10`,
más nuevo que el Kotlin `2.2.10` pineado del proyecto, y Gradle resuelve
el conflicto tomando la versión más alta de **todo** el árbol —
contaminando el proyecto entero. Corregido bajando a Coil `3.3.0`
(pide `kotlin-stdlib:2.2.0`, verificado versión por versión contra los
POMs reales). Detalle completo en **D-018** (`DECISIONES.md`).

**2) `Icons.Default.PhotoCamera` no existe en `material-icons-core`.**
Supuesto mío sin verificar (intenté confirmarlo por HTTP contra el repo
de `androidx`, no lo encontré, y seguí igual). El error real
(`Unresolved reference 'PhotoCamera'`) apareció recién cuando el primer
error de Coil ya no tapaba todo lo demás. Corregido con
`material-icons-extended`. Detalle en **D-020**.

**3) Warning nuevo de Kotlin sobre destino de anotación.** `@ApplicationContext`
en un parámetro de constructor (`ImageStorage`) generaba
`This annotation is currently applied to the value parameter only, but
in the future it will also be applied to field` (KT-73255). Corregido
con `@param:ApplicationContext`, explícito, siguiendo la sugerencia del
propio compilador. No es un error, pero es un warning nuevo, y
`assembleDebug` "sin warning nuevo" es un criterio real del proyecto
(Fase 00 lo dejó escrito y nunca se derogó).

**4) `Button` de Guardar con `.size(56.dp)` en vez de `.height(56.dp)`.**
Bug propio, encontrado releyendo el código antes de compilar (no por un
error del compilador): `.size()` fija ancho **y** alto, así que
`.fillMaxWidth().size(56.dp)` habría dejado un botón cuadrado de 56dp,
no un botón de ancho completo con 56dp de alto (CLAUDE.md sección 6, área
táctil mínima). Corregido a `.height(56.dp)` antes de la primera
compilación exitosa.

**Los tres primeros son bugs de dependencias/build, no de lógica de
negocio — pero son exactamente el tipo de error que CLAUDE.md sección
2.1 existe para atrapar ("una dependencia a la vez, compilás y
confirmás"): los encontré recién al escribir el primer código que las
usaba de verdad, no al agregarlas.**

### Verificación automática

- `./gradlew clean assembleDebug testDebugUnitTest` → `BUILD SUCCESSFUL`,
  sin ningún warning nuevo (los dos preexistentes desde Fase 00/01 —
  "Unable to strip..." y el de Robolectric — siguen igual).
- 55 tests, 0 fallos, en las 8 suites (`domain`, `data` y las dos nuevas
  de Fase 03: `MoneyDigitsInputTest` 7 tests, `ImageStorageScalingTest`
  5 tests).
- Criterio 3 (`grep '"'` en `ui/product/add`): solo aparecen comentarios,
  claves de default (`""`), y datos de ejemplo del `@Preview` — ningún
  `Text("texto literal")`. Verificado explícitamente que cada `Text(...)`
  del directorio usa `stringResource` o un parámetro ya resuelto.
- Criterio 2 (exactamente 3 campos obligatorios): por diseño —
  `AddProductUiState.canSave` solo exige `photoPath`, `costDigits`,
  `salePriceDigits`. Confirmado además funcionalmente (ver abajo):
  "Guardar" quedó deshabilitado hasta que los tres tuvieron valor.

### Verificación funcional (emulador `Medium_Phone_API_35`)

Instalé el APK, abrí la app, y ejercité el flujo completo con `adb`
(permiso de cámara, captura, escritura de costo/precio, expandir "Más
detalles", Guardar). Sin ningún `FATAL EXCEPTION` en `logcat` en ningún
punto.

**Nota sobre la metodología, para que quede clara la diferencia con el
criterio 4:** el flujo automatizado con `adb shell input tap/text` tuvo
un problema real de sincronización — el teclado numérico en pantalla
tapaba el botón "Guardar", así que mis primeros toques ahí en realidad
tocaban teclas del teclado y seguían agregando dígitos al campo de costo
en vez de guardar (por eso aparecieron valores como "Q40,009.97" en las
capturas: no es un bug de la app, es que un script sin pausas entre
pasos no espera a que el teclado se cierre, algo que una persona hace
sin pensarlo). Lo até con `uiautomator dump` (lee el árbol de
accesibilidad real, no una captura de pantalla) hasta encontrar las
coordenadas correctas. **Esto es exactamente el argumento de la sección
6 del plan:** un script no reproduce el ritmo de una persona real, y acá
quedó documentado un ejemplo concreto de por qué.

También encontré que `adb exec-out screencap` devolvía capturas
desactualizadas varias veces seguidas en este emulador (mostraban
`Q0.00` bastante después de que el campo ya tenía otro valor, confirmado
por `uiautomator dump` tomado en el mismo instante) — un problema de la
herramienta de captura en este entorno, no de la app; verificado
comparando contra el árbol de accesibilidad, que sí reflejaba el estado
real en todo momento.

**Resultado del flujo completo, verificado con `uiautomator dump` tras
sacar el teclado de en medio:**
1. Tocar "Tomar foto" → pide permiso de cámara (diálogo del sistema, en
   inglés porque el emulador está en inglés — no es texto de la app) →
   "While using the app" → vista previa de CameraX real (el emulador
   simula una habitación) → disparador → vuelve a `AddProductScreen`
   con el thumbnail de la foto, botón pasa a decir "Cambiar foto".
2. Escribir dígitos en "Costo" → el campo muestra el monto formateado en
   vivo, "Precio de venta" se pre-llena con el sugerido, y aparecen
   "Precio sugerido: Qxx.xx" y "Ganancia: Qxx.xx" en vivo — verificado
   que los tres números son consistentes entre sí con las fórmulas
   reales de `PricingCalculator` (ej. sugerido = costo × 2 redondeado a
   Q5; ganancia = precio − costo), no solo que "cambian".
3. "Guardar" queda deshabilitado hasta que hay foto + costo + precio, se
   habilita después — confirma el criterio 2.
4. Al guardar: banner **"Guardada como XP-000001"** (el `uid` se genera
   y se muestra, tal como pide el entregable), y el formulario se
   resetea a blanco (foto, costo y precio vuelven a estar vacíos, botón
   vuelve a decir "Tomar foto") — listo para la siguiente pieza sin
   navegar a ningún lado, coherente con el flujo real de cargar varias
   piezas seguidas.

### Verificación de la foto guardada (criterio 5 y punto A, rotación)

**Extracción correcta del archivo, nota para el futuro:** mi primer
intento de traer el archivo con `adb shell run-as ... cat archivo.jpg >
local.jpg` (sin `exec-out`) corrompió el archivo — el shell de `adb`
traduce bytes de control en el camino, típico problema de extraer
binarios por `adb shell` en vez de `adb exec-out`. El archivo "corrupto"
resultante tenía 134138 bytes contra los 133585 reales (553 bytes de
basura agregada). Repetido con `adb exec-out run-as ... cat archivo.jpg
> local.jpg` (binario seguro): tamaño exacto, JPEG válido. Anotado acá
para no repetir el error si hace falta traer otro archivo de un
dispositivo en una fase futura.

- **Tamaño:** `133585` bytes (~130KB) — bien por debajo de 1MB. Criterio
  5 cumplido.
- **Rotación (punto A):** confirmado **visualmente**, no solo por
  tamaño — abrí el JPEG extraído correctamente y la escena (una
  habitación simulada por el emulador) se ve derecha, en vertical, no de
  costado ni al revés. Dimensiones reales del archivo: 960×1280 (ancho
  < alto, proporción de retrato) — coherente con una rotación aplicada
  de verdad sobre una captura que el sensor entrega nativamente en
  landscape.
- **Fila real en la base de datos** (extraída con el mismo cuidado
  binario, incluyendo `-wal`/`-shm` porque Room usa WAL y los datos
  recién commiteados viven ahí, no en el archivo principal):
  ```
  (1, 'XP-000001', 'Pieza sin nombre', None, 4000997, 8002000, 1,
   '/data/user/0/gt.marcos.joyeria/files/photos/8f1763e0-....jpg',
   None, None, 0)
  ```
  `uid` correcto, `name` con el default esperado ("Pieza sin nombre",
  porque dejé el campo vacío a propósito), `category_id` y `supplier`
  en `null` (D-021 y la pregunta 3 respectivamente), `stock_qty = 1`
  (default), `photo_path` apuntando al archivo verificado arriba,
  `archived = 0`. Todo exactamente como se diseñó.

### Lo que falta — criterio 4, no lo doy por cumplido

Como quedó dicho en el plan: no puedo cronometrar un uso humano real. Lo
que sí hice (arriba) prueba que el flujo **funciona** de punta a punta
sin bugs; lo que falta es que se **use** de verdad. Necesito que le
pidas a tu mamá, en un teléfono real:

1. **Primera medición:** que abra la app por primera vez y registre una
   pieza cualquiera (foto + costo + precio, nada más), cronometrada
   desde que ve la pantalla hasta que aparece "Guardada como...".
2. Que registre 2 o 3 piezas más, sin pausa larga entre ellas.
3. **Segunda medición:** cronometrar una pieza más después de esas
   repeticiones.

Documentá o pasame los dos tiempos (el de la primera vez, y el de
después de practicar) tal como salgan. La fase se cierra sobre la
**segunda** medición (<20s); la primera se anota igual, y si es mucho
peor que la segunda es una señal de diseño a revisar, no un motivo de
rechazo automático.

### Deuda técnica

- **Fotos huérfanas si el proceso muere sin pasar por `onCleared()`**
  (sistema mata la app, o crash entre captura y guardado): documentado
  en la sección de respuestas de arriba, con la propuesta de barrida de
  arranque para una fase futura. El caso común (foto tomada, sale de la
  pantalla sin guardar) sí está cubierto.
- Ninguna otra deliberada.

### Suposición tomada durante la implementación, no preguntada antes (D-021)

Sin selector de categoría en el formulario de alta rápida —
`categoryId` siempre `null` desde esta pantalla, se asigna al editar
(Fase 04). Detalle y razonamiento completo en **D-021**
(`DECISIONES.md`). A diferencia de `supplier` (pregunta 3, ya aprobada
explícitamente), esta decisión la tomé yo solo durante la
implementación porque el conflicto de arquitectura (necesitaría un
`CategoryRepository` fuera de "Archivos permitidos", o repetir el mismo
atajo de DAO-directo-en-ViewModel que ya se corrigió una vez en esta
misma fase) apareció recién al escribir el código, no en la etapa de
plan. Queda pendiente de tu confirmación.

### Lo que NO hice

- No taguée `fase-03-ok` ni mergeé la rama — falta el criterio 4.
- No implementé la barrida de fotos huérfanas (deuda técnica explícita
  arriba).
- No agregué selector de categoría (D-021).
- No toqué ningún archivo fuera de "Archivos permitidos".

### Bloqueos / preguntas para el humano

1. **Criterio 4** — pendiente de que tu mamá haga las dos mediciones
   reales (sección de arriba). La medición es mañana.
2. ~~**D-021**~~ **Aprobada.** El humano confirmó que la decisión y el
   razonamiento son correctos, y pidió dejar anotado en `FASES.md`
   Fase 04 que el selector de categoría se suma también a la pantalla de
   alta rápida — ya aplicado en el commit `docs: version compatibility
   rule and fase-04 category selector` (rama `docs/version-compat-and-fase04-scope`,
   mergeada a `main`).

---

## Excepción al flujo de `FASES.md`: dos fases abiertas a la vez (Fase 03 y Fase 04)

**Fecha:** 2026-09-14. `FASES.md` dice "una fase a la vez... no se empieza
la siguiente sin autorización humana explícita" y da a entender que una
fase se cierra (tag) antes de que arranque la próxima. El humano autorizó
explícitamente una excepción puntual a esto:

- **Fase 03 sigue abierta**, sin tag `fase-03-ok`: todo lo demás está
  hecho y verificado (ver la entrada de cierre de Fase 03 más arriba),
  pero falta el criterio 4 — la medición real con cronómetro, que tiene
  que hacer la usuaria final (su mamá) en un teléfono real. Esa medición
  es mañana (2026-09-15).
- **Fase 04 arranca igual**, sin esperar esa medición, porque el humano
  lo autorizó explícitamente ("Podés arrancar la Fase 04 aunque la Fase
  03 siga abierta").
- **`fase/04-inventory` sale de `fase/03-quick-add`, no de `main`** —
  instrucción explícita, con la razón escrita: Fase 04 necesita el
  código de Fase 03 (la pantalla de alta rápida, que Fase 04 además va a
  modificar para sumarle el selector de categoría). Si la medición de
  mañana sale mal y hay que ajustar la pantalla de alta rápida, esos
  cambios van en `fase/03-quick-add` (o una rama de corrección que salga
  de ahí) y **después se propagan** a `fase/04-inventory` (merge o
  rebase, a decidir en el momento según qué tan grande sea el ajuste) —
  no se duplica el arreglo a mano en las dos ramas.
- Cuando la medición salga bien y se taguee `fase-03-ok`, `fase/03-quick-add`
  se mergea a `main` como siempre. `fase/04-inventory` en ese momento ya
  tiene ese código (porque salió de ahí), así que no hace falta ningún
  paso extra por ese lado — sigue su curso normal hasta su propio cierre.

Esto es una excepción puntual autorizada, no un cambio a la regla general
de `FASES.md` ("una fase a la vez"): la próxima fase después de la 04
vuelve a esperar el cierre formal de la anterior, salvo que se autorice
otra excepción explícita como esta.

---

## Fase 04 — Plan (antes de escribir código)

**Fecha:** 2026-09-14. Releí completos `CLAUDE.md`, `FASES.md` (Fase 04,
ya con el selector de categoría sumado), `ESQUEMA.md` (`product`,
`price_history`, `purchase`, `purchase_item`, `category`) y
`DECISIONES.md`. No abrí `fase/04-inventory` todavía, no toqué código —
sigo en `fase/03-quick-add`, esperando OK antes de crear la rama nueva
(la instrucción de abrirla y la de "plan y parás" apuntan en direcciones
distintas; elegí no crear nada hasta tener el OK, es lo más conservador
y reversible).

### La pregunta que pediste que conteste primero: ¿una fase o dos?

**Honestamente, dos.** No es una preferencia estética — hay tres razones
concretas:

1. **Tocan tablas y flujos distintos, sin superposición real.**
   Inventario opera sobre `product` (y ahora también `category` para el
   selector) y escribe en `price_history` como efecto secundario de
   editar un campo que ya existe. Compras crea filas nuevas en `purchase`
   y `purchase_item` — una entidad de negocio completamente distinta
   (una transacción con líneas), no una edición de algo que ya existe.
   Que las dos toquen la tabla `product` de refilón (compras También
   debería actualizar `stock_qty`, y probablemente `cost_cents` — ver
   pregunta abajo) no las vuelve la misma pantalla ni el mismo trabajo.
2. **"Compras" es estructuralmente una vista previa de "Venta" (Fase 05
   actual), no un anexo de "Inventario".** Una compra es "elegís
   productos existentes, les ponés cantidad y costo por línea, aplicás
   un prorrateo opcional, confirmás una transacción con varias líneas".
   Eso es el mismo patrón de UI y de dominio que va a tener el registro
   de venta (Fase 05/06), no el patrón de "lista + formulario de edición
   de un solo registro" que es Inventario. Meterlas juntas mezcla dos
   arquitecturas de pantalla distintas bajo un solo nombre de fase.
3. **El prorrateo ya pedía su propio rigor de tests** — el único
   `[TESTS OBLIGATORIOS]` inline de toda la Fase 04 original está en el
   criterio de prorrateo (criterio 2), no en el de `price_history`
   (criterio 3, sin la etiqueta). Eso ya era una señal de que el bloque
   de compras es del mismo calibre que fases que sí tuvieron el tag
   completo (02, 01) — separarlo le da ese peso de verdad, con su propio
   commit y su propio diff auditable, en vez de quedar diluido adentro
   de una fase que también tiene que revisarse por el listado y la
   edición.

Un cuarto punto, más práctico: **"Archivos permitidos" de la Fase 04
original ya era la lista más ancha de todo `FASES.md`**
(`ui/product/**`, `ui/purchase/**`, `domain/usecase/**` completo,
`data/**` completo) — más ancha que ninguna fase antes de esta. Eso va
justo en contra de lo que dice la razón del ritual en `CLAUDE.md` sección
7: "el humano audita... leyendo únicamente `ESTADO.md` y el diff del
tag. Si mezclás fases... la auditoría se vuelve imposible." Una lista de
archivos permitidos así de ancha ya era el síntoma.

### Propuesta de split

**Fase 04 — Inventario y edición** (lo que arrancaría ahora)
- Listado, búsqueda, filtro por categoría, detalle/edición,
  `price_history`, archivado, selector de categoría (también en alta
  rápida).

**Fase 05 — Compras a mayorista** (nueva, después de esta)
- Registro de compra con líneas, prorrateo de transporte local.

**Esto corre el número de todas las fases de la 05 en adelante en uno**
(05 Venta de contado → 06, 06 Clientes → 07, 07 WhatsApp → 08, 08
Reportes → 09, 09 Catálogo → 10, 10 Códigos de barras → 11, 11 Respaldo
→ 12, 12 Pulido → 13). También cambia "MVP = fases 00 a 05" a "00 a 06"
en el encabezado de `FASES.md` (Compras se suma al MVP, en el mismo
lugar donde ya estaba, solo que ahora es su propio número). Y hay que
corregir dos comentarios en el código ya commiteado de Fase 01
(`PurchaseEntity.kt`, `PurchaseItemEntity.kt`: "su DAO y su UI llegan en
Fase 04" → "Fase 05") y la nota equivalente en `ESQUEMA.md`.

**Alternativa que descarto pero dejo anotada:** no renumerar y llamar a
la segunda mitad "Fase 04b" o "Fase 04.1". La descarto porque rompe el
único patrón de nombres que tiene todo el proyecto hasta ahora
(`fase-NN-ok`, un entero por fase, sin excepciones) por ahorrarse una
edición de texto — el costo de renumerar es de una sola vez, hoy; el
costo de un nombre de fase inconsistente para siempre es peor.

**Si estás de acuerdo con el split:** lo aplico en un commit de solo
documentación (rama nueva desde `main`, mismo patrón que las anteriores)
antes de tocar código de Fase 04, y registro la decisión como **D-022**
en `DECISIONES.md`. Si preferís mantenerla como una sola fase, sigo con
el plan de abajo pero sin separar "Compras" — decímelo y ajusto.

### Una pregunta de diseño que el split deja explícita (no estaba resuelta ni en la fase original combinada)

¿Una compra **actualiza** `product.cost_cents` (y por lo tanto también
inserta en `price_history`, por la regla ya establecida en esta fase) y
`product.stock_qty` del producto comprado? `ESQUEMA.md` no lo dice
explícito para `purchase_item` — solo define cómo se calcula "el costo
real unitario" de la línea (`unit_cost_cents + allocated_extra_cents /
qty`), no qué hace la app con ese número después. Mi lectura, pero
prefiero confirmarla antes de que sea relevante (en Fase 05, no en esta):
sí a las dos — una compra sin que suba el stock no tendría sentido de
negocio (para eso existe "compra"), y el costo real de la línea debería
convertirse en el nuevo `cost_cents` del producto (con su fila de
`price_history`), para que el precio sugerido y la ganancia reflejen lo
que de verdad pagó la última vez. Lo dejo escrito acá para cuando
llegue Fase 05, no hace falta resolverlo ahora.

### Archivos que tocaría Fase 04 — Inventario (asumiendo el split)

- `app/src/main/java/gt/marcos/joyeria/ui/product/list/**` (listado)
- `app/src/main/java/gt/marcos/joyeria/ui/product/edit/**` (detalle/edición)
- `app/src/main/java/gt/marcos/joyeria/ui/product/add/**` (ya existe,
  Fase 03; se modifica para sumar el selector de categoría)
- `app/src/main/java/gt/marcos/joyeria/data/repository/CategoryRepository.kt`
  (ya en "Archivos permitidos", D-021/pedido explícito)
- `app/src/main/java/gt/marcos/joyeria/data/repository/ProductRepository.kt`
  (ya existe, Fase 03; se le agregan métodos de listado/búsqueda/edición/archivado)
- `app/src/main/java/gt/marcos/joyeria/data/repository/PriceHistoryRepository.kt` (nuevo)
- `app/src/main/java/gt/marcos/joyeria/data/local/dao/PriceHistoryDao.kt` (nuevo, la tabla ya existe desde Fase 01)
- `app/src/main/java/gt/marcos/joyeria/di/DatabaseModule.kt` (proveer el DAO nuevo — técnicamente no está en "Archivos permitidos" de ninguna fase desde que se creó en Fase 01; lo marco como pedido de archivo extra, ver bloqueos)
- `app/src/main/java/gt/marcos/joyeria/domain/usecase/**` (casos de uso de editar/archivar)
- `app/src/main/res/values/strings.xml`

**Navegación — esto es nuevo, y quiero que lo veas antes de que lo
agregue:** hasta ahora la app tiene una sola pantalla (`MainActivity`
muestra `AddProductRoute` directo, sin ningún tipo de navegación). Fase
04 necesita moverse entre Listado ↔ Detalle/Edición ↔ Alta rápida. CLAUDE.md
sección 2 (stack fijo) no incluye ninguna librería de navegación en la
tabla. Propongo agregar **`androidx.navigation:navigation-compose`**
(versión a verificar contra `maven-metadata.xml` real, y contra la regla
nueva de CLAUDE.md 2.1: que no exija un Kotlin/AGP/`compileSdk` más
nuevo que la línea base — la aplico de entrada esta vez) en vez de un
"switch" de pantallas armado a mano, porque: la app ya tiene 3+ pantallas
y va a seguir sumando (Fase 05 Compras, Fase 05/06 actual Venta, Fase 06/07
Clientes...), y un manejo de pantallas hecho a mano termina reinventando
mal una versión peor de lo mismo (manejo de back stack, restauración de
estado). Es una dependencia nueva fuera del stack fijo original de
`CLAUDE.md`, así que la marco para tu confirmación explícita en vez de
agregarla por mi cuenta.

**Pantalla de inicio, nota aparte:** CLAUDE.md sección 6 pide que la
pantalla de inicio tenga "Vender" y "Agregar pieza" como las dos
acciones grandes. "Vender" no existe todavía (llega en Fase 05/06
según el número final). Un botón "Vender" que no hace nada sería
exactamente el stub que miente que CLAUDE.md sección 5 prohíbe. Mi plan:
un `MainActivity`/home provisorio con las dos acciones que sí existen
hoy — "Agregar pieza" e "Inventario" — y dejo anotado que la pantalla de
inicio "de verdad" (con "Vender") llega cuando Vender exista. Si
preferís otra cosa, decímelo.

### Pantallas

**Listado** (`ui/product/list/ProductListScreen.kt` + ViewModel):
- Cada fila: foto (Coil), nombre, `uid`, stock, precio (`Money.format()`),
  ganancia (`PricingCalculator.profit`).
- Buscador (texto libre, filtra por nombre o `uid` — como pide el
  entregable).
- Filtro por categoría (chips u dropdown, usando `CategoryRepository`).
- Solo productos activos (`archived = false`) — igual que ya hace
  `ProductDao.observeActive()`.
- Tocar una fila navega al detalle/edición.

**Detalle/edición** (`ui/product/edit/ProductEditScreen.kt` + ViewModel):
- Todos los campos editables: nombre, categoría (selector), costo,
  precio de venta (con el mismo campo de "buffer de dígitos" de Fase 03,
  reusando `MoneyDigitsField`/`MoneyDigitsInput`), cantidad en stock,
  notas, proveedor (este sí entra acá, aunque quedó fuera del alta
  rápida por diseño — CLAUDE.md 1: "todo lo demás es opcional y se edita
  después", y "después" es esta pantalla).
- Al guardar: si `cost_cents` o `sale_price_cents` cambiaron respecto al
  valor cargado, inserta una fila en `price_history` con el valor
  **anterior** (o el nuevo — a definir con el test, ver criterios) antes
  de actualizar `product`.
- Botón "Archivar" con confirmación explícita (CLAUDE.md sección 6:
  texto claro de qué va a pasar) — nunca `DELETE`.

**Alta rápida** (`ui/product/add/**`, ya existe): se le agrega el
selector de categoría (opcional, sigue sin ser uno de los 3 campos
obligatorios) usando el mismo `CategoryRepository`.

### Tests planeados **[TESTS OBLIGATORIOS] aplica a esta fase completa si se separa el prorrateo, porque el criterio de `price_history` deja de compartir fase con uno que no lo pedía**

- `ProductRepositoryTest` o similar (Robolectric, Room in-memory, mismo
  patrón que `ProductDaoTest`/`SeedDataTest` de Fase 01): editar
  costo/precio de un producto inserta exactamente una fila en
  `price_history` con los valores correctos; editar un campo que no es
  costo/precio (ej. notas) **no** inserta ninguna fila — para que el
  test no pase "de casualidad" por escribir siempre, pase lo que pase.
- Test de que archivar no borra la fila (`SELECT` sigue encontrando el
  producto, solo con `archived = true`), y que desaparece de
  `observeActive()` — mismo patrón que `ProductDaoTest` de Fase 01.
- `grep -r "DELETE FROM product" app/src` vacío (criterio 4 original,
  literal).
- Test de búsqueda/filtro si la lógica de filtrado vive en el
  repositorio (no en SQL puro dentro de un `@Query` con `LIKE`, que ya
  se autoverifica con Room).

### Respuestas del humano a las 5 preguntas — todo resuelto

1. **Split confirmado.** "Partila", con la razón 3 (el `[TESTS OBLIGATORIOS]`
   inline del prorrateo) señalada como la más contundente. Aplicado en
   rama `docs/split-fase-04` (desde `main`), commit `docs: split
   fase-04 into inventory (04) and purchases (05), renumber 05-12 to
   06-13`, mergeado a `fase/04-inventory`. Registrado como **D-022** en
   `DECISIONES.md`. Verificadas y corregidas todas las referencias
   cruzadas a números de fase en `FASES.md`, `ESQUEMA.md` y
   `DECISIONES.md` (nota agregada a D-011, sin editar su texto
   original); `CLAUDE.md` no tenía ninguna.
2. **Navigation Compose: aprobado.** Es la primera ampliación del stack
   fijo de CLAUDE.md sección 2 desde que se escribió — registrado como
   **D-023** (ver abajo), con la comparación explícita contra la
   alternativa (`when` sobre un estado en `MainActivity`) que pidió el
   humano.
3. **`DatabaseModule.kt` a "Archivos permitidos": aprobado.** Ya está en
   la lista de Fase 04 (rama `docs/split-fase-04`).
4. **Pantalla de inicio provisoria: aprobada**, con el razonamiento de
   "no poner un botón muerto" confirmado como correcto. El humano pidió
   explícitamente que quede registrada como desviación temporal de
   CLAUDE.md sección 6, con fecha de vencimiento — hecho como **D-024**
   (ver abajo).
5. **Efecto de una compra sobre `product`: el humano lo rechazó
   explícitamente** — no es una decisión que yo pueda dar por resuelta
   con una lectura propia, porque cambia la ganancia histórica según la
   opción, y eso no es un detalle técnico menor. Ver la sección nueva
   abajo, "Compras y el costo del producto — decisión pendiente", con
   las opciones y sus consecuencias, sin resolver, para que el humano
   elija antes de que arranque Fase 05. No bloquea Fase 04.

### D-023 y D-024 — registradas en `DECISIONES.md` antes de escribir el código correspondiente

Con esto, el plan queda aprobado en su totalidad. Arranco la
implementación de Fase 04 — Inventario y edición a continuación.

---

## Compras y el costo del producto — decisión pendiente (no bloquea Fase 04)

**Fecha:** 2026-09-14. El humano rechazó explícitamente que yo resolviera
esto con mi propia lectura ("sube el costo al último"), porque es una
decisión de negocio con consecuencias reales sobre la ganancia histórica,
no un detalle de implementación. Esto se decide antes de que arranque
Fase 05 (Compras), no ahora.

**El problema concreto:** compra el mismo anillo a Q40 hoy y a Q55 dentro
de tres meses. ¿Qué pasa con `product.cost_cents` (y con `price_history`,
que se inserta cada vez que cambia)?

**Opción A — El costo pasa a ser el de la última compra (`unit_cost_cents`
de la línea más reciente).**
- Consecuencia: el precio sugerido y la ganancia que se muestran *hoy*
  reflejan lo último que pagó, que es lo más útil para decidir el
  próximo precio de venta.
- Consecuencia sobre reportes: la ganancia de piezas que ya estaban en
  stock **antes** de la compra nueva no cambia retroactivamente (gracias
  a los snapshots de `sale_item`, D-002) — pero el "capital invertido en
  inventario" (Fase 09/reportes) sí cambiaría de golpe para *todo* el
  stock existente de ese producto, aunque la mitad se haya comprado a
  Q40 y la otra mitad recién a Q55. Sobrestima o subestima el capital
  real invertido según de qué lado caiga.

**Opción B — Costo promedio ponderado por cantidad (`(stock_actual ×
costo_actual + qty_comprada × costo_real_de_la_línea) / (stock_actual +
qty_comprada)`).**
- Consecuencia: el "capital invertido en inventario" de reportes es más
  preciso (refleja lo que de verdad se gastó en el stock físico que hay
  hoy), porque no trata todo el stock como si costara lo último que se
  pagó.
- Consecuencia: el precio sugerido ya no refleja "lo que pagué la última
  vez" sino un promedio — puede sugerir un precio de venta más bajo del
  que en realidad necesita para cubrir la compra más cara reciente, si
  el promedio la diluye con stock viejo más barato.
- Más complejo de calcular y de explicarle a la usuaria ("¿por qué dice
  que me costó Q47 si la última vez pagué Q55?").

**Opción C — El costo no se toca; se mantiene el que ya tenía el
producto.**
- Consecuencia: el precio sugerido y la ganancia mostrados quedan
  desactualizados hasta que alguien edite el producto a mano (Fase 04,
  pantalla de edición) — la compra registra el gasto real (`purchase`/
  `purchase_item`, útil para saber cuánto se gastó en total), pero no
  "se entera" el producto.
- Consecuencia: es la opción más simple de implementar y la más difícil
  de justificar — ¿para qué registrar el costo real de la compra si el
  producto no lo usa para nada?

**Lo que las tres comparten:** cualquiera de las tres, si actualiza
`cost_cents`, tiene que insertar también una fila en `price_history`
(regla ya establecida en Fase 04 — "al cambiar costo o precio se
inserta fila en `price_history`") para que el historial de "por qué
ganaba más antes con este anillo" (`ESQUEMA.md`) siga siendo honesto.
Si se elige la Opción C, no hay nada que insertar (el costo no cambia).

**No elijo ninguna acá.** Queda para que el humano decida antes de que
Fase 05 arranque; se registra como decisión en `DECISIONES.md` recién
cuando se elija.

---

## Fase 04 — Bloqueo: KSP no resuelve `ProductRepository` para Hilt, sin ningún error real de por medio

**Fecha:** 2026-09-14. Todo el código de Fase 04 está escrito (Inventario:
listado, detalle/edición, navegación, selector de categoría en alta
rápida) pero **el build no compila**, y después de una sesión larga de
diagnóstico metódico no encontré la causa real. Paro acá, como pide
CLAUDE.md §2.1 después de intentos fallidos, y dejo todo documentado
para que decidas cómo seguir.

### El error, textual

```
e: [ksp] InjectProcessingStep was unable to process 'AddProductUseCase(ProductRepository)' because 'ProductRepository' could not be resolved.

Dependency trace:
    => element (CLASS): gt.marcos.joyeria.domain.usecase.AddProductUseCase
    => element (CONSTRUCTOR): AddProductUseCase(ProductRepository)
    => type (EXECUTABLE constructor): (ProductRepository)void
    => type (ERROR parameter type): ProductRepository

If type 'ProductRepository' is a generated type, check above for compilation errors that may have prevented the type from being generated. Otherwise, ensure that type 'ProductRepository' is on your classpath.
```

(Y lo mismo, palabra por palabra salvo el nombre de la clase, para
`ArchiveProductUseCase`, `EditProductUseCase`, `ProductEditViewModel` y
`ProductListViewModel` — los cinco consumidores de `ProductRepository`.)

`./gradlew assembleDebug` falla en la tarea `:app:kspDebugKotlin` con
`KSP failed with exit code: PROCESSING_ERROR`, sin llegar nunca a
`compileDebugKotlin`.

### Lo que verifiqué que **no** es la causa (cada uno probado con un build limpio de verdad)

1. **No es caché.** Probado con `./gradlew clean`, `--rerun-tasks`,
   `--no-configuration-cache`, borrando a mano `.gradle/configuration-cache`
   y `app/build`, y `./gradlew --stop` (mata el daemon, fuerza uno nuevo).
   El error persiste idéntico en un build 100% desde cero (`31 actionable
   tasks: 31 executed`, ninguna `UP-TO-DATE`).
2. **No es un ciclo de archivos `data.repository` ↔ `domain.usecase`.**
   Moví `AddProductInput`/`EditProductInput` a un archivo nuevo
   (`data/repository/ProductModels.kt`) para que `domain/usecase` solo
   dependa de `data/repository` en un sentido. Mismo error.
3. **No es `PriceHistoryDao`.** Lo saqué del constructor de
   `ProductRepository` (y del método que lo usa) por completo. Mismo error.
4. **No es el contenido de ningún archivo de UI.** Aislé el problema
   sacando *físicamente* del árbol de compilación (fuera de
   `app/src/main/java`, no solo renombrados) todos los archivos nuevos de
   `ui/` uno por uno. Con **todos** afuera y `ProductRepository` reducido
   a su forma exacta de Fase 03 (un solo método, un solo consumidor),
   **compiló** (`kspDebugKotlin` en verde). Reconstruí todo de vuelta
   método por método y consumidor por consumidor, confirmando cada paso
   con un build — **todo pasó**, incluidos los 3 casos de uso y los dos
   ViewModels. Pero apenas agregué el último archivo que faltaba
   (`CategoryDropdown.kt`, que **no tiene ninguna relación con Hilt ni
   con `ProductRepository`** — ni lo importa, ni lo usa) el error volvió
   a aparecer exactamente igual. Para descartar que fuera el *contenido*
   de ese archivo (usa `ExposedDropdownMenuBox`, una API que no había
   probado antes), lo reemplacé por un `@Composable` trivial de una sola
   línea (`Text("stub")`) que no usa nada raro — **el error volvió
   igual**. O sea: no importa qué dice el archivo, solo que exista.
5. **No es el modo incremental de KSP.** Probé
   `ksp { arg("ksp.incremental", "false") }` — mismo error en un build
   limpio.
6. **No es memoria del JVM.** Probé con `org.gradle.jvmargs=-Xmx4096m`
   (el doble del default del proyecto) — mismo error.

### Lo que esto sugiere

El único patrón que encontré: con **pocos** archivos nuevos en el
módulo, KSP/Hilt resuelve `ProductRepository` bien; en algún punto,
agregar **cualquier** archivo adicional (sin relación de tipos con
`ProductRepository`) hace que dejen de resolverlo. Esto no es un error
de mi código — es exactamente el mismo patrón, letra por letra, que ya
encontramos una vez con Coil en Fase 03 (D-018): una herramienta muy
nueva (`KSP 2.3.12`, backend "Analysis API" — el propio mensaje de error
dice `KspAAWorkerAction`) con un límite o bug de resolución que no
depende de que el código esté mal escrito. Mi sospecha, sin poder
confirmarla: un límite de rondas de resolución incremental de KSP2/AA
sensible a la cantidad total de archivos/símbolos del módulo, no al
contenido de ninguno en particular.

### Lo que NO probé (me quedé corto de tiempo/alcance para seguir adivinando)

- **Bajar la versión de KSP** (2.3.12 → una 2.2.x o 2.3.x anterior,
  verificada contra `maven-metadata.xml` como siempre). Es lo más
  parecido a lo que ya funcionó con Coil (D-018), pero KSP no es "una
  librería más" — está en el mismo nivel que Kotlin/AGP/Gradle en
  CLAUDE.md §2.1, así que no quise tocarlo sin preguntarte primero,
  aunque D-007 ya estableció que su versión SÍ se elige y verifica cada
  vez (no es parte de la "línea base congelada" de AGP/Gradle/Kotlin/
  compileSdk).
- Probar el mismo build en Android Studio directo (mejores diagnósticos
  que la CLI) en vez de `./gradlew` por línea de comandos.
- Reportarlo como bug real al repositorio de KSP con un caso mínimo
  reproducible (tengo casi armado el caso mínimo: el bisect de arriba).

### Estado del código

**Todo el código de Fase 04 está escrito y es, hasta donde pude
verificar, correcto** — el problema es exclusivamente de la herramienta
de build, no de diseño ni de lógica. Dejé el árbol de archivos completo,
restaurado a su forma real (no quedó nada a medio sacar del bisect):
listado, detalle/edición, navegación, selector de categoría, todo
presente. Nada se commiteó todavía — sigo en `fase/04-inventory`, sin
commit, con todo el trabajo en el árbol de trabajo.

### Lo que necesito de vos

Alguna de estas (o algo que se te ocurra que no probé):
1. Autorización para bajar la versión de KSP y probar si eso lo resuelve.
2. Que lo corras vos en Android Studio, con mejores herramientas de
   diagnóstico que la CLI, a ver si aparece un error real que la CLI no
   me mostró.
3. Cualquier otra pista — quizás ya viste este patrón antes.

No voy a seguir probando cosas al azar contra este error — ya pasé el
límite razonable de intentos que pide CLAUDE.md §2.1, y seguir así sin
una pista nueva es exactamente el "adivinar" que la regla prohíbe.

---

## Fase 04 — Corrección del bloqueo: no era versión de KSP/Hilt, era un comentario KDoc roto

**Fecha:** 2026-09-14. El humano señaló que la conclusión de la entrada
anterior ("límite de archivos de KSP") era equivocada, y dio una hipótesis
concreta con evidencia (fechas de KSP `2.3.12` y Hilt `2.60.1`) para
probar. **Esa hipótesis también resultó equivocada** — lo documento acá
en detalle porque el camino hasta encontrar la causa real importa tanto
como la causa misma.

### Chequeo previo pedido, antes de tocar versiones

`grep -rn "class ProductRepository\|interface ProductRepository"
app/src/main` → una sola coincidencia
(`data/repository/ProductRepository.kt:21`). No era el escenario de
declaración duplicada; autorizado a mover versiones.

### Intento 1: bajar KSP por fecha de publicación (falló)

Verificado vía la API de releases de GitHub (no `search.maven.org`, cuyo
índice mostró un Hilt desactualizado `2.56.2` — mismo patrón que ya
advirtió D-009): Hilt `2.60.1` es el release más reciente de
`google/dagger` (`2026-07-06T21:28:59Z`, no hay ninguno más nuevo), y KSP
`2.3.9` (`2026-05-26T19:53:05Z`) es la última `2.3.x` publicada antes de
esa fecha. Bajé `gradle/libs.versions.toml` de `ksp = "2.3.12"` a
`ksp = "2.3.9"` y corrí `./gradlew clean :app:kspDebugKotlin
--rerun-tasks --no-configuration-cache`. **El error se reprodujo
idéntico, letra por letra**, con un build 100% limpio.

### Intento 2: bajar KSP a la versión exacta que declara el POM de Dagger (falló)

Con `./gradlew :app:dependencies --configuration
kspDebugKotlinProcessorClasspath` encontré que `dagger-compiler:2.60.1`
declara una dependencia dura a `com.google.devtools.ksp:symbol-processing-api:2.3.7`
(no `2.3.9`) — evidencia más precisa que la fecha, tomada directo del POM
real de la librería, mismo tipo de verificación que ya usó D-018 con
`kotlin-stdlib` de Coil. Bajé KSP a `2.3.7` exacto. **El error se
reprodujo idéntico otra vez**, confirmado también con `--info` completo
(sin ningún diagnóstico adicional oculto).

### Intento 3 y 4: descartar Room y descartar `AppDatabase` del constructor (fallaron los dos, pero acotaron el problema)

Para separar "es una incompatibilidad de versión" de "es una interacción
entre los dos procesadores de KSP del módulo (Room + Hilt)", saqué
temporalmente `ksp(libs.androidx.room.compiler)` del `app/build.gradle.kts`
por completo. **Error idéntico** — no es interacción con Room.

Después, para probar si el parámetro `AppDatabase` del constructor de
`ProductRepository` (la única diferencia real con `CategoryRepository`,
que sí resolvía bien) era el disparador, lo saqué temporalmente junto con
sus dos usos de `db.withTransaction`. **Error idéntico** — tampoco era
eso. En este punto: tres versiones de KSP distintas y dos cambios
estructurales al código, mismo error exacto las cinco veces. Evidencia
suficiente de que la causa no era ninguna versión ni ninguna de esas dos
hipótesis de contenido.

### Causa real: un `/*` suelto dentro de un comentario KDoc

Con la versión de KSP ya en `2.3.7` (para aislar una sola variable a la
vez), reduje `ProductRepository.kt` a un solo método (la forma exacta de
Fase 03) y **compiló**. Fui agregando los métodos de vuelta uno por uno
(`archive`, `getDetail`, `observeFiltered`, `update`) — **todos
compilaron**, incluso los cuatro juntos. Restauré el archivo original
completo (con sus dos comentarios `/** ... */`) y **volvió a fallar**.
La única diferencia entre la versión que compilaba y la que no eran los
comentarios KDoc, no el código.

Aislado: el KDoc de clase (líneas 15-19 del archivo original) contiene el
texto `` `domain/usecase/*.kt` ``. Esa `/` seguida de `*` es, para el
lexer de Kotlin, la apertura de un **comentario anidado** — Kotlin, a
diferencia de Java/C, permite anidar `/* */`. El `/**` real de la línea 15
abre profundidad 1; el `/*` embebido en `usecase/*.kt` (línea 18) la sube
a profundidad 2; el `*/` de cierre (línea 20) solo la baja a profundidad
1. El comentario nunca cierra del todo ahí y sigue tragándose código real
después — en este caso, la propia declaración
`class ProductRepository @Inject constructor(...) { ... }`. Confirmado
contando delimitadores (`grep -o '/\*'` = 2, `grep -o '\*/'` = 1, con el
KDoc de clase presente y el del método `update` ausente) y con el bisect
inverso: agregar de vuelta *solo* el KDoc de clase (sin el de `update`)
alcanza para reproducir la falla por sí solo.

Desde la perspectiva del frontend que usa KSP, `ProductRepository`
literalmente no existe como declaración — el mensaje
"`ProductRepository` could not be resolved" no mentía, la causa nunca fue
de dependencias ni de versión.

### Qué se hizo

- `app/src/main/java/gt/marcos/joyeria/data/repository/ProductRepository.kt`:
  reescrito el comentario roto — `` `domain/usecase/*.kt` `` →
  `` `domain/usecase` ``. Contenido funcional sin cambios.
- `gradle/libs.versions.toml`: `ksp` devuelto a `2.3.12` (el valor
  original de D-007) — nunca fue el problema.
- Arreglados dos usos reales de Material3, recién visibles al llegar por
  primera vez a `compileDebugKotlin` (nunca se había llegado tan lejos con
  el build roto en `kspDebugKotlin`): en `CategoryDropdown.kt` y
  `ProductListScreen.kt`, `ExposedDropdownMenuDefaults.DropdownMenu(...)`
  no existe — `ExposedDropdownMenu` es una función miembro de
  `ExposedDropdownMenuBoxScope`, se llama sin calificar por receptor
  implícito dentro del lambda de `ExposedDropdownMenuBox`. Verificado
  leyendo el jar de fuentes real de `material3-android:1.4.0` (no
  adivinado). Confirmado que no es parte del bloqueo de KSP/Hilt — es un
  bug de código distinto que solo se hizo visible al destrabar el
  anterior.
- `CLAUDE.md`: retirada por completo la regla de "contemporaneidad
  KSP/Hilt" que había agregado en el intento 1 (no correspondía a la
  causa real). Agregada en su lugar, sección 5, la prohibición de escribir
  `/*` dentro del texto de un comentario de bloque, con este incidente
  documentado.
- `DECISIONES.md`: reescrita **D-025** completa con la historia real (era
  parte de este mismo trabajo sin commitear todavía, no una decisión ya
  cerrada de una fase anterior — mismo criterio que ya se usó una vez con
  D-013).

### Todos los archivos que se movieron temporalmente durante el diagnóstico volvieron a su lugar sin cambios

Durante los intentos 3-4 y la bisección de contenido moví 15 archivos
fuera del árbol de compilación (`ArchiveProductUseCase.kt`,
`EditProductUseCase.kt`, los tres de `ui/navigation/`,
`CategoryDropdown.kt`, los cuatro de `ui/product/edit/`, los cuatro de
`ui/product/list/`, y brevemente `CategoryRepository.kt` por error de
bisección — restaurado de inmediato al ver que rompía `AddProductViewModel`
de verdad). Verificado con `git status` y `git ls-files` después de
restaurarlos todos: el árbol coincide exactamente con el commit
`ad88fe3` (wip de Fase 04) salvo los archivos listados arriba.

### Resultado de la compilación y los tests

`./gradlew --stop && ./gradlew clean assembleDebug` → **BUILD
SUCCESSFUL** (tras el fix del comentario y de `ExposedDropdownMenu`;
solo quedan warnings preexistentes/no relacionados: `MenuAnchorType`
deprecado y `hiltViewModel` deprecado, ninguno introducido por este
arreglo).

`./gradlew testDebugUnitTest` → **BUILD SUCCESSFUL**, 55 tests, 0
fallos, 0 errores, en 8 clases: `ExampleUnitTest` (1), `ProductDaoTest`
(5), `ProductUidGeneratorTest` (2), `SeedDataTest` (2), `MoneyTest` (8),
`PricingCalculatorTest` (25), `MoneyDigitsInputTest` (7),
`ImageStorageScalingTest` (5).

### Archivos tocados

`app/src/main/java/gt/marcos/joyeria/data/repository/ProductRepository.kt`,
`app/src/main/java/gt/marcos/joyeria/ui/product/CategoryDropdown.kt`,
`app/src/main/java/gt/marcos/joyeria/ui/product/list/ProductListScreen.kt`,
`gradle/libs.versions.toml`, `CLAUDE.md`, `DECISIONES.md`, `ESTADO.md`.
Los tres primeros y `libs.versions.toml` ya están en "Archivos permitidos"
de Fase 04; los tres de proceso son los que CLAUDE.md §7 exige mantener.

### Bloqueos / preguntas para el humano

Ninguno nuevo. **La Fase 04 sigue sin cerrarse** — esto resuelve el
bloqueo de build y confirma que compila y pasa tests, pero falta la
revisión del humano antes del commit final y el tag, como se pidió
explícitamente.

---

## Fase 04 — Verificación manual en emulador (listado, búsqueda, filtro, edición de precio, archivado)

**Fecha:** 2026-09-14. El humano señaló que el build verde no alcanza para
cerrar la fase: pidió instalar en el emulador y verificar el flujo
completo, documentando cada punto con lo que se vio. Esto es aparte de lo
anterior (que resolvió el *build*); esta sección es la verificación
*funcional*.

### Entorno

Emulador `Medium_Phone_API_35` (AVD ya existente, usado en Fase 00/01),
booteado para esta sesión. La app ya estaba instalada de una prueba manual
anterior (Fase 03, con un producto real cargado por la usuaria — ver más
abajo); se reinstaló con `adb install -r` (conserva datos) sobre el APK
recién compilado con el fix de esta sesión.

Método de verificación: `adb exec-out screencap` para capturas,
`uiautomator dump` + parseo con `python3`/`ElementTree` para leer el
estado real de la UI (más confiable que la captura de pantalla — ver nota
de la sección "Deuda/observación" más abajo), y `adb exec-out run-as
gt.marcos.joyeria cat databases/joyeria.db{,-wal,-shm}` para traer la base
real de la app y consultarla con `sqlite3` local, **no asumiendo** el
resultado de ninguna acción sobre datos.

Todos los logs (`ui*.xml`, `crash-check.log`) y capturas de esta
verificación quedaron en `app/build/logs/` y `app/build/screenshots/`
respectivamente (regla nueva de `CLAUDE.md` sección 7, punto 10 —
ignorados por git, no se commitean).

### 1. El listado muestra las piezas que ya existen en la base

La base ya traía **XP-000001** ("Pieza sin nombre", cargado por la usuaria
real durante la prueba de Fase 03, costo Q40,009.97/precio Q80,020.00 —
valores reales que ella tipeó, no datos de prueba míos). Abrí Inventario
sin tocar nada más:
`app/build/screenshots/02-inventory-list.png` — el listado muestra esa
pieza con foto, nombre, uid, stock y ganancia calculada, correctamente.
Agregué dos piezas más por el flujo real de "Agregar pieza" (foto con
CameraX, costo, precio, nombre y categoría desde "Más detalles") —
**XP-000002** "Aretes luna" (categoría Aretes) y **XP-000003** "Anillo
sol" (categoría Anillos) — y el listado con las tres:
`app/build/screenshots/18-inventory-3items.png`. ✅

### 2. Búsqueda por nombre

Escribí "anillo" (minúscula) en "Buscar por nombre o código": filtra a una
sola pieza, "Anillo sol" (XP-000003) — `app/build/screenshots/21-search-anillo.png`,
confirmado también con `uiautomator dump` (`app/build/logs/ui35.xml`).
Búsqueda case-insensitive: el nombre real es "Anillo sol" con mayúscula,
la búsqueda en minúscula lo encontró igual (`LIKE` de SQLite es
case-insensitive para ASCII, tal como está la consulta en
`ProductDao.observeFiltered`). ✅

### 3. Búsqueda por código (uid)

Escribí "xp-000002" (minúscula): filtra a una sola pieza, "Aretes luna"
(XP-000002) — `app/build/screenshots/23-search-uid-exact.png`. Con el
prefijo parcial "xp-00000" (sin el dígito final) coincidieron las tres
piezas (las tres empiezan igual), confirmando que es una búsqueda por
substring, no exacta — comportamiento esperado de `LIKE` con comodines a
ambos lados. ✅

### 4. Filtro por categoría

Con el selector de categoría en "Aretes": el listado se reduce a una sola
pieza, "Aretes luna" — excluye "Anillo sol" (categoría Anillos) y "Pieza
sin nombre" (sin categoría) — `app/build/screenshots/24-filter-aretes.png`.
✅

### 5. Editar el precio inserta una fila en price_history (verificado consultando la tabla, no asumido)

Entré al detalle de "Aretes luna" (Costo Q30.00, Precio Q960.00 antes del
cambio), cambié el precio y guardé. Extraje la base real de la app
(`app/build/logs/db-dumps/joyeria.db`, con su -wal/-shm, porque Room usa
WAL y el cambio recién escrito puede no estar aún en el archivo principal
si no se hace checkpoint) y corrí sqlite3 directo:

```
=== product ===
2|XP-000002|Aretes luna|3000|960005|3
=== price_history ===
1|2|3000|960005|1789420483925
```

`product.sale_price_cents` quedó en 960005 (el precio nuevo) y se insertó
una fila en `price_history` con `product_id=2`, el costo y precio nuevos,
y un `changed_at` real. Esto es la base de datos real después de la
acción, no una inferencia de lo que la pantalla mostraba. ✅

### 6. Archivar saca la pieza del listado sin borrarla

Desde el detalle de "Aretes luna" toqué "Archivar pieza": apareció el
diálogo de confirmación exigido por CLAUDE.md sección 6 (texto claro, en
español, diciendo exactamente qué va a pasar) —
`app/build/screenshots/27-archive-confirm.png`: "¿Archivar esta pieza? Ya
no va a aparecer en el inventario ni se va a poder vender. No se borra:
el historial de esta pieza queda guardado." Confirmé, y el listado (con
el filtro de categoría limpio) quedó con solo dos piezas —
`app/build/screenshots/29-list-after-archive.png`. Verificado en la base
real:

```
1|XP-000001|Pieza sin nombre|0
2|XP-000002|Aretes luna|1     <- archived = 1
3|XP-000003|Anillo sol|0
SELECT COUNT(*) FROM product;  -> 3
```

Las tres piezas siguen en la tabla (nada se borró físicamente); solo la
archivada tiene archived = 1 y por eso desapareció del listado activo
(`ProductDao.observeFiltered`/`observeActive` filtran archived = 0).
Confirma además el criterio 3 de la fase: no hay ningún DELETE FROM
product en el flujo real. ✅

### 7. Navegación sin crash

Listado → detalle ("Anillo sol") → atrás: vuelve al listado sin crash
(`app/build/logs/ui53.xml` → `ui54.xml`). Alta rápida → atrás: probado
varias veces durante la carga de los tres productos, siempre volvió a
Inventario u Home sin problema. Revisé el logcat completo de toda la
sesión (`app/build/logs/crash-check.log`) filtrando FATAL EXCEPTION y
AndroidRuntime: las únicas apariciones son del propio proceso uiautomator
arrancando y parando (esperado, es la herramienta que usé para
inspeccionar la UI), ninguna del proceso de la app. El PID de
gt.marcos.joyeria (4163) fue el mismo desde el primer am start hasta el
final de la sesión — el proceso nunca murió ni se reinició. ✅

### Deuda/observación que dejo anotada (no bloquea el cierre, pero quede escrito)

1. adb exec-out screencap mostró contenido desactualizado varias veces
   mientras el teclado en pantalla estaba abierto (la captura seguía
   mostrando el frame anterior). Lo detecté porque contradecía el
   uiautomator dump tomado inmediatamente después (que sí refleja el
   estado real de accesibilidad). Dejé de confiar en la captura de
   pantalla como fuente de verdad durante edición de texto y usé el dump
   + consulta directa a la base para todo lo que importaba verificar. Es
   una limitación del emulador/herramienta de captura, no de la app.
2. El campo de dinero (MoneyDigitsField, Fase 03) se comportó de forma
   inconsistente cuando lo manejé con adb shell input keyevent/input
   text (varias veces el valor resultante no fue el que un cálculo
   simple de "dígito a dígito" hubiera predicho — ej. terminé con
   Q960.00 en vez del valor que buscaba). Cuando repetí la entrada con
   toques reales sobre las teclas visibles del teclado numérico (en vez
   de eventos sintéticos), el campo respondió de forma consistente
   (multiplicar por diez y sumar el dígito nuevo, tal como describe
   FASES.md) y el valor final coincidió exactamente con lo que quedó
   guardado en la base (sale_price_cents del paso 5). Mi conclusión, sin
   poder afirmarla con certeza total: es un artefacto de cómo la
   automatización por ADB dispara eventos de teclado sobre este campo
   custom, no un bug de la app — la lógica de dígito-a-dígito ya tiene 7
   tests unitarios propios en Fase 03 (MoneyDigitsInputTest, corridos en
   verde en esta misma sesión). Lo anoto igual para que quede constancia:
   si alguna vez alguien reporta un valor de precio "raro" después de
   editar rápido, esto es un lugar por dónde empezar a mirar, con la
   salvedad de que no lo pude reproducir con toques reales.
3. Los productos XP-000002 y XP-000003 quedaron con precios de prueba sin
   redondear (Q960.00 antes de editar, Q9,600.05 después; Q30.00 para
   XP-000003) — no son valores "de negocio" representativos, son
   simplemente los que resultaron de la interacción por ADB descrita
   arriba. No afecta ningún criterio de aceptación (solo se necesitaba
   costo y precio numéricos válidos para probar listado/búsqueda/filtro/
   historial/archivado), pero lo anoto para que no se confunda con datos
   reales de la usuaria si alguien revisa la base más adelante — el único
   dato real ahí es XP-000001, cargado por ella en la prueba de Fase 03.

### Archivos tocados en esta verificación

Ninguno de código. Se generaron app/build/logs/** y
app/build/screenshots/** (ambos bajo build/, ignorados por git, no se
commitean — regla nueva de CLAUDE.md sección 7, punto 10).

### Resultado

Los 6 puntos que pidió el humano quedaron verificados con evidencia
directa (capturas + dumps de accesibilidad + consultas SQL reales a la
base extraída del dispositivo), no por inferencia de lo que "debería"
pasar. Sujeto a la revisión del humano, la Fase 04 está lista para el
commit final y el tag.

---

## Fix de usabilidad de alta rápida — Plan (antes de escribir código)

**Fecha:** 2026-09-17. Rama `fix/quick-add-usability`, abierta desde
`fase/04-inventory` (commit `86b9b1b`, sin taguear ni mergear) — **no**
desde `main`, porque la Fase 04 ya contiene la Fase 03 y el selector de
categoría, y esta corrección los necesita a los dos.

**Motivo:** prueba real con la usuaria en un teléfono. Le pareció
intuitiva y registró piezas sola, pero salieron tres cosas a corregir en
la pantalla de alta rápida (`ui/product/add/**`) antes de cerrar la Fase
03. El humano pidió el plan acá y una parada antes de tocar código, como
siempre.

Como es la primera vez que corrijo una fase ya implementada fuera del
ciclo normal de `FASES.md` (no es una fase nueva, es un fix pedido
directo sobre la 03), el alcance de archivos no sale de una tabla de
`FASES.md` sino de este mismo plan: `ui/product/add/**`, `strings.xml`,
y los archivos de proceso (`CLAUDE.md`, `DECISIONES.md`, `ESTADO.md`).
Si durante la implementación hace falta tocar algo fuera de esa lista,
paro y lo anoto acá antes de hacerlo, igual que en cualquier fase.

### 1. Bug — el teclado tapa el campo de costo/precio

**Diagnóstico (leyendo el código, todavía sin correr nada):**

- `MainActivity.kt` llama `enableEdgeToEdge()` antes de `setContent`.
  Con edge-to-edge activado, Android ya no redimensiona la ventana de la
  forma clásica de `adjustResize` — los insets (barras de sistema **e
  IME**) se entregan como `WindowInsets` a la jerarquía de Compose en vez
  de encogerla el sistema operativo por su cuenta. Esto no es específico
  de esta pantalla: es el comportamiento documentado de Compose con
  edge-to-edge, y `android:windowSoftInputMode="adjustResize"` (ya
  presente en el manifest) queda inerte con edge-to-edge — se deja tal
  cual, no es lo que hay que tocar.
- `AddProductScreen.kt` pone el formulario en un `Column` con
  `.verticalScroll(rememberScrollState())` dentro de un `Scaffold`, pero
  **ningún modifier de la jerarquía reacciona al IME** (`grep -rn
  "imePadding\|WindowInsets.ime"` sobre `ui/product/add` no encuentra
  nada). Sin eso, cuando aparece el teclado el `Column` no se entera de
  que su espacio disponible cambió: no se encoge, no dispara el scroll
  automático hacia el campo enfocado, y el teclado queda dibujado encima
  del contenido tal cual estaba layouteado con el teclado cerrado —
  exactamente el síntoma que describiste.
- Confirmé además que `ui/product/edit/ProductEditScreen.kt` tiene el
  mismo patrón (`Scaffold` + `Column` + `verticalScroll`, sin
  `imePadding`), así que probablemente tiene el mismo bug. **Queda fuera
  del alcance de este fix** porque solo pediste la pantalla de alta
  rápida — lo anoto acá como pendiente, no lo toco sin que lo pidas.

**Arreglo propuesto:** agregar `Modifier.imePadding()` al `Column`
formulario de `AddProductScreen.kt`, antes de `.verticalScroll(...)`
(orden: `.fillMaxSize().imePadding().verticalScroll(...).padding(16.dp)`)
para que el espacio que ocupa el teclado se reste del alto disponible
*antes* de que el scroll decida qué es visible — así el campo enfocado
queda dentro del viewport reducido y el propio `OutlinedTextField` pide
que lo traigan a la vista (comportamiento estándar de Compose para
campos de texto dentro de un contenedor scrolleable).

**Por qué no lo doy por cerrado solo con este razonamiento:** vos mismo
pediste diagnosticar la causa real y verificarlo con teclado abierto, con
captura, no solo leyendo el código — lo cual comparto: el orden exacto de
modifiers y si el `contentWindowInsets` por defecto del `Scaffold` ya
reserva algo que interfiera es el tipo de cosa que a veces se comporta
distinto de lo que predice la teoría. Plan de verificación una vez
implementado: instalar en el emulador, abrir "Agregar pieza", tocar el
campo de costo, capturar con el teclado abierto
(`app/build/screenshots/`), confirmar visualmente que el campo y el
dígito que se está tecleando quedan arriba del teclado, y repetir para el
campo de precio de venta. Si el primer intento no alcanza, ajusto el
modifier (por ejemplo `contentWindowInsets = WindowInsets(0)` en el
`Scaffold` si su default choca con `imePadding`) y vuelvo a verificar
antes de darlo por resuelto.

### 2. Campos opcionales visibles, categoría obligatoria con chips

- **Elimino `MoreDetailsSection`** (el bloque colapsable con el botón
  "Más detalles") de `AddProductScreen.kt` — deja de existir el
  `TextButton` de expandir/contraer y el estado local `detailsExpanded`.
  Todos los campos pasan a la columna principal, siempre visibles, sin
  nada que expandir.
- **Categoría → chips de un solo toque, obligatoria.** Reemplazo
  `CategoryDropdown` (que sigue existiendo para `ui/product/edit/**`, sin
  tocarlo) por un componente nuevo en `ui/product/add/**`
  (`CategoryChipRow.kt` o similar) con un `FilterChip` de Material3 por
  categoría, selección única (tocar una la selecciona, tocar otra cambia
  la selección; no hay "Sin categoría" acá, a diferencia del dropdown de
  edición, porque ahora es obligatoria). Sin preselección: hay que
  tocarla una vez, igual que cualquier otro campo obligatorio.
  Área táctil e tamaño de texto: CLAUDE.md sección 6 exige mínimo 56dp
  de área táctil y 18sp de texto de cuerpo en *toda* la UI, sin excepción
  para chips — el `FilterChip` por defecto de Material3 es más chico
  (target táctil ~48dp, texto `labelLarge` 14sp), así que le fuerzo
  `Modifier.heightIn(min = 56.dp)` y un `labelLarge` en el tamaño de
  cuerpo de 18sp del tema (ya lo sube `Type.kt` desde Fase 00) en vez del
  estilo por defecto de chip. Lo superviso visualmente en el emulador
  antes de darlo por bueno — un chip de 56dp de alto con texto de 18sp es
  más grande que el chip estándar de Material y quiero confirmar que no
  se ve roto antes de cerrar esto.
- **Cantidad:** queda visible siempre (sale del bloque colapsable), sigue
  sin ser obligatoria. Cambio el valor inicial de `quantityText` en
  `AddProductUiState` de `""` a `"1"`, para que se vea el valor por
  defecto en el campo en vez de un campo vacío que "por dentro" vale 1
  pero no lo muestra. `stockQty` sigue con el mismo fallback
  (`coerceAtLeast(1)`) por si lo borra del todo.
- **Nombre y notas:** quedan visibles siempre, sin cambio de
  comportamiento — solo salen del bloque colapsable.
- **`AddProductUiState.canSave`** suma `categoryId != null` a la
  condición existente (foto, costo, precio, no-guardando). Los cuatro
  campos obligatorios pasan a ser foto, costo, precio y categoría.
- **Strings:** actualizo el texto de `add_product_category_label` (saca
  "(opcional)", ahora es una etiqueta de sección arriba de los chips, no
  el label de un `OutlinedTextField`). Elimino `add_product_more_details`
  de `strings.xml` (deja de usarse, y CLAUDE.md prohíbe dejar strings
  muertos igual que prohíbe stubs que compilen y mientan). Dejo
  `add_product_category_none` intacto porque lo sigue usando
  `CategoryDropdown` en la pantalla de edición.
- Orden final de la pantalla (todo visible, sin scroll oculto de
  contenido colapsado): Foto → Costo → Precio de venta (con precio
  sugerido/ganancia en vivo debajo, sin cambios) → Categoría (chips) →
  Cantidad → Nombre → Notas → Guardar.

### 3. Actualizar CLAUDE.md sección 6 (tres campos → cuatro) y registrar la decisión

**Redacción propuesta para CLAUDE.md sección 6** (reemplaza el primer
bullet):

> - **Registrar una pieza nueva: máximo 4 taps y menos de 20 segundos.**
>   Pantalla de alta rápida: foto, costo, precio y categoría. Todo lo
>   demás (nombre, cantidad, notas) es opcional, visible sin necesidad de
>   expandir nada, y se puede editar después. Si el formulario de alta
>   pide más de 4 campos obligatorios, está mal.

**Entrada nueva para `DECISIONES.md`** (borrador, número real `D-026` a
confirmar contra la última entrada de la rama en la que se mergee esto):

> ## D-026 — CLAUDE.md sección 6: de 3 a 4 campos obligatorios en el alta
> rápida (categoría se vuelve obligatoria)
>
> **Contexto:** la prueba real con la usuaria (2026-09-17) mostró que los
> campos opcionales de Fase 03 quedaban escondidos en un bloque
> colapsable y no los descubría. Al hacerlos todos visibles, categoría
> pasa de opcional a **obligatoria**: sin categoría, la búsqueda/filtro de
> Fase 04 y el catálogo agrupado de Fase 10 sirven a medias, y dejarla
> visible pero opcional no resuelve ese problema de fondo — solo lo
> pospone hasta que alguien la complete a mano en edición, cosa que en la
> práctica no va a pasar si nunca es obligatoria en el único flujo que la
> usuaria usa todos los días.
>
> **Decisión:** CLAUDE.md sección 6 pasa de "máximo 3 taps" a "máximo 4
> taps": foto, costo, precio y categoría son los cuatro campos
> obligatorios del alta rápida. Categoría se selecciona con chips de un
> solo toque (sin preselección), no con un dropdown, para no perder el
> espíritu de "un toque por campo obligatorio" de la regla original.
>
> **Por qué no una excepción tácita:** el propio CLAUDE.md exige (sección
> 10) no adivinar y dejar las dudas escritas; cambiar el comportamiento
> real de la pantalla sin actualizar la regla que la describe habría
> dejado documentación y código diciendo cosas distintas — exactamente lo
> que el ritual de auditoría de la sección 7 (leer `ESTADO.md` + el diff)
> no puede tolerar.
>
> **Descartado:** dejar categoría opcional y visible (no resuelve el
> problema real: catálogo/filtro sin categoría); mantener el bloque
> colapsable solo para categoría (ya se demostró con la usuaria real que
> lo colapsado no se descubre).
>
> **Consecuencia:** el criterio de tiempo (menos de 20 segundos, Fase 03
> criterio 4) se vuelve a medir después de este cambio — un campo
> obligatorio más puede empujar el flujo real por encima del límite. Ver
> la sección de abajo, "Re-verificación de tiempo pendiente".

No aplico ninguno de los dos cambios de arriba (CLAUDE.md ni
DECISIONES.md) todavía — quedan acá como texto propuesto, a la espera de
tu OK, igual que el resto de este plan.

### Re-verificación de tiempo pendiente

Como pediste: después de implementar, vuelvo a medir el tiempo de alta de
una pieza (foto + costo + precio + categoría) con cronómetro, sobre el
emulador o dispositivo — no puedo correr la prueba con la usuaria real
otra vez sin coordinarlo, así que esta primera remedición la hago yo
mismo como señal rápida de si el cambio la sacó de rango, no como
reemplazo del criterio 4 de Fase 03 (que exige que la mida ella). Si mi
propia medición ya pasa de 20 segundos, te aviso antes de dar el fix por
cerrado, tal como pediste. Si queda holgada, igual dejo anotado que la
medición real con la usuaria sigue pendiente para no cerrar Fase 03 con
una medición mía haciendo de sustituto.

### Archivos que voy a tocar (una vez que confirmes)

- `app/src/main/java/gt/marcos/joyeria/ui/product/add/AddProductScreen.kt`
  (imePadding, quitar `MoreDetailsSection`, layout plano, chips de
  categoría)
- `app/src/main/java/gt/marcos/joyeria/ui/product/add/AddProductUiState.kt`
  (`canSave` con categoría, `quantityText` inicial `"1"`)
- `app/src/main/java/gt/marcos/joyeria/ui/product/add/CategoryChipRow.kt`
  (nuevo, componente de chips)
- `app/src/main/res/values/strings.xml` (label de categoría, borrar
  `add_product_more_details`)
- `CLAUDE.md` (sección 6)
- `DECISIONES.md` (D-026)
- `ESTADO.md` (cierre de este fix, con la verificación real del teclado y
  la remedición de tiempo)

**No** toco `ui/product/edit/**` (mismo bug de teclado probablemente
presente ahí, pero fuera de lo que pediste), ni `AndroidManifest.xml`
(el `adjustResize` existente se deja, no es la causa).

### Bloqueos / preguntas para el humano

Ninguno que me detenga a esperar respuesta antes de seguir — las
decisiones de diseño de arriba (altura de chip 56dp, orden final de
campos, valor inicial de cantidad) las tomé yo como las opciones más
directas dentro de lo que ya pediste, y quedan escritas para que las
corrijas si no son lo que imaginabas. Quedo parado acá, sin escribir
código todavía, esperando tu confirmación para arrancar.

---

## Fix de usabilidad de alta rápida — Implementación y verificación

**Fecha:** 2026-09-17. Mismo commit sin cerrar todavía en
`fix/quick-add-usability`. El humano aprobó el plan de arriba con dos
cambios: sumar `ProductEditScreen.kt` al alcance (mismo bug, misma clase
de pantalla, evitar una rama nueva por una línea) y exigir verificación
con captura real, teclado abierto, en **las dos** pantallas, con especial
cuidado en el orden `imePadding()` antes de `verticalScroll()` aplicado
al contenedor que scrollea.

### Qué se hizo

- **Bug del teclado (los dos screens):** agregado
  `Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp)`
  al `Column` formulario de `AddProductScreen.kt` **y** de
  `ProductEditScreen.kt` (sumado al alcance por pedido explícito). El
  orden importa exactamente como señalaste: `imePadding()` va antes de
  `verticalScroll()` para que encoja el contenedor que scrollea, no el
  contenido de adentro.
- **Campos siempre visibles:** eliminado `MoreDetailsSection` y el estado
  `detailsExpanded` de `AddProductScreen.kt`. Orden final, todo visible
  sin expandir nada: Foto → Costo → Precio de venta (sugerido/ganancia en
  vivo) → Categoría (chips) → Cantidad → Nombre → Notas → Guardar.
- **Categoría obligatoria con chips:** `CategoryChipRow.kt` nuevo
  (`ui/product/add/`), `FilterChip` de Material3 por categoría,
  `Modifier.heightIn(min = 56.dp)` y texto en `bodyLarge` (18sp) para
  cumplir CLAUDE.md sección 6 — verificado visualmente en el emulador
  (`app/build/screenshots/fix-01-*.png` en adelante), no se ve roto, solo
  más alto que un chip de Material3 estándar. Sin preselección, sin
  opción "sin categoría".
- **`AddProductUiState`:** `quantityText` por defecto `"1"` (antes `""`);
  `canSave` suma `categoryId != null` a las condiciones existentes.
- **Strings:** `add_product_category_label` de "Categoría (opcional)" a
  "Categoría" (la usa también `CategoryDropdown` en edición y el filtro
  de `ProductListScreen`, ninguno de los dos pedía la palabra
  "opcional" realmente). Borrado `add_product_more_details` (quedó sin
  uso).
- **`CLAUDE.md` sección 6** y **`DECISIONES.md` D-026** aplicados tal
  como quedaron redactados en el plan de arriba (sin cambios de último
  momento).

### Verificación de build y tests

| Comando | Resultado |
|---|---|
| `./gradlew assembleDebug` | `BUILD SUCCESSFUL` en 5m15s, sin warnings nuevos |
| `./gradlew testDebugUnitTest` | `BUILD SUCCESSFUL` en 1m41s, mismos tests de antes en verde (no había tests de `canSave`/UI de Fase 03 antes de este fix, y sigue sin haberlos — `ui/product/add` nunca estuvo bajo `[TESTS OBLIGATORIOS]`; no invento una obligación nueva que no estaba) |

### Verificación real en emulador (teclado abierto, las dos pantallas)

Emulador `Medium_Phone_API_35`, instalación limpia del APK con este fix.
Todas las capturas en `app/build/screenshots/` (ignoradas por git, regla
de `CLAUDE.md` sección 7 punto 10).

1. **Alta rápida, campo Costo:** tapeado el campo, tecleado "4599" con el
   teclado abierto — `Costo: Q45.99` y `Precio de venta: Q95.00`
   (sugerido en vivo) quedan **completos y legibles arriba del
   teclado** — `app/build/screenshots/fix-01-cost-field-ime-open.png`. ✅
2. **Alta rápida, campo Precio de venta:** mismo tapeado, tecleado
   "12345" — `Q9,500,134.52` con el cursor visible, arriba del
   teclado — `app/build/screenshots/fix-02-price-field-ime-open.png`. ✅
3. **Flujo completo de alta, con categoría obligatoria:** foto real
   tomada con la cámara del emulador (permiso de cámara concedido en el
   diálogo real, "While using the app"), costo Q30.00, precio sugerido
   Q60.00/ganancia Q30.00 en vivo, chip "Anillos" tocado y confirmado
   `checked="true"` en el dump de accesibilidad, Guardar tocado →
   "Guardada como XP-000001" —
   `app/build/screenshots/fix-03-saved-confirmation.png`. Confirmado
   además en la base real extraída del dispositivo
   (`app/build/logs/db-dumps/joyeria.db`):
   `1|XP-000001|Pieza sin nombre|3000|6000|1|...` (`category_id = 1`,
   Anillos). ✅
4. **Edición, campo Precio de venta:** abierto el detalle de XP-000001,
   tapeado "Precio de venta", tecleado "9999" con el teclado abierto —
   `Q600,099.99` con el cursor visible, completo arriba del teclado,
   mismo comportamiento que en alta rápida —
   `app/build/screenshots/fix-04-edit-price-field-ime-open.png`. ✅ (los
   cambios de esta prueba de edición no se guardaron, se salió sin
   tocar "Guardar cambios" — XP-000001 sigue con sus valores
   originales.)
5. **`canSave` con categoría (el cambio real de código, no solo visual):**
   con foto, costo (Q15.00) y precio ya cargados y **sin** tocar ningún
   chip de categoría, el botón "Guardar" queda deshabilitado —
   confirmado no por apariencia sino leyendo el árbol de accesibilidad:
   el nodo clickeable que envuelve el botón reporta `enabled="false"`
   (`app/build/logs/ui_nosel.xml`) —
   `app/build/screenshots/fix-07-save-disabled-without-category.png`. ✅

### Incidente durante la verificación: ANR del emulador (no es un bug de la app)

Al intentar medir el tiempo con un primer script de taps automatizados
(inmediatamente después de un `am force-stop` + `am start` en frío), el
emulador mostró **"Mother App isn't responding"**. Antes de asumir nada,
lo investigué en vez de descartarlo o reintentar a ciegas (CLAUDE.md
§2.1: leer el error completo, no adivinar):

- `adb logcat` mostró la causa exacta: `ANR in ActivityRecord{...
  gt.marcos.joyeria/.MainActivity...}. Reason: Input dispatching timed
  out (Application does not have a focused window)` — un ANR de
  despacho de input, no un `FATAL EXCEPTION` ni un deadlock del hilo
  principal de la app (`grep` de `FATAL EXCEPTION`/`AndroidRuntime` para
  `gt.marcos.joyeria` en todo el logcat de la sesión: **0 resultados**).
- El mismo logcat mostró ANRs de **`com.android.vending`** y
  **`com.google.android.inputmethod.latin`** (el teclado de Gboard) en
  la misma ventana de tiempo, antes incluso de que mi script tocara la
  app — imposible que mi código haya causado esos dos.
- `adb shell top` confirmó el emulador con **600% CPU** (6 núcleos) casi
  saturado por procesos ajenos a la app: `dex2oat64` recompilando
  Google Docs en segundo plano, `com.google.android.gms` y
  `com.android.vending:background`, y `kswapd0` (el sistema estaba
  intercambiando memoria a disco). En el lado del host, `Get-Process`
  mostró `qemu-system-x86_64` con miles de segundos de CPU acumulados y
  dos daemons de Gradle todavía residentes de los builds anteriores de
  esta misma sesión.
- Conclusión: fue contención de recursos del entorno (emulador +
  compilaciones/servicios de fondo + los dos `./gradlew` que corrí antes
  en la misma máquina), no un problema introducido por el fix. Lo
  confirmé además indirectamente: el intento de medición que coincidió
  con el ANR **no llegó a guardar** ningún producto nuevo (verificado
  contra la base real: seguía existiendo solo XP-000001) — se cayó a
  mitad de camino, así que ese primer número (que había calculado en
  16.60s) es inválido y lo descarto explícitamente, no lo uso para nada.
- Una vez que `top` mostró el sistema en reposo (592% idle de 600%),
  repetí la medición limpia y el flujo completó de punta a punta
  ("Guardada como XP-000002", verificado también contra la base real).

No hay ninguna acción de código de este fix relacionada con este
incidente — lo dejo documentado en detalle porque un ANR durante una
verificación merece la evidencia completa, no una mención de una línea.

### Remedición de tiempo (pedida por el humano)

**Metodología y su límite, dicho de entrada:** esto es una repetición
mecánica de la secuencia de taps por ADB (`input tap`/`input text`),
cronometrada con `date` de un extremo a otro, sobre el emulador ya en
reposo (verificado con `top` antes de arrancar). **No es una medición
con la usuaria real** ni con un humano tocando la pantalla — no tiene
tiempo de lectura de las etiquetas, ni de decidir qué categoría tocar,
ni la torpeza normal de un dedo real. Sirve como señal rápida de si el
campo obligatorio nuevo (categoría) hace que el flujo se dispare muy por
encima de 20 segundos incluso en el caso más optimista — no reemplaza
el criterio 4 de la Fase 03, que exige la medición de ella con
cronómetro real.

Secuencia cronometrada (tap a tap, incluyendo pausas deliberadas para
apertura de cámara ~1s y compresión/guardado de la foto ~1.2s, que sí
son tiempos reales de espera y no artificios del script): tocar
"Agregar pieza" → tocar "Tomar foto" → tocar el obturador → tocar Costo
→ escribir "3000" → cerrar teclado → tocar el chip "Anillos" → deslizar
para ver Guardar → tocar Guardar.

**Resultado: 7.81 segundos**, con guardado confirmado ("Guardada como
XP-000002", verificado contra la base real) — corrida válida, a
diferencia del intento anterior interrumpido por el ANR.

**Lectura honesta de este número:** 7.81s de ejecución mecánica no dice
que una persona real tarde 7.81s — dice que la *secuencia* de toques
necesaria (incluyendo el toque nuevo del chip de categoría, que no
exige escribir nada, solo elegir entre 5 opciones visibles) no agrega
pasos pesados ni tiempos de espera nuevos al flujo. El campo que más
tiempo real le puede sumar a la usuaria es el de leer las 5 categorías y
decidir cuál tocar — algo que un script no necesita hacer y que no se
puede medir con ADB. Con el margen que deja este número frente al
límite de 20 segundos, no veo señal de que el cambio lo dispare por
encima — pero, tal como dejé anotado en el plan, **no cierro la Fase 03
con esto**: sigue pendiente la medición real con la usuaria, con
cronómetro, en su teléfono, como pide el criterio 4 original.

### Archivos tocados (vs. lo planeado)

Igual que el plan, más `ProductEditScreen.kt` (sumado al alcance por
tu segundo pedido):

- `app/src/main/java/gt/marcos/joyeria/ui/product/add/AddProductScreen.kt`
- `app/src/main/java/gt/marcos/joyeria/ui/product/add/AddProductUiState.kt`
- `app/src/main/java/gt/marcos/joyeria/ui/product/add/CategoryChipRow.kt` (nuevo)
- `app/src/main/java/gt/marcos/joyeria/ui/product/edit/ProductEditScreen.kt` ⚠️ (agregado al alcance por pedido explícito del humano, no estaba en el plan original)
- `app/src/main/res/values/strings.xml`
- `CLAUDE.md`
- `DECISIONES.md`
- `ESTADO.md`

No se tocó `AndroidManifest.xml` ni ningún otro archivo fuera de esta
lista.

### Suposiciones que tomé

- Altura de chip `56.dp` vía `Modifier.heightIn(min = 56.dp)` sobre
  `FilterChip` de Material3 (que por defecto es más chico): verificado
  visualmente que no se ve roto, pero es mi criterio, no algo que
  confirmaste vos mismo viendo el emulador.
- Orden final de los campos (categoría inmediatamente después de
  precio/ganancia, antes de cantidad/nombre/notas): elegido por mí
  dentro de lo que el plan ya proponía.
- No se tocaron los productos XP-000001/XP-000002 creados durante esta
  verificación (son datos de prueba desechables, igual que en la
  verificación de Fase 04); quedan en la base del emulador, no en
  ningún archivo del repo.

### Lo que NO hice

- No cerré la Fase 03 (sigue pendiente su criterio 4: medición real con
  la usuaria).
- No toqué nada de `ui/product/list/**` ni de compras/ventas — fuera de
  alcance de este fix.
- No agregué tests nuevos para `canSave`/`CategoryChipRow`: `ui/product/add`
  nunca estuvo bajo `[TESTS OBLIGATORIOS]` (Fase 03 solo lo exigía para
  `MoneyDigitsInputTest`/`ImageStorageScalingTest`, funciones puras). No
  inventé una obligación de test que no estaba pedida; lo verifiqué a
  mano en el emulador como está documentado arriba.

### Deuda técnica que dejé

- Ninguna nueva relacionada con este fix. La deuda de Fase 03 (fotos
  huérfanas ante muerte de proceso) sigue igual, sin relación con este
  cambio.

### Bloqueos / preguntas para el humano

Ninguno. Build y tests en verde, las dos pantallas verificadas con
captura real y teclado abierto como pediste, el orden de modifiers
correcto y confirmado (no solo "casi bien"), y la remedición de tiempo
dio 7.81s mecánicos — muy por debajo de 20s, aunque con la salvedad
explícita de que no reemplaza la medición real pendiente con la
usuaria. Quedo esperando tu revisión antes de comitear/taguear.

---

## Fase 03 — Cierre: criterio 4 cumplido (medición real)

**Fecha:** 2026-09-17.

La usuaria registró una pieza en un teléfono real, **sin guía**, en
**poco más de 15 segundos**, ya con la versión corregida por el fix de
usabilidad (categoría obligatoria con chips de un toque, todos los
campos visibles sin expandir nada). Margen contra el límite de 20
segundos de `FASES.md`: **~5 segundos**.

Con este número, el criterio 4 de Fase 03 queda cumplido con evidencia
real (no con la remedición mecánica de 7.81s de la sección anterior,
que era solo una señal previa, explícitamente no sustituta de esto).
Fase 03 cerrada.

**Tags aplicados y mergeados a `main` por el humano:**
- `fase-03-ok` (Fase 03 — Alta rápida de pieza)
- `fase-04-ok` (Fase 04 — Inventario y edición)
- `fix-quick-add-usability` (este fix de usabilidad)

**Nota para el futuro, dejada explícita por el humano:** volver a medir
cuando la pantalla de inicio sume el botón "Vender" (Fase 06 — Venta de
contado). D-024 (`DECISIONES.md`) ya documenta que la pantalla de
inicio actual es provisoria (solo "Agregar pieza" e "Inventario") y que
Fase 06 la reemplaza por la definitiva (Vender + Agregar pieza). Cambiar
la pantalla de inicio puede cambiar cuántos toques hacen falta para
llegar a "Agregar pieza" desde donde arranque el flujo real de venta —
el margen de ~5 segundos de esta medición no queda garantizado después
de ese cambio, hay que volver a cronometrar en Fase 06.

---

## Compras y el costo del producto — resumen y recomendación (2026-09-17)

A pedido del humano, antes de escribir nada de Fase 05: resumen de las
tres opciones de la sección "Compras y el costo del producto — decisión
pendiente" (arriba, 2026-09-14), enfocado específicamente en su efecto
sobre **el cálculo de ganancia** (no repito ahí el detalle de "capital
invertido"/"precio sugerido", que ya está completo arriba), más una
recomendación. **No decido yo** — esto es para que el humano elija.

### Lo que las tres comparten, antes de comparar

La ganancia de **ventas pasadas nunca cambia** pase lo que pase acá —
son snapshots en `sale_item` (D-002), ninguna opción los toca. Esto es
únicamente sobre la ganancia que se **muestra para ventas futuras**,
mientras conviven en el mismo stock unidades compradas a costos
distintos. Y una limitación que comparten las tres por igual:
`ESQUEMA.md` no trackea lotes — `product.cost_cents` es un único valor,
no "las 10 unidades viejas a Q40 y las 5 nuevas a Q55" por separado.
Ninguna opción puede ser exacta unidad por unidad sin ese cambio de
esquema, que no estoy proponiendo (fuera de alcance de esta decisión).

### Ejemplo concreto (mismo ejemplo en las tres, para comparar directo)

Tenía 10 unidades compradas a Q40, vendiéndolas a Q100 (ganancia real
Q60). Compra 5 unidades más, ahora a Q55 (el mayorista subió el
precio). Ella no cambia el precio de venta todavía: sigue en Q100. La
ganancia que el sistema **muestra** para la próxima venta, de las 15
unidades que hay en stock:

| Opción | `cost_cents` después de la compra | Ganancia mostrada (precio Q100) | Dirección del error sobre las 10 unidades viejas (costo real Q40, ganancia real Q60) |
|---|---|---|---|
| **A — última compra** | Q55 | Q45 | **Subestima** (dice Q45, la ganancia real de esas unidades es Q60) |
| **B — promedio ponderado** | Q45 ((10·40+5·55)/15) | Q55 | Ni exacto para las viejas (real Q60) ni para las nuevas (real Q45) — a mitad de camino |
| **C — no se toca** | Q40 | Q60 | Exacto para las 10 viejas, pero **sobreestima** Q15 sobre cada una de las 5 nuevas (costo real Q55, ganancia real Q45) hasta que alguien edite el producto a mano |

### Recomendación: Opción A (costo = última compra)

1. **Es la única cuyo error, cuando existe, va en la dirección segura.**
   Subestimar ganancia temporalmente (A) es preferible a sobreestimarla
   indefinidamente (C) — para una usuaria no técnica que confía en el
   número que ve (CLAUDE.md sección 1), es mucho peor que crea que gana
   más de lo que gana y tome decisiones de precio o de gasto sobre esa
   base falsa. B no tiene una dirección consistente: a veces subestima,
   a veces sobreestima, según qué lote se esté vendiendo en ese momento.
2. **Es la más simple de las tres.** Mismo mecanismo que ya existe para
   editar el producto a mano en Fase 04 (actualiza `cost_cents`, inserta
   fila en `price_history`) — sin fórmula nueva, sin recalcular nada en
   cada compra.
3. **Es la más fácil de explicarle a ella.** "El costo que ves es lo
   último que pagaste" no necesita ninguna aclaración. La Opción B sí la
   necesita (ya está anotado arriba: "¿por qué dice que me costó Q45 si
   la última vez pagué Q55?") — CLAUDE.md sección 6 pide cero jerga y
   cero sorpresas, y explicar un promedio ponderado a alguien que no lee
   inglés ni "SKU" es exactamente ese tipo de fricción.
4. **Es la que mejor conecta con para qué sirve el precio sugerido**
   (D-014, `suggestedPrice`): decidir cuánto cobrar la próxima vez, dado
   lo que acaba de pagar. Eso es literalmente "el costo de la última
   compra", no un promedio histórico — la Opción A hace que el precio
   sugerido reaccione de inmediato a que el mayorista le subió el
   precio, que es la señal que más le importa a ella en el momento de
   reponer stock.
5. Dado que ninguna opción es exacta por unidad (limitación de esquema
   compartida, arriba), la precisión extra que ofrece B para el reporte
   de "capital invertido" (Fase 09, todavía sin diseñar en detalle) no
   justifica el doble de complejidad y la pérdida de claridad en el
   número que ella ve todos los días.

**No implemento nada de esto todavía.** Queda para que el humano elija
(A, B, C, o algo distinto) antes de que arranque el código de Fase 05;
se registra en `DECISIONES.md` recién cuando se elija, como ya decía la
sección original de 2026-09-14.

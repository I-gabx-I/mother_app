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

- **Fase en curso:** Fase 00 — Andamiaje del proyecto (rama `fase/00-scaffolding`). Criterios de aceptación cumplidos y verificados; pendiente de tag `fase-00-ok` y merge a `main` por pedido explícito del humano (todavía no autorizado).
- **Última fase cerrada:** ninguna
- **Versión de base de datos:** —
- **Bloqueos abiertos:** ninguno

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
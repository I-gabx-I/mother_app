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

- **Fase en curso:** Fase 03 — Alta rápida de pieza (rama `fase/03-quick-add`).
  Implementada y verificada (build, tests, y flujo funcional real en
  emulador con `uid` generado, foto guardada <1MB y bien orientada).
  **No cerrada:** falta el criterio 4 (cronómetro real, tiene que
  medirlo la usuaria final) y confirmar D-021 (sin selector de
  categoría, decisión tomada durante la implementación).
- **Última fase cerrada:** `fix/seed-markup-bp` (tag `fix-seed-markup-bp`, mergeada a `main`); antes, Fase 02 — Motor de dinero y precios (tag `fase-02-ok`).
- **Versión de base de datos:** 1
- **Bloqueos abiertos:** ver "Fase 03 — Plan", sección "Bloqueos / preguntas para el humano" (6 preguntas)

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
   reales (sección de arriba).
2. **D-021** — confirmar que está bien no tener selector de categoría
   en esta fase, o pedir que lo agregue de otra forma.
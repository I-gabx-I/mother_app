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
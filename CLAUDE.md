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
- Usá BOM siempre que exista (`androidx.compose:compose-bom`). Las librerías
  cubiertas por el BOM se declaran **sin versión**. Esto elimina de raíz una
  familia entera de incompatibilidades.
- **KSP, desde la versión 2.3.0, tiene versionado independiente de Kotlin.**
  Ya no usa el formato compuesto `<kotlin>-<ksp>` (ese formato solo aplica a
  versiones de KSP anteriores a 2.3.0). Usá la última versión estable 2.3.x
  verificada en `github.com/google/ksp/releases` o en el `maven-metadata.xml`
  de `com.google.devtools.ksp`. No apliques la regla vieja de "debe coincidir
  con Kotlin" a versiones de KSP ≥ 2.3.0.
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
- Nada de `!!`. Nada de `runBlocking` fuera de tests. Nada de `GlobalScope`.
- Nada de `TODO()` ni stubs vacíos que compilen y mientan. Si algo no se implementó
  en esta fase, no existe en el código todavía.

---

## 6. Reglas de UI para la usuaria final

Estas son requisitos, no sugerencias estéticas.

- **Registrar una pieza nueva: máximo 3 taps y menos de 20 segundos.**
  Pantalla de alta rápida: foto, costo, precio. Todo lo demás es opcional y se
  edita después. Si el formulario de alta pide más de 4 campos obligatorios, está mal.
- La pantalla de inicio tiene dos acciones grandes y obvias: **Vender** y **Agregar pieza**.
  No es un dashboard de métricas.
- Tamaño mínimo de texto de cuerpo: 18sp. Área táctil mínima: 56dp.
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
9. **Todo análisis, resumen o hallazgo que me pidas lo escribís en un `.md` del repo antes de imprimirlo en consola. La consola se pierde, el archivo se audita.**

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
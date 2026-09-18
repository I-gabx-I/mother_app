# DECISIONES.md — Registro de decisiones

Formato corto por decisión: contexto, decisión, por qué, qué se descartó, consecuencias.
Las primeras seis las tomó el humano antes de empezar. Claude Code agrega una entrada
nueva cada vez que toma una decisión de diseño que no estaba escrita.

**Nunca se edita ni se borra una decisión pasada.** Si algo cambia, se agrega una
decisión nueva que dice "reemplaza a D-00X" y se explica por qué.

---

## D-001 — Dinero como `Long` en centavos

**Contexto:** toda la app gira alrededor de costos, precios, ganancias y abonos.

**Decisión:** persistir y operar dinero exclusivamente como `Long` en centavos,
envuelto en un value class `Money` en `domain`.

**Por qué:** los flotantes binarios no representan decimales exactos. Con `Double`,
sumar cien ventas genera descuadres de centavos imposibles de rastrear, y en una app
contable un descuadre destruye la confianza de la usuaria en todo el sistema.

**Descartado:** `Double` (impreciso), `BigDecimal` (correcto pero pesado en Room y
verboso para operaciones que aquí son triviales).

**Consecuencia:** hay que formatear siempre al presentar, y nunca dividir sin definir
el redondeo. Se acepta a cambio de exactitud.

---

## D-002 — Snapshots de costo y precio en cada línea de venta

**Contexto:** los precios de la joyería cambian con el tiempo.

**Decisión:** `sale_item` guarda copia del nombre, `uid`, costo unitario y precio
unitario al momento de la venta. Los reportes históricos usan solo esos snapshots.

**Por qué:** si la ganancia histórica se calculara con JOIN al precio actual, corregir
el costo de un producto hoy reescribiría las ganancias de meses cerrados. Un reporte
que cambia solo no sirve para tomar decisiones.

**Descartado:** normalización pura con JOIN al producto.

**Consecuencia:** duplicación de datos deliberada. Es lo correcto para datos contables.

---

## D-003 — El código de barras codifica el `uid`, no el precio

**Contexto:** la idea original era imprimir el precio en la etiqueta.

**Decisión:** la etiqueta lleva el `uid` (`XP-000042`). El precio se consulta en la app.

**Por qué:** los precios cambian por promoción, por variación del mayorista, o porque
la pieza se maltrató. Si el precio está impreso, cada cambio obliga a reimprimir y
reetiquetar físicamente. Con el `uid`, se edita un campo y listo.

**Descartado:** codificar precio, o precio + uid concatenados.

**Consecuencia:** para saber el precio hay que escanear con la app. Aceptable, porque
la app siempre está en la mano de la vendedora.

---

## D-004 — Sin importación ni aduana en el modelo

**Contexto:** la vendedora **compra el producto localmente en Guatemala** y lo revende
en su trabajo. No manda a traer nada del exterior.

**Decisión:** no se modela flete internacional, impuestos de importación ni tipo de
cambio. El único gasto extra de una compra es transporte local opcional
(`purchase.extra_cost_cents`), que por defecto es `0`.

**Por qué:** modelar importación agrega tres entidades y un prorrateo complejo que
nadie va a usar, y complica la pantalla de compra que la usuaria sí va a ver.

**Descartado:** modelo de lotes con costeo de importación.

**Consecuencia:** si algún día ella empieza a importar, se agrega en una decisión nueva.

---

## D-005 — Offline-first, sin cuentas ni backend

**Contexto:** vende en su trabajo, en la calle, donde la señal no es confiable.

**Decisión:** Room es la única fuente de verdad. Cero red, cero login, cero analytics.
El respaldo es un archivo que ella comparte a donde quiera (fase 11).

**Por qué:** la app debe funcionar en modo avión al 100%. Además, un backend implica
costo mensual, mantenimiento y responsabilidad sobre datos de clientas reales, todo
eso para un negocio de una sola persona con un solo teléfono.

**Descartado:** Firebase, Supabase, cualquier sync en tiempo real.

**Consecuencia:** no hay multi-dispositivo. Se acepta hasta que haga falta de verdad.

---

## D-006 — Archivado en vez de borrado

**Contexto:** es un registro contable.

**Decisión:** productos y clientes se archivan; ventas se anulan con motivo y fecha.
No hay borrado físico en ninguna parte.

**Por qué:** borrar una venta destruye la ganancia histórica del mes y la usuaria no
tiene forma de deshacerlo. El daño es silencioso y permanente.

**Descartado:** `DELETE` con papelera temporal (más complejo, mismo riesgo).

**Consecuencia:** las consultas siempre filtran por `archived = false` / `status != CANCELLED`.
Esto debe estar en los DAOs, no repetido en cada pantalla.

---

## D-007 — Reemplazo de la regla de versionado de KSP (reemplaza parcialmente lo asumido en CLAUDE.md §2.1)

**Contexto:** CLAUDE.md §2.1 decía que la versión de KSP debe coincidir exactamente
con la de Kotlin, formato `<kotlin>-<ksp>`. Eso dejó de ser cierto desde KSP 2.3.0:
el propio changelog de esa versión dice "KSP version is no longer tied to the Kotlin
compiler version". El humano corrigió la regla en CLAUDE.md antes de que se empezara
a escribir código de Fase 00.

**Decisión:** usar KSP `2.3.12` (última estable verificada). Verificado en
`github.com/google/ksp/releases/tag/2.3.12` (publicado 2026-09-09) y en el
`maven-metadata.xml` de `com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin`
en Maven Central.

**Por qué:** es la última versión estable publicada a la fecha, y al ser independiente
de Kotlin (release notes de 2.3.0), es compatible con el Kotlin `2.2.10` ya fijado en
el proyecto sin necesitar ningún sufijo compuesto.

**Descartado:** inventar o asumir un sufijo `<kotlin>-<ksp>` como se hacía en
versiones de KSP anteriores a 2.3.0 — ya no aplica.

**Consecuencia:** CLAUDE.md §2.1 quedó actualizado con la regla nueva. Cualquier
futura actualización de KSP debe seguir verificándose contra el `maven-metadata.xml`
real, nunca inventando el número.

---

## D-008 — Namespace y applicationId definitivos: `gt.marcos.joyeria`

**Contexto:** el wizard de Android Studio generó el proyecto con el placeholder
`com.example.motherapp` como `namespace` y `applicationId`.

**Decisión:** el humano definió `gt.marcos.joyeria` como namespace y applicationId
definitivos, aplicado en Fase 00 antes de escribir ninguna otra clase Kotlin.

**Por qué:** cambiar el `applicationId` después de publicar o de tener más código
escrito es más costoso (mueve todos los paquetes, y si ya hubiera instalaciones
reales en un teléfono, sería una app distinta a efectos de Play/instalación).
Hacerlo en Fase 00, con el proyecto vacío, es el momento más barato.

**Descartado:** mantener `com.example.motherapp`.

**Consecuencia:** todo el código Kotlin del scaffold (`MainActivity`, tema, tests)
se mueve del paquete `com.example.motherapp` a `gt.marcos.joyeria`.

---

## D-009 — Versión de Hilt: `2.60.1`

**Contexto:** Fase 00 requiere Hilt configurado con `@HiltAndroidApp`. CLAUDE.md
prohíbe inventar versiones; había que buscar la última estable real.

**Decisión:** usar Hilt (Dagger) `2.60.1` para `hilt-android`, `hilt-compiler` y
`hilt-android-gradle-plugin`.

**Por qué:** es la última versión listada en el `maven-metadata.xml` de
`com.google.dagger:hilt-android` en Maven Central (`lastUpdated` 20260706203408),
y se verificó por HTTP que los tres artefactos (`hilt-android`, `hilt-compiler`,
`hilt-android-gradle-plugin`) existen publicados en esa versión exacta. El índice
de búsqueda de `search.maven.org` mostraba una versión vieja (`2.56.2`) que no
coincidía con el `maven-metadata.xml` real, así que no se usó como fuente única.

**Descartado:** `2.56.2` (versión desactualizada que devolvía el buscador).

**Consecuencia:** ninguna relevante; se configura con KSP, no con KAPT, según
CLAUDE.md §2.1.

---

## D-010 — `default_markup_percent` (Int) en vez de `default_markup_multiplier` (decimal)

**Contexto:** `ESQUEMA.md` tenía `default_markup_multiplier` = `2.0` como clave
de `app_setting`, un valor decimal guardado como string. CLAUDE.md prohíbe
`Float`/`Double` para dinero en cualquier capa; dejar un `"2.0"` en la
configuración invita a parsearlo a `Double` en el momento de calcular el
precio sugerido, aunque las tablas de transacciones ya estén correctas.

**Decisión:** reemplazar `default_markup_multiplier` (`2.0`) por
`default_markup_percent` (`200`, entero). El precio sugerido se calcula como
`costo_cents * percent / 100` en `Long`. Todos los valores de `app_setting`
se parsean siempre a `Long` o `Int`, nunca a `Double`.

**Por qué:** mantiene la regla de "dinero siempre en enteros" también en la
configuración, no solo en las tablas de ventas y compras. Un `"2.0"` parseado
a `Double` para multiplicar centavos reintroduce exactamente el problema que
`Money` (D-001) existe para evitar.

**Descartado:** dejar el valor como string decimal (`"2.0"`) y parsearlo a
`Double` o `BigDecimal` al calcular.

**Consecuencia:** cuando se implemente `PricingCalculator.suggestedPrice`
(Fase 02), su parámetro es un `percent: Int`, no un `multiplier: Double`, y
calcula `cost * percent / 100`. El valor por defecto `200` es equivalente al
`2.0` anterior: precio sugerido = costo × 2.

---

## D-011 — Todas las tablas del esquema se crean en la versión 1 (Fase 01)

**Contexto:** el esquema original repartía la creación de tablas fase por
fase (`price_history`/`purchase`/`purchase_item` en Fase 04,
`customer`/`payment` en Fase 06), cada una con su propia migración Room y su
`app/schemas/N.json`. No hay usuarios instalados hasta después de la Fase 05
(fin del MVP), así que no existe ninguna instalación real que migrar todavía.

**Decisión:** todas las tablas de `ESQUEMA.md` se declaran en la base de
datos versión 1, en la Fase 01. Las fases posteriores (04, 05, 06...) siguen
agregando DAOs, repositorios, casos de uso y pantallas fase por fase, contra
tablas que ya existen desde la Fase 01. Se quitan los criterios de migración
y la referencia a `app/schemas/2.json` de las fases que los tenían (solo la
Fase 06 los tenía; las Fases 04 y 05 no hacían referencia a migraciones).

**Por qué:** una migración intermedia sin ningún usuario instalado es
ceremonia pura: no hay datos reales que preservar, y cada migración exige su
propio test y su propio schema commiteado sin aportar nada hasta que exista
una versión publicada. Cuando exista una instalación real, ahí sí las
migraciones vuelven a ser necesarias y CLAUDE.md ya las exige aditivas,
versionadas y testeadas.

**Descartado:** mantener migraciones incrementales fase por fase desde el
día 1.

**Consecuencia:** la Fase 01 crece: ahora declara las diez tablas completas
(`category`, `product`, `price_history`, `purchase`, `purchase_item`,
`customer`, `sale`, `sale_item`, `payment`, `app_setting`), sus índices y su
semilla, aunque varias no tengan DAO ni UI hasta fases posteriores.
`app/schemas/1.json` es el único schema commiteado hasta que exista una
necesidad real de migrar.

---

## D-012 — Robolectric para tests de DAO, nunca para tests de `domain`

**Contexto:** los tests de DAO con Room in-memory necesitan un `Context` de
Android, que no existe en un test JVM puro. La propuesta original (ver
`ESTADO.md`, análisis previo a Fase 01) fue usar Robolectric para que esos
tests sigan corriendo bajo `./gradlew testDebugUnitTest` sin emulador. El
humano la aprobó con dos ajustes.

**Decisión:**
1. `org.robolectric:robolectric` `4.17` y `androidx.test:core` `1.7.0`
   (`testImplementation`), verificados contra `maven-metadata.xml` real.
2. `app/build.gradle.kts` lleva
   `testOptions { unitTests { isIncludeAndroidResources = true } }`,
   obligatorio, no opcional.
3. Robolectric se usa **exclusivamente** para tests de Room/DAO
   (`app/src/test/java/**/data/**`). Prohibido en `app/src/test/java/**/domain/**`:
   los tests de `Money`/`PricingCalculator` (Fase 02) son aritmética pura y
   corren con JUnit4 normal, en milisegundos. Regla anotada en `CLAUDE.md`
   sección 8.

**Por qué:** Robolectric simula un `Context` de Android completo (con su
propio SQLite), que es exactamente lo que necesita Room y nada más liviano lo
resuelve sin emulador. Pero es un runtime completo: cargarlo para tests que
no lo necesitan (aritmética de `domain`) los volvería lentos sin ninguna
razón, y además abriría la puerta a que código de `domain` dependa de Android
sin que nadie lo note (viola la sección 5 de `CLAUDE.md`).

**Descartado:** usar Robolectric en todos los tests unitarios por
uniformidad; mover los tests de DAO a `androidTest` (rompe el criterio de un
solo comando `testDebugUnitTest`).

**Consecuencia:** dos perfiles de test conviven en `app/src/test`: los de
`data/` (con Robolectric, más lentos, necesitan `Context`) y los de `domain/`
(JUnit4 puro, rápidos). Si algún test de `domain` empieza a necesitar
Robolectric, es una señal de que se filtró una dependencia de Android donde
no debería haber ninguna.

---

## D-013 — Porcentajes como `Int` en puntos básicos, nunca `Double`

**Contexto:** `FASES.md` (Fase 02) y `ESQUEMA.md` no definían cómo se
representa un porcentaje calculado (`marginOnSale`, `markupOnCost`,
`margenSobreVenta`, `recargoSobreCosto`). Sin una regla explícita, la
implementación más obvia en Kotlin es devolver un `Double` (`0.6` para
60%), que es exactamente el tipo que CLAUDE.md prohíbe para todo lo
relacionado con dinero, y por la misma razón: los flotantes binarios no
representan decimales exactos, y acá se derivan directo de centavos.

**Decisión:** todo porcentaje calculado se representa como `Int` en puntos
básicos: 1 punto básico = 0.01%, `valor / 100` = el porcentaje con dos
decimales. Ejemplos: `6000` = `60.00%`, `15000` = `150.00%`. Fórmulas:
`margenSobreVenta = gananciaUnitaria * 10000 / salePriceCents`,
`recargoSobreCosto = gananciaUnitaria * 10000 / costCents` (ambas en Long
antes de convertir a Int, para no desbordar). El formateo a texto (dividir
por 100, agregar `%`) es de `ui`, igual que `Money.format()`.

**Por qué:** mismo argumento que D-001 (`Money` en centavos): un entero
exacto es rápido, determinístico y trivial de testear con casos de borde;
un `Double` acumula error de redondeo y hace que dos productos con el
"mismo" margen a simple vista comparen distinto en un test.

**Descartado:** `Double` (impreciso), `BigDecimal` (correcto pero pesado
para algo que se calcula constantemente en listados de inventario).

**Consecuencia:** `default_markup_percent` en `app_setting` (D-010, valor
`200` = 200%) usa una convención **distinta** (porcentaje entero simple, no
puntos básicos) — son dos campos separados, aprobados por separado en
momentos distintos. No se unifican acá; si conviene unificarlos se hace en
una decisión nueva, explícita. **Actualización:** esa unificación se hizo
en **D-014**, que reemplaza a D-010 y ajusta la nota de arriba — ya no hay
dos convenciones de "porcentaje" distintas conviviendo en el esquema.

---

## D-014 — `default_markup_bp`: unificado con el recargo real de la sección 3.5 (reemplaza a D-010, ajusta D-013)

**Contexto:** D-010 no era solo una diferencia de unidad con D-013 (entero
simple vs. puntos básicos) — era un significado distinto disfrazado de
"porcentaje". `default_markup_percent = 200` con la fórmula
`costo * percent / 100` da precio = costo × 2. Pero según CLAUDE.md 3.5
(`markup = ganancia / costo`), eso es un recargo del **100%**
(ganancia = costo, costo × 2), no del 200%. Mientras tanto, `markupOnCost`
(Fase 02, D-013) sí calcula el recargo real: costo Q40 → precio Q100 da
150%. Es decir, "markup percent" significaba una cosa en `app_setting` y
otra distinta en `PricingCalculator`, con el mismo nombre.

**Decisión:**
- `default_markup_percent` → **`default_markup_bp`**, valor `10000` (en vez
  de `200`). Con la fórmula nueva de abajo, `10000` sigue dando el mismo
  comportamiento de siempre (precio sugerido = costo × 2): recargo del
  100%, calculado exactamente como en 3.5.
- `suggestedPrice(cost, markupBp, roundingStep) = cost + cost * markupBp / 10000`.
- `markupOnCost` y `marginOnSale` devuelven puntos básicos (`Int`), sin
  cambios respecto a D-013 — ya estaban bien.
- Fase 02 (`FASES.md`) suma un criterio de aceptación: `suggestedPrice` y
  `markupOnCost` son inversas para un par (costo, precio) — pasarle a
  `suggestedPrice` el `markupBp` que devolvió `markupOnCost` para ese par
  reproduce el mismo precio, sin redondeo de por medio.

**Por qué:** un mismo nombre ("recargo", "markup", "percent") no puede
significar dos fórmulas distintas en el mismo proyecto — es la clase de
inconsistencia que hace que alguien lea `default_markup_percent = 200`,
asuma (razonablemente, por 3.5) que es un recargo del 200%, y calcule mal
el precio por reflejo. Unificar todo a la definición de 3.5
(`markup = ganancia / costo`), en puntos básicos (D-013), elimina la
ambigüedad de raíz: hay una sola fórmula de "recargo" en todo el proyecto.

**Descartado:** dejar `default_markup_percent` como estaba y solo
documentar la diferencia (lo que hacía D-013 antes de esta decisión) — es
parchar la confusión con una nota en vez de sacarla del esquema.

**Consecuencia:** el criterio de inversa (`suggestedPrice` ∘ `markupOnCost`
= identidad) sirve además como test de regresión: si alguien vuelve a
introducir dos fórmulas de "recargo" distintas, ese test lo detecta sin
necesidad de leer el código. La igualdad exacta (sin redondeo) depende de
que la división entera de `markupOnCost` no trunque para el par de prueba;
no es una propiedad matemática universal para cualquier (costo, precio) —
es válida para el ejemplo canónico y para los pares que se elijan como
casos de test, no una garantía general de round-trip sin pérdida.

---

## D-015 — Las dos divisiones entre cero de Fase 02 devuelven `null` (`Int?`), no `0`

**Contexto:** `ESQUEMA.md` y `FASES.md` (Fase 02, criterio 4) exigen que
`marginOnSale` (divide entre `salePrice.cents`) y `markupOnCost` (divide
entre `cost.cents`) manejen su división entre cero con "un resultado
explícito, sin crash ni `NaN`", pero ninguno de los dos documentos dice
**qué** valor. La primera propuesta (de Claude Code, en `ESTADO.md`,
sección "Fase 02 — Plan") fue devolver `0` como centinela para ambos
casos. El humano la rechazó con un contraejemplo concreto: una pieza
comprada a Q50 y vendida a Q50 tiene margen real **0** y recargo real
**0** — son resultados legítimos y aritméticamente correctos, no casos de
error. Con `0` como centinela, "no se puede calcular" y "se calculó y dio
cero" quedan indistinguibles, y esa diferencia le importa a la Fase 03:
una pieza sin precio cargado debe mostrar un guión (`—`), no `0%`, porque
`0%` afirma algo falso sobre el negocio (que se calculó un margen real de
cero, cuando en realidad no hay margen que calcular).

**Decisión:** `marginOnSale(cost, salePrice)` y `markupOnCost(cost,
salePrice)` devuelven `Int?` (puntos básicos, D-013), no `Int`:
- `marginOnSale`: `null` cuando `salePrice.cents == 0` (la venta no tiene
  precio con el cual expresar un porcentaje). En cualquier otro caso,
  incluido costo igual a precio, devuelve el `Int` real —
  `gananciaUnitaria * 10000 / salePriceCents`, que da `0` cuando la
  ganancia es cero de verdad.
- `markupOnCost`: `null` cuando `cost.cents == 0` (no hay costo con el
  cual expresar un recargo). Mismo razonamiento para el resto de los
  casos.
- `profit(cost, salePrice)` no cambia: es una resta, nunca divide, así
  que nunca tiene un caso indefinido — siempre devuelve el `Money` real,
  incluida una pérdida negativa.
- `suggestedPrice(cost, markupBp, roundingStep)`: si `roundingStep.cents
  <= 0`, devuelve el precio crudo sin redondear (no divide, no crashea).
  Este caso no necesita `Money?`/`null` porque `suggestedPrice` no tiene
  una operación indefinida cuando el paso es inválido — simplemente no
  hay nada que redondear, y el precio sigue siendo un número real y
  usable. La razón para tolerarlo en vez de exigir `roundingStep > 0`:
  `roundingStep` en producción sale de `price_rounding_step_cents` en
  `app_setting`, un valor `String` genérico que se parsea a entero (ver
  D-010/`ESQUEMA.md`); si ese valor llegara corrupto, vacío o mal escrito
  y se parseara a `0` o negativo, preferimos un precio sugerido sin
  redondear antes que un crash o una división entre cero en medio del
  flujo de alta rápida de pieza (Fase 03, CLAUDE.md sección 6: "máximo 3
  taps y menos de 20 segundos" — un crash ahí es lo peor posible).

**Por qué `null` y no otro centinela (`Int.MAX_VALUE`, una excepción,
etc.):** `null` es la representación nativa de Kotlin para "ausencia de
valor", no un número que alguien podría confundir con un resultado real.
Devolver `Int?` obliga al compilador a que quien consuma el valor (un
caso de uso, un ViewModel, un Composable en fases futuras) decida
explícitamente qué hacer con la ausencia — típicamente mostrar `—` en vez
de un `%` — en vez de que ese `0` se cuele silenciosamente en un reporte
o en un cálculo posterior como si fuera un dato real. Una excepción
violaría "sin crash" explícito del criterio de aceptación.

**Descartado:**
- `0` como centinela para ambos casos (primera propuesta, rechazada:
  colisiona con el caso real de margen/recargo cero).
- `Int.MAX_VALUE` u otro valor grande como centinela de "infinito": no se
  llegó a proponer en firme, pero se descarta por la misma razón que `0`
  — sigue siendo un número que alguien puede tratar como dato real, y
  además arriesga overflow al multiplicar por `10000` antes de dividir.
- Lanzar excepción: viola "sin crash" del criterio 4 de Fase 02.

**Consecuencia:** `PricingCalculatorTest` incluye un par de tests que
existen específicamente para evitar que esta distinción se "simplifique"
de vuelta a un centinela en el futuro: un caso de costo y precio iguales
(Q50/Q50) que debe devolver `0` (no `null`) en ambas funciones, y los
casos de costo/precio cero que deben devolver `null` (no `0`). El
criterio 5 de Fase 02 (inversa de `suggestedPrice`/`markupOnCost`) ahora
trabaja sobre un `Int?`: el test falla explícitamente si `markupOnCost`
devuelve `null` para el par canónico, en vez de tratar `null` como un
caso válido. Fase 03, al construir la pantalla de alta rápida, tiene que
decidir en `ui` cómo mostrar un `null` de estas funciones (guión, texto
"—" o similar) — se deja para esa fase, no se resuelve acá.

**Adenda — `roundUpToMultiple` con `value` negativo (revisión de código,
mismo commit sin mergear):** la implementación original de
`roundUpToMultiple` (`value + (step - value % step)` cuando el resto no
es cero) da un resultado incorrecto para `value` negativo: con
`value = -4100` y `step = 500` devolvía `-3500`, pero el múltiplo de
`500` más chico que sigue siendo `>= -4100` es `-4000`, no `-3500`. La
causa es que `%` en Kotlin sigue el signo del dividendo (resto negativo
para `value` negativo), y la fórmula original no lo contemplaba. Se
corrigió a `value.floorDiv(step) * step` (múltiplo más grande que no
supera a `value`, correcto para cualquier signo porque `floorDiv`
redondea hacia menos infinito) más un `step` si ese múltiplo no es ya
exactamente `value`. Con las firmas actuales de `suggestedPrice`
(`cost >= 0` en cualquier uso real, `markupBp` acotado en la práctica a
`>= -10000` porque el precio de venta no es negativo) este caso no es
alcanzable — haría falta `markupBp < -10000`, un recargo peor que
-100%, que no ocurre con datos de negocio reales — pero se eligió
corregir la función en vez de solo documentar la limitación: el arreglo
es una fórmula única sin casos especiales por signo, no más compleja que
la original, y una función de dinero que es "correcta salvo para
entradas que hoy no llegan" es exactamente el tipo de bug silencioso que
la sección 3 de `CLAUDE.md` quiere evitar de raíz. Test agregado:
`suggestedPrice_roundingNegativeRawPrice_minus4100RoundsUpToMinus4000`.

---

## D-016 — Comentarios de código en español, no en inglés (reemplaza parcialmente lo asumido en CLAUDE.md §5)

**Contexto:** `CLAUDE.md` §5 decía "identificadores, nombres de archivo,
comentarios y commits: en inglés". Al revisar `Money.kt` y
`PricingCalculator.kt` (Fase 02), el humano encontró que esa regla,
aplicada de forma literal, produce comentarios en inglés explicando
decisiones de negocio en quetzales, puntos básicos y flujos pensados para
una vendedora guatemalteca — el equipo que escribe y lee este código es
hispanohablante, y esos comentarios documentan el porqué de reglas como
"por qué `markupOnCost` devuelve `null`" o "por qué el redondeo es hacia
arriba", no una API pública en inglés que otro equipo internacional vaya
a consumir.

**Decisión:** `CLAUDE.md` §5 queda: identificadores, nombres de archivo y
mensajes de commit **en inglés**; comentarios de código **en español**,
con tildes correctas (los archivos son UTF-8, no hay ninguna razón
técnica para evitarlas). El texto visible por la usuaria sigue en
español y solo en `strings.xml`, sin cambios — esa regla nunca estuvo en
duda, es una regla distinta con un motivo distinto (legibilidad para la
usuaria final, no para quien lee el código).

**Por qué:** un comentario que nadie más que el propio equipo
hispanohablante va a leer, escrito en un segundo idioma "porque sí", no
mejora nada — al contrario, un comentario mal traducido o con matices
perdidos (por ejemplo, la diferencia entre "ganancia", "margen" y
"recargo" de CLAUDE.md 3.5, que son tres palabras técnicas del negocio
sin traducción trivial y sin ambigüedad al español) es peor que uno
directo en el idioma en el que se piensa el negocio. El código en sí
(nombres de función, de variable, de archivo) sigue en inglés porque ahí
sí importa la convención del ecosistema Kotlin/Android y la
consistencia con librerías de terceros.

**Descartado:** mantener la regla original (comentarios en inglés) y
aceptar la pérdida de matiz; una regla híbrida "comentarios técnicos en
inglés, comentarios de negocio en español" — se descartó por ser
ambigua línea por línea, sin un criterio objetivo de cuál es cuál.

**Consecuencia:** los comentarios ya escritos en Fase 02
(`Money.kt`, `PricingCalculator.kt`, `MoneyTest.kt`,
`PricingCalculatorTest.kt`) se revisaron para que estén en español con
tildes correctas, en la misma línea de estilo que
`data/local/AppDatabase.kt` (ya escrito así desde Fase 01, sin que
existiera esta regla explícita todavía — queda como el estilo de
referencia). No se tocaron comentarios de fases ya cerradas y tageadas
más allá de ese archivo de referencia: si aparece algo por corregir en
`data/` o en cualquier archivo fuera de "Archivos permitidos" de Fase 02,
queda anotado como pendiente en `ESTADO.md`, no se corrige de paso acá.

---

## D-017 — CameraX `1.6.2` (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`)

**Contexto:** Fase 03 necesita capturar una foto con la cámara del
teléfono (CLAUDE.md sección 2 ya fija CameraX como stack de captura). Los
cuatro artefactos se publican en lockstep (misma versión siempre).

**Decisión:** usar `1.6.2` para los cuatro. Verificado en
`dl.google.com/android/maven2/androidx/camera/<artefacto>/maven-metadata.xml`
para cada uno de los cuatro por separado: todos listan `1.6.2` como la
última versión estable antes de `1.7.0-alpha01/02/03` (pre-release,
descartado). `minSdk 24` del proyecto cubre de sobra el mínimo de CameraX
1.6.x (21+).

**Descartado:** `1.7.0-alpha0x` (pre-release, CLAUDE.md prohíbe usar algo
que no sea estable salvo justificación explícita, que acá no existe).

**Consecuencia:** se agregan los cuatro artefactos uno por uno, con
`./gradlew assembleDebug` entre cada uno (CLAUDE.md §2.1).

---

## D-018 — Coil `3.3.0` (`io.coil-kt.coil3:coil-compose`), no `3.6.2` (reemplaza la decisión original de esta misma entrada)

**Contexto:** Fase 03 necesita mostrar la foto capturada en la pantalla
(CLAUDE.md sección 2 fija Coil para carga de imágenes). El grupo Maven de
Coil 3.x es `io.coil-kt.coil3` (renombrado desde `io.coil-kt` en la
versión 2.x, que ya no es la que corresponde usar).

**Primer intento, revertido:** `io.coil-kt.coil3:coil-compose:3.6.2` (la
última estable según `maven-metadata.xml`, `lastUpdated` 2026-09-04).
Compilaba solo mientras el proyecto no tenía ningún código que
**usara** Coil de verdad — al escribir el primer Composable que sí lo
usa, `./gradlew assembleDebug` falló con `Class 'kotlin.Unit' was
compiled with an incompatible version of Kotlin. The actual metadata
version is 2.4.0, but the compiler version 2.2.0 can read versions up to
2.3.0`, y errores de "Unresolved reference" sobre funciones básicas del
stdlib (`apply`, `filter`, `to`, `maxOf`...) en archivos que ni siquiera
tocan Coil (`ImageStorage.kt`, `MoneyDigitsInput.kt`). Diagnóstico con
`./gradlew :app:dependencyInsight --dependency org.jetbrains.kotlin:kotlin-stdlib
--configuration debugRuntimeClasspath`: `coil-android:3.6.2` (transitivo
de `coil-compose`) declara una dependencia dura a
`kotlin-stdlib:2.4.10` — una versión de Kotlin más nueva que la `2.2.10`
que este proyecto tiene pineada (CLAUDE.md §2.1: prohibido subir Kotlin
sin pararse a preguntar). Gradle resuelve el conflicto de versiones
tomando la más alta de todo el árbol (`2.4.10`), así que **todo** el
proyecto terminaba compilando contra un stdlib que el compilador 2.2.10
no puede leer — de ahí que hasta código sin relación con Coil fallara.

**Decisión final:** `io.coil-kt.coil3:coil-compose:3.3.0`. Verificado el
POM de cada versión de `coil-android` desde `3.0.4` hasta `3.6.2`
(`curl` contra `repo1.maven.org/maven2/io/coil-kt/coil3/coil-android/<version>/coil-android-<version>.pom`,
buscando su dependencia a `kotlin-stdlib`):

| Coil | `kotlin-stdlib` que pide |
|---|---|
| 3.0.4 | 2.0.21 |
| 3.1.0 | 2.1.10 |
| 3.2.0 | 2.1.20 |
| **3.3.0** | **2.2.0** ✅ (≤ 2.2.10 del proyecto) |
| 3.4.0 | 2.3.10 |
| 3.5.0 | 2.4.0 |
| 3.6.0 – 3.6.2 | 2.4.10 |

`3.3.0` es la versión más alta cuyo `kotlin-stdlib` es igual o anterior
al `2.2.10` ya pineado — no hace falta arriesgar con `3.4.0` (`2.3.10`,
zona ambigua contra el límite "hasta 2.3.0" del mensaje de error) cuando
hay una versión claramente segura disponible. Confirmado con
`assembleDebug` en verde después del cambio.

**Descartado:**
- `3.6.2` (revertido, ver arriba).
- `3.4.0`/`3.5.0` (piden `kotlin-stdlib` por encima de lo que el
  compilador 2.2.10 puede leer con certeza, o justo en el límite —
  no vale la pena probar cuando `3.3.0` ya es segura).
- Coil 2.x (`io.coil-kt:coil-compose`, grupo viejo).
- Subir la versión de Kotlin del proyecto para poder usar Coil 3.6.x:
  CLAUDE.md §2.1 lo prohíbe explícitamente sin pausar a preguntar, y
  hacerlo solo para acomodar una librería de imágenes es
  desproporcionado.

**Consecuencia:** un solo artefacto (`coil-compose`), sin artefactos de
red (las fotos son locales, `filesDir`). Queda anotado como precedente:
antes de pinear una versión "la más nueva disponible" de cualquier
librería, conviene revisar su propia dependencia de `kotlin-stdlib`
contra la del proyecto, no solo que exista y esté publicada.

---

## D-019 — `lifecycle-viewmodel-ktx` y `lifecycle-runtime-compose`, reutilizando la versión `2.11.0` ya pineada

**Contexto:** Fase 03 agrega el primer `ViewModel` de la app
(`AddProductViewModel`, CLAUDE.md sección 5: "los ViewModels exponen un
único `StateFlow<UiState>`"). Hacen falta `ViewModel`/`viewModelScope`
(`lifecycle-viewmodel-ktx`) y `collectAsStateWithLifecycle()`
(`lifecycle-runtime-compose`) para conectarlo a Compose.

**Decisión:** agregar ambos artefactos usando el mismo `version.ref
= "lifecycleRuntimeKtx"` (`2.11.0`) que `libs.versions.toml` ya tiene
pineado para `androidx-lifecycle-runtime-ktx` desde Fase 00, en vez de
buscar/pinear un número nuevo. Verificado que `2.11.0` existe y es la
última estable para los dos artefactos en
`dl.google.com/android/maven2/androidx/lifecycle/<artefacto>/maven-metadata.xml`
(el tren de versión de `androidx.lifecycle` se publica en conjunto).

**Por qué:** CLAUDE.md §2.1 pide usar la versión que el proyecto ya tiene
cuando corresponde, en vez de inventar un número nuevo — acá corresponde
literalmente, es el mismo grupo Maven (`androidx.lifecycle`) en el mismo
tren de release.

**Descartado:** pinear una versión distinta para estos dos artefactos
sin verificar si coincide con la ya presente.

**Consecuencia:** dos artefactos nuevos, mismo `version.ref` que ya
existía — no se agrega ninguna clave nueva a `[versions]` en
`libs.versions.toml`, solo dos entradas nuevas en `[libraries]`.

---

## D-020 — `androidx.compose.material:material-icons-extended`, sin versión propia (cubierta por el BOM) — corrige un supuesto equivocado sobre `material-icons-core`

**Contexto:** el botón "Tomar foto" necesita un ícono de cámara
reconocible junto al texto (CLAUDE.md sección 6: claridad para usuaria no
técnica; el humano aprobó explícitamente "ícono con texto al lado, no
solo el ícono").

**Primer intento, revertido:** agregar `material-icons-core` asumiendo
que `Icons.Default.PhotoCamera` estaba ahí. Era un supuesto sin
verificar (intenté confirmarlo buscando el archivo fuente en el repo de
`androidx` por HTTP y no lo encontré, pero seguí adelante igual en vez de
tratar eso como "no verificado"). `./gradlew assembleDebug` lo confirmó
mal con un error real y específico:
`Unresolved reference 'PhotoCamera'` en `CameraCaptureView.kt` — el ícono
no existe en el set `core`, solo en `extended`.

**Decisión final:** `androidx.compose.material:material-icons-extended`,
**sin versión propia** (cubierta por `compose-bom`, ya presente,
`2026.02.01`, como toda lib de Compose del proyecto). Confirmado con
`assembleDebug` en verde después del cambio: `PhotoCamera` resuelve
desde ahí.

**Por qué:** es el único módulo que trae `PhotoCamera` (y en general,
cualquier ícono fuera del subconjunto reducido de `core`) publicado por
Google — no hay una alternativa "liviana" que lo incluya sola. El
paquete es más pesado que `core`, pero es lo que hace falta para el
ícono que el humano pidió explícitamente.

**Descartado:**
- `material-icons-core` (revertido: no trae `PhotoCamera`, ver arriba).
- Dibujar un ícono vectorial propio para evitar el paquete "extended":
  desproporcionado para un solo ícono estándar de Material.

**Consecuencia:** dependencia más pesada que la propuesta original, pero
sin versión propia que mantener (BOM). Anotado como el mismo tipo de
error que D-018 (Coil): verificar contra la fuente real antes de asumir
dónde vive algo, no alcanza con que "suene razonable".

---

## D-021 — Sin selector de categoría en el alta rápida (Fase 03)

**Contexto:** `FASES.md` Fase 03 dice que "categoría" es uno de los
campos opcionales del formulario. Implementar un selector de verdad
necesita una lista de las categorías existentes, que hoy solo se puede
leer desde `CategoryDao` (`data/local`, Fase 01). El humano ya había
rechazado explícitamente el mismo patrón para `default_markup_bp`/
`price_rounding_step_cents` (inyectar un DAO directo en el ViewModel,
saltándose `data/repository`) y pidió `AppSettingRepository` en su
lugar — pero "Archivos permitidos" de Fase 03 no incluye ningún
`CategoryRepository`, y esta decisión no se preguntó explícitamente
antes de escribir código (a diferencia de las 6 preguntas que sí se
hicieron antes de arrancar).

**Decisión:** el formulario de alta rápida **no** tiene selector de
categoría. `categoryId` queda siempre `null` desde esta pantalla —
se asigna después, al editar la pieza (Fase 04), igual que `supplier`.

**Por qué:** las dos alternativas eran peores: (a) repetir el mismo
atajo que el humano ya corrigió una vez en esta misma fase (DAO directo
en el ViewModel) sienta el precedente que se quería evitar, con el
agravante de que ahora sé que está mal; (b) agregar un
`CategoryRepository` nuevo fuera de "Archivos permitidos" es tocar un
archivo no autorizado sin pausar a preguntar, la falta explícita que
`CLAUDE.md` sección 7 prohíbe. Omitir el selector es la única opción
que no repite un error ya corregido ni se salta la regla de archivos
permitidos, y es coherente con CLAUDE.md sección 1: "Todo lo demás es
opcional y se edita después" — categoría encaja ahí tan bien como
`supplier`, que ya estaba aprobado fuera del formulario.

**Descartado:**
- `CategoryDao` inyectado directo en el ViewModel (mismo patrón ya
  rechazado para `AppSettingDao`).
- Un `CategoryRepository` nuevo, fuera de "Archivos permitidos".

**Consecuencia:** a diferencia de `supplier` (que el humano sí revisó y
aprobó explícitamente como pregunta 3), esta la tomé yo solo durante la
implementación, no antes. La marco así en `ESTADO.md` para que quede
clara la diferencia, y queda pendiente de tu confirmación igual que
`supplier` lo estuvo.

---

## D-022 — La Fase 04 original (inventario + compras) se parte en Fase 04 (Inventario) y Fase 05 (Compras)

**Contexto:** `FASES.md` tenía una sola "Fase 04 — Inventario, edición y
compras" que mezclaba dos bloques: ver/buscar/editar el inventario
existente (`product`, `price_history`, `category`) y registrar compras a
mayorista con líneas y prorrateo (`purchase`, `purchase_item`). Al
planear esta fase (`ESTADO.md`, "Fase 04 — Plan"), el humano pidió una
opinión honesta sobre si eran una fase o dos.

**Decisión:** partirla en dos fases separadas, cada una con su propio
commit, tag y "Archivos permitidos":
- **Fase 04 — Inventario y edición**: listado, búsqueda, filtro por
  categoría, detalle/edición, `price_history`, archivado, selector de
  categoría (también en la pantalla de alta rápida de Fase 03).
- **Fase 05 — Compras a mayorista**: registro de compra con líneas,
  prorrateo de transporte local. Sube a `[TESTS OBLIGATORIOS]` a nivel
  de fase completa (antes era una etiqueta inline solo en el criterio
  del prorrateo).

Todas las fases de la 05 en adelante corren un número: la Venta de
contado pasa de Fase 05 a **Fase 06** (sigue siendo el fin del MVP;
`MVP = fases 00 a 06` en vez de `00 a 05`), Clientes de Fase 06 a
**Fase 07**, y así sucesivamente hasta Pulido, que pasa de Fase 12 a
**Fase 13**. Revisado y corregido cualquier número de fase mencionado en
`ESQUEMA.md` (las notas de "DAO y UI en Fase X" de `purchase`,
`purchase_item`, `customer`, `payment`, y la mención de "no hay usuarios
instalados hasta después de la Fase 05") y en `DECISIONES.md` (nota
agregada a D-011, sin editar su texto original — ver esa nota). `CLAUDE.md`
no tenía ninguna mención a un número de fase específico; no necesitó
cambios.

**Por qué:** tres razones concretas, no una preferencia de estilo:
1. Tocan tablas y flujos de negocio distintos, sin superposición real:
   inventario edita algo que ya existe; compras crea una transacción
   nueva con líneas, una entidad de negocio distinta.
2. "Compras" es estructuralmente un anticipo del patrón de "Venta"
   (elegís productos existentes, cantidad y costo por línea, un
   prorrateo, confirmás una transacción) — no el patrón de "listado +
   editar un registro" de Inventario. Mezclarlas combina dos
   arquitecturas de pantalla distintas bajo un solo nombre de fase.
3. **La razón que más pesó:** el único criterio `[TESTS OBLIGATORIOS]`
   inline de toda la Fase 04 original estaba en el prorrateo (criterio
   2), no en el de `price_history` (criterio 3, sin la etiqueta). Eso ya
   era una señal escrita en el propio `FASES.md` de que el bloque de
   compras pesaba como una fase aparte, del mismo calibre que otras
   fases con el tag completo (01, 02) — antes de que nadie lo señalara
   explícitamente.

Un cuarto punto, práctico: "Archivos permitidos" de la Fase 04 combinada
ya era la lista más ancha de todo `FASES.md`, justo en contra de la razón
del ritual de `CLAUDE.md` sección 7 (auditar leyendo solo `ESTADO.md` y
el diff del tag; una lista de archivos tan ancha ya era el síntoma de que
había demasiado adentro de una sola fase).

**Descartado:** mantenerla combinada; partirla sin renumerar (llamar a
la segunda mitad "Fase 04b" o "Fase 04.1") — se descartó por romper el
único patrón de nombres de todo el proyecto (`fase-NN-ok`, entero
secuencial, sin excepciones) para ahorrarse una edición de texto que es
un costo de una sola vez.

**Consecuencia:** `fase/04-inventory` (rama de la Fase 04 ya renombrada)
solo implementa Inventario. Compras queda como Fase 05, futura, con una
pregunta de diseño explícitamente sin resolver (qué efecto tiene una
compra sobre `product.cost_cents`/`stock_qty` — ver `ESTADO.md`, no se
decide en esta entrada) que hay que cerrar antes de que esa fase
arranque.

---

## D-023 — Navigation Compose `2.10.1`: primera ampliación del stack fijo de CLAUDE.md sección 2

**Contexto:** hasta Fase 03, la app tiene una sola pantalla
(`MainActivity` muestra `AddProductRoute` directo). Fase 04 necesita
moverse entre Listado ↔ Detalle/edición ↔ Alta rápida, y las fases
siguientes van a seguir sumando pantallas (Compras, Venta, Clientes...).
`CLAUDE.md` sección 2 fija el stack del proyecto y **no** incluye
ninguna librería de navegación — es la primera vez que una fase necesita
agregar algo a esa tabla, no solo pinear versiones de lo que ya está.

**Decisión:** agregar `androidx.navigation:navigation-compose:2.10.1`.
Verificado que es la última versión estable
(`dl.google.com/android/maven2/androidx/navigation/navigation-compose/maven-metadata.xml`,
`2.10.1` es la más nueva antes de ninguna prerelease pendiente) y,
aplicando la regla nueva de CLAUDE.md §2.1 (D-018 fue la lección),
verificado que **no** exige un Kotlin más nuevo que la línea base: su
POM declara `kotlin-stdlib` `2.1.20` (`≤ 2.2.10` del proyecto) —
compatible, sin necesitar bajar de versión como pasó con Coil.

**Por qué esta y no la alternativa de un `when` sobre estado en
`MainActivity`:** un `when (currentScreen) { ... }` a mano, con un
`sealed class`/enum de "pantalla actual" y una pila manual de
"pantallas anteriores" para el botón atrás, funciona mientras hay 2 o 3
pantallas — pero la app va a seguir sumando pantallas en casi todas las
fases que quedan (05 Compras, 06 Venta, 07 Clientes, 09 Catálogo, 10
Códigos de barras...). Un manejo a mano termina reinventando, peor y sin
tests, exactamente lo que ya resuelve una librería madura: pila de
retroceso, restauración de estado ante rotación/proceso destruido,
argumentos tipados entre pantallas, animaciones de transición. La
alternativa manual no es "evitar una dependencia", es escribir una
versión propia y peor de una, con el costo de mantenerla creciendo en
cada fase nueva en vez de una sola vez ahora.

**Descartado:**
- `when` sobre estado manual en `MainActivity` (razón arriba).
- Versiones de `navigation-compose` anteriores a `2.10.1`: no hacía
  falta bajar, `2.10.1` ya es compatible con la línea base.

**Consecuencia:** primera entrada en la tabla de stack fijo de
`CLAUDE.md` sección 2 que no estaba ahí desde el día 1. No se edita esa
tabla (son las tecnologías elegidas por el humano antes de empezar);
esta decisión queda como el registro de la ampliación. Cada pantalla
nueva de acá en adelante se agrega como un destino más del mismo
`NavHost`, no como su propio mecanismo de navegación.

**Adenda — `hilt-navigation-compose` `1.3.0`:** Navigation Compose con
varios destinos necesita `hiltViewModel()` para que cada pantalla del
`NavHost` tenga su propio ViewModel inyectado por Hilt y correctamente
acotado a su entrada en la pila de navegación (a diferencia de Fase 03,
de una sola pantalla, donde `by viewModels()` en la Activity alcanzaba).
Sin esto, todos los ViewModels quedarían acotados a `MainActivity`
entera y no se reiniciarían al navegar — ej. `AddProductViewModel`
conservaría el formulario a medio llenar de la visita anterior. Es la
librería estándar que conecta Hilt con Navigation Compose, no una
ampliación de stack aparte, así que se documenta acá mismo, no en una
decisión nueva. Versión: `1.3.0` (no la última, `1.4.0`) — verificado el
POM de cada una: `1.4.0` pide `kotlin-stdlib` `2.2.20` (más nuevo que el
`2.2.10` pineado), `1.3.0` pide `2.0.21` (compatible). Misma regla nueva
de CLAUDE.md §2.1 aplicada de nuevo.

---

## D-024 — Pantalla de inicio provisoria (Fase 04): desviación temporal de CLAUDE.md sección 6, con vencimiento

**Contexto:** CLAUDE.md sección 6 exige que la pantalla de inicio tenga
"dos acciones grandes y obvias: Vender y Agregar pieza". "Vender" no
existe todavía — llega en Fase 06 (Venta de contado, con la
renumeración de D-022). Un botón "Vender" que no hace nada sería
exactamente el stub que miente que CLAUDE.md sección 5 prohíbe
("Nada de `TODO()` ni stubs vacíos que compilen y mientan").

**Decisión:** Fase 04 muestra una pantalla de inicio provisoria con las
**dos acciones que sí existen hoy**: "Agregar pieza" (Fase 03) e
"Inventario" (Fase 04). Esto es una **desviación temporal explícita**
de CLAUDE.md sección 6, no el diseño final.

**Vencimiento explícito:** esta desviación se corrige en **Fase 06 —
Venta de contado**, que es la fase que hace que "Vender" exista de
verdad. Esa fase tiene que reemplazar la pantalla de inicio provisoria
por la definitiva (Vender + Agregar pieza), no dejarla como quedó acá.
Anotado también en `FASES.md` (entregable de Fase 04) para que quien
abra Fase 06 lo vea sin tener que buscar en `DECISIONES.md`.

**Por qué:** el humano confirmó que el razonamiento (no poner un botón
muerto) es correcto, pero pidió explícitamente que la desviación de una
regla de `CLAUDE.md` quede registrada como tal, con fecha/condición de
vencimiento — no "pasar de largo" un incumplimiento aunque sea
temporal y bien intencionado.

**Descartado:**
- Esperar a Fase 06 para tener cualquier pantalla de inicio (dejaría
  Fase 04 y 05 sin forma de navegar a Inventario más que por código).
- Poner un botón "Vender" deshabilitado o que muestre "próximamente":
  sigue siendo UI que promete algo que no existe todavía; CLAUDE.md
  sección 6 pide texto claro sobre qué va a pasar, y "va a pasar" que
  no pasa nada no lo es.

**Consecuencia:** criterio de aceptación para Fase 06 (a agregar en
`FASES.md` cuando se abra esa fase, no ahora): la pantalla de inicio
final reemplaza a la provisoria, con "Vender" funcional.

---

## D-025 — Descartada la hipótesis de versión: el bloqueo de KSP/Hilt en Fase 04 era un comentario KDoc mal escrito, no una incompatibilidad de versiones

**Contexto:** Fase 04 quedó bloqueada (`ESTADO.md`, "Fase 04 — Bloqueo: KSP
no resuelve `ProductRepository` para Hilt") con `[ksp] InjectProcessingStep
was unable to process ... because 'ProductRepository' could not be
resolved`, afectando a los cinco consumidores de `ProductRepository`
(`AddProductUseCase`, `ArchiveProductUseCase`, `EditProductUseCase`,
`ProductEditViewModel`, `ProductListViewModel`). El diagnóstico de la
sesión anterior descartó caché, ciclos de dependencia, `PriceHistoryDao`,
contenido de UI, modo incremental de KSP y memoria de la JVM, y bisectó el
síntoma a "agregar cualquier archivo nuevo al módulo, sin relación con
`ProductRepository`, hace que el procesador deje de resolverlo pasado un
umbral" — concluyendo que era un límite de KSP.

**Hipótesis inicial de esta sesión (con evidencia real, pero equivocada):**
KSP `2.3.12` (publicado 2026-09-09, verificado vía la API de releases de
GitHub) y Hilt/Dagger `2.60.1` (publicado 2026-07-06, coincide con el
`lastUpdated` que ya tenía D-009) tienen dos meses de brecha. Se probó
bajar KSP a `2.3.9` (última `2.3.x` publicada antes de julio de 2026) —
**el error se reprodujo idéntico**. Se investigó más a fondo y se encontró
que `dagger-compiler:2.60.1` declara en su propio POM una dependencia dura
a `com.google.devtools.ksp:symbol-processing-api:2.3.7` (no `2.3.9`) — se
probó bajar KSP exactamente a esa versión, la que Dagger declara haber
usado. **El error se reprodujo idéntico otra vez.** Se probó además sacar
el compilador de Room del classpath de KSP por completo (para descartar
una interacción Room+Hilt) y quitar `AppDatabase` del constructor de
`ProductRepository` (la única diferencia real con `CategoryRepository`,
que sí resolvía) — **el error se reprodujo idéntico las dos veces**. Tres
versiones de KSP distintas y dos cambios estructurales al código,
reproduciendo el mismo error letra por letra, es evidencia empírica sólida
de que la causa no era ninguna versión.

**Causa real, encontrada por bisección directa del contenido de
`ProductRepository.kt`:** reducir la clase a un solo método (la forma
exacta de Fase 03) compiló. Se fueron agregando los métodos de vuelta uno
por uno (`archive`, `getDetail`, `observeFiltered`, `update`) y todos
compilaron — hasta restaurar el archivo original completo, que volvió a
fallar. La diferencia real resultó ser los dos comentarios KDoc
(`/** ... */`) del archivo, no su código. Aislado a uno solo: el KDoc de
clase, que contiene el texto `` `domain/usecase/*.kt` `` — esa `/` seguida
de `*` es, para el lexer de Kotlin, la apertura de un **comentario
anidado** (Kotlin, a diferencia de Java/C, permite anidar `/* */`). El
`/**` real de la línea 15 abre profundidad 1; el `/*` de `usecase/*.kt` en
la línea 18 la sube a profundidad 2; el `*/` de cierre de la línea 20 solo
la baja a profundidad 1 — el comentario nunca llega a profundidad 0 en ese
punto y sigue "abierto", tragándose la declaración real de
`class ProductRepository @Inject constructor(...) { ... }` que viene
después. Desde la perspectiva del frontend que usa KSP, esa clase
simplemente no existe como declaración — de ahí el mensaje literal
"`ProductRepository` could not be resolved", que no es una mentira del
mensaje de error: es exacto, solo que la razón no tiene nada que ver con
classpath ni con versiones. Confirmado contando delimitadores
(`grep -o` de `/\*` y `\*/` en el archivo: 2 aperturas, 1 cierre, con el
KDoc de clase presente y el del método `update` ausente) y con el bisect
inverso: agregar de vuelta el KDoc de clase solo (sin el de `update`)
alcanza para reproducir la falla por sí solo.

**Por qué el bisect de la sesión anterior no encontró esto:** el patrón
que observaron ("cualquier archivo nuevo lo rompe") era real pero mal
interpretado — no habían identificado que el archivo que finalmente
"rompía todo" (`ProductRepository.kt` con sus dos KDoc completos) ya
llevaba el bug desde antes; lo que variaba entre sus pruebas no era la
cantidad de archivos sino, coincidentemente, si `ProductRepository.kt`
en su forma completa estaba presente o no en cada corrida.

**Decisión:**
- Reescribir el comentario roto: `` `domain/usecase/*.kt` `` →
  `` `domain/usecase` `` (se pierde el sufijo de glob, el significado
  para quien lee el comentario no cambia).
- Revertir KSP a `2.3.12` (la versión que ya tenía el proyecto, D-007) —
  nunca fue el problema, no hay ninguna razón para quedarse en una versión
  más vieja.
- De paso, arreglados dos usos reales de la API de Material3 en
  `CategoryDropdown.kt` y `ProductListScreen.kt` que aparecieron recién al
  llegar por primera vez a `compileDebugKotlin` (nunca se había llegado
  tan lejos con el build roto en `kspDebugKotlin`): `ExposedDropdownMenu`
  es una función miembro de `ExposedDropdownMenuBoxScope` (se llama sin
  calificar, por receptor implícito, dentro del lambda de
  `ExposedDropdownMenuBox`), no un miembro de `ExposedDropdownMenuDefaults`
  como estaba escrito. No es parte de este bloqueo de KSP/Hilt; es un bug
  de código distinto que solo se hizo visible al destrabar el anterior.

**Descartado:**
- Las dos versiones de KSP intermedias (`2.3.9` y `2.3.7`) probadas en el
  camino — ninguna cambiaba nada, quedó demostrado con evidencia directa.
- La teoría de "límite de archivos de KSP" de la sesión anterior — el
  archivo importaba por su contenido roto, no por ser "uno más".
- Mantener KSP en una versión distinta a `2.3.12` "por las dudas" — no hay
  ninguna razón real para eso ahora que se conoce la causa.

**Consecuencia:** agregada una regla nueva a `CLAUDE.md` sección 5:
prohibido escribir la secuencia literal `/*` dentro del texto de un
comentario de bloque, con este incidente como ejemplo. La regla que se
había agregado primero a la sección 2.1 (sobre contemporaneidad de
KSP/Hilt) se retiró por completo: no correspondía a la causa real y
dejarla habría quedado como una lección falsa. Si en el futuro aparece
otro "podría ser un límite/versión de la herramienta" sin una causa clara
en el propio mensaje de error, el orden correcto es primero descartar algo
tan simple como un comentario roto (`grep -c '/\*'` vs `grep -c '\*/'` en
el archivo sospechoso) antes de gastar intentos cambiando versiones.

---

## D-026 — CLAUDE.md sección 6: de 3 a 4 campos obligatorios en el alta rápida (categoría se vuelve obligatoria)

**Contexto:** la prueba real con la usuaria (2026-09-17) mostró que los
campos opcionales de la Fase 03 quedaban escondidos en un bloque
colapsable ("Más detalles") y no los descubría sola. Al hacerlos todos
visibles sin necesidad de expandir nada, categoría queda en una posición
distinta a la que tenía: dejarla visible pero opcional no resuelve el
problema de fondo que motivó D-021 (Fase 03 la dejó afuera del alta por
falta de `CategoryRepository` en ese momento, no porque no importara) —
sin categoría, la búsqueda/filtro de Fase 04 y el catálogo agrupado de
Fase 10 sirven a medias, y en la práctica una usuaria que nunca está
obligada a completarla no lo va a hacer después en edición.

**Decisión:** CLAUDE.md sección 6 pasa de "máximo 3 taps" a "máximo 4
taps": foto, costo, precio y categoría son los cuatro campos obligatorios
del alta rápida. Categoría se selecciona con chips de un solo toque
(`CategoryChipRow`, sin preselección y sin opción "sin categoría"), no
con el `CategoryDropdown` compartido que sigue usando la pantalla de
edición — para no perder el espíritu de "un toque por campo obligatorio"
de la regla original.

**Por qué no una excepción tácita:** CLAUDE.md sección 10 exige no
adivinar y dejar las dudas escritas. Cambiar el comportamiento real de la
pantalla sin actualizar la regla que lo describe habría dejado
documentación y código diciendo cosas distintas — justo lo que el ritual
de auditoría de la sección 7 (leer `ESTADO.md` + el diff del tag) no
puede tolerar.

**Descartado:**
- Dejar categoría opcional y visible: no resuelve el problema real
  (catálogo/filtro sin categoría), solo lo pospone.
- Mantener el bloque colapsable solo para categoría: la prueba con la
  usuaria real ya mostró que lo colapsado no se descubre.
- Un `CategoryDropdown` en vez de chips: exige dos toques (abrir el menú,
  elegir la opción) para un campo ahora obligatorio, más lento que un
  chip de un solo toque para el caso más común (5 categorías fijas).

**Consecuencia:** `AddProductUiState.canSave` suma `categoryId != null`.
El criterio de tiempo de la Fase 03 (menos de 20 segundos, criterio 4)
se vuelve a medir después de este cambio — ver `ESTADO.md`, "Fix de
usabilidad de alta rápida", remedición de tiempo.

---

<!--
## D-00X — Título

**Contexto:**
**Decisión:**
**Por qué:**
**Descartado:**
**Consecuencia:**
-->

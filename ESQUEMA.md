# ESQUEMA.md — Modelo de datos

Este es el modelo completo del proyecto, definido antes de escribir código.
**Claude Code no puede crear tablas ni columnas que no estén aquí.**
Si una fase parece necesitar algo que falta, se propone en `ESTADO.md` y se espera
aprobación humana antes de tocar el esquema.

Razón de definirlo todo desde el día 1: un agente que improvisa el esquema fase por
fase termina con tres formas distintas de guardar lo mismo y migraciones imposibles
de auditar.

Convenciones:
- Todo monto es `Long` en **centavos de quetzal**.
- Toda fecha es `Long` epoch millis UTC.
- `id` es `Long` autogenerado.
- Nombres de tabla y columna en `snake_case`, entidades Kotlin en `PascalCase`.

---

## Versión de base de datos: 1 (fase 01)

**Todas las tablas de este documento se crean en esta versión 1, en la Fase 01**
(ver D-011 en `DECISIONES.md`). No hay usuarios instalados hasta después de la
Fase 06 (Venta de contado, fin del MVP — ver D-022 sobre por qué el número
cambió de la Fase 05 original), así que repartir la creación de tablas en
migraciones intermedias es ceremonia sin beneficio. Las fases posteriores agregan DAOs, repositorios y
pantallas contra tablas que ya existen; no agregan columnas ni tablas nuevas
salvo que este documento se actualice explícitamente. A partir de que exista
una instalación real, un cambio de esquema sí incrementa la versión y agrega
una `Migration` nueva con su test, como dice la regla de abajo.

Cada cambio posterior incrementa la versión y agrega una `Migration` nueva con su test.

---

## Versión de base de datos: 2 (fase 05) — primera migración real del proyecto

Agrega dos columnas a `price_history` (`purchase_id`, `cycle_start`, ver
esa tabla más abajo) y la clave `min_margin_bp` a `app_setting`. A
diferencia de la versión 1 (D-011: todas las tablas creadas de una sola
vez porque no había instalaciones reales), esta sí es una migración de
verdad — es la primera vez que el esquema cambia con la posibilidad de
que ya exista una base instalada, y es intencional adelantarla a Fase 05
mientras la usuaria todavía no tiene inventario real, para no tener que
migrar datos reales la primera vez que se toque el esquema.

`Migration(1, 2)` recrea `price_history` completa (tabla nueva, copiar
filas, borrar la vieja, renombrar) en vez de `ALTER TABLE ADD COLUMN`
para la columna con `FOREIGN KEY` (`purchase_id`) — `ALTER TABLE` de
SQLite no agrega la restricción de forma confiable ni recrea los
índices; el patrón de recrear la tabla es el que documenta Room para
agregar una columna con FK. Las filas existentes quedan con
`purchase_id = NULL` y `cycle_start = 0` (ninguna fila vieja pudo venir
de una compra, porque `purchase`/`purchase_item` no tenían DAO hasta
esta misma fase). `app/schemas/gt.marcos.joyeria.data.local.AppDatabase/2.json`
se commitea junto con `1.json`, sin tocarlo — ver D-031/D-032/D-033 en
`DECISIONES.md`.

---

## `category` — Categoría de producto

| Columna | Tipo | Notas |
|---|---|---|
| `id` | Long PK | |
| `name` | String | "Anillos", "Cadenas", "Aretes", "Pulseras", "Juegos". **Sin restricción de unicidad en la base** — ver advertencia abajo |
| `archived` | Boolean | default `false` |

Semilla inicial: las cinco categorías de arriba, creadas en la primera apertura.

Agregada en Fase 09 (categorías propias, antes solo tenía DAO/UI de lectura
desde Fase 04): alta, renombre y archivado desde una pantalla de
administración. Nunca se borra (D-006); archivar no destruye la
clasificación de las piezas que ya la tenían asignada (D-057, Opción A).

### ⚠️ `name` no tiene índice único — la unicidad la garantiza `CategoryRepository`, no la base

**Decisión deliberada, no un olvido (D-057):** dos categorías **activas**
nunca deberían llamarse igual (confunde el desplegable), pero una
categoría **archivada** sí puede compartir nombre con una activa — no
compiten por el mismo lugar en ningún selector a la vez, y por eso un
índice único bloquearía también ese caso válido. La regla real es "única
entre las activas", que una restricción de la base no puede expresar sin
bloquear de más.

`CategoryRepository.add()`/`rename()` rechazan el duplicado (sin
distinguir mayúsculas ni espacios al borde) **antes** de escribir. Es
exactamente el mismo patrón que D-050 (una consulta que expresa toda su
condición en vez de confiar en un invariante de otro archivo): acá el
invariante ("nunca hay dos activas con el mismo nombre") lo mantiene un
solo repositorio, en código Kotlin, no una restricción de esquema.

**Consecuencia para cualquier escritura futura que no pase por
`CategoryRepository`:** puede crear duplicados sin que nada lo impida.
La semilla (`AppDatabase.SeedCallback`, `execSQL` directo) no corre este
chequeo, pero inserta nombres fijos y distintos entre sí, así que no
colisiona. **Una importación de respaldo (Fase 14, hecha) sí valida
los nombres de categoría que traiga contra esta misma regla** (`BackupImporter.checkCategories`, con la misma
consulta `countActiveWithName`; dos activas iguales → se rechaza todo el archivo, D-061) —
si importa pasando por debajo de `CategoryRepository` (`execSQL`/`insert`
directo a la tabla, por velocidad o por restaurar ids exactos), nada en
la base la va a frenar de crear dos "Pulseras" activas.

---

## `product` — Pieza de joyería

| Columna | Tipo | Notas |
|---|---|---|
| `id` | Long PK | |
| `uid` | String | **único**, formato `XP-000001`, secuencial, autogenerado |
| `name` | String | lo que ella escribe, ej. "Anillo corazón dorado" |
| `category_id` | Long? | FK → `category.id`, `ON DELETE SET NULL` |
| `cost_cents` | Long | costo unitario real de compra |
| `sale_price_cents` | Long | precio al que ella vende |
| `stock_qty` | Int | nunca negativo (validado en `domain`) |
| `photo_path` | String? | ruta local en almacenamiento interno de la app |
| `supplier` | String? | tienda/mayorista local donde la compró |
| `notes` | String? | |
| `created_at` | Long | |
| `updated_at` | Long | |
| `archived` | Boolean | default `false` |

Índices: `uid` UNIQUE, `category_id`, `archived`.

Notas:
- El `uid` se genera con un contador persistido en `app_setting`, no con `MAX(id)+1`.
  Razón: si se archiva o se reimporta, `MAX(id)` puede repetir códigos ya impresos
  en etiquetas físicas. Un código impreso nunca se debe reutilizar.
- `cost_cents` y `sale_price_cents` son los valores **actuales**. El histórico está
  en `price_history` y en los snapshots de `sale_item`.

---

## `price_history` — Historial de precios (tabla en Fase 01; DAO y UI en Fase 04; `purchase_id`/`cycle_start` en Fase 05, versión 2)

| Columna | Tipo | Notas |
|---|---|---|
| `id` | Long PK | |
| `product_id` | Long | FK → `product.id`, `ON DELETE CASCADE` |
| `cost_cents` | Long | |
| `sale_price_cents` | Long | |
| `changed_at` | Long | |
| `purchase_id` | Long? | FK → `purchase.id`, `ON DELETE SET NULL`. `null` cuando la fila viene de una edición manual (Fase 04), no de una compra. Agregada en versión 2 (Fase 05) |
| `cycle_start` | Boolean | default `false`. `true` únicamente cuando la fila se escribió porque `stock_qty` era `0` justo antes de esa compra (el reemplazo limpio de D-029) — es el inicio de un ciclo de compra nuevo. Se guarda en el momento en que se procesa la compra, no se reconstruye después. Agregada en versión 2 (Fase 05) |

Se inserta una fila cada vez que cambia costo o precio. Sirve para responder
"¿por qué ganaba más antes con este anillo?", y con `purchase_id`/`cycle_start`
también para reconstruir el reporte de "ganancia por ciclo de compra"
(Fase 11, futura) sin tener que inferir después cuándo empezó cada ciclo —
ver D-033 en `DECISIONES.md` sobre por qué esto no se pudo diferir a lo que entonces era la Fase 09 (hoy Fase 11, renumerada en D-049).

Índice nuevo: `purchase_id`.

---

## `purchase` — Compra a un mayorista local (tabla en Fase 01; DAO y UI en Fase 05)

Ella compra lotes en tiendas del país. **No hay importación, ni aduana, ni flete internacional.**
El único gasto extra posible es transporte local (pasaje, parqueo), y es opcional.

| Columna | Tipo | Notas |
|---|---|---|
| `id` | Long PK | |
| `purchased_at` | Long | |
| `supplier` | String? | |
| `extra_cost_cents` | Long | default `0`. Transporte local, opcional |
| `notes` | String? | |

---

## `purchase_item` — Línea de compra (tabla en Fase 01; DAO y UI en Fase 05)

| Columna | Tipo | Notas |
|---|---|---|
| `id` | Long PK | |
| `purchase_id` | Long | FK → `purchase.id`, `ON DELETE CASCADE` |
| `product_id` | Long | FK → `product.id` |
| `qty` | Int | |
| `unit_cost_cents` | Long | lo que pagó por unidad |
| `allocated_extra_cents` | Long | parte del `extra_cost_cents` que le toca a esta línea |

Regla de prorrateo (solo si `extra_cost_cents > 0`):
por defecto **proporcional al valor** de la línea sobre el total de la compra.
El residuo por redondeo se asigna a la línea de mayor valor, para que la suma de
`allocated_extra_cents` sea exactamente igual a `extra_cost_cents`. Esto lleva test.

El costo real unitario es `unit_cost_cents + (allocated_extra_cents / qty)`.
Si `extra_cost_cents = 0`, que es el caso normal, el costo real es simplemente
`unit_cost_cents`.

---

## `customer` — Cliente (tabla en Fase 01; DAO y UI en Fase 07)

| Columna | Tipo | Notas |
|---|---|---|
| `id` | Long PK | |
| `name` | String | |
| `phone` | String? | usado para el enlace de WhatsApp |
| `notes` | String? | ej. "compañera de trabajo, turno de la tarde" |
| `created_at` | Long | |
| `archived` | Boolean | default `false` |

---

## `sale` — Venta

| Columna | Tipo | Notas |
|---|---|---|
| `id` | Long PK | |
| `sold_at` | Long | |
| `customer_id` | Long? | tres estados, ver abajo |
| `type` | String | `CASH` \| `CREDIT` |
| `status` | String | `PAID` \| `PENDING` \| `CANCELLED` |
| `total_cents` | Long | suma de las líneas, calculada al guardar |
| `total_cost_cents` | Long | **snapshot** del costo total, para la ganancia histórica |
| `discount_cents` | Long | default `0`. **Columna de legado — leer la advertencia de abajo antes de tocarla.** |
| `notes` | String? | |
| `cancelled_at` | Long? | |
| `cancel_reason` | String? | |

Índices: `sold_at`, `customer_id`, `status`.

Una venta `CASH` nace `PAID`. Una venta `CREDIT` nace `PENDING` y pasa a `PAID`
cuando la suma de abonos alcanza `total_cents - discount_cents`.

### `customer_id` — tres estados, y un `null` que significa dos cosas distintas

| Estado | Significa | Desde |
|---|---|---|
| `NOT NULL` + `type = 'CREDIT'` | La clienta que **debe** esta venta. Es la que alimenta "¿Quién me debe?" y el estado de cuenta | Fase 07 |
| `NOT NULL` + `type = 'CASH'` | Clienta a la que se le vendió de contado. Queda asociada a ella y aparece en su perfil, **pero no es una deuda** | Fase 08 |
| `NULL` | Venta sin clienta — **pero significa dos cosas distintas según la antigüedad de la fila** (ver abajo) | — |

**El `null` de una fila vieja y el de una nueva no son lo mismo:**

- **Fila escrita antes de Fase 08**, de contado: significa **"no se
  sabe"**. La app no guardaba a quién se le vendía, así que la venta
  *pudo* haber sido a una clienta conocida y el dato simplemente no se
  capturó.
- **Fila escrita desde Fase 08**, de contado: significa **"sin clienta, a
  propósito"**. Es el caso común — venderle a quien pasa por ahí — y la
  usuaria tuvo la opción de asociarla y no la usó.

**No hay forma de distinguir los dos casos mirando la fila.** La columna es
`null` en los dos y no existe ninguna marca de versión por fila. Por eso el
perfil de clienta **no** afirma nada sobre una venta puntual: deduce una
fecha de corte de toda la base
(`MIN(sold_at) WHERE type = 'CASH' AND customer_id IS NOT NULL`) y avisa,
por clienta, que su historial *puede* estar incompleto. El detalle completo
y el caso que tumbó la versión por clienta están en **D-054**.

**Consecuencia para cualquier consulta nueva:** `customer_id IS NOT NULL`
**no** quiere decir "venta a crédito" ni "deuda". Desde Fase 08 una venta
de contado también lo tiene. Toda consulta de deuda pide además
`type = 'CREDIT' AND status = 'PENDING'` — ver **D-050**, que es la regla
general de por qué esas condiciones se escriben aunque parezcan
redundantes.

---

### ⚠️ `discount_cents` es una columna de legado y SIGUE EN USO

El descuento general sobre la venta **se eliminó de la app** en la ronda de
corrección de uso real (2026-10-10, D-047): lo reemplazó el **precio editable
por línea**, porque un descuento solo puede restar y la usuaria a veces vende
**más caro** que el precio de lista. La columna **no se eliminó de la base**.

**Las ventas nuevas escriben `0`. Las ventas viejas conservan su valor real.**

**Prohibido sacar `discount_cents` de los cálculos "porque ya no se usa".**
Las fórmulas no cambian y siguen siendo exactamente estas:

```
gananciaDeVenta = (total_cents - discount_cents) - total_cost_cents
saldoDeVenta    = (total_cents - discount_cents) - Σ payment.amount_cents
```

Qué pasa si alguien las "simplifica" quitando el término:

- Toda venta vieja con descuento pasaría a mostrar **más ganancia de la que
  ella tuvo** — el historial se reescribiría solo, que es justo lo que
  `CLAUDE.md` 3.7 prohíbe.
- Peor: en una venta a crédito el saldo quedaría **mayor al que la clienta
  debe de verdad**. Eso no es un detalle de reporte, es un **error de cobro**
  contra una persona real.

Como las ventas nuevas escriben `0`, el término suma cero para ellas: dejarlo
no cuesta nada y sacarlo rompe el pasado. Por eso se queda.

---

## `sale_item` — Línea de venta (con snapshots)

| Columna | Tipo | Notas |
|---|---|---|
| `id` | Long PK | |
| `sale_id` | Long | FK → `sale.id`, `ON DELETE CASCADE` |
| `product_id` | Long? | FK → `product.id`, `ON DELETE SET NULL` |
| `product_uid_snapshot` | String | |
| `product_name_snapshot` | String | |
| `qty` | Int | |
| `unit_price_cents` | Long | **snapshot** del precio al momento de vender |
| `unit_cost_cents` | Long | **snapshot** del costo al momento de vender |

Los snapshots son obligatorios. El reporte de ganancia del mes pasado se calcula
únicamente con estos campos, nunca consultando `product` en vivo.

---

## `payment` — Abono (tabla en Fase 01; DAO y UI en Fase 07)

| Columna | Tipo | Notas |
|---|---|---|
| `id` | Long PK | |
| `sale_id` | Long | FK → `sale.id`, `ON DELETE CASCADE` |
| `paid_at` | Long | |
| `amount_cents` | Long | > 0 |
| `method` | String | `CASH` \| `TRANSFER` \| `OTHER` |
| `notes` | String? | |

Índice: `sale_id`.

Reglas:
- La suma de abonos nunca puede exceder el saldo de la venta. Validado en `domain`.
- El saldo pendiente es un cálculo derivado, **no se persiste**.
  Razón: un saldo guardado se desincroniza; uno calculado no puede mentir.

---

## `app_setting` — Configuración (clave/valor)

| Columna | Tipo | Notas |
|---|---|---|
| `key` | String PK | |
| `value` | String | |

Claves iniciales:

| Clave | Default | Significado |
|---|---|---|
| `default_markup_bp` | `10000` | recargo sobre costo para el precio sugerido, en puntos básicos (10000 = recargo del 100% = precio costo × 2) |
| `price_rounding_step_cents` | `500` | redondea el sugerido a múltiplos de Q5 |
| `low_stock_threshold` | `2` | alerta de bajo inventario |
| `stale_stock_days` | `90` | alerta de capital estancado |
| `next_product_uid_seq` | `1` | contador del `uid` |
| `owner_name` | `""` | para encabezar catálogos y estados de cuenta |
| `min_margin_bp` | `2500` | piso de margen sobre venta, en puntos básicos (D-031). Por debajo de esto, Fase 05 muestra una ADVERTENCIA al registrar una compra. `2500` = 25.00%: con `default_markup_bp = 10000` (recargo 100%, margen 50%), ganar menos de la mitad de lo normal. Agregada en versión 2 (Fase 05) |

**Clave que NO se siembra** (Fase 14, D-061): `last_backup_at` — milisegundos de la última vez que armó y
entregó un respaldo, o la fecha del respaldo que importó. **Ausente significa «nunca»**: por eso no hay migración
ni cambio de esquema. No viaja dentro del archivo de respaldo. La escribe `BackupStatusRepository` (con
`INSERT OR REPLACE`: `setValue` es un UPDATE y no haría nada si la fila no está).

**Los valores de `app_setting` se parsean siempre a `Long` o `Int`, nunca a
`Double`** (ver D-010 en `DECISIONES.md`). `value` se persiste como `String`
únicamente porque la tabla es genérica clave/valor; quien la lee siempre
convierte al tipo entero que corresponda según la clave.

---

## Cálculos derivados (viven en `domain`, nunca se persisten)

**Representación de porcentajes: puntos básicos en `Int`, nunca `Double`.**
1 punto básico = 0.01%, o sea `valor / 100` = el porcentaje con dos
decimales. Ejemplos: `6000` = `60.00%`, `15000` = `150.00%`. Es la misma
lógica que los centavos para dinero: un entero exacto en vez de un
flotante que no representa decimales de forma exacta. El formateo a texto
con el símbolo `%` y los decimales (dividir por 100 y mostrar) se hace en
`ui`, igual que `Money` — nunca en `domain` ni en `data`. Ver D-013 en
`DECISIONES.md`.

```
gananciaUnitaria   = sale_price_cents - cost_cents                    // Long, centavos
margenSobreVenta   = gananciaUnitaria * 10000 / sale_price_cents      // Int, puntos básicos
recargoSobreCosto  = gananciaUnitaria * 10000 / cost_cents            // Int, puntos básicos
valorInventario    = Σ (stock_qty * cost_cents) de productos no archivados
capitalRecuperado  = Σ total_cost_cents de toda venta con status != 'CANCELLED', de siempre   // Fase 11
gananciaDeVenta    = (total_cents - discount_cents) - total_cost_cents
saldoDeVenta       = (total_cents - discount_cents) - Σ payment.amount_cents
```

**`capitalRecuperado` (Fase 11, reportes):** el costo de todo lo que ya
salió del inventario y se convirtió en venta, sin acotar a ningún mes —
es la cifra que, junto a `valorInventario`, responde "cuánto de lo que
alguna vez compré ya volvió, contra cuánto sigue guardado en el
estante". Acotarlo a un mes respondería una pregunta distinta (ya cubierta
por `gananciaDeVenta` sumada sobre el mes) y rompería esa comparación.

`discount_cents` es una **columna de legado que sigue en uso**: las ventas
nuevas escriben `0`, las viejas conservan su descuento real, y el término
**no se puede quitar** de estas dos fórmulas sin reescribir el historial e
inflar los saldos de las clientas. Ver la advertencia completa en la tabla
`sale`, arriba.

Todos llevan test unitario, incluyendo los casos `cost_cents = 0` y
`sale_price_cents = 0` (las dos divisiones posibles): no se divide entre cero,
se devuelve un resultado explícito, no un crash ni un `NaN`.

**`valorInventario` no es exacto al centavo cuando hubo compras con
promedio ponderado (D-029, adenda de Fase 05):** el redondeo hacia
arriba del costo tras una compra (`weightedAverageCost`) multiplica el
centavo de más por todo el `stock_qty`, no solo por lo comprado, y el
sesgo se acumula compra tras compra sin autocorregirse. El error es
siempre hacia arriba (nunca subestima) y en la práctica son centavos,
pero cualquier reporte que muestre este cálculo (Fase 11, futura)
tiene que saber que no es un número exacto — ver D-029 en
`DECISIONES.md` para el ejemplo numérico y el test que lo mide.

**`default_markup_bp` unificado con `recargoSobreCosto` (D-014, reemplaza a D-010):**
`suggestedPrice(cost, markupBp, roundingStep) = cost + cost * markupBp / 10000`.
Con el default `10000`, el precio sugerido sigue siendo costo × 2 (mismo
comportamiento de siempre), pero ahora el número significa lo mismo que en
todos lados: un recargo del 100% sobre el costo, calculado exactamente
igual que `recargoSobreCosto` (`gananciaUnitaria * 10000 / cost_cents`). Ya
no hay dos significados distintos de "porcentaje" conviviendo en el
esquema.
# Política de permisos de Claude Code

Estado: **aplicada** el 2026-10-10. Documento de política permanente
(sección 7, regla 9 de `CLAUDE.md`): se consulta solo, no es bitácora de fase.

Si vas a "simplificar" las reglas de permisos de este proyecto, leé la
sección 3 antes. Está escrita exactamente para eso.

---

## 1. Objetivo

1. No aprobar manualmente cada `./gradlew` ni cada comando de lectura.
2. **Seguir aprobando manualmente cualquier cambio de dependencias o de
   versiones.** Es donde más caro salió el proyecto: ver D-018 (Coil `3.6.2`
   exigía un `kotlin-stdlib` más nuevo que el pineado y rompió la compilación
   entera) y D-025 (dos rondas de cambiar la versión de KSP para un error cuya
   causa real era un `/*` dentro de un KDoc) en `DECISIONES.md`.

Los dos objetivos están en tensión directa, y la sección 3 explica dónde.

---

## 2. Dónde vive y qué dice

Archivo: **`.claude/settings.local.json`**, no versionado.

`.gitignore` línea 20 ignora `.claude/` entero con el comentario *"Claude Code
session-local state (not project content)"*, y **se decidió no tocarlo**: la
política de permisos es configuración de la máquina del humano, no del
proyecto. Si mañana trabaja desde otra computadora, no debe venir arrastrada.
Lo que sí es del proyecto es *este* documento, que explica el razonamiento y sí
está commiteado.

```json
{
  "permissions": {
    "allow": [
      "Bash(./gradlew:*)",
      "Bash(git status:*)",
      "Bash(git diff:*)",
      "Bash(git log:*)",
      "Bash(git show:*)",
      "Bash(ls:*)",
      "Bash(cat:*)",
      "Bash(head:*)",
      "Bash(tail:*)",
      "Bash(wc:*)",
      "Bash(sed -n:*)",
      "Bash(grep:*)",
      "Bash(rg:*)",
      "Bash(find:*)"
    ],
    "ask": [
      "Edit(gradle/libs.versions.toml)",
      "Write(gradle/libs.versions.toml)",
      "Edit(app/build.gradle.kts)",
      "Write(app/build.gradle.kts)",
      "Edit(gradle/wrapper/gradle-wrapper.properties)",
      "Write(gradle/wrapper/gradle-wrapper.properties)",
      "Edit(build.gradle.kts)",
      "Write(build.gradle.kts)",
      "Edit(settings.gradle.kts)",
      "Write(settings.gradle.kts)",
      "Edit(gradle.properties)",
      "Write(gradle.properties)"
    ]
  }
}
```

Precedencia en Claude Code: **`deny` > `ask` > `allow`**. Se usa `ask` y no
`deny` porque el humano quiere *aprobar* esos cambios, no prohibirlos. Como
`ask` gana sobre `allow`, estas reglas sobreviven a cualquier allowlist más
amplia que se agregue en el futuro.

---

## 3. Por qué la lista de lectura está enumerada y no es `Bash(*)`

**Esta es la sección importante del documento.**

`Bash(*)` cumpliría el objetivo 1 con una línea en vez de trece, y es la
"simplificación" obvia que alguien va a proponer. **Destruye el objetivo 2.**

El motivo: las reglas `ask` sobre archivos **solo interceptan las herramientas
`Edit` y `Write`**. No interceptan una escritura hecha por shell. Con `Bash(*)`
activo, esto cambia una versión de dependencia sin que ninguna regla `ask` lo
vea y sin que aparezca un prompt:

```bash
sed -i 's/coil = "3.5.0"/coil = "3.6.2"/' gradle/libs.versions.toml
```

Hoy eso pide aprobación, pero **no por las reglas `ask`** — pide aprobación
porque la allowlist de bash es enumerada y no contiene ningún comando capaz de
escribir. Esa protección es indirecta y frágil: se cae sola el día que alguien
agregue `Bash(*)`, `Bash(sed:*)` o cualquier otro comodín de escritura.

Corolarios que se siguen de lo mismo:

- **`sed -n` y no `sed`.** `sed -i` escribe en el lugar. El prefijo `sed -n:*`
  matchea solo el modo de impresión, porque `:*` es **prefijo**: compara contra
  el principio del comando.
- **Fuera `git tag` y `git branch`**, aunque se usen para leer: `git tag -a` y
  `git branch -D` crean y borran referencias. El ritual de la sección 7 de
  `CLAUDE.md` depende de los tags, y los tags los pone el humano.
- **Fuera `git add`, `git commit`, `git merge`, `git push`** por lo mismo.

Regla de oro para quien edite `.claude/settings.local.json`: **antes de agregar
un patrón a `allow`, preguntate si el comando puede escribir un archivo. Si
puede, no va.**

---

## 4. Por qué el bloque `ask` cubre seis archivos y no dos

El pedido original nombró `gradle/libs.versions.toml` y `app/build.gradle.kts`.
La intención era "nadie cambia versiones sin que yo lo vea", y dos de las
versiones que la sección 2.1 de `CLAUDE.md` prohíbe tocar **no están en esos
dos archivos**:

| Archivo | Qué se protege ahí |
|---|---|
| `gradle/libs.versions.toml` | versiones de todas las dependencias, Kotlin, AGP, KSP |
| `app/build.gradle.kts` | `compileSdk`, `minSdk`, `targetSdk`, plugins, dependencias del módulo |
| `gradle/wrapper/gradle-wrapper.properties` | **la versión de Gradle** (hoy `9.5.0`) |
| `build.gradle.kts` (raíz) | plugins y versiones a nivel proyecto |
| `settings.gradle.kts` | repositorios y módulos |
| `gradle.properties` | flags de Gradle/Kotlin que cambian el comportamiento del build |

Las rutas son relativas al directorio que contiene el archivo de settings, o
sea la raíz del proyecto. Por eso `Edit(build.gradle.kts)` matchea solo el de
la raíz y `app/build.gradle.kts` va listado aparte: no es redundancia.

---

## 5. Shell: todo build va por `Bash`

Este entorno expone herramientas `Bash` y `PowerShell` **separadas**, y un
prefijo `Bash(...)` aplica únicamente a la primera. La sintaxis del prefijo
`PowerShell(...)` **no está verificada acá**.

Decisión: **todo build se corre con la herramienta `Bash` y `./gradlew`.** Está
escrito como regla 12 de la sección 7 de `CLAUDE.md` para que no dependa de la
costumbre — antes solo estaba implícito en que los comandos de `FASES.md` se
escriben como `./gradlew`.

Si algún día hace falta PowerShell de verdad: se verifica el prefijo con
`/permissions` **antes**, se registra en `DECISIONES.md`, y recién entonces se
usa.

---

## 6. Límites que las reglas no cubren

1. **`Bash(./gradlew:*)` auto-aprueba también tareas de Gradle que escriben**:
   `clean` borra `build/`, `--refresh-dependencies` toca caches. Es lo pedido
   explícitamente y es reversible, pero queda dicho.
2. **Comandos compuestos** (con `|`, `&&`, redirecciones) pueden seguir pidiendo
   aprobación aunque cada parte esté en la allowlist. No es un bug: es el
   comportamiento conservador esperado, y conviene dejarlo así.
3. **Nada de esto reemplaza la regla de los 2 intentos** de la sección 2.1 de
   `CLAUDE.md`: si un build falla dos veces, se para, se pega el error textual
   en `ESTADO.md` bajo Bloqueos y se espera al humano. Permisos automáticos no
   son licencia para iterar a ciegas.
4. Las reglas de `allow` **no aflojan ninguna prohibición de `CLAUDE.md`**. Que
   `./gradlew` corra sin prompt no autoriza `--offline`, ni bajar `minSdk`, ni
   borrar caches de Gradle, ni desactivar checks (sección 2.1).

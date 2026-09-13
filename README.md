# Joyería

App Android offline-first para que una vendedora de joyería Xuping lleve su
inventario, sus ventas y sus cobros a plazos, reemplazando un cuaderno físico.

Las reglas del proyecto están en `CLAUDE.md`, el plan de fases en `FASES.md`,
el modelo de datos en `ESQUEMA.md`, el historial de decisiones en
`DECISIONES.md` y la bitácora de avance en `ESTADO.md`. Empezá por ahí.

## Stack

Kotlin, Jetpack Compose + Material 3, Room, Hilt, Coroutines/Flow, CameraX,
ML Kit Barcode Scanning, ZXing. Sin backend, sin cuentas, 100% local.

## Compilar

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

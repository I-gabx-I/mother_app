package gt.marcos.joyeria.ui.permission

/** En qué punto está el permiso de cámara, para decidir qué se le muestra a ella. */
enum class CameraPermissionStep {
    /** Ya otorgado: se abre la cámara directo, sin explicación ni diálogo. */
    GRANTED,

    /** Todavía no se le pidió: primero la explicación en español; el diálogo del sistema recién al tocar el botón. */
    EXPLAIN,

    /** Dijo que no, pero Android todavía deja volver a preguntar. */
    DENIED,

    /** Dijo que no y Android ya no vuelve a preguntar: la única salida son los ajustes de la app. */
    DENIED_FOR_GOOD,
}

/**
 * La máquina de estados del permiso de cámara. Pura, para probarla sin Android.
 *
 * @param askedBefore si en esta pantalla ya se lanzó el diálogo del sistema.
 * @param canAskAgain lo que responde `shouldShowRequestPermissionRationale`: verdadero
 * si negó una vez y Android todavía deja preguntar; falso si negó para siempre, y
 * también falso si nunca se le preguntó (por eso hace falta [askedBefore]).
 */
object CameraPermissionLogic {
    fun step(granted: Boolean, askedBefore: Boolean, canAskAgain: Boolean): CameraPermissionStep = when {
        granted -> CameraPermissionStep.GRANTED
        !askedBefore -> CameraPermissionStep.EXPLAIN
        canAskAgain -> CameraPermissionStep.DENIED
        else -> CameraPermissionStep.DENIED_FOR_GOOD
    }
}

package gt.marcos.joyeria.domain.backup

/** Por qué se rechazó un respaldo o una importación. La pantalla lo traduce a un mensaje en español claro. */
enum class BackupFailure {
    /** No es un zip, o no tiene el manifiesto: no es un respaldo de esta app. */
    NOT_A_BACKUP,

    /** Es un respaldo pero está dañado (truncado, o el SHA-256 no coincide). */
    CORRUPT,

    /** La versión del contenedor, o el cifrado, que no se sabe leer. */
    UNSUPPORTED_FORMAT,

    /** Se hizo con una versión más nueva de la app. */
    SCHEMA_TOO_NEW,

    /** Es de una versión vieja del esquema y no hay cómo pasarlo a la actual. */
    SCHEMA_NO_UPGRADER,

    /** Una entrada con una ruta que intenta salirse de la carpeta, o que el formato no define. */
    UNSAFE_FILE,

    /** Más grande de lo que el formato admite. */
    TOO_BIG,

    /** Los datos no respetan el esquema (columnas, tipos, valores, referencias). */
    INVALID_DATA,

    /** Dos categorías activas con el mismo nombre (D-057). */
    CATEGORY_CONFLICT,

    /** No se pudo guardar la copia previa: no se importa nada sin red de seguridad. */
    SAFETY_COPY_FAILED,

    /** «Deshacer» sin una copia previa que restaurar. */
    NOTHING_TO_UNDO,
}

class BackupException(val failure: BackupFailure, val detail: String? = null, cause: Throwable? = null) :
    Exception("${failure.name}${detail?.let { ": $it" } ?: ""}", cause)

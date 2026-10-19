package gt.marcos.joyeria

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

/**
 * El guardián de la sección 9 de `CLAUDE.md` («Backend, red, analytics, … telemetría
 * de cualquier tipo»; «permisos de Android que no se usen»): **el paquete instalado
 * no pide `INTERNET`**, venga de donde venga.
 *
 * Mira el paquete ya instalado, o sea el manifiesto FUSIONADO: lo que una librería
 * nueva agrega por su cuenta. Por eso existe: ML Kit traía `INTERNET` (y una tubería
 * de telemetría de Google) por una dependencia de una dependencia, y se descartó
 * (D-059). Una dependencia futura que lo vuelva a traer rompe este test, y quien
 * la agregó se entera ese mismo día y no meses después.
 */
@RunWith(AndroidJUnit4::class)
class NetworkPermissionTest {

    @Test
    fun requestedPermissions_doesNotContainInternet() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val requested = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions?.toList().orEmpty()

        assertWithMessage(
            "La app no puede pedir INTERNET (CLAUDE.md sección 9: sin red ni telemetría). Permisos que pide: $requested. " +
                "Si apareció, mirá qué dependencia nueva lo trae (`./gradlew :app:dependencies`) y qué dice el manifiesto fusionado.",
        )
            .that(requested)
            .doesNotContain("android.permission.INTERNET")
    }
}

package gt.marcos.joyeria

import android.content.pm.ApplicationInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

/**
 * D-062 (Fase 14): la copia automática de Android está apagada. Los datos de la usuaria se respaldan SOLO con el
 * archivo que ella misma guarda; una restauración automática y silenciosa de Google sobre una base distinta es peor
 * que ninguna. Mira el paquete instalado (manifiesto fusionado), igual que `NetworkPermissionTest`: una librería
 * que lo vuelva a encender se nota ese mismo día.
 */
@RunWith(AndroidJUnit4::class)
class AutoBackupPolicyTest {

    @Test
    fun installedApp_hasAndroidAutoBackupTurnedOff() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val flags = context.packageManager.getApplicationInfo(context.packageName, 0).flags

        assertWithMessage("allowBackup debe seguir en false (D-062 en DECISIONES.md)")
            .that(flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
            .isEqualTo(0)
    }
}

package gt.marcos.joyeria.ui.permission

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CameraPermissionLogicTest {

    private fun step(granted: Boolean, askedBefore: Boolean, canAskAgain: Boolean) =
        CameraPermissionLogic.step(granted, askedBefore, canAskAgain)

    @Test
    fun step_alreadyGranted_isGrantedWhateverTheRest() {
        assertThat(step(granted = true, askedBefore = false, canAskAgain = false)).isEqualTo(CameraPermissionStep.GRANTED)
        assertThat(step(granted = true, askedBefore = true, canAskAgain = true)).isEqualTo(CameraPermissionStep.GRANTED)
    }

    @Test
    fun step_neverAsked_explainsFirstEvenThoughAndroidSaysItCannotAskAgain() {
        // shouldShowRequestPermissionRationale es falso también antes de preguntar por primera vez.
        assertThat(step(granted = false, askedBefore = false, canAskAgain = false)).isEqualTo(CameraPermissionStep.EXPLAIN)
    }

    @Test
    fun step_deniedOnceAndAndroidStillAsks_isDenied() {
        assertThat(step(granted = false, askedBefore = true, canAskAgain = true)).isEqualTo(CameraPermissionStep.DENIED)
    }

    @Test
    fun step_deniedForGood_offersTheSettings() {
        assertThat(step(granted = false, askedBefore = true, canAskAgain = false)).isEqualTo(CameraPermissionStep.DENIED_FOR_GOOD)
    }
}

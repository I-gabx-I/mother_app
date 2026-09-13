package gt.marcos.joyeria

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Entry point for Hilt's dependency graph. Empty on purpose: no modules exist
// yet, they're added starting Fase 01 (DatabaseModule) and later phases.
@HiltAndroidApp
class JoyeriaApp : Application()

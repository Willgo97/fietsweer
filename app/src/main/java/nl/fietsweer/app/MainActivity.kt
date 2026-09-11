package nl.fietsweer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import nl.fietsweer.app.data.Prefs
import nl.fietsweer.app.notify.AlertScheduler
import nl.fietsweer.app.ui.AppViewModel
import nl.fietsweer.app.ui.FietsweerRoot

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(0, 0),
            navigationBarStyle = SystemBarStyle.auto(0, 0)
        )

        Prefs.get(this).reload()
        AlertScheduler.rescheduleAll(this)

        val versionName = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull() ?: "1.0"

        setContent {
            FietsweerRoot(vm, versionName)
        }

        vm.refresh(force = false)
    }
}

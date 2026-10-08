package nl.fietsweer.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import nl.fietsweer.app.notify.AlertScheduler
import nl.fietsweer.app.ui.AppViewModel
import nl.fietsweer.app.ui.FietsweerRoot

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(0, 0),
            navigationBarStyle = SystemBarStyle.auto(0, 0)
        )

        AlertScheduler.rescheduleAll(this)

        val versionName = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull() ?: "1.0"

        setContent {
            FietsweerRoot(viewModel, versionName)
        }
    }

    companion object {
        fun openIntent(context: Context, requestCode: Int, clearTop: Boolean = true): PendingIntent {
            val flags = if (clearTop) Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP else Intent.FLAG_ACTIVITY_NEW_TASK
            return PendingIntent.getActivity(
                context, requestCode,
                Intent(context, MainActivity::class.java).addFlags(flags),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}

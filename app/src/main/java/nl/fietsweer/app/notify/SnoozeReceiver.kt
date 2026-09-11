package nl.fietsweer.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ALERT_ID).orEmpty()
        NotificationManagerCompat.from(context).cancelAll()
        AlertScheduler.scheduleOneShot(
            context, id, System.currentTimeMillis() + 60 * 60 * 1000L
        )
    }

    companion object {
        const val EXTRA_ALERT_ID = "alert_id"
    }
}

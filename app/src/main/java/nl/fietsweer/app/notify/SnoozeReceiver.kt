package nl.fietsweer.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import nl.fietsweer.app.domain.HOUR_MS

class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alertId = intent.getStringExtra(AlertReceiver.EXTRA_ALERT_ID).orEmpty()
        NotificationManagerCompat.from(context).cancelAll()
        AlertScheduler.scheduleOneShot(context, alertId, System.currentTimeMillis() + SNOOZE_MS)
    }

    private companion object {
        const val SNOOZE_MS = HOUR_MS
    }
}

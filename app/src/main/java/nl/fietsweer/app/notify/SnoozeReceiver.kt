package nl.fietsweer.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import nl.fietsweer.app.domain.HOUR_MS

class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alertId = intent.getStringExtra(AlertReceiver.EXTRA_ALERT_ID).orEmpty()
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        NotificationManagerCompat.from(context).cancel(notificationId)
        AlertScheduler.scheduleSnooze(context, alertId, System.currentTimeMillis() + SNOOZE_MS)
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val SNOOZE_MS = HOUR_MS
    }
}

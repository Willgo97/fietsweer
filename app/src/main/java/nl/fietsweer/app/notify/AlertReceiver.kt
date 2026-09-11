package nl.fietsweer.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import nl.fietsweer.app.data.Prefs

class AlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ALERT_ID).orEmpty()
        enqueue(context, id)

        // Put the next occurrence of this very alert back on the calendar.
        val prefs = Prefs.get(context)
        prefs.reload()
        prefs.current.alerts.firstOrNull { it.id == id }?.let {
            AlertScheduler.scheduleNext(context, it)
        }
    }

    companion object {
        const val EXTRA_ALERT_ID = "alert_id"

        fun enqueue(context: Context, alertId: String) {
            val request = OneTimeWorkRequestBuilder<AlertWorker>()
                .setInputData(Data.Builder().putString(EXTRA_ALERT_ID, alertId).build())
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .addTag("fietsweer-alert")
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork("alert-$alertId", ExistingWorkPolicy.REPLACE, request)
        }
    }
}

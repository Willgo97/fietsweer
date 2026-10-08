package nl.fietsweer.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import nl.fietsweer.app.data.Alert
import nl.fietsweer.app.data.SettingsStore
import nl.fietsweer.app.domain.nextTriggerMs

object AlertScheduler {

    private const val TAG = "AlertScheduler"
    private const val SCHEDULED_ALARMS_FILE = "alert_book"
    private const val KEY_SCHEDULED_IDS = "scheduled_ids"
    private const val ACTION_ALERT = "nl.fietsweer.app.ALERT"

    // A separate action, so rescheduling the regular alarms leaves a pending snooze alone.
    const val ACTION_SNOOZE = "nl.fietsweer.app.SNOOZE_ALERT"

    private fun requestCode(alertId: String): Int = (alertId.hashCode() and 0x0FFFFFFF) or 1

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    fun rescheduleAll(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val scheduledAlarms = appContext.getSharedPreferences(SCHEDULED_ALARMS_FILE, Context.MODE_PRIVATE)

        for (alertId in scheduledAlarms.getStringSet(KEY_SCHEDULED_IDS, emptySet()).orEmpty()) {
            alarmManager.cancel(pendingIntent(appContext, alertId, ACTION_ALERT, PendingIntent.FLAG_NO_CREATE) ?: continue)
        }

        val alerts = SettingsStore.get(appContext).current.alerts
        val scheduledIds = mutableSetOf<String>()
        for (alert in alerts) {
            val triggerMs = alert.nextTriggerMs() ?: continue
            schedule(appContext, alarmManager, alert.id, ACTION_ALERT, triggerMs)
            scheduledIds += alert.id
        }
        scheduledAlarms.edit().putStringSet(KEY_SCHEDULED_IDS, scheduledIds).apply()
        Log.i(TAG, "scheduled ${scheduledIds.size} alert(s)")
    }

    fun scheduleNext(context: Context, alert: Alert) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val triggerMs = alert.nextTriggerMs() ?: return
        schedule(appContext, alarmManager, alert.id, ACTION_ALERT, triggerMs)
    }

    fun scheduleSnooze(context: Context, alertId: String, atMs: Long) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        schedule(appContext, alarmManager, alertId, ACTION_SNOOZE, atMs)
    }

    private fun schedule(context: Context, alarmManager: AlarmManager, alertId: String, action: String, atMs: Long) {
        val intent = pendingIntent(context, alertId, action, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        try {
            if (canScheduleExact(context)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, intent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, intent)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, intent)
        }
    }

    private fun pendingIntent(context: Context, alertId: String, action: String, extraFlags: Int): PendingIntent? {
        val intent = Intent(context, AlertReceiver::class.java)
            .setAction(action)
            .putExtra(AlertReceiver.EXTRA_ALERT_ID, alertId)
        return PendingIntent.getBroadcast(
            context, requestCode(alertId), intent,
            PendingIntent.FLAG_IMMUTABLE or extraFlags
        )
    }
}

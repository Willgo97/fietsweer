package nl.fietsweer.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import nl.fietsweer.app.data.Alert
import nl.fietsweer.app.data.SettingsStore
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object AlertScheduler {

    private const val TAG = "AlertScheduler"
    private const val SCHEDULED_ALARMS_FILE = "alert_book"
    private const val KEY_SCHEDULED_IDS = "scheduled_ids"

    private fun requestCode(alertId: String): Int = (alertId.hashCode() and 0x0FFFFFFF) or 1

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    fun nextTrigger(alert: Alert): Long? {
        if (!alert.enabled || alert.days.isEmpty()) return null
        val fromMs = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(fromMs), zone)
        for (daysAhead in 0..8L) {
            val date = now.toLocalDate().plusDays(daysAhead)
            if (date.dayOfWeek.value !in alert.days) continue
            val triggerAt = date.atTime(alert.hour.coerceIn(0, 23), alert.minute.coerceIn(0, 59))
                .atZone(zone)
            val triggerMs = triggerAt.toInstant().toEpochMilli()
            if (triggerMs > fromMs + 1000) return triggerMs
        }
        return null
    }

    fun rescheduleAll(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val scheduledAlarms = appContext.getSharedPreferences(SCHEDULED_ALARMS_FILE, Context.MODE_PRIVATE)

        for (alertId in scheduledAlarms.getStringSet(KEY_SCHEDULED_IDS, emptySet()).orEmpty()) {
            alarmManager.cancel(pendingIntent(appContext, alertId, PendingIntent.FLAG_NO_CREATE) ?: continue)
        }

        val alerts = SettingsStore.get(appContext).current.alerts
        val scheduledIds = mutableSetOf<String>()
        for (alert in alerts) {
            val triggerMs = nextTrigger(alert) ?: continue
            schedule(appContext, alarmManager, alert.id, triggerMs)
            scheduledIds += alert.id
        }
        scheduledAlarms.edit().putStringSet(KEY_SCHEDULED_IDS, scheduledIds).apply()
        Log.i(TAG, "scheduled ${scheduledIds.size} alert(s)")
    }

    fun scheduleNext(context: Context, alert: Alert) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val triggerMs = nextTrigger(alert) ?: return
        schedule(appContext, alarmManager, alert.id, triggerMs)
    }

    fun scheduleOneShot(context: Context, alertId: String, atMs: Long) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        schedule(appContext, alarmManager, alertId, atMs)
    }

    private fun schedule(context: Context, alarmManager: AlarmManager, alertId: String, atMs: Long) {
        val intent = pendingIntent(context, alertId, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
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

    private fun pendingIntent(context: Context, alertId: String, extraFlags: Int): PendingIntent? {
        val intent = Intent(context, AlertReceiver::class.java)
            .setAction("nl.fietsweer.app.ALERT")
            .putExtra(AlertReceiver.EXTRA_ALERT_ID, alertId)
        return PendingIntent.getBroadcast(
            context, requestCode(alertId), intent,
            PendingIntent.FLAG_IMMUTABLE or extraFlags
        )
    }
}

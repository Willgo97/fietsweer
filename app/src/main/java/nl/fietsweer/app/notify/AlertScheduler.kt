package nl.fietsweer.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import nl.fietsweer.app.data.Alert
import nl.fietsweer.app.data.Prefs
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Turns the configured alerts into exact alarms. Every firing reschedules
 * itself, and the set of scheduled ids is remembered so alerts the user
 * deleted can still be cancelled.
 */
object AlertScheduler {

    private const val TAG = "AlertScheduler"
    private const val BOOK = "alert_book"
    private const val KEY_IDS = "scheduled_ids"

    fun requestCode(id: String): Int = (id.hashCode() and 0x0FFFFFFF) or 1

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(AlarmManager::class.java) ?: return false
        return am.canScheduleExactAlarms()
    }

    /** Next moment this alert should fire, or null when no day is selected. */
    fun nextTrigger(alert: Alert, fromMs: Long = System.currentTimeMillis()): Long? {
        if (!alert.enabled || alert.days.isEmpty()) return null
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(fromMs), zone)
        for (offset in 0..8L) {
            val date = now.toLocalDate().plusDays(offset)
            if (date.dayOfWeek.value !in alert.days) continue
            val at = date.atTime(alert.hour.coerceIn(0, 23), alert.minute.coerceIn(0, 59))
                .atZone(zone)
            val ms = at.toInstant().toEpochMilli()
            if (ms > fromMs + 1000) return ms
        }
        return null
    }

    fun rescheduleAll(context: Context) {
        val ctx = context.applicationContext
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val book = ctx.getSharedPreferences(BOOK, Context.MODE_PRIVATE)

        // Cancel everything we scheduled before, including deleted alerts.
        for (id in book.getStringSet(KEY_IDS, emptySet()).orEmpty()) {
            am.cancel(pendingIntent(ctx, id, PendingIntent.FLAG_NO_CREATE) ?: continue)
        }

        val alerts = Prefs.get(ctx).current.alerts
        val live = mutableSetOf<String>()
        for (alert in alerts) {
            val at = nextTrigger(alert) ?: continue
            schedule(ctx, am, alert.id, at)
            live += alert.id
        }
        book.edit().putStringSet(KEY_IDS, live).apply()
        Log.i(TAG, "scheduled ${live.size} alert(s)")
    }

    fun scheduleNext(context: Context, alert: Alert) {
        val ctx = context.applicationContext
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val at = nextTrigger(alert) ?: return
        schedule(ctx, am, alert.id, at)
    }

    fun scheduleOneShot(context: Context, alertId: String, atMs: Long) {
        val ctx = context.applicationContext
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        schedule(ctx, am, alertId, atMs)
    }

    private fun schedule(context: Context, am: AlarmManager, id: String, atMs: Long) {
        val pi = pendingIntent(context, id, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        try {
            if (canScheduleExact(context)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
            } else {
                // Still wakes the device, just with a window Android chooses.
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
            }
        } catch (se: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
        }
    }

    private fun pendingIntent(context: Context, id: String, extraFlags: Int): PendingIntent? {
        val intent = Intent(context, AlertReceiver::class.java)
            .setAction("nl.fietsweer.app.ALERT")
            .putExtra(AlertReceiver.EXTRA_ALERT_ID, id)
        return PendingIntent.getBroadcast(
            context, requestCode(id), intent,
            PendingIntent.FLAG_IMMUTABLE or extraFlags
        )
    }
}

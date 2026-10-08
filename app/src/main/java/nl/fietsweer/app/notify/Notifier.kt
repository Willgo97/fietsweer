package nl.fietsweer.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import nl.fietsweer.app.MainActivity
import nl.fietsweer.app.R
import nl.fietsweer.app.data.Alert
import nl.fietsweer.app.domain.Advice
import nl.fietsweer.app.domain.AdviceText
import nl.fietsweer.app.domain.Formatter
import nl.fietsweer.app.domain.Strings
import nl.fietsweer.app.ui.theme.SystemColors

object Notifier {

    private const val CHANNEL_ID = "commute_advice"
    private const val ADVICE_NOTIFICATION_BASE_ID = 7000
    private const val PROBLEM_NOTIFICATION_ID = 7500
    private const val OPEN_FROM_ADVICE = 1
    private const val OPEN_FROM_PROBLEM = 3

    fun ensureChannel(context: Context, strings: Strings) {
        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            strings.notifChannelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = strings.notifChannelBody
            enableLights(true)
            lightColor = SystemColors.notificationLight
            enableVibration(true)
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun postAdvice(context: Context, alert: Alert?, advice: Advice, strings: Strings) {
        if (!canPost(context)) return
        ensureChannel(context, strings)
        val format = Formatter(strings)

        val title = AdviceText.headline(advice, strings)
        val summaryLine = AdviceText.chipLine(advice, strings)

        val legLines = advice.rides.map { AdviceText.legLine(it, strings, format) }
        val expandedText = buildString {
            append(summaryLine)
            for (line in legLines) { append('\n'); append(line) }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(legLines.firstOrNull() ?: summaryLine)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(SystemColors.adviceAccent(advice))
            .setAutoCancel(true)
            .setContentIntent(MainActivity.openIntent(context, OPEN_FROM_ADVICE))

        val notificationId = ADVICE_NOTIFICATION_BASE_ID + (alert?.id?.hashCode()?.and(0xFF) ?: 0)
        if (advice.definite) {
            // One snooze per notification: request codes keep them apart, extras alone would not.
            val snooze = PendingIntent.getBroadcast(
                context, notificationId,
                Intent(context, SnoozeReceiver::class.java)
                    .putExtra(AlertReceiver.EXTRA_ALERT_ID, alert?.id ?: "")
                    .putExtra(SnoozeReceiver.EXTRA_NOTIFICATION_ID, notificationId),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(0, strings.notifSnooze, snooze)
        }

        runCatching { NotificationManagerCompat.from(context).notify(notificationId, builder.build()) }
    }

    fun postProblem(context: Context, strings: Strings, message: String) {
        if (!canPost(context)) return
        ensureChannel(context, strings)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(strings.notifNoData)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(MainActivity.openIntent(context, OPEN_FROM_PROBLEM, clearTop = false))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(PROBLEM_NOTIFICATION_ID, notification) }
    }
}

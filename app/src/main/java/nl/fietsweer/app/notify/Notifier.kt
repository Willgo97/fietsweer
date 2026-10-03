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
    private const val SUMMARY_BASE_ID = 7000

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

        val openApp = PendingIntent.getActivity(
            context, 1,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(legLines.firstOrNull() ?: summaryLine)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(SystemColors.adviceAccent(advice))
            .setColorized(false)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)

        if (advice.definite) {
            val snooze = PendingIntent.getBroadcast(
                context, 2,
                Intent(context, SnoozeReceiver::class.java)
                    .putExtra(SnoozeReceiver.EXTRA_ALERT_ID, alert?.id ?: ""),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(0, strings.notifSnooze, snooze)
        }

        val notificationId = SUMMARY_BASE_ID + (alert?.id?.hashCode()?.and(0xFF) ?: 0)
        runCatching { NotificationManagerCompat.from(context).notify(notificationId, builder.build()) }
    }

    fun postProblem(context: Context, strings: Strings, message: String) {
        if (!canPost(context)) return
        ensureChannel(context, strings)
        val openApp = PendingIntent.getActivity(
            context, 3,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(strings.notifNoData)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(SUMMARY_BASE_ID + 500, notification) }
    }
}

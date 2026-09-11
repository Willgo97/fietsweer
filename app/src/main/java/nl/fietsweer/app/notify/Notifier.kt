package nl.fietsweer.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import nl.fietsweer.app.MainActivity
import nl.fietsweer.app.R
import nl.fietsweer.app.data.Alert
import nl.fietsweer.app.domain.Advice
import nl.fietsweer.app.domain.AdviceText
import nl.fietsweer.app.domain.Fmt
import nl.fietsweer.app.domain.Need
import nl.fietsweer.app.domain.Txt

object Notifier {

    const val CHANNEL_ID = "commute_advice"
    private const val SUMMARY_BASE_ID = 7000

    fun ensureChannel(context: Context, txt: Txt) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            txt.notifChannelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = txt.notifChannelBody
            enableLights(true)
            lightColor = Color.parseColor("#17A2A8")
            enableVibration(true)
            setShowBadge(true)
        }
        nm.createNotificationChannel(channel)
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

    fun postAdvice(context: Context, alert: Alert?, advice: Advice, txt: Txt) {
        if (!canPost(context)) return
        ensureChannel(context, txt)
        val fmt = Fmt(txt)

        val title = AdviceText.headline(advice, txt)
        val chips = AdviceText.chips(advice, txt)
        val shortLine = if (chips.isEmpty()) txt.adviceNoneSub
        else chips.joinToString(" · ") { it.first }

        val lines = advice.rides.map { AdviceText.legLine(it, txt, fmt) }
        val big = buildString {
            append(shortLine)
            for (l in lines) { append('\n'); append(l) }
        }

        val open = PendingIntent.getActivity(
            context, 1,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val accent = when {
            advice.rain == Need.YES -> Color.parseColor("#2D7FF0")
            advice.warm == Need.YES -> Color.parseColor("#E8873D")
            advice.rain == Need.MAYBE || advice.warm == Need.MAYBE -> Color.parseColor("#E5B33C")
            else -> Color.parseColor("#35C46F")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(lines.firstOrNull() ?: shortLine)
            .setStyle(NotificationCompat.BigTextStyle().bigText(big))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(accent)
            .setColorized(false)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)

        if (advice.definite) {
            val snooze = PendingIntent.getBroadcast(
                context, 2,
                Intent(context, SnoozeReceiver::class.java)
                    .putExtra(SnoozeReceiver.EXTRA_ALERT_ID, alert?.id ?: ""),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(0, txt.notifSnooze, snooze)
        }

        val id = SUMMARY_BASE_ID + (alert?.id?.hashCode()?.and(0xFF) ?: 0)
        runCatching { NotificationManagerCompat.from(context).notify(id, builder.build()) }
    }

    fun postProblem(context: Context, txt: Txt, message: String) {
        if (!canPost(context)) return
        ensureChannel(context, txt)
        val open = PendingIntent.getActivity(
            context, 3,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(txt.notifNoData)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(SUMMARY_BASE_ID + 500, n) }
    }
}

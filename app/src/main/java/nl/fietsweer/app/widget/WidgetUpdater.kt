package nl.fietsweer.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import nl.fietsweer.app.data.ForecastRepository
import nl.fietsweer.app.data.RouteForecast
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.data.SettingsStore
import nl.fietsweer.app.domain.SkyLight
import java.util.concurrent.TimeUnit

object WidgetUpdater {

    private const val PERIODIC_WORK = "widget-refresh-periodic"
    private const val SKY_REDRAW_REQUEST = 43
    private const val NOW_FALLBACK_WIDTH_DP = 340
    private const val NOW_FALLBACK_HEIGHT_DP = 76

    private fun widgetIds(context: Context, provider: Class<*>): IntArray =
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, provider))

    private fun anyWidgets(context: Context): Boolean =
        widgetIds(context, JacketWidget::class.java).isNotEmpty() || widgetIds(context, NowWidget::class.java).isNotEmpty()

    fun redraw(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val settings = SettingsStore.get(appContext).current
        val snapshot = WidgetStore.load(appContext)
        val artwork = WidgetRenderer.storedArtwork(appContext)

        for (id in widgetIds(appContext, JacketWidget::class.java)) {
            val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                WidgetRenderer.responsive(appContext, settings, snapshot, artwork)
            } else {
                val options = manager.getAppWidgetOptions(id)
                val size = WidgetRenderer.sizeFor(
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250).toFloat(),
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 60).toFloat()
                )
                WidgetRenderer.layout(appContext, settings, snapshot, artwork, size)
            }
            manager.updateAppWidget(id, views)
        }
        for (id in widgetIds(appContext, NowWidget::class.java)) {
            val options = manager.getAppWidgetOptions(id)
            // In portrait a widget is its minimum width wide and its maximum height tall.
            val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, NOW_FALLBACK_WIDTH_DP)
            val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, NOW_FALLBACK_HEIGHT_DP)
            manager.updateAppWidget(id, WidgetRenderer.nowViews(appContext, settings, snapshot, widthDp, heightDp))
        }
        if (widgetIds(appContext, NowWidget::class.java).isNotEmpty()) scheduleSkyRedraw(appContext, snapshot?.now)
    }

    // The sky in the weather-now picture changes around sunrise and sunset without any new data,
    // so a cheap redraw is planned then; inexact and without waking the phone.
    private fun scheduleSkyRedraw(context: Context, now: WidgetNow?) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = PendingIntent.getBroadcast(
            context, SKY_REDRAW_REQUEST,
            Intent(context, SkyRedrawReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val atMs = now?.let { SkyLight.nextChangeMs(System.currentTimeMillis(), it.sun) }
        if (atMs == null) alarmManager.cancel(intent) else alarmManager.set(AlarmManager.RTC, atMs, intent)
    }

    fun forgetIfUnused(context: Context) {
        val appContext = context.applicationContext
        if (anyWidgets(appContext)) return
        WidgetStore.clear(appContext)
        WorkManager.getInstance(appContext).cancelUniqueWork(PERIODIC_WORK)
    }

    // Android allows no periodic work more often than every 15 minutes.
    fun keepFresh(context: Context) {
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WidgetWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
        )
    }

    fun publish(context: Context, settings: Settings, forecast: RouteForecast?) {
        val appContext = context.applicationContext
        if (!anyWidgets(appContext)) return
        WidgetRenderer.contentFor(settings, forecast)?.let { content ->
            WidgetStore.save(appContext, content.snapshot)
            WidgetArt.clear(appContext)
            for ((picture, bitmap) in WidgetRenderer.artworkFor(appContext, content).pictures) {
                WidgetArt.save(bitmap, WidgetArt.file(appContext, picture))
            }
        }
        redraw(appContext)
    }

    private const val MIN_REFRESH_GAP_MS = 10 * 60 * 1000L

    // Rate limited: enqueuing work fires PACKAGE_CHANGED, which calls JacketWidget.onUpdate again, endlessly.
    fun requestRefresh(context: Context, force: Boolean = false) {
        val appContext = context.applicationContext
        if (!anyWidgets(appContext)) return
        val now = System.currentTimeMillis()
        if (!force && now - WidgetStore.lastAttempt(appContext) < MIN_REFRESH_GAP_MS) return
        WidgetStore.markAttempt(appContext, now)
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            "widget-refresh",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<WidgetWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
        )
    }
}

class SkyRedrawReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = WidgetUpdater.redraw(context)
}

class WidgetWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsStore.get(applicationContext).current
        val forecast = if (settings.hasRoute) ForecastRepository.fetchDirect(settings) else null
        WidgetUpdater.publish(applicationContext, settings, forecast)
        return Result.success()
    }
}

package nl.fietsweer.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import nl.fietsweer.app.data.ForecastRepository
import nl.fietsweer.app.data.RouteForecast
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.data.SettingsStore

object WidgetUpdater {

    private fun widgetIds(context: Context, provider: Class<*>): IntArray =
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, provider))

    private fun anyWidgets(context: Context): Boolean =
        widgetIds(context, JacketWidget::class.java).isNotEmpty() || widgetIds(context, BadgeWidget::class.java).isNotEmpty()

    fun redraw(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val settings = SettingsStore.get(appContext).current
        val snapshot = WidgetStore.load(appContext)
        val artwork = WidgetRenderer.storedArtwork(appContext)

        for (id in widgetIds(appContext, JacketWidget::class.java)) {
            val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                WidgetRenderer.build(appContext, settings, snapshot, artwork)
            } else {
                val options = manager.getAppWidgetOptions(id)
                val size = WidgetRenderer.sizeFor(
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250).toFloat(),
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 60).toFloat()
                )
                WidgetRenderer.build(appContext, settings, snapshot, artwork, size)
            }
            manager.updateAppWidget(id, views)
        }
        val badges = widgetIds(appContext, BadgeWidget::class.java)
        if (badges.isNotEmpty()) manager.updateAppWidget(badges, WidgetRenderer.badgeViews(appContext, snapshot, artwork))
    }

    fun forgetIfUnused(context: Context) {
        if (!anyWidgets(context.applicationContext)) WidgetStore.clear(context)
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

class WidgetWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsStore.get(applicationContext).current
        val forecast = if (settings.hasRoute) ForecastRepository.fetchDirect(settings) else null
        WidgetUpdater.publish(applicationContext, settings, forecast)
        return Result.success()
    }
}

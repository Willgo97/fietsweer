package nl.fietsweer.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
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

    private fun widgetIds(context: Context): IntArray =
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, JacketWidget::class.java))

    fun redraw(context: Context) {
        val appContext = context.applicationContext
        val ids = widgetIds(appContext)
        if (ids.isEmpty()) return
        val views = WidgetRenderer.build(appContext, SettingsStore.get(appContext).current, WidgetStore.load(appContext))
        AppWidgetManager.getInstance(appContext).updateAppWidget(ids, views)
    }

    fun publish(context: Context, settings: Settings, forecast: RouteForecast?) {
        val appContext = context.applicationContext
        if (widgetIds(appContext).isEmpty()) return
        WidgetRenderer.snapshotFor(settings, forecast)?.let { WidgetStore.save(appContext, it) }
        redraw(appContext)
    }

    private const val MIN_REFRESH_GAP_MS = 10 * 60 * 1000L

    // Rate limited: enqueuing work fires PACKAGE_CHANGED, which calls JacketWidget.onUpdate again, endlessly.
    fun requestRefresh(context: Context, force: Boolean = false) {
        val appContext = context.applicationContext
        if (widgetIds(appContext).isEmpty()) return
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
        val store = SettingsStore.get(applicationContext)
        store.reload()
        val settings = store.current
        val forecast = if (settings.hasRoute) ForecastRepository.fetchDirect(settings) else null
        WidgetUpdater.publish(applicationContext, settings, forecast)
        return Result.success()
    }
}

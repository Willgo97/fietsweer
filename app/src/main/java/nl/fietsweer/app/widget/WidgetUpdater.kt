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
import nl.fietsweer.app.data.Prefs
import nl.fietsweer.app.data.Repository
import nl.fietsweer.app.data.RouteForecast
import nl.fietsweer.app.data.Settings

object WidgetUpdater {

    private fun ids(context: Context): IntArray =
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, JacketWidget::class.java))

    /** Repaints from the stored snapshot. Cheap: no forecasting, no network. */
    fun redraw(context: Context) {
        val ctx = context.applicationContext
        val widgetIds = ids(ctx)
        if (widgetIds.isEmpty()) return
        val views = WidgetRenderer.build(ctx, Prefs.get(ctx).current, WidgetStore.load(ctx))
        AppWidgetManager.getInstance(ctx).updateAppWidget(widgetIds, views)
    }

    /**
     * Recomputes from a fresh forecast, stores the result and repaints. A
     * forecast that yields nothing leaves the previous snapshot alone rather
     * than replacing good information with an error card.
     */
    fun publish(context: Context, settings: Settings, forecast: RouteForecast?) {
        val ctx = context.applicationContext
        if (ids(ctx).isEmpty()) return
        WidgetRenderer.snapshotFor(settings, forecast)?.let { WidgetStore.save(ctx, it) }
        redraw(ctx)
    }

    /** Fetches in the background and then republishes. */
    fun requestRefresh(context: Context) {
        val ctx = context.applicationContext
        if (ids(ctx).isEmpty()) return
        WorkManager.getInstance(ctx).enqueueUniqueWork(
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
        val prefs = Prefs.get(applicationContext)
        prefs.reload()
        val settings = prefs.current
        val forecast = if (settings.ready) Repository.fetchDirect(settings) else null
        WidgetUpdater.publish(applicationContext, settings, forecast)
        return Result.success()
    }
}

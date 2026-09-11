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

object WidgetUpdater {

    private fun ids(context: Context): IntArray =
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, JacketWidget::class.java))

    /** Repaints every placed widget from the cached forecast, no network. */
    fun redraw(context: Context) {
        val ctx = context.applicationContext
        val widgetIds = ids(ctx)
        if (widgetIds.isEmpty()) return
        val settings = Prefs.get(ctx).current
        val views = WidgetRenderer.build(ctx, settings, Repository.state.value.forecast)
        AppWidgetManager.getInstance(ctx).updateAppWidget(widgetIds, views)
    }

    /** Fetches in the background and then repaints. */
    fun requestRefresh(context: Context) {
        val ctx = context.applicationContext
        if (ids(ctx).isEmpty()) return
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            "widget-refresh",
            ExistingWorkPolicy.REPLACE,
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
        if (settings.ready) Repository.fetchDirect(settings)
        WidgetUpdater.redraw(applicationContext)
        return Result.success()
    }
}

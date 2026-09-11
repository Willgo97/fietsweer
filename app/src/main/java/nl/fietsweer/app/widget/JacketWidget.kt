package nl.fietsweer.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import nl.fietsweer.app.data.Prefs
import nl.fietsweer.app.data.Repository

class JacketWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Paint immediately with whatever is cached, then go and fetch.
        val settings = Prefs.get(context).also { it.reload() }.current
        val views = WidgetRenderer.build(context, settings, Repository.state.value.forecast)
        manager.updateAppWidget(appWidgetIds, views)
        WidgetUpdater.requestRefresh(context)
    }

    override fun onEnabled(context: Context) {
        WidgetUpdater.requestRefresh(context)
    }
}

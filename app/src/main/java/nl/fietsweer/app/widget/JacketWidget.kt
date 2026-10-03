package nl.fietsweer.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle

class JacketWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        WidgetUpdater.redraw(context)
        WidgetUpdater.requestRefresh(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        // Older launchers need a repaint to pick the size-specific layout.
        WidgetUpdater.redraw(context)
    }

    override fun onEnabled(context: Context) {
        WidgetUpdater.requestRefresh(context, force = true)
    }

    override fun onDisabled(context: Context) {
        WidgetStore.clear(context)
    }
}

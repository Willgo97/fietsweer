package nl.fietsweer.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle

class JacketWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Draw the stored snapshot first so the widget never blinks empty.
        WidgetUpdater.redraw(context)
        WidgetUpdater.requestRefresh(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        // Older launchers need a repaint to pick the size-specific layout.
        WidgetUpdater.redraw(context)
    }

    override fun onEnabled(context: Context) {
        // First one placed: skip the refresh gap.
        WidgetUpdater.requestRefresh(context, force = true)
    }

    override fun onDisabled(context: Context) {
        WidgetStore.clear(context)
    }
}

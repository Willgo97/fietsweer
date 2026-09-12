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
        // Paint the stored snapshot straight away so the widget never blinks
        // through an empty state, then go and see whether it is still true.
        WidgetUpdater.redraw(context)
        WidgetUpdater.requestRefresh(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        // Resized: the size-specific layouts are already in the RemoteViews, but
        // repaint so an older launcher picks the right one up.
        WidgetUpdater.redraw(context)
    }

    override fun onEnabled(context: Context) {
        // First one placed: fetch straight away rather than waiting out the gap.
        WidgetUpdater.requestRefresh(context, force = true)
    }

    override fun onDisabled(context: Context) {
        WidgetStore.clear(context)
    }
}

package nl.fietsweer.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle

abstract class FietsweerWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdater.redraw(context)
        WidgetUpdater.requestRefresh(context)
        WidgetUpdater.keepFresh(context)
    }

    // A new size needs a repaint: older launchers pick the layout by size, and the
    // weather-now picture is drawn to fit.
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        WidgetUpdater.redraw(context)
    }

    override fun onEnabled(context: Context) {
        WidgetUpdater.requestRefresh(context, force = true)
        WidgetUpdater.keepFresh(context)
    }

    override fun onDisabled(context: Context) {
        WidgetUpdater.forgetIfUnused(context)
    }
}

class JacketWidget : FietsweerWidget()

class NowWidget : FietsweerWidget()

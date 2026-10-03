package nl.fietsweer.app.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.SizeF
import android.widget.RemoteViews
import nl.fietsweer.app.MainActivity
import nl.fietsweer.app.R
import nl.fietsweer.app.data.Coverage
import nl.fietsweer.app.data.RouteForecast
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.domain.Advice
import nl.fietsweer.app.domain.AdviceText
import nl.fietsweer.app.domain.Commute
import nl.fietsweer.app.domain.Engine
import nl.fietsweer.app.domain.Formatter
import nl.fietsweer.app.domain.Jacket
import nl.fietsweer.app.domain.Strings
import nl.fietsweer.app.ui.theme.SystemColors

object WidgetRenderer {

    fun snapshotFor(settings: Settings, forecast: RouteForecast?): WidgetSnapshot? {
        if (!settings.hasRoute || forecast == null || !forecast.hasModels) return null
        val strings = Strings.of(settings.language)
        val format = Formatter(strings)

        val engine = Engine(forecast, settings)
        val rides = Commute.plannedRides(settings, Coverage.BOTH, alertDaysOnly = true).map(engine::assess)
        val advice: Advice = Jacket.forRides(rides, settings)

        val legLines = rides.map { ride ->
            val rain = if (ride.risk < 0.10) strings.notifDry else format.percent(ride.risk)
            val feel = if (ride.hasConditions) " · ${format.temp(ride.bikeFeelC)}" else ""
            "${AdviceText.legName(ride.leg, strings)}  ${AdviceText.moment(ride, format)}  $rain$feel"
        }

        return WidgetSnapshot(
            headline = AdviceText.headline(advice, strings),
            chipLine = AdviceText.chipLine(advice, strings),
            firstLegLine = legLines.getOrElse(0) { "" },
            secondLegLine = legLines.getOrElse(1) { "" },
            updatedTime = format.time(forecast.fetchedAt),
            accent = SystemColors.adviceAccent(advice)
        )
    }

    fun build(context: Context, settings: Settings, snapshot: WidgetSnapshot?): RemoteViews {
        val strings = Strings.of(settings.language)
        val full = layout(context, strings, snapshot, compact = false)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return full
        val compact = layout(context, strings, snapshot, compact = true)
        return RemoteViews(
            mapOf(
                SizeF(140f, 40f) to compact,
                SizeF(140f, 108f) to full
            )
        )
    }

    fun singleLayout(
        context: Context,
        settings: Settings,
        snapshot: WidgetSnapshot?,
        compact: Boolean
    ): RemoteViews = layout(context, Strings.of(settings.language), snapshot, compact)

    private fun layout(
        context: Context,
        strings: Strings,
        snapshot: WidgetSnapshot?,
        compact: Boolean
    ): RemoteViews {
        val views = RemoteViews(
            context.packageName,
            if (compact) R.layout.widget_advice_compact else R.layout.widget_advice
        )
        val rootId = if (compact) R.id.widget_compact_root else R.id.widget_root
        val accentId = if (compact) R.id.widget_compact_accent else R.id.widget_accent

        views.setOnClickPendingIntent(
            rootId,
            PendingIntent.getActivity(
                context, 42,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )

        if (snapshot == null) {
            val headline = strings.setupNeededTitle
            val body = strings.setupNeededBody
            if (compact) {
                views.setTextViewText(R.id.widget_compact_headline, headline)
                views.setTextViewText(R.id.widget_compact_legs, body)
            } else {
                views.setTextViewText(R.id.widget_kicker, strings.appName.uppercase())
                views.setTextViewText(R.id.widget_stamp, "")
                views.setTextViewText(R.id.widget_headline, headline)
                views.setTextViewText(R.id.widget_chips, body)
                views.setTextViewText(R.id.widget_leg1, "")
                views.setTextViewText(R.id.widget_leg2, "")
            }
            views.setInt(accentId, "setColorFilter", SystemColors.widgetIdle)
            return views
        }

        if (compact) {
            views.setTextViewText(R.id.widget_compact_headline, snapshot.headline)
            views.setTextViewText(
                R.id.widget_compact_legs,
                listOf(snapshot.firstLegLine, snapshot.secondLegLine).filter { it.isNotBlank() }.joinToString(" · ")
            )
        } else {
            views.setTextViewText(R.id.widget_kicker, strings.appName.uppercase())
            views.setTextViewText(R.id.widget_stamp, snapshot.updatedTime)
            views.setTextViewText(R.id.widget_headline, snapshot.headline)
            views.setTextViewText(R.id.widget_chips, snapshot.chipLine)
            views.setTextViewText(R.id.widget_leg1, snapshot.firstLegLine)
            views.setTextViewText(R.id.widget_leg2, snapshot.secondLegLine)
        }
        views.setInt(accentId, "setColorFilter", snapshot.accent)
        return views
    }
}

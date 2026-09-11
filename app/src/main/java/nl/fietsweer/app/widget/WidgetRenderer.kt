package nl.fietsweer.app.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import nl.fietsweer.app.MainActivity
import nl.fietsweer.app.R
import nl.fietsweer.app.data.Coverage
import nl.fietsweer.app.data.RouteForecast
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.domain.Advice
import nl.fietsweer.app.domain.AdviceText
import nl.fietsweer.app.domain.Engine
import nl.fietsweer.app.domain.Fmt
import nl.fietsweer.app.domain.Jacket
import nl.fietsweer.app.domain.Need
import nl.fietsweer.app.domain.Txt
import nl.fietsweer.app.notify.Commute

/** Builds the home-screen widget from whatever forecast we have. */
object WidgetRenderer {

    fun build(context: Context, settings: Settings, forecast: RouteForecast?): RemoteViews {
        val txt = Txt.of(settings.lang)
        val fmt = Fmt(txt)
        val views = RemoteViews(context.packageName, R.layout.widget_advice)

        views.setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getActivity(
                context, 42,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        views.setTextViewText(R.id.widget_kicker, txt.appName.uppercase())

        if (!settings.ready) {
            views.setTextViewText(R.id.widget_headline, txt.setupNeededTitle)
            views.setTextViewText(R.id.widget_chips, txt.setupNeededBody)
            views.setTextViewText(R.id.widget_leg1, "")
            views.setTextViewText(R.id.widget_leg2, "")
            views.setTextViewText(R.id.widget_stamp, "")
            tint(views, Color.parseColor("#7D8B95"))
            return views
        }

        if (forecast == null || !forecast.hasModels) {
            views.setTextViewText(R.id.widget_headline, txt.updateFailedTitle)
            views.setTextViewText(R.id.widget_chips, txt.updateFailedBody)
            views.setTextViewText(R.id.widget_leg1, "")
            views.setTextViewText(R.id.widget_leg2, "")
            views.setTextViewText(R.id.widget_stamp, "")
            tint(views, Color.parseColor("#DD6234"))
            return views
        }

        val engine = Engine(forecast, settings)
        val rides = Commute.plannedRides(settings, Coverage.BOTH)
            .map { (leg, at) -> engine.assess(at, leg) }
        val advice: Advice = Jacket.forRides(rides, settings)

        views.setTextViewText(R.id.widget_headline, AdviceText.headline(advice, txt))
        val chips = AdviceText.chips(advice, txt)
        views.setTextViewText(
            R.id.widget_chips,
            if (chips.isEmpty()) txt.adviceNoneSub else chips.joinToString(" · ") { it.first }
        )
        views.setTextViewText(R.id.widget_stamp, fmt.time(forecast.fetchedAt))

        val lines = rides.map { r ->
            val rain = if (r.risk < 0.10) txt.notifDry else "${r.riskPercent}%"
            val feel = if (r.hasConditions) " · ${fmt.temp(r.bikeFeelC)}" else ""
            "${AdviceText.legName(r.leg, txt)}  ${fmt.time(r.departureMs)}  $rain$feel"
        }
        views.setTextViewText(R.id.widget_leg1, lines.getOrElse(0) { "" })
        views.setTextViewText(R.id.widget_leg2, lines.getOrElse(1) { "" })

        tint(views, accentFor(advice))
        return views
    }

    private fun tint(views: RemoteViews, color: Int) {
        views.setInt(R.id.widget_accent, "setColorFilter", color)
    }

    private fun accentFor(a: Advice): Int = when {
        a.rain == Need.YES && a.warm == Need.YES -> Color.parseColor("#8A5BD6")
        a.rain == Need.YES -> Color.parseColor("#2D7FF0")
        a.warm == Need.YES -> Color.parseColor("#E07B32")
        a.rain == Need.MAYBE || a.warm == Need.MAYBE -> Color.parseColor("#DD9A26")
        else -> Color.parseColor("#1F9D55")
    }
}

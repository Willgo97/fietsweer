package nl.fietsweer.app.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
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
import nl.fietsweer.app.domain.Engine
import nl.fietsweer.app.domain.Fmt
import nl.fietsweer.app.domain.Jacket
import nl.fietsweer.app.domain.Need
import nl.fietsweer.app.domain.Txt
import nl.fietsweer.app.notify.Commute

/** Builds the home-screen widget. */
object WidgetRenderer {

    /**
     * Runs the forecast through the engine and turns it into the strings the
     * widget draws. Null when there is nothing worth showing yet, in which case
     * the caller should keep whatever snapshot it already has.
     */
    fun snapshotFor(settings: Settings, forecast: RouteForecast?): WidgetSnapshot? {
        if (!settings.ready || forecast == null || !forecast.hasModels) return null
        val txt = Txt.of(settings.lang)
        val fmt = Fmt(txt)

        val engine = Engine(forecast, settings)
        val rides = Commute.plannedRides(settings, Coverage.BOTH)
            .map { (leg, at) -> engine.assess(at, leg) }
        val advice: Advice = Jacket.forRides(rides, settings)
        val chips = AdviceText.chips(advice, txt)

        val lines = rides.map { r ->
            val rain = if (r.risk < 0.10) txt.notifDry else "${r.riskPercent}%"
            val feel = if (r.hasConditions) " · ${fmt.temp(r.bikeFeelC)}" else ""
            "${AdviceText.legName(r.leg, txt)}  ${fmt.time(r.departureMs)}  $rain$feel"
        }

        return WidgetSnapshot(
            headline = AdviceText.headline(advice, txt),
            chips = if (chips.isEmpty()) txt.adviceNoneSub else chips.joinToString(" · ") { it.first },
            leg1 = lines.getOrElse(0) { "" },
            leg2 = lines.getOrElse(1) { "" },
            stamp = fmt.time(forecast.fetchedAt),
            accent = accentFor(advice),
            savedAt = System.currentTimeMillis()
        )
    }

    /**
     * Draws [snapshot], or a short "set me up" card when there is none at all.
     * Deliberately does no forecasting work: a redraw must stay cheap, because
     * the launcher can ask for one at any moment.
     */
    fun build(context: Context, settings: Settings, snapshot: WidgetSnapshot?): RemoteViews {
        val txt = Txt.of(settings.lang)
        val full = layout(context, txt, snapshot, compact = false)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return full
        val compact = layout(context, txt, snapshot, compact = true)
        // The launcher picks the largest layout that fits the cell it was given.
        return RemoteViews(
            mapOf(
                SizeF(140f, 40f) to compact,
                SizeF(140f, 108f) to full
            )
        )
    }

    /** One specific variant, for the previews in Settings. */
    fun single(
        context: Context,
        settings: Settings,
        snapshot: WidgetSnapshot?,
        compact: Boolean
    ): RemoteViews = layout(context, Txt.of(settings.lang), snapshot, compact)

    private fun layout(
        context: Context,
        txt: Txt,
        snapshot: WidgetSnapshot?,
        compact: Boolean
    ): RemoteViews {
        val views = RemoteViews(
            context.packageName,
            if (compact) R.layout.widget_advice_compact else R.layout.widget_advice
        )
        val rootId = if (compact) R.id.widget_c_root else R.id.widget_root
        val accentId = if (compact) R.id.widget_c_accent else R.id.widget_accent

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
            val headline = txt.setupNeededTitle
            val sub = txt.setupNeededBody
            if (compact) {
                views.setTextViewText(R.id.widget_c_headline, headline)
                views.setTextViewText(R.id.widget_c_legs, sub)
            } else {
                views.setTextViewText(R.id.widget_kicker, txt.appName.uppercase())
                views.setTextViewText(R.id.widget_stamp, "")
                views.setTextViewText(R.id.widget_headline, headline)
                views.setTextViewText(R.id.widget_chips, sub)
                views.setTextViewText(R.id.widget_leg1, "")
                views.setTextViewText(R.id.widget_leg2, "")
            }
            views.setInt(accentId, "setColorFilter", Color.parseColor("#7D8B95"))
            return views
        }

        if (compact) {
            views.setTextViewText(R.id.widget_c_headline, snapshot.headline)
            views.setTextViewText(
                R.id.widget_c_legs,
                listOf(snapshot.leg1, snapshot.leg2).filter { it.isNotBlank() }.joinToString(" · ")
            )
        } else {
            views.setTextViewText(R.id.widget_kicker, txt.appName.uppercase())
            views.setTextViewText(R.id.widget_stamp, snapshot.stamp)
            views.setTextViewText(R.id.widget_headline, snapshot.headline)
            views.setTextViewText(R.id.widget_chips, snapshot.chips)
            views.setTextViewText(R.id.widget_leg1, snapshot.leg1)
            views.setTextViewText(R.id.widget_leg2, snapshot.leg2)
        }
        views.setInt(accentId, "setColorFilter", snapshot.accent)
        return views
    }

    private fun accentFor(a: Advice): Int = when {
        a.rain == Need.YES && a.warm == Need.YES -> Color.parseColor("#8A5BD6")
        a.rain == Need.YES -> Color.parseColor("#2D7FF0")
        a.warm == Need.YES -> Color.parseColor("#E07B32")
        a.rain == Need.MAYBE || a.warm == Need.MAYBE -> Color.parseColor("#DD9A26")
        else -> Color.parseColor("#1F9D55")
    }
}

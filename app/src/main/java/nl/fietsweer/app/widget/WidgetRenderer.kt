package nl.fietsweer.app.widget

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.SizeF
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import nl.fietsweer.app.MainActivity
import nl.fietsweer.app.R
import nl.fietsweer.app.data.Coverage
import nl.fietsweer.app.data.RouteForecast
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.data.WidgetStyle
import nl.fietsweer.app.domain.AdviceText
import nl.fietsweer.app.domain.Commute
import nl.fietsweer.app.domain.DepartureWindow
import nl.fietsweer.app.domain.Engine
import nl.fietsweer.app.domain.Formatter
import nl.fietsweer.app.domain.Jacket
import nl.fietsweer.app.domain.Strings
import nl.fietsweer.app.domain.departureWindow
import nl.fietsweer.app.ui.theme.LightAccents
import nl.fietsweer.app.ui.theme.SystemColors
import nl.fietsweer.app.widget.WidgetArt.Picture

enum class WidgetSize { COMPACT, ROW, LARGE }

class WidgetArtwork(val pictures: Map<Picture, Bitmap>)

object WidgetRenderer {

    private const val COMPACT_BELOW_WIDTH_DP = 200f
    private const val LARGE_FROM_HEIGHT_DP = 100f
    private const val MIN_WIDTH_DP = 60f
    private const val MIN_HEIGHT_DP = 40f
    private const val OPEN_FROM_WIDGET = 42

    class Content internal constructor(val snapshot: WidgetSnapshot, internal val window: DepartureWindow?)

    fun contentFor(settings: Settings, forecast: RouteForecast?): Content? {
        if (!settings.hasRoute || forecast == null || !forecast.hasModels) return null
        val strings = Strings.of(settings.language)
        val format = Formatter(strings)
        val now = System.currentTimeMillis()

        val engine = Engine(forecast, settings)
        val planned = Commute.plannedRides(settings, Coverage.BOTH, now, alertDaysOnly = true)
        val assessed = planned.map(engine::assess)
        val advice = Jacket.forNextDay(assessed, settings)
        val windows = planned.map { departureWindow(forecast, engine, it, now) }

        val rides = planned.indices.map { i ->
            val best = windows[i]?.best ?: assessed[i]
            WidgetRide(
                name = AdviceText.legName(planned[i].leg, strings),
                time = AdviceText.moment(best, format),
                rain = if (AdviceText.saysDry(best.risk)) strings.notifDry else format.percent(best.risk),
                temperature = if (best.hasConditions) format.temp(best.bikeFeelC) else "",
                riskColour = LightAccents.forRisk(best.risk).toArgb()
            )
        }
        val next = assessed.firstOrNull()
        return Content(
            WidgetSnapshot(
                headline = AdviceText.headline(advice, strings),
                chipLine = AdviceText.chipLine(advice, strings),
                rides = rides,
                accent = SystemColors.adviceAccent(advice),
                jacket = advice.anythingNeeded,
                riskPercent = ((windows.firstOrNull()?.best ?: next)?.riskPercent) ?: 0,
                temperature = next?.takeIf { it.hasConditions }?.let { format.temp(it.bikeFeelC) }.orEmpty()
            ),
            windows.firstOrNull()
        )
    }

    private fun iconFor(snapshot: WidgetSnapshot?): ImageVector =
        if (snapshot?.jacket != false) Icons.Rounded.Checkroom else Icons.AutoMirrored.Rounded.DirectionsBike

    fun artworkFor(context: Context, content: Content): WidgetArtwork {
        val snapshot = content.snapshot
        val pictures = mutableMapOf(
            Picture.BADGE to WidgetArt.badge(context, iconFor(snapshot), snapshot.accent),
            Picture.RING to WidgetArt.ring(context, iconFor(snapshot), snapshot.accent, snapshot.riskPercent, snapshot.temperature)
        )
        content.window?.let { window ->
            pictures[Picture.CHART] = WidgetArt.chart(context, window, 150, 48)
            pictures[Picture.CHART_WIDE] = WidgetArt.chart(context, window, 320, 72)
        }
        return WidgetArtwork(pictures)
    }

    fun storedArtwork(context: Context): WidgetArtwork =
        WidgetArtwork(Picture.entries.mapNotNull { picture -> WidgetArt.load(context, picture)?.let { picture to it } }.toMap())

    fun sizeFor(widthDp: Float, heightDp: Float): WidgetSize = when {
        heightDp >= LARGE_FROM_HEIGHT_DP && widthDp >= COMPACT_BELOW_WIDTH_DP -> WidgetSize.LARGE
        widthDp < COMPACT_BELOW_WIDTH_DP -> WidgetSize.COMPACT
        else -> WidgetSize.ROW
    }

    // Android 12+ picks the layout per size itself; older launchers get layout() for the current size.
    @RequiresApi(Build.VERSION_CODES.S)
    fun responsive(context: Context, settings: Settings, snapshot: WidgetSnapshot?, artwork: WidgetArtwork): RemoteViews =
        RemoteViews(
            mapOf(
                SizeF(MIN_WIDTH_DP, MIN_HEIGHT_DP) to layout(context, settings, snapshot, artwork, WidgetSize.COMPACT),
                SizeF(COMPACT_BELOW_WIDTH_DP, MIN_HEIGHT_DP) to layout(context, settings, snapshot, artwork, WidgetSize.ROW),
                SizeF(COMPACT_BELOW_WIDTH_DP, LARGE_FROM_HEIGHT_DP) to layout(context, settings, snapshot, artwork, WidgetSize.LARGE)
            )
        )

    fun layout(
        context: Context,
        settings: Settings,
        snapshot: WidgetSnapshot?,
        artwork: WidgetArtwork,
        size: WidgetSize,
        style: WidgetStyle = settings.widgetStyle
    ): RemoteViews {
        val views = RemoteViews(context.packageName, layoutIdFor(size, style))
        views.setOnClickPendingIntent(R.id.widget_root, openApp(context))

        val badge = if (snapshot == null) WidgetArt.badge(context, iconFor(null), SystemColors.widgetIdle)
        else artwork.pictures[Picture.BADGE]
        views.setImageViewBitmap(R.id.widget_badge, badge)

        val showsRides = size == WidgetSize.LARGE || (size == WidgetSize.ROW && style == WidgetStyle.RIDES)
        if (snapshot == null) {
            fillSetupNeeded(views, Strings.of(settings.language), size, showsRides)
            return views
        }
        fillSummary(views, snapshot, artwork, size, style)
        if (showsRides) {
            fillRide(views, 1, snapshot.rides.getOrNull(0))
            fillRide(views, 2, snapshot.rides.getOrNull(1))
        }
        return views
    }

    private fun layoutIdFor(size: WidgetSize, style: WidgetStyle): Int = when (size) {
        WidgetSize.COMPACT -> R.layout.widget_compact
        WidgetSize.LARGE -> R.layout.widget_large
        WidgetSize.ROW -> when (style) {
            WidgetStyle.CLASSIC -> R.layout.widget_classic
            WidgetStyle.CHART -> R.layout.widget_chart
            WidgetStyle.RIDES -> R.layout.widget_rides
        }
    }

    private fun fillSetupNeeded(views: RemoteViews, strings: Strings, size: WidgetSize, showsRides: Boolean) {
        if (showsRides) {
            fillRide(views, 1, WidgetRide(strings.setupNeededTitle, "", "", "", SystemColors.widgetIdle))
            fillRide(views, 2, null)
        }
        if (!showsRides || size == WidgetSize.LARGE) {
            views.setTextViewText(R.id.widget_headline, strings.setupNeededTitle)
            views.setTextViewText(R.id.widget_detail, strings.setupNeededBody)
        }
    }

    private fun fillSummary(views: RemoteViews, snapshot: WidgetSnapshot, artwork: WidgetArtwork, size: WidgetSize, style: WidgetStyle) {
        when {
            size == WidgetSize.ROW && style == WidgetStyle.RIDES -> Unit
            size == WidgetSize.ROW && style == WidgetStyle.CHART -> {
                views.setTextViewText(R.id.widget_headline, snapshot.headline)
                val next = snapshot.rides.firstOrNull()
                views.setTextViewText(R.id.widget_detail, next?.let { "${it.name} · ${it.time} · ${it.rain}" }.orEmpty())
                views.setImageViewBitmap(R.id.widget_chart, artwork.pictures[Picture.CHART])
            }
            size == WidgetSize.LARGE -> {
                views.setTextViewText(R.id.widget_headline, snapshot.headline)
                views.setTextViewText(R.id.widget_detail, snapshot.chipLine)
                views.setImageViewBitmap(R.id.widget_chart, artwork.pictures[Picture.CHART_WIDE])
            }
            else -> {
                views.setTextViewText(R.id.widget_headline, snapshot.headline)
                views.setTextViewText(R.id.widget_detail, snapshot.ridesInline)
            }
        }
    }

    private fun fillRide(views: RemoteViews, number: Int, ride: WidgetRide?) {
        val (nameId, lineId, dotId) = if (number == 1) Triple(R.id.widget_ride1_name, R.id.widget_ride1_line, R.id.widget_ride1_dot)
        else Triple(R.id.widget_ride2_name, R.id.widget_ride2_line, R.id.widget_ride2_dot)
        if (ride == null) {
            views.setTextViewText(nameId, "")
            views.setTextViewText(lineId, "")
            views.setInt(dotId, "setColorFilter", 0)
            views.setInt(dotId, "setImageAlpha", 0)
            return
        }
        views.setTextViewText(nameId, "${ride.name} ${ride.time}".trim())
        views.setTextViewText(lineId, listOf(ride.rain, ride.temperature).filter { it.isNotBlank() }.joinToString(" · "))
        views.setInt(dotId, "setColorFilter", ride.riskColour)
        views.setInt(dotId, "setImageAlpha", 255)
    }

    fun badgeViews(context: Context, snapshot: WidgetSnapshot?, artwork: WidgetArtwork): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_badge)
        views.setOnClickPendingIntent(R.id.widget_root, openApp(context))
        val ring = artwork.pictures[Picture.RING]
            ?: WidgetArt.ring(context, iconFor(snapshot), snapshot?.accent ?: SystemColors.widgetIdle, 0, "")
        views.setImageViewBitmap(R.id.widget_badge, ring)
        return views
    }

    private fun openApp(context: Context): PendingIntent = MainActivity.openIntent(context, OPEN_FROM_WIDGET)
}

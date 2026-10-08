package nl.fietsweer.app.ui.screens.forecast

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch
import nl.fietsweer.app.data.ForecastState
import nl.fietsweer.app.data.Leg
import nl.fietsweer.app.data.LocalForecast
import nl.fietsweer.app.data.RouteForecast
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.domain.Commute
import nl.fietsweer.app.domain.Engine
import nl.fietsweer.app.domain.Timeline
import nl.fietsweer.app.ui.components.FillScreen
import nl.fietsweer.app.ui.components.InfoCard
import nl.fietsweer.app.ui.components.LoadingBlock
import nl.fietsweer.app.ui.components.NoForecastCard
import nl.fietsweer.app.ui.components.NoRouteState
import nl.fietsweer.app.ui.components.ScreenList
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.components.charts.ChartSeries
import nl.fietsweer.app.ui.components.charts.TimeChart
import nl.fietsweer.app.ui.components.charts.drawWindArrow
import nl.fietsweer.app.ui.theme.AppTheme
import nl.fietsweer.app.ui.theme.caption
import java.time.Instant
import java.time.ZoneId

private const val HOUR_MS = 60 * 60_000L
private const val SPAN_MS = 48 * HOUR_MS
private const val VISIBLE_MS = 8 * HOUR_MS
private const val AXIS_STEP_MS = 2 * HOUR_MS

private class RideBand(val leg: Leg, val departMs: Long, val arriveMs: Long)

private class WindSample(val timeMs: Long, val kmh: Double, val fromDegrees: Double)

private class Outlook(
    val series: ChartSeries,
    val rides: List<RideBand>,
    val nights: List<Pair<Long, Long>>,
    val wind: List<WindSample>,
    val place: String?
)

@Composable
fun ForecastScreen(
    settings: Settings,
    forecastState: ForecastState,
    contentPadding: PaddingValues,
    onRefresh: () -> Unit,
    onSetup: () -> Unit,
    onAskedLocation: () -> Unit
) {
    val strings = AppTheme.strings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val local by LocalForecast.state.collectAsState()

    if (!settings.hasRoute) {
        NoRouteState(onSetup)
        return
    }

    val refreshLocal = { scope.launch { LocalForecast.refresh(context, settings, strings.locale.language) } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refreshLocal() }
    LaunchedEffect(Unit) {
        if (!settings.askedLocation && !LocalForecast.hasPermission(context)) {
            onAskedLocation()
            permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshLocal() }

    val forecast = local?.forecast ?: forecastState.forecast
    val outlook = remember(forecast, settings, local) {
        forecast?.takeIf { it.hasModels }?.let { outlookFor(it, settings, local?.placeName) }
    }

    if (forecast == null || outlook == null) {
        ScreenList(contentPadding) {
            if (forecast == null) item { LoadingBlock(strings.updating) }
            else item { NoForecastCard(forecast, onRefresh) }
            if (forecastState.error != null) {
                item { InfoCard(strings.updateFailedTitle, strings.updateFailedBody, strings.retry, onAction = onRefresh) }
            }
            if (forecast != null) item { DailyOutlook(forecast) }
        }
        return
    }

    FillScreen(contentPadding, stretchIndex = 0, stretchMin = 160.dp) {
        OutlookCard(outlook, "${settings.home?.name ?: strings.home} → ${settings.work?.name ?: strings.work}")
        DailyOutlook(forecast)
    }
}

private fun outlookFor(forecast: RouteForecast, settings: Settings, place: String?): Outlook {
    val stepMs = Timeline.STEP_MINUTES * 60_000L
    val startMs = (System.currentTimeMillis() / stepMs) * stepMs
    val endMs = startMs + SPAN_MS
    val times = Timeline.times(startMs, endMs)
    val engine = Engine(forecast, settings)
    val planned = if (place != null) emptyList() else Commute.ridesBetween(settings, startMs, endMs)
    return Outlook(
        ChartSeries(startMs, endMs, times, Timeline.rainRate(forecast, times), Timeline.temperature(forecast, times)),
        planned.map { RideBand(it.leg, it.departureMs, engine.assess(it.departureMs, it.leg).arrivalMs) },
        nightsBetween(forecast, startMs, endMs),
        windBetween(forecast, startMs, endMs),
        place
    )
}

// Open-Meteo sends sunrise and sunset as unix seconds.
private fun nightsBetween(forecast: RouteForecast, startMs: Long, endMs: Long): List<Pair<Long, Long>> {
    val sunrises = forecast.daily["sunrise"]?.map { (it * 1000).toLong() }.orEmpty()
    val sunsets = forecast.daily["sunset"]?.map { (it * 1000).toLong() }.orEmpty()
    val edges = (sunsets.map { it to true } + sunrises.map { it to false }).sortedBy { it.first }
    val nights = ArrayList<Pair<Long, Long>>()
    var nightStart: Long? = if (edges.firstOrNull()?.second == false) startMs else null
    for ((timeMs, isSunset) in edges) {
        if (isSunset) nightStart = timeMs
        else nightStart?.let { nights += it to timeMs; nightStart = null }
    }
    nightStart?.let { nights += it to endMs }
    return nights.filter { it.second > startMs && it.first < endMs }
}

private fun windBetween(forecast: RouteForecast, startMs: Long, endMs: Long): List<WindSample> {
    val speeds = forecast.hourly["wind_speed_10m"] ?: return emptyList()
    val directions = forecast.hourly["wind_direction_10m"] ?: return emptyList()
    return forecast.hourlyTimes.indices
        .filter { forecast.hourlyTimes[it] in startMs..endMs && isEvenHour(forecast.hourlyTimes[it]) }
        .map { WindSample(forecast.hourlyTimes[it], speeds.getOrElse(it) { Double.NaN }, directions.getOrElse(it) { Double.NaN }) }
}

@Composable
private fun OutlookCard(outlook: Outlook, routeName: String) {
    val format = AppTheme.format
    val accents = AppTheme.accents
    val dayLine = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val plotColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
    val rideColors = outlook.rides.map { accents.forLeg(it.leg).copy(alpha = 0.12f) }
    val nightColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.3f)
    val windColor = MaterialTheme.colorScheme.onSurfaceVariant
    val windDisc = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.75f)
    val series = outlook.series

    SectionCard(contentPadding = 14) {
        TimeChart(
            series = series,
            visibleMs = VISIBLE_MS,
            homeMs = series.startMs,
            homeAt = 0f,
            axisStepMs = AXIS_STEP_MS,
            axisLabel = { timeMs -> if (isMidnight(timeMs)) format.dayShort(timeMs) else format.time(timeMs) },
            modifier = Modifier.weight(1f),
            background = { xOf ->
                drawRect(plotColor)
                for ((nightStart, nightEnd) in outlook.nights) {
                    val left = xOf(nightStart)
                    drawRect(nightColor, Offset(left, 0f), Size(xOf(nightEnd) - left, size.height))
                }
                outlook.rides.forEachIndexed { i, ride ->
                    val left = xOf(ride.departMs)
                    drawRoundRect(
                        color = rideColors[i],
                        topLeft = Offset(left, 0f),
                        size = Size(xOf(ride.arriveMs) - left, size.height),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
                var midnight = nextMidnight(series.startMs)
                while (midnight < series.endMs) {
                    val x = xOf(midnight)
                    drawLine(dayLine, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                    midnight = nextMidnight(midnight + 1)
                }
            },
            foreground = { xOf ->
                val disc = 11.dp.toPx()
                for (sample in outlook.wind) {
                    val centre = Offset(xOf(sample.timeMs), size.height - disc - 4.dp.toPx())
                    drawCircle(windDisc, disc, centre)
                    drawWindArrow(centre, sample.fromDegrees, sample.kmh, windColor, disc * 1.35f)
                }
            },
            overlay = { PlaceChip(outlook.place ?: routeName, if (outlook.place != null) Icons.Rounded.Place else Icons.AutoMirrored.Rounded.DirectionsBike) }
        )
    }
}

@Composable
private fun BoxScope.PlaceChip(name: String, icon: ImageVector) {
    Row(
        Modifier
            .align(Alignment.TopStart)
            .padding(6.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f), RoundedCornerShape(50))
            .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(name, style = MaterialTheme.typography.caption, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

private fun isEvenHour(timeMs: Long): Boolean =
    Instant.ofEpochMilli(timeMs).atZone(ZoneId.systemDefault()).hour % 2 == 0

private fun isMidnight(timeMs: Long): Boolean {
    val time = Instant.ofEpochMilli(timeMs).atZone(ZoneId.systemDefault())
    return time.hour == 0 && time.minute == 0
}

private fun nextMidnight(timeMs: Long): Long =
    Instant.ofEpochMilli(timeMs).atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1)
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

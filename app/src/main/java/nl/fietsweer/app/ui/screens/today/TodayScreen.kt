package nl.fietsweer.app.ui.screens.today

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.data.Coverage
import nl.fietsweer.app.data.ForecastState
import nl.fietsweer.app.data.RouteForecast
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.domain.AdviceText
import nl.fietsweer.app.domain.Commute
import nl.fietsweer.app.domain.Engine
import nl.fietsweer.app.domain.Jacket
import nl.fietsweer.app.domain.RideAssessment
import nl.fietsweer.app.ui.components.Caption
import nl.fietsweer.app.ui.components.ChipFlow
import nl.fietsweer.app.ui.components.InfoCard
import nl.fietsweer.app.ui.components.LegendDot
import nl.fietsweer.app.ui.components.LoadingBlock
import nl.fietsweer.app.ui.components.NoForecastCard
import nl.fietsweer.app.ui.components.NoRouteState
import nl.fietsweer.app.ui.components.ScreenList
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.components.charts.ChartBand
import nl.fietsweer.app.ui.components.charts.ChartPoint
import nl.fietsweer.app.ui.components.charts.PrecipTempChart
import nl.fietsweer.app.ui.theme.AppTheme

private const val DAY_MS = 24 * 60 * 60 * 1000L

@Composable
fun TodayScreen(
    settings: Settings,
    forecastState: ForecastState,
    nowTick: Long,
    contentPadding: PaddingValues,
    onRefresh: () -> Unit,
    onOpenForecast: () -> Unit,
    onSetup: () -> Unit
) {
    val strings = AppTheme.strings
    val format = AppTheme.format
    val accents = AppTheme.accents

    if (!settings.hasRoute) {
        NoRouteState(onSetup)
        return
    }

    val forecast = forecastState.forecast
    val engine = remember(forecast, settings) { forecast?.let { Engine(it, settings) } }
    val plannedRides = remember(settings, nowTick) {
        Commute.plannedRides(settings, Coverage.BOTH, nowTick, alertDaysOnly = true)
    }
    val rides = remember(engine, plannedRides) {
        engine?.let { plannedRides.map(it::assess) }.orEmpty()
    }
    val advice = remember(rides) { Jacket.forRides(rides, settings) }

    ScreenList(contentPadding) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        strings.appName,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        format.longDate(nowTick),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (forecastState.loading) {
                    CircularProgressIndicator(strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                } else {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Rounded.Refresh, strings.refresh)
                    }
                }
            }
        }

        if (forecastState.error != null) {
            item {
                InfoCard(
                    title = strings.updateFailedTitle,
                    body = if (forecast == null) strings.updateFailedBody else strings.staleNotice(format.time(forecast.fetchedAt)),
                    actionLabel = strings.retry,
                    accent = if (forecast == null) MaterialTheme.colorScheme.error else accents.uncertain,
                    onAction = onRefresh
                )
            }
        }

        if (forecast == null) {
            item { LoadingBlock(strings.updating) }
            return@ScreenList
        }

        if (!forecast.hasModels) {
            item { NoForecastCard(forecast, onRefresh) }
            return@ScreenList
        }

        item { HeroCard(advice, forecast.current) }

        for (ride in rides) {
            item(key = "ride-${ride.leg}-${ride.departureMs}") {
                RideCard(ride, engine, onOpenForecast)
            }
        }

        item {
            val points = remember(forecast, nowTick) { buildChartPoints(forecast, nowTick) }
            val charted = rides.filter { it.departureMs < nowTick + DAY_MS }
            val bands = remember(charted) {
                charted.map {
                    ChartBand(
                        it.departureMs, it.arrivalMs,
                        accents.forLeg(it.leg),
                        AdviceText.legName(it.leg, strings)
                    )
                }
            }
            SectionCard(title = strings.next24h) {
                PrecipTempChart(points, bands)
                Spacer(Modifier.height(10.dp))
                ChipFlow {
                    LegendDot(strings.precipitation, accents.rain)
                    LegendDot(strings.temperature, MaterialTheme.colorScheme.tertiary)
                    charted.forEach {
                        LegendDot(legendName(it, format.isToday(it.departureMs)), accents.forLeg(it.leg))
                    }
                }
            }
        }

        item { BestMomentCard(engine, plannedRides, nowTick, onOpenForecast) }

        item {
            Caption(strings.lastUpdated(format.time(forecast.fetchedAt)), Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun legendName(ride: RideAssessment, today: Boolean): String {
    val strings = AppTheme.strings
    val format = AppTheme.format
    val name = AdviceText.legName(ride.leg, strings)
    return if (today) name else "$name ${format.dayWord(ride.departureMs)}"
}

private fun buildChartPoints(
    forecast: RouteForecast,
    nowMs: Long
): List<ChartPoint> {
    val times = forecast.hourlyTimes
    val temps = forecast.hourly["temperature_2m"]
    val precipitation = forecast.hourly["precipitation"]
    if (times.isEmpty() || temps == null || precipitation == null) return emptyList()
    val startMs = nowMs - 60 * 60 * 1000L
    val endMs = nowMs + DAY_MS
    val points = ArrayList<ChartPoint>()
    for (i in times.indices) {
        val timeMs = times[i]
        if (timeMs < startMs || timeMs > endMs) continue
        points += ChartPoint(
            timeMs,
            precipitation.getOrElse(i) { Double.NaN },
            temps.getOrElse(i) { Double.NaN }
        )
    }
    return points
}

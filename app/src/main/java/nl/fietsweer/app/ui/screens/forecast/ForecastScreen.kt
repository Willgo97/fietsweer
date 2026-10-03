package nl.fietsweer.app.ui.screens.forecast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.data.ForecastState
import nl.fietsweer.app.data.Leg
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.domain.DryWindow
import nl.fietsweer.app.domain.Engine
import nl.fietsweer.app.domain.RideAssessment
import nl.fietsweer.app.ui.components.Caption
import nl.fietsweer.app.ui.components.Dot
import nl.fietsweer.app.ui.components.InfoCard
import nl.fietsweer.app.ui.components.LegendDot
import nl.fietsweer.app.ui.components.LoadingBlock
import nl.fietsweer.app.ui.components.NoForecastCard
import nl.fietsweer.app.ui.components.NoRouteState
import nl.fietsweer.app.ui.components.ScreenList
import nl.fietsweer.app.ui.components.ScreenTitle
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.components.SegmentedChoice
import nl.fietsweer.app.ui.components.SoftDivider
import nl.fietsweer.app.ui.components.charts.DepartureTimeline
import nl.fietsweer.app.ui.components.charts.ModelMatrix
import nl.fietsweer.app.ui.theme.AppTheme

@Composable
fun ForecastScreen(
    settings: Settings,
    forecastState: ForecastState,
    contentPadding: PaddingValues,
    onRefresh: () -> Unit,
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
    var leg by rememberSaveable { mutableStateOf(Leg.OUTBOUND) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    val engine = remember(forecast, settings) { forecast?.let { Engine(it, settings) } }
    val slots = remember(engine, leg) { engine?.scan(leg, 24 * 60).orEmpty() }
    val windows = remember(slots) { engine?.dryWindows(slots).orEmpty() }

    ScreenList(contentPadding) {
        item {
            ScreenTitle(strings.tabForecast)
        }

        item {
            SegmentedChoice(
                options = listOf(
                    Leg.OUTBOUND to "${settings.home?.name ?: strings.home} → ${settings.work?.name ?: strings.work}",
                    Leg.RETURN to "${settings.work?.name ?: strings.work} → ${settings.home?.name ?: strings.home}"
                ),
                selected = leg,
                onSelect = { leg = it; selectedIndex = 0 },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (forecast == null) {
            item { LoadingBlock(strings.updating) }
            if (forecastState.error != null) {
                item {
                    InfoCard(strings.updateFailedTitle, strings.updateFailedBody, strings.retry, onAction = onRefresh)
                }
            }
            return@ScreenList
        }

        if (slots.isEmpty()) {
            item { NoForecastCard(forecast, onRefresh) }
        } else {
            val index = selectedIndex.coerceIn(0, slots.size - 1)
            val selectedSlot = slots[index]

            item {
                SectionCard(title = strings.departureTimeline) {
                    DepartureTimeline(
                        slots = slots,
                        selectedIndex = index,
                        onSelect = { selectedIndex = it }
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LegendDot(strings.legendDry, accents.dry)
                        LegendDot(strings.legendMostlyDry, accents.mostlyDry)
                        LegendDot(strings.legendUncertain, accents.uncertain)
                    }
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LegendDot(strings.legendLikelyWet, accents.likelyWet)
                        LegendDot(strings.legendWet, accents.wet)
                    }
                    Spacer(Modifier.height(5.dp))
                    Caption(strings.legendNight)
                }
            }

            item { DetailCard(selectedSlot) }

            item {
                DryWindowsCard(windows) { best ->
                    selectedIndex = slots.indexOfFirst { it.departureMs == best.departureMs }.coerceAtLeast(0)
                }
            }

            item {
                SectionCard(title = strings.modelMatrix, subtitle = strings.modelMatrixSub) {
                    ModelMatrix(slots)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        LegendDot(strings.legendModelDry, accents.dry)
                        LegendDot(strings.legendModelWet, accents.wet)
                    }
                }
            }
        }

        item { DailyOutlook(forecast) }
        item { SourcesCard(forecast) }
    }
}

@Composable
private fun DryWindowsCard(windows: List<DryWindow>, onSelect: (RideAssessment) -> Unit) {
    val strings = AppTheme.strings
    val format = AppTheme.format
    val accents = AppTheme.accents

    SectionCard(title = strings.dryWindows) {
        if (windows.isEmpty()) {
            Text(
                strings.noDryWindows,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        windows.take(4).forEachIndexed { i, window ->
            if (i > 0) SoftDivider(Modifier.padding(vertical = 2.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(window.best) }
                    .padding(vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Dot(accents.forRisk(window.averageRisk), 11.dp)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "${format.time(window.from.departureMs)} – ${format.time(window.to.departureMs)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "${format.dayWord(window.from.departureMs)} · " +
                            strings.slackRoom(format.durationText(window.minutes)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    format.percent(window.averageRisk),
                    style = MaterialTheme.typography.titleSmall,
                    color = accents.forRisk(window.averageRisk)
                )
            }
        }
    }
}

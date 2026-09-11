package nl.fietsweer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.data.ForecastUi
import nl.fietsweer.app.domain.Bike
import nl.fietsweer.app.data.Leg
import nl.fietsweer.app.data.RouteForecast
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.data.SourceState
import nl.fietsweer.app.domain.Engine
import nl.fietsweer.app.domain.RideAssessment
import nl.fietsweer.app.domain.Sky
import nl.fietsweer.app.domain.WeatherCode
import nl.fietsweer.app.ui.components.DepartureTimeline
import nl.fietsweer.app.ui.components.EmptyState
import nl.fietsweer.app.ui.components.InfoCard
import nl.fietsweer.app.ui.components.LegendDot
import nl.fietsweer.app.ui.components.LoadingBlock
import nl.fietsweer.app.ui.components.ModelMatrix
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.components.SegmentedChoice
import nl.fietsweer.app.ui.theme.AppTheme
import kotlin.math.roundToInt
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

@Composable
fun ForecastScreen(
    settings: Settings,
    ui: ForecastUi,
    contentPadding: PaddingValues,
    onRefresh: () -> Unit,
    onSetup: () -> Unit
) {
    val t = AppTheme.txt
    val fmt = AppTheme.fmt
    val accents = AppTheme.accents

    if (!settings.ready) {
        EmptyState(
            Icons.Rounded.LocationOff, t.setupNeededTitle, t.setupNeededBody,
            t.welcomeStart, onSetup
        )
        return
    }

    val fc = ui.forecast
    var leg by rememberSaveable { mutableStateOf(Leg.OUTBOUND) }
    var selected by rememberSaveable { mutableIntStateOf(0) }

    val engine = remember(fc, settings) { fc?.let { Engine(it, settings) } }
    val slots = remember(engine, leg) { engine?.scan(leg, 24 * 60).orEmpty() }
    val windows = remember(slots) { engine?.windows(slots).orEmpty() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(t.tabForecast, style = MaterialTheme.typography.headlineMedium)
        }

        item {
            SegmentedChoice(
                options = listOf(
                    Leg.OUTBOUND to "${settings.home?.name ?: t.home} → ${settings.work?.name ?: t.work}",
                    Leg.RETURN to "${settings.work?.name ?: t.work} → ${settings.home?.name ?: t.home}"
                ),
                selected = leg,
                onSelect = { leg = it; selected = 0 },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (fc == null) {
            item { LoadingBlock(t.updating) }
            if (ui.error != null) {
                item {
                    InfoCard(t.updateFailedTitle, t.updateFailedBody, t.retry, onAction = onRefresh)
                }
            }
            return@LazyColumn
        }

        if (slots.isEmpty()) {
            item {
                InfoCard(
                    title = if (fc.allSourcesFailed) t.updateFailedTitle else t.noModelsTitle,
                    body = if (fc.allSourcesFailed) t.updateFailedBody else t.noModelsBody,
                    actionLabel = t.retry,
                    onAction = onRefresh
                )
            }
        } else {
            val index = selected.coerceIn(0, slots.size - 1)
            val current = slots[index]

            item {
                SectionCard(title = t.departureTimeline) {
                    DepartureTimeline(
                        slots = slots,
                        selectedIndex = index,
                        onSelect = { selected = it }
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LegendDot(t.legendDry, accents.dry)
                        LegendDot(t.legendMostlyDry, accents.mostlyDry)
                        LegendDot(t.legendUncertain, accents.uncertain)
                    }
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LegendDot(t.legendLikelyWet, accents.likelyWet)
                        LegendDot(t.legendWet, accents.wet)
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        t.legendNight,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { DetailCard(current) }

            item {
                SectionCard(title = t.dryWindows) {
                    if (windows.isEmpty()) {
                        Text(
                            t.noDryWindows,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        windows.take(4).forEachIndexed { i, w ->
                            if (i > 0) HorizontalDivider(Modifier.padding(vertical = 2.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selected = slots.indexOfFirst { it.departureMs == w.best.departureMs }
                                            .coerceAtLeast(0)
                                    }
                                    .padding(vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(11.dp)
                                        .background(accents.forRisk(w.avgRisk), CircleShape)
                                )
                                Spacer(Modifier.width(11.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "${fmt.time(w.from.departureMs)} – ${fmt.time(w.to.departureMs)}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "${fmt.dayWord(w.from.departureMs)} · " +
                                            t.slackRoom(fmt.durationText(w.minutes)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "${(w.avgRisk * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = accents.forRisk(w.avgRisk)
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(title = t.modelMatrix, subtitle = t.modelMatrixSub) {
                    ModelMatrix(slots)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        LegendDot(t.legendModelDry, accents.dry)
                        LegendDot(t.legendModelWet, accents.wet)
                    }
                }
            }
        }

        item { DailyOutlook(fc) }
        item { SourcesCard(fc) }
    }
}

// ---------------------------------------------------------------- detail card

@Composable
private fun DetailCard(r: RideAssessment) {
    val t = AppTheme.txt
    val fmt = AppTheme.fmt
    val accents = AppTheme.accents

    SectionCard(title = t.detailsFor(fmt.dayTime(r.departureMs))) {
        DetailRow(t.arrival, fmt.time(r.arrivalMs))
        DetailRow(
            t.verdict,
            "${r.riskPercent}% — ${fmt.riskWord(r.risk)}",
            accents.forRisk(r.risk)
        )
        DetailRow(t.modelsSeeingRain, "${r.modelsWetCount} / ${r.modelCount}")
        DetailRow(
            t.agreement,
            when {
                r.agreement > 0.7 -> t.agreeStrong
                r.agreement > 0.35 -> t.agreeSome
                else -> t.agreeSplit
            }
        )
        DetailRow(
            t.ensembleChance,
            r.ensembleProb?.let { "${(it * 100).roundToInt()}%" } ?: t.outOfRange
        )
        DetailRow(
            t.radarLabel,
            when {
                r.radarRisk == null -> t.radarBeyond
                r.radarRisk == 0.0 -> t.radarNoEcho
                else -> t.radarRain(fmt.mm(r.radarMaxMmh))
            }
        )
        DetailRow(t.expectedOnTheWay, t.avgMaxMm(fmt.mm2(r.avgMm), fmt.mm2(r.maxMm)))
        DetailRow(
            t.paceOnRoad,
            "${fmt.kmh(r.paceKmh)} ${t.speedUnit} · ${r.durationMin} min" +
                if (r.windMinutes != 0)
                    "  (${t.stillAirShort} " +
                        "${fmt.kmh(Bike.paceFor(r.distanceKm, r.stillAirDurationMin.toDouble()))} " +
                        "${t.speedUnit} · ${r.stillAirDurationMin} min)"
                else "",
            if (r.windMinutes > 1) accents.likelyWet
            else if (r.windMinutes < -1) accents.dry
            else null
        )
        DetailRow(
            t.windAndFeel,
            if (!r.hasConditions) t.unknown else
                "${fmt.kmh(r.windKmh)} ${t.speedUnit} ${fmt.windRelationWord(r.windRelation)} " +
                    "${fmt.compass(r.windFromDeg)} · ${t.feelsLike} ${fmt.temp(r.bikeFeelC)}"
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color? = null) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor ?: MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1.2f),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}

// -------------------------------------------------------------- daily outlook

@Composable
private fun DailyOutlook(fc: RouteForecast) {
    val t = AppTheme.txt
    val fmt = AppTheme.fmt
    val accents = AppTheme.accents
    val times = fc.dayTimes
    if (times.isEmpty()) return
    val max = fc.daily["temperature_2m_max"]
    val min = fc.daily["temperature_2m_min"]
    val sum = fc.daily["precipitation_sum"]
    val prob = fc.daily["precipitation_probability_max"]
    val code = fc.daily["weather_code"]

    val globalMin = min?.filter { !it.isNaN() }?.minOrNull() ?: 0.0
    val globalMax = max?.filter { !it.isNaN() }?.maxOrNull() ?: 20.0
    val span = (globalMax - globalMin).coerceAtLeast(1.0)

    SectionCard(title = t.dailyOutlook) {
        for (i in times.indices) {
            val lo = min?.getOrNull(i) ?: continue
            val hi = max?.getOrNull(i) ?: continue
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (i == 0) t.today.replaceFirstChar { it.uppercase() } else fmt.dayShort(times[i]),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(56.dp)
                )
                Text(
                    skyGlyph(WeatherCode.sky((code?.getOrNull(i) ?: 0.0).toInt())),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(30.dp)
                )
                Text(
                    "${(prob?.getOrNull(i) ?: 0.0).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = accents.rain,
                    modifier = Modifier.width(38.dp)
                )
                Text(
                    fmt.temp(lo),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(34.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .height(7.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                ) {
                    TempBar(
                        startF = ((lo - globalMin) / span).toFloat().coerceIn(0f, 1f),
                        endF = ((hi - globalMin) / span).toFloat().coerceIn(0f, 1f),
                        cLo = accents.forTemperature(lo),
                        cHi = accents.forTemperature(hi)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    fmt.temp(hi),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(34.dp)
                )
                Text(
                    if ((sum?.getOrNull(i) ?: 0.0) > 0.05) "${fmt.mm(sum!![i])}mm" else "",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(48.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun TempBar(startF: Float, endF: Float, cLo: Color, cHi: Color) {
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        val x0 = size.width * startF
        val x1 = (size.width * endF).coerceAtLeast(x0 + size.height)
        drawRoundRect(
            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                listOf(cLo, cHi), startX = x0, endX = x1
            ),
            topLeft = androidx.compose.ui.geometry.Offset(x0, 0f),
            size = androidx.compose.ui.geometry.Size(x1 - x0, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
        )
    }
}

private fun skyGlyph(sky: Sky): String = when (sky) {
    Sky.CLEAR -> "☀"
    Sky.PARTLY -> "⛅"
    Sky.CLOUDY -> "☁"
    Sky.FOG -> "🌫"
    Sky.DRIZZLE -> "🌦"
    Sky.RAIN -> "🌧"
    Sky.SHOWERS -> "🌦"
    Sky.SNOW -> "❄"
    Sky.THUNDER -> "⛈"
    Sky.UNKNOWN -> "·"
}

// -------------------------------------------------------------------- sources

@Composable
private fun SourcesCard(fc: RouteForecast) {
    val t = AppTheme.txt
    val accents = AppTheme.accents
    SectionCard(title = t.sources) {
        fc.sources.forEach { s ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(
                            when (s.state) {
                                SourceState.OK -> accents.dry
                                SourceState.EMPTY -> accents.uncertain
                                SourceState.FAILED -> accents.wet
                                SourceState.SKIPPED -> MaterialTheme.colorScheme.outline
                            },
                            CircleShape
                        )
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    s.label,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                if (s.note.isNotBlank()) {
                    Text(
                        s.note,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            t.aboutData,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

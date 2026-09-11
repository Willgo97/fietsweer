package nl.fietsweer.app.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.data.Coverage
import nl.fietsweer.app.data.ForecastUi
import nl.fietsweer.app.data.Leg
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.domain.Advice
import nl.fietsweer.app.domain.AdviceText
import nl.fietsweer.app.domain.Bike
import nl.fietsweer.app.domain.Engine
import nl.fietsweer.app.domain.Jacket
import nl.fietsweer.app.domain.Need
import nl.fietsweer.app.domain.RideAssessment
import nl.fietsweer.app.domain.WindRelation
import nl.fietsweer.app.notify.Commute
import nl.fietsweer.app.ui.components.ChartBand
import nl.fietsweer.app.ui.components.ChartPoint
import nl.fietsweer.app.ui.components.ChipFlow
import nl.fietsweer.app.ui.components.EmptyState
import nl.fietsweer.app.ui.components.InfoCard
import nl.fietsweer.app.ui.components.LegendDot
import nl.fietsweer.app.ui.components.LoadingBlock
import nl.fietsweer.app.ui.components.PrecipTempChart
import nl.fietsweer.app.ui.components.RainSparkline
import nl.fietsweer.app.ui.components.RiskRing
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.components.WindDial
import nl.fietsweer.app.ui.theme.AppTheme
import kotlin.math.abs

@Composable
fun TodayScreen(
    settings: Settings,
    ui: ForecastUi,
    nowTick: Long,
    contentPadding: PaddingValues,
    onRefresh: () -> Unit,
    onOpenForecast: () -> Unit,
    onSetup: () -> Unit
) {
    val t = AppTheme.txt
    val fmt = AppTheme.fmt
    val accents = AppTheme.accents

    if (!settings.ready) {
        EmptyState(
            icon = Icons.Rounded.LocationOff,
            title = t.setupNeededTitle,
            body = t.setupNeededBody,
            actionLabel = t.welcomeStart,
            onAction = onSetup
        )
        return
    }

    val fc = ui.forecast
    val engine = remember(fc, settings) { fc?.let { Engine(it, settings) } }
    val rides = remember(fc, settings, nowTick) {
        engine?.let { e ->
            Commute.plannedRides(settings, Coverage.BOTH, nowTick)
                .map { (leg, at) -> e.assess(at, leg) }
        }.orEmpty()
    }
    val advice = remember(rides) { Jacket.forRides(rides, settings) }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        t.appName,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        fmt.longDate(nowTick),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (ui.loading) {
                    CircularProgressIndicator(strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                } else {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Rounded.Refresh, t.refresh)
                    }
                }
            }
        }

        if (ui.error != null) {
            item {
                InfoCard(
                    title = t.updateFailedTitle,
                    body = if (fc == null) t.updateFailedBody else t.staleNotice(fmt.time(fc.fetchedAt)),
                    actionLabel = t.retry,
                    accent = if (fc == null) MaterialTheme.colorScheme.error else accents.uncertain,
                    onAction = onRefresh
                )
            }
        }

        if (fc == null) {
            item { LoadingBlock(t.updating) }
            return@LazyColumn
        }

        if (!fc.hasModels) {
            // Without models there is no verdict to give, and showing a green
            // "nothing needed" card here would be a confident lie.
            item {
                InfoCard(
                    title = if (fc.allSourcesFailed) t.updateFailedTitle else t.noModelsTitle,
                    body = if (fc.allSourcesFailed) t.updateFailedBody else t.noModelsBody,
                    actionLabel = t.retry,
                    onAction = onRefresh
                )
            }
            return@LazyColumn
        }

        item { HeroCard(advice) }

        items@ for (r in rides) {
            item(key = "ride-${r.leg}-${r.departureMs}") {
                RideCard(r, engine, onOpenForecast)
            }
        }

        item { NowCard(fc.current) }

        item {
            val points = remember(fc, nowTick) { buildChartPoints(fc, nowTick) }
            val bands = remember(rides) {
                rides.map {
                    ChartBand(
                        it.departureMs, it.arrivalMs,
                        if (it.leg == Leg.OUTBOUND) accents.rain else accents.warm,
                        AdviceText.legName(it.leg, t)
                    )
                }
            }
            SectionCard(title = t.next24h) {
                PrecipTempChart(points, bands)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    LegendDot(t.precipitation, accents.rain)
                    LegendDot(t.temperature, MaterialTheme.colorScheme.tertiary)
                    rides.forEach {
                        LegendDot(
                            AdviceText.legName(it.leg, t),
                            if (it.leg == Leg.OUTBOUND) accents.rain else accents.warm
                        )
                    }
                }
            }
        }

        item {
            BestMomentCard(engine, rides.firstOrNull()?.leg ?: Leg.OUTBOUND, onOpenForecast)
        }

        item {
            Text(
                t.lastUpdated(fmt.time(fc.fetchedAt)),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// ------------------------------------------------------------------ hero card

@Composable
private fun HeroCard(advice: Advice) {
    val t = AppTheme.txt
    val accents = AppTheme.accents

    val (c1, c2) = when {
        advice.rain == Need.YES && advice.warm == Need.YES -> accents.rain to accents.warm
        advice.rain == Need.YES -> accents.rain to accents.cold
        advice.warm == Need.YES -> accents.warm to accents.heat
        advice.anythingNeeded -> accents.uncertain to accents.warm
        else -> accents.dry to accents.mostlyDry
    }

    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(c1, c2)))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    Color.White.copy(alpha = 0.09f),
                    radius = size.height * 0.85f,
                    center = Offset(size.width * 1.02f, size.height * 0.12f)
                )
                drawCircle(
                    Color.White.copy(alpha = 0.07f),
                    radius = size.height * 0.5f,
                    center = Offset(size.width * 0.86f, size.height * 0.92f)
                )
            }
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (advice.anythingNeeded) Icons.Rounded.Checkroom else Icons.AutoMirrored.Rounded.DirectionsBike,
                        null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        (if (advice.anythingNeeded) t.takeWithYou else t.nothingNeeded).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    AdviceText.headline(advice, t),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                if (!advice.anythingNeeded && advice.temperatureKnown) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        t.adviceNoneSub,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                val chips = AdviceText.chips(advice, t)
                if (chips.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    ChipFlow {
                        chips.forEach { (label, strong) ->
                            HeroChip(label, strong, iconFor(label, t))
                        }
                    }
                }
            }
        }
    }
}

private val HERO_CHIP_INK = Color(0xFF10171C)

@Composable
private fun HeroChip(label: String, strong: Boolean, icon: ImageVector?) {
    Surface(
        shape = CircleShape,
        color = if (strong) Color.White else Color.White.copy(alpha = 0.22f)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The chip sits on the coloured hero, so its ink is fixed rather
            // than taken from the scheme: a white pill needs dark text in both
            // light and dark mode.
            val fg = if (strong) HERO_CHIP_INK else Color.White
            if (icon != null) {
                Icon(icon, null, tint = fg, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                fontWeight = if (strong) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun iconFor(label: String, t: nl.fietsweer.app.domain.Txt): ImageVector? = when (label) {
    t.chipRainJacket, t.chipHeavy -> Icons.Rounded.Umbrella
    t.chipWarmJacket, t.chipGloves, t.chipHat -> Icons.Rounded.Checkroom
    t.chipFrost -> Icons.Rounded.AcUnit
    t.chipWindy -> Icons.Rounded.Air
    t.chipHot -> Icons.Rounded.WaterDrop
    else -> null
}

// ------------------------------------------------------------------ ride card

@Composable
private fun RideCard(r: RideAssessment, engine: Engine?, onOpen: () -> Unit) {
    val t = AppTheme.txt
    val fmt = AppTheme.fmt
    val accents = AppTheme.accents
    val riskColor = accents.forRisk(r.risk)

    SectionCard(
        modifier = Modifier.clickable(onClick = onOpen)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    AdviceText.legName(r.leg, t).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${fmt.time(r.departureMs)} → ${fmt.time(r.arrivalMs)}",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    "${fmt.dayWord(r.departureMs)} · ${fmt.km(r.distanceKm)} km · ${r.durationMin} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    fmt.riskWord(r.risk),
                    style = MaterialTheme.typography.titleMedium,
                    color = riskColor
                )
                if (abs(r.windMinutes) >= 2) {
                    Spacer(Modifier.height(4.dp))
                    val slower = r.windMinutes > 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Air, null,
                            tint = if (slower) accents.likelyWet else accents.dry,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            t.windTimeLine(
                                "${if (slower) "+" else "−"}${abs(r.windMinutes)} min",
                                fmt.windRelationWord(r.windRelation),
                                fmt.kmh(r.paceKmh),
                                "${fmt.kmh(Bike.paceFor(r.distanceKm, r.stillAirDurationMin.toDouble()))} " +
                                    t.speedUnit
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (slower) accents.likelyWet else accents.dry
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            RiskRing(r.risk, riskColor, caption = t.chanceOfRain, diameter = 86)
        }

        Spacer(Modifier.height(14.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiniStat(
                Icons.Rounded.Thermostat,
                fmt.temp(r.bikeFeelC),
                t.onTheBike,
                accents.forTemperature(r.bikeFeelC),
                Modifier.weight(1f)
            )
            MiniStat(
                Icons.Rounded.Air,
                "${fmt.kmh(r.windKmh)} ${t.speedUnit}",
                fmt.windRelationWord(r.windRelation),
                when (r.windRelation) {
                    WindRelation.HEAD -> accents.likelyWet
                    WindRelation.TAIL -> accents.dry
                    WindRelation.CROSS -> accents.uncertain
                },
                Modifier.weight(1f)
            )
            WindDial(r.travelBearing, r.windFromDeg, r.windKmh, diameter = 62)
        }

        if (engine != null) {
            val profile = remember(r.departureMs, r.leg, r.durationMin) {
                engine.ridePrecipProfile(r.departureMs, r.leg, r.durationMin)
            }
            if (profile.any { it > 0.03 }) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "${t.expectedRain}: ${fmt.mm2(r.avgMm)} mm",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                RainSparkline(profile, accents.rain)
            }
        }
    }
}

@Composable
private fun MiniStat(
    icon: ImageVector,
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(32.dp)
                .background(accent.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(value, style = MaterialTheme.typography.titleSmall)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

// ------------------------------------------------------------------- now card

@Composable
private fun NowCard(current: Map<String, Double>) {
    if (current.isEmpty()) return
    val t = AppTheme.txt
    val fmt = AppTheme.fmt
    val accents = AppTheme.accents
    val temp = current["temperature_2m"] ?: Double.NaN
    val app = current["apparent_temperature"] ?: Double.NaN
    val wind = current["wind_speed_10m"] ?: Double.NaN
    val dir = current["wind_direction_10m"] ?: Double.NaN
    val precip = current["precipitation"] ?: 0.0

    SectionCard(title = t.rightNow) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                fmt.temp(temp),
                style = MaterialTheme.typography.displaySmall,
                color = accents.forTemperature(temp)
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${t.feelsLike} ${fmt.temp(app)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "${t.wind} ${fmt.kmh(wind)} ${t.speedUnit} ${fmt.compass(dir)}" +
                        if (precip > 0.02) " · ${fmt.mm(precip)} mm" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ------------------------------------------------------------ best departure

@Composable
private fun BestMomentCard(engine: Engine?, leg: Leg, onOpen: () -> Unit) {
    if (engine == null) return
    val t = AppTheme.txt
    val fmt = AppTheme.fmt
    val accents = AppTheme.accents

    val slots = remember(engine, leg) { engine.scan(leg, 24 * 60) }
    if (slots.isEmpty()) return
    val windows = remember(slots) { engine.windows(slots) }
    val best = windows.firstOrNull()

    SectionCard(
        title = t.bestMomentTitle,
        modifier = Modifier.clickable(onClick = onOpen)
    ) {
        if (best == null) {
            val least = slots.minByOrNull { it.risk }!!
            Text(t.bestMomentNone, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "${fmt.dayTime(least.departureMs)} · ${least.riskPercent}% ${t.chanceOfRain}",
                style = MaterialTheme.typography.titleMedium,
                color = accents.forRisk(least.risk)
            )
        } else {
            val b = best.best
            val soon = b.departureMs - System.currentTimeMillis() < 20 * 60 * 1000L
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(11.dp)
                        .background(accents.forRisk(b.risk), CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (soon) t.leaveNow else fmt.dayTime(b.departureMs),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "${fmt.time(best.from.departureMs)} – ${fmt.time(best.to.departureMs)} · " +
                    t.slackRoom(fmt.durationText(best.minutes)) + " · " +
                    "${b.riskPercent}% ${t.chanceOfRain}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Schedule, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                t.departureTimeline,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ---------------------------------------------------------------- chart data

private fun buildChartPoints(
    fc: nl.fietsweer.app.data.RouteForecast,
    nowMs: Long
): List<ChartPoint> {
    val times = fc.hourTimes
    val temp = fc.hourly["temperature_2m"]
    val precip = fc.hourly["precipitation"]
    if (times.isEmpty() || temp == null || precip == null) return emptyList()
    val start = nowMs - 60 * 60 * 1000L
    val end = nowMs + 24 * 60 * 60 * 1000L
    val out = ArrayList<ChartPoint>()
    for (i in times.indices) {
        val ts = times[i]
        if (ts < start || ts > end) continue
        out += ChartPoint(
            ts,
            precip.getOrElse(i) { Double.NaN },
            temp.getOrElse(i) { Double.NaN }
        )
    }
    return out
}

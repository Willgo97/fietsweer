package nl.fietsweer.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.HorizontalDivider
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
import nl.fietsweer.app.domain.Layer
import nl.fietsweer.app.domain.Need
import nl.fietsweer.app.domain.RideAssessment
import nl.fietsweer.app.domain.WindRelation
import nl.fietsweer.app.notify.Commute
import nl.fietsweer.app.notify.Planned
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
    // Each leg drops off this screen once its slack has run out, so after the
    // morning ride the card on show is already tomorrow's.
    val planned = remember(settings, nowTick) {
        Commute.plannedRides(settings, Coverage.BOTH, nowTick)
    }
    val rides = remember(engine, planned) {
        engine?.let { e -> planned.map { e.assess(it.departureMs, it.leg) } }.orEmpty()
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

        item { HeroCard(advice, fc.current) }

        for (r in rides) {
            item(key = "ride-${r.leg}-${r.departureMs}") {
                RideCard(r, engine, onOpenForecast)
            }
        }

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
                ChipFlow {
                    LegendDot(t.precipitation, accents.rain)
                    LegendDot(t.temperature, MaterialTheme.colorScheme.tertiary)
                    rides.forEach {
                        LegendDot(
                            legendName(it, fmt.isToday(it.departureMs)),
                            if (it.leg == Leg.OUTBOUND) accents.rain else accents.warm
                        )
                    }
                }
            }
        }

        item { BestMomentCard(engine, planned, nowTick, onOpenForecast) }

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

@Composable
private fun legendName(r: RideAssessment, today: Boolean): String {
    val t = AppTheme.txt
    val fmt = AppTheme.fmt
    val name = AdviceText.legName(r.leg, t)
    return if (today) name else "$name ${fmt.dayWord(r.departureMs)}"
}

// ------------------------------------------------------------------ hero card

@Composable
private fun HeroCard(advice: Advice, current: Map<String, Double>) {
    val t = AppTheme.txt
    val accents = AppTheme.accents

    val (c1, c2) = when {
        advice.rain == Need.YES && advice.layer == Layer.WINTER -> accents.rain to accents.heat
        advice.rain == Need.YES -> accents.rain to accents.cold
        advice.layer == Layer.WINTER -> accents.heat to accents.warm
        advice.layer == Layer.VEST -> accents.warm to accents.uncertain
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
                // What it is doing outside right now sits in the same card: the
                // advice and the thermometer answer one question together.
                if (hasNow(current)) {
                    Spacer(Modifier.height(16.dp))
                    NowStrip(current)
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
    t.chipVest, t.chipWinter, t.chipGloves, t.chipHat -> Icons.Rounded.Checkroom
    t.chipFrost -> Icons.Rounded.AcUnit
    t.chipWindy -> Icons.Rounded.Air
    t.chipHot -> Icons.Rounded.WaterDrop
    else -> null
}

// -------------------------------------------------------------- now, in-hero

private fun hasNow(current: Map<String, Double>): Boolean =
    current["temperature_2m"] != null || current["wind_speed_10m"] != null

@Composable
private fun NowStrip(current: Map<String, Double>) {
    val t = AppTheme.txt
    val fmt = AppTheme.fmt
    val temp = current["temperature_2m"] ?: Double.NaN
    val app = current["apparent_temperature"] ?: Double.NaN
    val wind = current["wind_speed_10m"] ?: Double.NaN
    val dir = current["wind_direction_10m"] ?: Double.NaN
    val precip = current["precipitation"] ?: 0.0

    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.18f)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                fmt.temp(temp),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    t.rightNow.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                if (!app.isNaN()) {
                    Text(
                        "${t.feelsLike} ${fmt.temp(app)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
                val line = buildString {
                    if (!wind.isNaN()) {
                        append("${t.wind} ${fmt.kmh(wind)} ${t.speedUnit} ${fmt.compass(dir)}")
                    }
                    if (precip > 0.02) {
                        if (isNotEmpty()) append(" · ")
                        append("${fmt.mm(precip)} mm")
                    }
                }
                if (line.isNotEmpty()) {
                    Text(
                        line,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ ride card

/**
 * Which day a ride falls on, spelled out. Once a leg has rolled over you are
 * looking at tomorrow while standing in today's weather, so the day cannot be
 * left to a footnote.
 */
@Composable
private fun DayBadge(ms: Long) {
    val fmt = AppTheme.fmt
    val today = fmt.isToday(ms)
    Surface(
        shape = CircleShape,
        color = if (today) MaterialTheme.colorScheme.surfaceContainerHighest
        else MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Text(
            fmt.dayWord(ms).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (today) FontWeight.Medium else FontWeight.Bold,
            color = if (today) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun LegHeader(leg: Leg, departureMs: Long) {
    val t = AppTheme.txt
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            AdviceText.legName(leg, t).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        DayBadge(departureMs)
    }
}

@Composable
private fun RideCard(r: RideAssessment, engine: Engine?, onOpen: () -> Unit) {
    val t = AppTheme.txt
    val fmt = AppTheme.fmt
    val accents = AppTheme.accents
    val riskColor = accents.forRisk(r.risk)
    val today = fmt.isToday(r.departureMs)

    SectionCard(
        modifier = Modifier.clickable(onClick = onOpen),
        border = if (today) null
        else BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                LegHeader(r.leg, r.departureMs)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${fmt.time(r.departureMs)} → ${fmt.time(r.arrivalMs)}",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    "${fmt.km(r.distanceKm)} km · ${r.durationMin} min",
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

// ------------------------------------------------------------ best departure

@Composable
private fun BestMomentCard(
    engine: Engine?,
    planned: List<Planned>,
    nowTick: Long,
    onOpen: () -> Unit
) {
    if (engine == null || planned.isEmpty()) return
    val t = AppTheme.txt

    SectionCard(
        title = t.bestMomentTitle,
        subtitle = t.bestMomentSub,
        modifier = Modifier.clickable(onClick = onOpen)
    ) {
        planned.forEachIndexed { i, p ->
            if (i > 0) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                Spacer(Modifier.height(14.dp))
            }
            DepartureWindow(engine, p, nowTick)
        }
        Spacer(Modifier.height(14.dp))
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

/** The slack around one planned departure, scored quarter by quarter. */
@Composable
private fun DepartureWindow(engine: Engine, p: Planned, nowTick: Long) {
    val t = AppTheme.txt
    val fmt = AppTheme.fmt
    val accents = AppTheme.accents

    // Never offer a departure that has already gone by; round up to the grid so
    // the list only changes once a quarter rather than once a minute.
    val grid = Engine.GRID_MIN * 60_000L
    val from = maxOf(p.earliestMs, ((nowTick + grid - 1) / grid) * grid)
    val slots = remember(engine, p, from) {
        engine.scanWindow(p.leg, from, p.latestMs).ifEmpty {
            if (p.departureMs in from..p.latestMs) listOf(engine.assess(p.departureMs, p.leg))
            else emptyList()
        }
    }

    LegHeader(p.leg, p.departureMs)
    Spacer(Modifier.height(6.dp))

    if (slots.isEmpty()) {
        Text(
            t.bestMomentNoSlots,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    // A couple of percentage points are not worth shifting your day for, so of
    // everything within reach of the driest slot the one nearest the time you
    // actually planned wins. Only a clearly better slot pulls you away from it.
    val floor = slots.minOf { it.risk }
    val best = slots.filter { it.risk <= floor + 0.05 }
        .minBy { abs(it.departureMs - p.departureMs) }
    val deltaMin = ((best.departureMs - p.departureMs) / 60_000L).toInt()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                fmt.time(best.departureMs),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                when {
                    deltaMin == 0 -> t.onPlannedTime
                    deltaMin < 0 -> t.minutesEarlier(-deltaMin)
                    else -> t.minutesLater(deltaMin)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${best.riskPercent}%",
                style = MaterialTheme.typography.titleLarge,
                color = accents.forRisk(best.risk)
            )
            Text(
                t.chanceOfRain,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (slots.size > 1) {
        Spacer(Modifier.height(10.dp))
        DepartureStrip(slots, best)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                fmt.time(slots.first().departureMs),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                fmt.time(slots.last().departureMs),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** One block per quarter of the window, the chosen one at full strength. */
@Composable
private fun DepartureStrip(slots: List<RideAssessment>, best: RideAssessment) {
    val accents = AppTheme.accents
    Row(
        Modifier
            .fillMaxWidth()
            .height(26.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        for (s in slots) {
            val chosen = s.departureMs == best.departureMs
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(7.dp))
                    .background(accents.forRisk(s.risk).copy(alpha = if (chosen) 1f else 0.32f)),
                contentAlignment = Alignment.Center
            ) {
                if (chosen) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    )
                }
            }
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

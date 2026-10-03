package nl.fietsweer.app.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.domain.Engine
import nl.fietsweer.app.domain.PlannedRide
import nl.fietsweer.app.domain.RideAssessment
import nl.fietsweer.app.ui.components.Caption
import nl.fietsweer.app.ui.components.Dot
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.components.SoftDivider
import nl.fietsweer.app.ui.theme.AppTheme
import kotlin.math.abs

@Composable
internal fun BestMomentCard(
    engine: Engine?,
    plannedRides: List<PlannedRide>,
    nowTick: Long,
    onOpen: () -> Unit
) {
    if (engine == null || plannedRides.isEmpty()) return
    val strings = AppTheme.strings

    SectionCard(
        title = strings.bestMomentTitle,
        subtitle = strings.bestMomentSub,
        modifier = Modifier.clickable(onClick = onOpen)
    ) {
        plannedRides.forEachIndexed { i, ride ->
            if (i > 0) {
                Spacer(Modifier.height(14.dp))
                SoftDivider()
                Spacer(Modifier.height(14.dp))
            }
            DepartureWindow(engine, ride, nowTick)
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
                strings.departureTimeline,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DepartureWindow(engine: Engine, planned: PlannedRide, nowTick: Long) {
    val strings = AppTheme.strings
    val format = AppTheme.format
    val accents = AppTheme.accents

    val gridMs = Engine.GRID_MINUTES * 60_000L
    val firstSlotMs = maxOf(planned.earliestMs, ((nowTick + gridMs - 1) / gridMs) * gridMs)
    val slots = remember(engine, planned, firstSlotMs) {
        engine.scanWindow(planned.leg, firstSlotMs, planned.latestMs).ifEmpty {
            if (planned.departureMs in firstSlotMs..planned.latestMs) listOf(engine.assess(planned.departureMs, planned.leg))
            else emptyList()
        }
    }

    LegHeader(planned.leg, planned.departureMs)
    Spacer(Modifier.height(6.dp))

    if (slots.isEmpty()) {
        Text(
            strings.bestMomentNoSlots,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val lowestRisk = slots.minOf { it.risk }
    val best = slots.filter { it.risk <= lowestRisk + 0.05 }
        .minBy { abs(it.departureMs - planned.departureMs) }
    val shiftMinutes = ((best.departureMs - planned.departureMs) / 60_000L).toInt()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                format.time(best.departureMs),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                when {
                    shiftMinutes == 0 -> strings.onPlannedTime
                    shiftMinutes < 0 -> strings.minutesEarlier(-shiftMinutes)
                    else -> strings.minutesLater(shiftMinutes)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                format.percent(best.risk),
                style = MaterialTheme.typography.titleLarge,
                color = accents.forRisk(best.risk)
            )
            Caption(strings.chanceOfRain)
        }
    }

    if (slots.size > 1) {
        Spacer(Modifier.height(10.dp))
        DepartureStrip(slots, best)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Caption(format.time(slots.first().departureMs))
            Caption(format.time(slots.last().departureMs))
        }
    }
}

@Composable
private fun DepartureStrip(slots: List<RideAssessment>, best: RideAssessment) {
    val accents = AppTheme.accents
    Row(
        Modifier
            .fillMaxWidth()
            .height(26.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        for (slot in slots) {
            val chosen = slot.departureMs == best.departureMs
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(7.dp))
                    .background(accents.forRisk(slot.risk).copy(alpha = if (chosen) 1f else 0.32f)),
                contentAlignment = Alignment.Center
            ) {
                if (chosen) Dot(Color.White.copy(alpha = 0.9f), 6.dp)
            }
        }
    }
}

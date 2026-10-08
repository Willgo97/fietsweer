package nl.fietsweer.app.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.fietsweer.app.data.Leg
import nl.fietsweer.app.domain.AdviceText
import nl.fietsweer.app.domain.RideAssessment
import nl.fietsweer.app.domain.WindRelation
import nl.fietsweer.app.ui.components.Pill
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.components.SectionLabel
import nl.fietsweer.app.ui.theme.AppTheme
import nl.fietsweer.app.ui.theme.caption
import kotlin.math.abs

@Composable
internal fun RideCard(
    ride: RideAssessment,
    window: DepartureWindow?,
    modifier: Modifier = Modifier
) {
    val strings = AppTheme.strings
    val format = AppTheme.format
    val accents = AppTheme.accents
    val riskColor = accents.forRisk(ride.risk)
    val rainLabel = if (ride.averageMm >= 0.05)
        "${strings.chanceOfRainShort} ${format.millimetres(ride.averageMm)} mm"
    else strings.chanceOfRainShort

    SectionCard(
        modifier = modifier,
        contentPadding = 14
    ) {
        LegHeader(ride.leg, ride.departureMs) {
            Text(
                format.riskWord(ride.risk),
                style = MaterialTheme.typography.titleSmall,
                color = riskColor,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = MaterialTheme.typography.titleSmall.fontSize),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${format.time(ride.departureMs)} → ${format.time(ride.arrivalMs)}",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.width(8.dp))
            Text(
                strings.minutesShort(ride.durationMinutes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            if (abs(ride.windDelayMinutes) >= 2) {
                val slower = ride.windDelayMinutes > 0
                Spacer(Modifier.width(6.dp))
                Text(
                    (if (slower) "+" else "−") + strings.minutesShort(abs(ride.windDelayMinutes)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (slower) accents.likelyWet else accents.dry,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiniStat(
                Icons.Rounded.Thermostat,
                format.temp(ride.bikeFeelC),
                strings.onTheBike,
                accents.forTemperature(ride.bikeFeelC),
                Modifier.weight(1f)
            )
            MiniStat(
                Icons.Rounded.Air,
                format.speedWithUnit(ride.windKmh),
                format.windRelationWord(ride.windRelation),
                when (ride.windRelation) {
                    WindRelation.HEAD -> accents.likelyWet
                    WindRelation.TAIL -> accents.dry
                    WindRelation.CROSS -> accents.uncertain
                },
                Modifier.weight(1f)
            )
            MiniStat(
                Icons.Rounded.Umbrella,
                format.percent(ride.risk),
                rainLabel,
                riskColor,
                Modifier.weight(1f)
            )
        }

        if (window != null) DepartureChart(window, Modifier.weight(1f))
    }
}

@Composable
internal fun LegHeader(leg: Leg, departureMs: Long, trailing: @Composable () -> Unit = {}) {
    val strings = AppTheme.strings
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SectionLabel(AdviceText.legName(leg, strings))
        Spacer(Modifier.width(8.dp))
        DayBadge(departureMs)
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun DayBadge(departureMs: Long) {
    val format = AppTheme.format
    val today = format.isToday(departureMs)
    Pill(
        format.dayWord(departureMs).uppercase(),
        containerColor = if (today) MaterialTheme.colorScheme.surfaceContainerHighest
        else MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = if (today) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onTertiaryContainer,
        fontWeight = FontWeight.Bold
    )
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
        Spacer(Modifier.width(6.dp))
        Column {
            Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(
                label,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = MaterialTheme.typography.caption.fontSize)
            )
        }
    }
}

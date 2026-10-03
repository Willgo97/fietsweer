package nl.fietsweer.app.ui.screens.forecast

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.domain.Bike
import nl.fietsweer.app.domain.RideAssessment
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.components.SoftDivider
import nl.fietsweer.app.ui.theme.AppTheme

@Composable
internal fun DetailCard(ride: RideAssessment) {
    val strings = AppTheme.strings
    val format = AppTheme.format
    val accents = AppTheme.accents

    SectionCard(title = strings.detailsFor(format.dayTime(ride.departureMs))) {
        DetailRow(strings.arrival, format.time(ride.arrivalMs))
        DetailRow(
            strings.verdict,
            "${format.percent(ride.risk)} — ${format.riskWord(ride.risk)}",
            accents.forRisk(ride.risk)
        )
        DetailRow(strings.modelsSeeingRain, "${ride.modelsWetCount} / ${ride.modelCount}")
        DetailRow(
            strings.agreement,
            when {
                ride.agreement > 0.7 -> strings.agreeStrong
                ride.agreement > 0.35 -> strings.agreeSome
                else -> strings.agreeSplit
            }
        )
        DetailRow(
            strings.ensembleChance,
            ride.ensembleProbability?.let { format.percent(it) } ?: strings.outOfRange
        )
        DetailRow(
            strings.radarLabel,
            when {
                ride.radarRisk == null -> strings.radarBeyond
                ride.radarRisk == 0.0 -> strings.radarNoEcho
                else -> strings.radarRain(format.millimetres(ride.radarMaxMmh))
            }
        )
        DetailRow(strings.expectedOnTheWay, strings.avgMaxMm(format.millimetresPrecise(ride.averageMm), format.millimetresPrecise(ride.maxMm)))
        DetailRow(
            strings.paceOnRoad,
            "${format.speedWithUnit(ride.paceKmh)} · ${strings.minutesShort(ride.durationMinutes)}" +
                if (ride.windDelayMinutes != 0) {
                    val stillAirPace = Bike.paceFor(ride.distanceKm, ride.stillAirDurationMinutes.toDouble())
                    "  (${strings.stillAirShort} ${format.speedWithUnit(stillAirPace)} · " +
                        "${strings.minutesShort(ride.stillAirDurationMinutes)})"
                } else "",
            if (ride.windDelayMinutes > 1) accents.likelyWet
            else if (ride.windDelayMinutes < -1) accents.dry
            else null
        )
        DetailRow(
            strings.windAndFeel,
            if (!ride.hasConditions) strings.unknown else
                "${format.speedWithUnit(ride.windKmh)} ${format.windRelationWord(ride.windRelation)} " +
                    "${format.compass(ride.windFromDeg)} · ${strings.feelsLike} ${format.temp(ride.bikeFeelC)}"
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color? = null) {
    Column {
        SoftDivider()
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
                textAlign = TextAlign.End
            )
        }
    }
}

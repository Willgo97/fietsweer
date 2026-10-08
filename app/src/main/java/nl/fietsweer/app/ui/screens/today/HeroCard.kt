package nl.fietsweer.app.ui.screens.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.fietsweer.app.domain.Advice
import nl.fietsweer.app.domain.AdviceChip
import nl.fietsweer.app.domain.AdviceText
import nl.fietsweer.app.domain.ChipKind
import nl.fietsweer.app.domain.Layer
import nl.fietsweer.app.domain.Need
import nl.fietsweer.app.ui.components.ChipFlow
import nl.fietsweer.app.ui.components.SectionLabel
import nl.fietsweer.app.ui.components.WeatherBackdrop
import nl.fietsweer.app.ui.components.WeatherEffect
import nl.fietsweer.app.ui.theme.AppTheme

@Composable
internal fun HeroCard(advice: Advice, current: Map<String, Double>) {
    val strings = AppTheme.strings
    val accents = AppTheme.accents

    val (gradientStart, gradientEnd) = when {
        advice.rain == Need.YES && advice.layer == Layer.WINTER -> accents.rain to accents.heat
        advice.rain == Need.YES -> accents.rain to accents.cold
        advice.layer == Layer.WINTER -> accents.heat to accents.warm
        advice.layer == Layer.VEST -> accents.warm to accents.uncertain
        advice.anythingNeeded -> accents.uncertain to accents.warm
        else -> accents.dry to accents.mostlyDry
    }

    // Try-out: a long press steps through every effect so each can be judged.
    var preview by remember { mutableStateOf<WeatherEffect?>(null) }
    val effect = preview ?: WeatherEffect.forCurrent(current)
    var cardCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var stripCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val stripBounds = cardCoordinates?.let { card ->
        stripCoordinates?.takeIf { it.isAttached }?.let { card.localBoundingBoxOf(it) }
    }

    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(gradientStart, gradientEnd)))
                .onGloballyPositioned { cardCoordinates = it }
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = {
                        val entries = WeatherEffect.entries
                        preview = entries[((preview?.ordinal ?: -1) + 1) % entries.size]
                    })
                }
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
            WeatherBackdrop(effect, cutout = stripBounds)
            preview?.let {
                Text(
                    it.name.lowercase().replace('_', ' '),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 6.dp)
                )
            }
            Column(Modifier.padding(20.dp)) {
                AdviceText.coverage(advice, strings, AppTheme.format)?.let {
                    SectionLabel(it, color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.height(6.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (advice.anythingNeeded) Icons.Rounded.Checkroom else Icons.AutoMirrored.Rounded.DirectionsBike,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        AdviceText.headline(advice, strings),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 14.sp,
                            maxFontSize = MaterialTheme.typography.headlineMedium.fontSize
                        )
                    )
                }
                if (!advice.anythingNeeded && advice.temperatureKnown) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        strings.adviceNoneSub,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                val chips = AdviceText.chips(advice, strings)
                if (hasCurrentConditions(current)) {
                    Spacer(Modifier.height(14.dp))
                    NowStrip(current, Modifier.onGloballyPositioned { stripCoordinates = it }) { HeroChips(chips) }
                } else if (chips.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    ChipFlow { HeroChips(chips) }
                }
            }
        }
    }
}

@Composable
private fun HeroChips(chips: List<AdviceChip>) {
    chips.forEach { HeroChip(it.label, it.strong, iconFor(it.kind)) }
}

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
            val contentColor = if (strong) AppTheme.accents.heroInk else Color.White
            if (icon != null) {
                Icon(icon, null, tint = contentColor, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = if (strong) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

private fun iconFor(kind: ChipKind): ImageVector? = when (kind) {
    ChipKind.RAIN_JACKET, ChipKind.HEAVY_SHOWER -> Icons.Rounded.Umbrella
    ChipKind.VEST, ChipKind.WINTER, ChipKind.GLOVES, ChipKind.HAT -> Icons.Rounded.Checkroom
    ChipKind.FROST -> Icons.Rounded.AcUnit
    ChipKind.WINDY -> Icons.Rounded.Air
    ChipKind.HOT -> Icons.Rounded.WaterDrop
    ChipKind.DARK -> null
}

private fun hasCurrentConditions(current: Map<String, Double>): Boolean =
    current["temperature_2m"] != null || current["wind_speed_10m"] != null

@Composable
private fun NowStrip(
    current: Map<String, Double>,
    modifier: Modifier,
    trailing: @Composable ColumnScope.() -> Unit
) {
    val strings = AppTheme.strings
    val format = AppTheme.format
    val temp = current["temperature_2m"] ?: Double.NaN
    val apparent = current["apparent_temperature"] ?: Double.NaN
    val wind = current["wind_speed_10m"] ?: Double.NaN
    val windDirection = current["wind_direction_10m"] ?: Double.NaN
    val precip = current["precipitation"] ?: 0.0

    Surface(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.18f)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                format.temp(temp),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                SectionLabel(strings.rightNow, color = Color.White.copy(alpha = 0.8f))
                if (!apparent.isNaN()) {
                    Text(
                        "${strings.feelsLike} ${format.temp(apparent)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
                val line = buildString {
                    if (!wind.isNaN()) {
                        append("${strings.wind} ${format.speedWithUnit(wind)} ${format.compass(windDirection)}")
                    }
                    if (precip > 0.02) {
                        if (isNotEmpty()) append(" · ")
                        append("${format.millimetres(precip)} mm")
                    }
                }
                if (line.isNotEmpty()) {
                    Text(
                        line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                content = trailing
            )
        }
    }
}

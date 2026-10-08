package nl.fietsweer.app.ui.screens.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.BackHand
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Umbrella
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.fietsweer.app.domain.Advice
import nl.fietsweer.app.domain.AdviceText
import nl.fietsweer.app.domain.ChipKind
import nl.fietsweer.app.domain.SkyLight
import nl.fietsweer.app.ui.components.ClothingIcons
import nl.fietsweer.app.ui.components.SectionLabel
import nl.fietsweer.app.ui.components.WeatherBackdrop
import nl.fietsweer.app.ui.components.WeatherEffect
import nl.fietsweer.app.ui.components.weatherGradient
import nl.fietsweer.app.ui.theme.AppTheme

private const val PANEL_CORNER_DP = 18

// White on a light cloud needs a little help: a soft shadow under the text.
private val textShadow = Shadow(Color.Black.copy(alpha = 0.28f), Offset(0f, 2f), blurRadius = 8f)

// The whole card is the weather of this moment; the panel on top says what to wear on the next rides.
@Composable
internal fun HeroCard(advice: Advice, current: Map<String, Double>, sky: SkyLight) {
    // Try-out: a long press steps through every effect, a double tap through day, dusk and night.
    var preview by remember { mutableStateOf<WeatherEffect?>(null) }
    var previewSky by remember { mutableStateOf<Pair<String, SkyLight>?>(null) }
    val effect = preview ?: WeatherEffect.forCurrent(current)
    val shownSky = previewSky?.second ?: sky
    val (gradientStart, gradientEnd) = AppTheme.accents.weatherGradient(effect, shownSky)

    var cardCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var panelCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val panelBounds = cardCoordinates?.let { card ->
        panelCoordinates?.takeIf { it.isAttached }?.let { card.localBoundingBoxOf(it) }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(gradientStart, gradientEnd)))
            .onGloballyPositioned { cardCoordinates = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        val entries = WeatherEffect.entries
                        preview = entries[((preview?.ordinal ?: -1) + 1) % entries.size]
                    },
                    onDoubleTap = {
                        val skies = listOf("day" to SkyLight.DAY, "dusk" to SkyLight.DUSK, "night" to SkyLight.NIGHT, null)
                        previewSky = skies[(skies.indexOf(previewSky) + 1) % skies.size]
                    }
                )
            }
    ) {
        WeatherBackdrop(effect, shownSky, cutout = panelBounds, cutoutCornerDp = PANEL_CORNER_DP)
        // A soft shade behind the 'right now' text, fading out towards the open sky on the right.
        Canvas(Modifier.matchParentSize()) {
            drawRect(
                Brush.radialGradient(
                    listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.18f, 56.dp.toPx()),
                    radius = size.width * 0.55f
                )
            )
        }
        Column(Modifier.padding(20.dp)) {
            if (hasCurrentConditions(current)) {
                NowLine(current)
                Spacer(Modifier.size(18.dp))
            }
            ClothingPanel(advice, Modifier.onGloballyPositioned { panelCoordinates = it })
        }
        listOfNotNull(preview?.name?.lowercase()?.replace('_', ' '), previewSky?.first).takeIf { it.isNotEmpty() }?.let {
            Text(
                it.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 4.dp)
            )
        }
    }
}

private fun hasCurrentConditions(current: Map<String, Double>): Boolean =
    current["temperature_2m"] != null || current["wind_speed_10m"] != null

@Composable
private fun NowLine(current: Map<String, Double>) {
    val strings = AppTheme.strings
    val format = AppTheme.format
    val apparent = current["apparent_temperature"]
    val windAndRain = format.windAndRain(current)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            format.temp(current["temperature_2m"] ?: Double.NaN),
            style = MaterialTheme.typography.displaySmall.copy(shadow = textShadow),
            color = Color.White
        )
        Spacer(Modifier.width(16.dp))
        Column {
            SectionLabel(strings.rightNow, color = Color.White.copy(alpha = 0.85f))
            if (apparent != null) {
                Text(
                    "${strings.feelsLike} ${format.temp(apparent)}",
                    style = MaterialTheme.typography.bodyLarge.copy(shadow = textShadow),
                    color = Color.White
                )
            }
            if (windAndRain.isNotEmpty()) {
                Text(
                    windAndRain,
                    style = MaterialTheme.typography.bodyMedium.copy(shadow = textShadow),
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun ClothingPanel(advice: Advice, modifier: Modifier) {
    val strings = AppTheme.strings
    val format = AppTheme.format
    val firstRide = advice.rides.minByOrNull { it.departureMs } ?: return
    val chips = AdviceText.chips(advice, strings)
    Surface(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PANEL_CORNER_DP.dp),
        color = Color.White.copy(alpha = 0.18f)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(
                strings.neededOn(format.dayWord(firstRide.departureMs)),
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.widthIn(max = 72.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (chips.isEmpty()) {
                    HeroChip(strings.adviceNone, Icons.AutoMirrored.Rounded.DirectionsBike)
                }
                // Two per row: rain jacket and coat side by side, the extras below.
                for (row in chips.chunked(2)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { HeroChip(it.label, iconFor(it.kind), Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroChip(label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier,
        shape = CircleShape,
        color = Color.White
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val contentColor = AppTheme.accents.heroInk
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 10.sp,
                    maxFontSize = MaterialTheme.typography.labelLarge.fontSize
                )
            )
        }
    }
}

private fun iconFor(kind: ChipKind): ImageVector = when (kind) {
    ChipKind.RAIN_JACKET -> Icons.Rounded.Umbrella
    ChipKind.GLOVES -> Icons.Rounded.BackHand
    ChipKind.VEST, ChipKind.WINTER -> Icons.Rounded.Checkroom
    ChipKind.SCARF -> ClothingIcons.Scarf
    ChipKind.HAT -> ClothingIcons.Hat
}

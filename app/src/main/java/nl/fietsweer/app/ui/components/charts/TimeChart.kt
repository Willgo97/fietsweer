package nl.fietsweer.app.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch
import nl.fietsweer.app.domain.ChartSeries
import nl.fietsweer.app.domain.Timeline
import nl.fietsweer.app.ui.components.Caption
import nl.fietsweer.app.ui.theme.AppTheme
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt

class ChartMarker(val timeMs: Long, val text: String, val color: Color)

// A horizontally scrolling rain and temperature chart. It opens with homeMs at homeAt
// (0 = left edge, 0.5 = centre) and offers a reset button once scrolled away.
@Composable
fun TimeChart(
    series: ChartSeries,
    visibleMs: Long,
    homeMs: Long,
    homeAt: Float,
    axisStepMs: Long,
    axisLabel: (Long) -> String,
    modifier: Modifier = Modifier,
    markers: List<ChartMarker> = emptyList(),
    background: DrawScope.(xOf: (Long) -> Float) -> Unit = {},
    foreground: DrawScope.(xOf: (Long) -> Float) -> Unit = {},
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val format = AppTheme.format
    val strings = AppTheme.strings
    val tempColor = MaterialTheme.colorScheme.tertiary
    val spanMs = (series.endMs - series.startMs).toFloat()
    val temperature = TemperatureScale(series.temperatureC)
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    fun fractionOf(timeMs: Long) = (timeMs - series.startMs) / spanMs

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val viewportPx = constraints.maxWidth
        val contentPx = (viewportPx * spanMs / visibleMs).roundToInt()
        val contentDp = with(LocalDensity.current) { contentPx.toDp() }
        val homeScroll = (fractionOf(homeMs) * contentPx - viewportPx * homeAt)
            .roundToInt().coerceIn(0, max(0, contentPx - viewportPx))
        LaunchedEffect(homeMs, contentPx) { scroll.scrollTo(homeScroll) }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { scope.launch { scroll.scrollTo(homeScroll) } }

        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .horizontalScroll(scroll)
            ) {
                Box(
                    Modifier
                        .width(contentDp)
                        .fillMaxHeight()
                ) {
                    ChartCanvas(series, temperature, background, foreground)
                    for (marker in markers) {
                        Pinned(fractionOf(marker.timeMs), 0f) {
                            Text(
                                marker.text,
                                style = MaterialTheme.typography.labelLarge,
                                color = marker.color,
                                modifier = Modifier.padding(start = 6.dp, top = 4.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .fadeEdges(24.dp)
            ) {
                Box(
                    Modifier
                        .wrapContentWidth(Alignment.Start, unbounded = true)
                        .width(contentDp)
                        .offset { IntOffset(-scroll.value, 0) }
                ) {
                    val offsetMs = ZoneId.systemDefault().rules.getOffset(Instant.ofEpochMilli(series.startMs)).totalSeconds * 1000L
                    val firstLabel = ((series.startMs + offsetMs + axisStepMs - 1) / axisStepMs) * axisStepMs - offsetMs
                    for (timeMs in generateSequence(firstLabel) { it + axisStepMs }.takeWhile { it <= series.endMs }) {
                        AtFraction(fractionOf(timeMs)) { Caption(axisLabel(timeMs)) }
                    }
                }
            }
        }

        val leftMs = series.startMs + (scroll.value.toFloat() / contentPx * spanMs).toLong()
        val leftTemp = temperatureAt(series, leftMs)
        val rightTemp = temperatureAt(series, leftMs + visibleMs)
        val awayFromHome = abs(scroll.value - homeScroll) > 8
        if (!leftTemp.isNaN()) {
            Pinned(0f, temperature.fraction(leftTemp)) {
                Caption(format.temp(leftTemp), color = tempColor, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            }
        }
        if (!rightTemp.isNaN()) {
            val buttonRoom = 40.dp / maxHeight
            val y = temperature.fraction(rightTemp).let { if (awayFromHome) max(it, buttonRoom) else it }
            Pinned(1f, y, alignEnd = true) {
                Caption(format.temp(rightTemp), color = tempColor, modifier = Modifier.padding(end = 4.dp, top = 4.dp))
            }
        }
        overlay()
        if (awayFromHome) {
            FilledTonalIconButton(
                onClick = { scope.launch { scroll.animateScrollTo(homeScroll) } },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
            ) {
                Icon(Icons.Rounded.Replay, strings.recentre, Modifier.size(18.dp))
            }
        }
    }
}

private fun temperatureAt(series: ChartSeries, timeMs: Long): Double =
    Timeline.interpolateLinear(series.times, series.temperatureC, timeMs.coerceIn(series.startMs, series.endMs))

private class TemperatureScale(temps: DoubleArray) {
    private val known = temps.filter { !it.isNaN() }
    private val low = (known.minOrNull() ?: 0.0) - 1
    private val span = max(2.0, (known.maxOrNull() ?: 0.0) + 1 - low)
    val drawable = known.size >= 2
    fun fraction(celsius: Double): Float = 0.12f + (1f - ((celsius - low) / span).toFloat()) * 0.5f
}

@Composable
private fun ChartCanvas(
    series: ChartSeries,
    temperature: TemperatureScale,
    background: DrawScope.(xOf: (Long) -> Float) -> Unit,
    foreground: DrawScope.(xOf: (Long) -> Float) -> Unit
) {
    val rainColor = AppTheme.accents.rain
    val tempColor = MaterialTheme.colorScheme.tertiary

    Canvas(Modifier.fillMaxSize()) {
        val spanMs = (series.endMs - series.startMs).toFloat()
        val xOf = { timeMs: Long -> (timeMs - series.startMs) / spanMs * size.width }
        background(xOf)

        val rain = series.rainMmPerHour
        if (rain.any { it > 0.02 }) {
            val area = Path().apply {
                moveTo(xOf(series.times.first()), size.height)
                for (i in rain.indices) {
                    lineTo(xOf(series.times[i]), size.height * (1f - rainIntensity(rain[i]) * 0.8f))
                }
                lineTo(xOf(series.times.last()), size.height)
                close()
            }
            drawPath(area, rainColor.copy(alpha = 0.85f))
        }

        if (temperature.drawable) {
            val temps = series.temperatureC
            val line = Path()
            var started = false
            for (i in temps.indices) {
                if (temps[i].isNaN()) continue
                val x = xOf(series.times[i])
                val y = size.height * temperature.fraction(temps[i])
                if (started) line.lineTo(x, y) else { line.moveTo(x, y); started = true }
            }
            drawPath(line, tempColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }

        foreground(xOf)
    }
}

// Fixed log scale so a height means the same every day: drizzle shows, 10 mm/h fills the chart.
internal fun rainIntensity(mmPerHour: Double): Float =
    (ln(1 + mmPerHour / 0.2) / ln(1 + 10 / 0.2)).toFloat().coerceIn(0f, 1f)

@Composable
private fun AtFraction(fraction: Float, content: @Composable () -> Unit) {
    Layout(content, Modifier.fillMaxWidth()) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
        val width = constraints.maxWidth
        layout(width, placeable.height) {
            val x = (fraction * width - placeable.width / 2f).toInt().coerceIn(0, width - placeable.width)
            placeable.place(x, 0)
        }
    }
}

@Composable
private fun Pinned(x: Float, y: Float, alignEnd: Boolean = false, content: @Composable () -> Unit) {
    Layout(content, Modifier.fillMaxSize()) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
        layout(constraints.maxWidth, constraints.maxHeight) {
            val left = if (alignEnd) x * constraints.maxWidth - placeable.width else x * constraints.maxWidth
            placeable.place(
                left.toInt().coerceIn(0, constraints.maxWidth - placeable.width),
                (y * constraints.maxHeight).toInt().coerceIn(0, constraints.maxHeight - placeable.height)
            )
        }
    }
}

private fun Modifier.fadeEdges(width: Dp): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val fade = (width.toPx() / size.width).coerceIn(0f, 0.5f)
        drawRect(
            Brush.horizontalGradient(
                0f to Color.Transparent, fade to Color.Black,
                1f - fade to Color.Black, 1f to Color.Transparent
            ),
            blendMode = BlendMode.DstIn
        )
    }

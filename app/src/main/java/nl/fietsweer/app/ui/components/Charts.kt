package nl.fietsweer.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.fietsweer.app.domain.RideAssessment
import nl.fietsweer.app.ui.theme.AppTheme
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

// ------------------------------------------------------------------ risk ring

@Composable
fun RiskRing(
    risk: Double,
    color: Color,
    modifier: Modifier = Modifier,
    diameter: Int = 92,
    caption: String? = null
) {
    val target = risk.coerceIn(0.0, 1.0).toFloat()
    val animated by animateFloatAsState(target, tween(700), label = "ring")
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(diameter.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = size.minDimension * 0.11f
                val inset = stroke / 2
                drawArc(
                    color = track,
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                if (animated > 0.004f) {
                    drawArc(
                        color = color,
                        startAngle = -90f, sweepAngle = 360f * animated, useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Text(
                "${(risk * 100).roundToInt()}%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (caption != null) {
            Spacer(Modifier.height(5.dp))
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// -------------------------------------------------------------- 24 hour chart

data class ChartPoint(val timeMs: Long, val precipMm: Double, val tempC: Double)
data class ChartBand(val startMs: Long, val endMs: Long, val color: Color, val label: String)

@Composable
fun PrecipTempChart(
    points: List<ChartPoint>,
    bands: List<ChartBand>,
    modifier: Modifier = Modifier,
    height: Int = 168
) {
    if (points.size < 2) return
    val fmt = AppTheme.fmt
    val accents = AppTheme.accents
    val rainColor = accents.rain
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val tempColor = MaterialTheme.colorScheme.tertiary
    val nowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)

    val t0 = points.first().timeMs
    val t1 = points.last().timeMs
    val span = max(1L, t1 - t0).toFloat()

    val maxPrecip = max(0.6, points.maxOf { if (it.precipMm.isNaN()) 0.0 else it.precipMm } * 1.15)
    val temps = points.map { it.tempC }.filter { !it.isNaN() }
    val minT = (temps.minOrNull() ?: 0.0) - 1.0
    val maxT = (temps.maxOrNull() ?: 10.0) + 1.0
    val tSpan = max(1.0, maxT - minT)

    val density = LocalDensity.current

    val realMin = temps.minOrNull()
    val realMax = temps.maxOrNull()

    Column(modifier.fillMaxWidth()) {
      Box(
          Modifier
              .fillMaxWidth()
              .height(height.dp)
      ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val bottomPad = 2f
            val plotH = h - bottomPad

            fun xOf(ms: Long): Float = ((ms - t0) / span) * w

            // shaded ride windows
            for (b in bands) {
                val x0 = xOf(b.startMs).coerceIn(0f, w)
                val x1 = xOf(b.endMs).coerceIn(0f, w)
                if (x1 <= x0) continue
                drawRect(
                    color = b.color.copy(alpha = 0.14f),
                    topLeft = Offset(x0, 0f),
                    size = Size(x1 - x0, plotH)
                )
                drawLine(
                    color = b.color.copy(alpha = 0.55f),
                    start = Offset(x0, 0f), end = Offset(x0, plotH),
                    strokeWidth = with(density) { 1.5.dp.toPx() }
                )
            }

            // horizontal guides
            for (i in 1..3) {
                val y = plotH * i / 4f
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y), end = Offset(w, y),
                    strokeWidth = with(density) { 1.dp.toPx() },
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f))
                )
            }

            // precipitation bars
            val barW = (w / points.size) * 0.62f
            for (p in points) {
                val v = if (p.precipMm.isNaN()) 0.0 else p.precipMm
                if (v <= 0.005) continue
                val bh = (v / maxPrecip).coerceIn(0.0, 1.0).toFloat() * plotH * 0.72f
                val x = xOf(p.timeMs)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(rainColor, rainColor.copy(alpha = 0.55f)),
                        startY = plotH - bh, endY = plotH
                    ),
                    topLeft = Offset(x - barW / 2, plotH - bh),
                    size = Size(barW, bh),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2.4f)
                )
            }

            // temperature curve
            val line = Path()
            val fill = Path()
            var started = false
            for (p in points) {
                if (p.tempC.isNaN()) continue
                val x = xOf(p.timeMs)
                val y = plotH * 0.10f + (1f - ((p.tempC - minT) / tSpan).toFloat()) * (plotH * 0.62f)
                if (!started) {
                    line.moveTo(x, y); fill.moveTo(x, plotH); fill.lineTo(x, y)
                    started = true
                } else {
                    line.lineTo(x, y); fill.lineTo(x, y)
                }
            }
            if (started) {
                fill.lineTo(xOf(points.last().timeMs), plotH)
                fill.close()
                drawPath(
                    fill,
                    Brush.verticalGradient(
                        listOf(tempColor.copy(alpha = 0.24f), tempColor.copy(alpha = 0.0f))
                    )
                )
                drawPath(
                    line, tempColor,
                    style = Stroke(width = with(density) { 2.4.dp.toPx() }, cap = StrokeCap.Round)
                )
            }

            // now marker
            val nowX = xOf(System.currentTimeMillis())
            if (nowX in 0f..w) {
                drawLine(
                    color = nowColor,
                    start = Offset(nowX, 0f), end = Offset(nowX, plotH),
                    strokeWidth = with(density) { 1.5.dp.toPx() },
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )
            }
        }

        if (realMin != null && realMax != null) {
            Text(
                "${realMin.roundToInt()}\u00b0 \u2013 ${realMax.roundToInt()}\u00b0",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
                color = tempColor,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
        val peak = points.maxOf { if (it.precipMm.isNaN()) 0.0 else it.precipMm }
        if (peak > 0.05) {
            Text(
                "${fmt.mm(peak)} mm",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
                color = rainColor,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
      }

        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            val labelCount = 5
            for (i in 0 until labelCount) {
                val ms = t0 + ((t1 - t0) * i / (labelCount - 1))
                Text(
                    fmt.time(ms),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = when (i) {
                        0 -> androidx.compose.ui.text.style.TextAlign.Start
                        labelCount - 1 -> androidx.compose.ui.text.style.TextAlign.End
                        else -> androidx.compose.ui.text.style.TextAlign.Center
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------- departure ribbon

/**
 * The whole 24-hour departure outlook as one continuous ribbon. Fitting it to
 * the width beats a scrollable strip: the shape of the day is the point, and
 * the tap position maps straight onto a slot so the thin cells stay usable.
 */
@Composable
fun DepartureTimeline(
    slots: List<RideAssessment>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Int = 56
) {
    if (slots.isEmpty()) return
    val accents = AppTheme.accents
    val fmt = AppTheme.fmt
    val outline = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current.density
    val n = slots.size

    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height.dp)
                .pointerInput(n) {
                    detectTapGestures { pos ->
                        onSelect(((pos.x / size.width) * n).toInt().coerceIn(0, n - 1))
                    }
                }
                .pointerInput(n) {
                    detectHorizontalDragGestures { change, _ ->
                        onSelect(((change.position.x / size.width) * n).toInt().coerceIn(0, n - 1))
                    }
                }
        ) {
            val slotW = size.width / n
            val gap = (slotW * 0.12f).coerceAtMost(1.6f * density)
            val radius = androidx.compose.ui.geometry.CornerRadius(1.6f * density)

            slots.forEachIndexed { i, s ->
                val x = i * slotW
                drawRoundRect(
                    color = accents.forRisk(s.risk).copy(alpha = if (s.night) 0.40f else 1f),
                    topLeft = Offset(x + gap / 2, 0f),
                    size = Size((slotW - gap).coerceAtLeast(1f), size.height),
                    cornerRadius = radius
                )
            }

            // hour boundaries, so the ribbon can be read like a clock
            for (i in slots.indices) {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = slots[i].departureMs }
                if (cal.get(java.util.Calendar.MINUTE) != 0) continue
                if (cal.get(java.util.Calendar.HOUR_OF_DAY) % 6 != 0) continue
                drawLine(
                    color = Color.White.copy(alpha = 0.55f),
                    start = Offset(i * slotW, 0f),
                    end = Offset(i * slotW, size.height),
                    strokeWidth = 1f * density
                )
            }

            val sel = selectedIndex.coerceIn(0, n - 1)
            val sx = sel * slotW
            drawRoundRect(
                color = outline,
                topLeft = Offset(sx - 1f * density, -1f * density),
                size = Size(slotW + 2f * density, size.height + 2f * density),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * density),
                style = Stroke(width = 2.2f * density)
            )
        }

        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth()) {
            val marks = 5
            for (i in 0 until marks) {
                val idx = (i * (n - 1) / (marks - 1).coerceAtLeast(1)).coerceIn(0, n - 1)
                Text(
                    fmt.time(slots[idx].departureMs),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = when (i) {
                        0 -> androidx.compose.ui.text.style.TextAlign.Start
                        marks - 1 -> androidx.compose.ui.text.style.TextAlign.End
                        else -> androidx.compose.ui.text.style.TextAlign.Center
                    }
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "${fmt.dayTime(slots[selectedIndex.coerceIn(0, n - 1)].departureMs)} \u00b7 " +
                "${slots[selectedIndex.coerceIn(0, n - 1)].riskPercent}%",
            style = MaterialTheme.typography.labelLarge,
            color = accents.forRisk(slots[selectedIndex.coerceIn(0, n - 1)].risk)
        )
    }
}

// ------------------------------------------------------------- model matrix

/**
 * One row per weather service, one column per departure slot. Drawn rather than
 * laid out so all 24 hours fit the screen without a scroll container.
 */
@Composable
fun ModelMatrix(
    slots: List<RideAssessment>,
    modifier: Modifier = Modifier
) {
    if (slots.isEmpty() || slots.first().perModel.isEmpty()) return
    val accents = AppTheme.accents
    val t = AppTheme.txt
    val density = LocalDensity.current.density
    val models = slots.first().perModel

    Column(modifier.fillMaxWidth()) {
        models.forEachIndexed { mi, m ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    m.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(118.dp)
                )
                Spacer(Modifier.width(6.dp))
                Canvas(
                    Modifier
                        .weight(1f)
                        .height(11.dp)
                ) {
                    val w = size.width / slots.size
                    slots.forEachIndexed { i, s ->
                        val v = s.perModel.getOrNull(mi)
                        drawRoundRect(
                            color = (if (v?.wet == true) accents.wet else accents.dry)
                                .copy(alpha = if (s.night) 0.38f else 1f),
                            topLeft = Offset(i * w, 0f),
                            size = Size((w - 0.6f * density).coerceAtLeast(0.8f), size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f * density)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .height(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                t.combined,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(118.dp)
            )
            Spacer(Modifier.width(6.dp))
            Canvas(
                Modifier
                    .weight(1f)
                    .height(16.dp)
            ) {
                val w = size.width / slots.size
                slots.forEachIndexed { i, s ->
                    drawRoundRect(
                        color = accents.forRisk(s.risk).copy(alpha = if (s.night) 0.38f else 1f),
                        topLeft = Offset(i * w, 0f),
                        size = Size((w - 0.6f * density).coerceAtLeast(0.8f), size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f * density)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------- wind dial

@Composable
fun WindDial(
    travelBearing: Double,
    windFromDeg: Double,
    windKmh: Double,
    modifier: Modifier = Modifier,
    diameter: Int = 76
) {
    val ring = MaterialTheme.colorScheme.surfaceContainerHighest
    val bike = MaterialTheme.colorScheme.primary
    val windColor = AppTheme.accents.cold
    val fmt = AppTheme.fmt

    Box(modifier.size(diameter.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            val c = Offset(size.width / 2, size.height / 2)
            drawCircle(ring, radius = r, center = c, style = Stroke(width = r * 0.16f))

            if (!travelBearing.isNaN()) {
                drawArrow(c, r * 0.72f, travelBearing, bike, r * 0.10f)
            }
            if (!windFromDeg.isNaN() && !windKmh.isNaN()) {
                // the wind blows towards bearing + 180
                drawArrow(c, r * 0.52f, (windFromDeg + 180.0) % 360.0, windColor, r * 0.09f)
            }
        }
        Text(
            if (windKmh.isNaN()) "–" else "${windKmh.roundToInt()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
        )
        Text(
            fmt.compass(windFromDeg),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 1.dp)
        )
    }
}

private fun DrawScope.drawArrow(
    center: Offset,
    length: Float,
    bearingDeg: Double,
    color: Color,
    strokeWidth: Float
) {
    val rad = Math.toRadians(bearingDeg - 90.0)
    val dx = kotlin.math.cos(rad).toFloat()
    val dy = kotlin.math.sin(rad).toFloat()
    val tip = Offset(center.x + dx * length, center.y + dy * length)
    val tail = Offset(center.x - dx * length * 0.55f, center.y - dy * length * 0.55f)
    drawLine(color, tail, tip, strokeWidth = strokeWidth, cap = StrokeCap.Round)
    // arrow head
    val headLen = length * 0.34f
    for (side in listOf(-1f, 1f)) {
        val a = Math.toRadians(bearingDeg - 90.0 + side * 148.0)
        drawLine(
            color, tip,
            Offset(tip.x + kotlin.math.cos(a).toFloat() * headLen, tip.y + kotlin.math.sin(a).toFloat() * headLen),
            strokeWidth = strokeWidth, cap = StrokeCap.Round
        )
    }
}

// --------------------------------------------------------------- sparkline

@Composable
fun RainSparkline(
    values: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
    height: Int = 26
) {
    if (values.isEmpty()) return
    val peak = max(0.3, values.maxOrNull() ?: 0.0)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height.dp)
    ) {
        val w = size.width / values.size
        values.forEachIndexed { i, v ->
            val frac = (v / peak).coerceIn(0.0, 1.0).toFloat()
            val bh = max(size.height * 0.06f, size.height * frac)
            drawRoundRect(
                color = if (v <= 0.001) color.copy(alpha = 0.18f) else color,
                topLeft = Offset(i * w + w * 0.15f, size.height - bh),
                size = Size(w * 0.7f, bh),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(min(w * 0.35f, 4f))
            )
        }
    }
}

@Composable
fun LegendDot(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(9.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

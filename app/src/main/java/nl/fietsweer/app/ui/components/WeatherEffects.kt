package nl.fietsweer.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

enum class WeatherEffect {
    SUN, PARTLY_CLOUDY, CLOUDS, FOG, DRIZZLE, LIGHT_RAIN, RAIN, HEAVY_RAIN, DOWNPOUR, FREEZING_RAIN, SNOW, STORM, HAIL;

    companion object {
        // WMO weather codes as Open-Meteo sends them; a clear night shows no effect.
        fun forCurrent(current: Map<String, Double>): WeatherEffect? {
            val code = current["weather_code"]?.roundToInt() ?: return null
            val day = current["is_day"] != 0.0
            return when (code) {
                0, 1 -> if (day) SUN else null
                2 -> if (day) PARTLY_CLOUDY else CLOUDS
                3 -> CLOUDS
                45, 48 -> FOG
                51, 53, 55 -> DRIZZLE
                56, 57, 66, 67 -> FREEZING_RAIN
                61, 80 -> LIGHT_RAIN
                63, 81 -> RAIN
                65 -> HEAVY_RAIN
                82 -> DOWNPOUR
                71, 73, 75, 77, 85, 86 -> SNOW
                95 -> STORM
                96, 99 -> HAIL
                else -> null
            }
        }
    }
}

// The weather of the moment, moving behind a card's content; cutout keeps it off a panel on top.
@Composable
fun BoxScope.WeatherBackdrop(effect: WeatherEffect?, cutout: Rect? = null, cutoutCorner: Float = 18f) {
    val seconds = rememberSeconds()
    Crossfade(effect, Modifier.matchParentSize(), animationSpec = tween(900), label = "weather") { shown ->
        if (shown == null) return@Crossfade
        val particles = remember(shown) { Particles(shown) }
        Canvas(Modifier.fillMaxSize()) {
            val hole = Path().apply { cutout?.let { addRoundRect(RoundRect(it, CornerRadius(cutoutCorner.dp.toPx()))) } }
            clipPath(hole, ClipOp.Difference) { drawWeatherEffect(shown, particles, seconds.value) }
        }
    }
}

@Composable
private fun rememberSeconds(): State<Float> = produceState(0f) {
    val start = withFrameNanos { it }
    while (true) withFrameNanos { value = (it - start) / 1_000_000_000f }
}

// Every particle gets a fixed random place in its loop, so the picture never repeats visibly.
class Particle(val x: Float, val y: Float, val phase: Float, val size: Float, val speed: Float)

class Particles(effect: WeatherEffect) {
    private val random = Random(effect.ordinal + 7)
    val far = List(40) { next() }
    val near = List(40) { next() }
    private fun next() = Particle(random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat())
}

fun DrawScope.drawWeatherEffect(effect: WeatherEffect, particles: Particles, seconds: Float): Unit = when (effect) {
    WeatherEffect.SUN -> sun(seconds)
    WeatherEffect.PARTLY_CLOUDY -> {
        sun(seconds)
        clouds(particles.near.take(3), seconds, alpha = 0.18f, scale = 1f)
    }
    WeatherEffect.CLOUDS -> {
        clouds(particles.far.take(3), seconds, alpha = 0.10f, scale = 0.7f)
        clouds(particles.near.take(3), seconds, alpha = 0.20f, scale = 1.1f)
    }
    WeatherEffect.FOG -> fog(particles.far.take(5), seconds)
    WeatherEffect.DRIZZLE -> {
        streaks(particles.far, seconds, fallSeconds = 2.8f, alpha = 0.10f, widthDp = 0.8f, lengthDp = 4f..7f, slant = 0.08f)
        streaks(particles.near, seconds, fallSeconds = 1.9f, alpha = 0.16f, widthDp = 1f, lengthDp = 6f..9f, slant = 0.08f)
    }
    WeatherEffect.LIGHT_RAIN -> {
        streaks(particles.far.take(16), seconds, fallSeconds = 1.8f, alpha = 0.09f, widthDp = 0.9f, lengthDp = 8f..12f, slant = 0.15f)
        streaks(particles.near.take(14), seconds, fallSeconds = 1.0f, alpha = 0.16f, widthDp = 1.1f, lengthDp = 12f..18f, slant = 0.15f)
    }
    WeatherEffect.HEAVY_RAIN -> {
        streaks(particles.far, seconds, fallSeconds = 1.0f, alpha = 0.14f, widthDp = 1.2f, lengthDp = 14f..22f, slant = 0.25f)
        streaks(particles.near, seconds, fallSeconds = 0.5f, alpha = 0.26f, widthDp = 1.6f, lengthDp = 22f..32f, slant = 0.25f)
    }
    WeatherEffect.DOWNPOUR -> {
        drawRect(Color.White.copy(alpha = 0.07f))
        streaks(particles.far, seconds, fallSeconds = 0.8f, alpha = 0.18f, widthDp = 1.4f, lengthDp = 20f..30f, slant = 0.32f)
        streaks(particles.near, seconds, fallSeconds = 0.38f, alpha = 0.32f, widthDp = 2f, lengthDp = 30f..46f, slant = 0.32f)
        splashes(particles.near.take(22), seconds)
    }
    WeatherEffect.RAIN -> {
        streaks(particles.far.take(28), seconds, fallSeconds = 1.4f, alpha = 0.10f, widthDp = 1f, lengthDp = 10f..16f, slant = 0.22f)
        streaks(particles.near.take(28), seconds, fallSeconds = 0.7f, alpha = 0.20f, widthDp = 1.4f, lengthDp = 16f..26f, slant = 0.22f)
    }
    WeatherEffect.FREEZING_RAIN -> {
        frost()
        streaks(particles.far.take(18), seconds, fallSeconds = 1.1f, alpha = 0.18f, widthDp = 1.2f, lengthDp = 8f..12f, slant = 0.1f)
        glints(particles.near.take(16), seconds)
    }
    WeatherEffect.SNOW -> {
        flakes(particles.far, seconds, fallSeconds = 11f, radiusDp = 1.2f..2.2f, alpha = 0.35f)
        flakes(particles.near.take(26), seconds, fallSeconds = 7f, radiusDp = 2.2f..3.6f, alpha = 0.65f)
    }
    WeatherEffect.STORM -> {
        streaks(particles.far, seconds, fallSeconds = 0.9f, alpha = 0.14f, widthDp = 1.2f, lengthDp = 14f..22f, slant = 0.45f)
        streaks(particles.near, seconds, fallSeconds = 0.45f, alpha = 0.26f, widthDp = 1.6f, lengthDp = 22f..34f, slant = 0.45f)
        lightning(seconds)
    }
    WeatherEffect.HAIL -> {
        pellets(particles.far.take(24), seconds, fallSeconds = 1.1f, radiusDp = 1.4f..2f, alpha = 0.35f)
        pellets(particles.near.take(18), seconds, fallSeconds = 0.75f, radiusDp = 2.2f..3.2f, alpha = 0.6f)
    }
}

private fun loop(phase: Float, seconds: Float, periodSeconds: Float) = (phase + seconds / periodSeconds) % 1f

private fun ClosedFloatingPointRange<Float>.at(fraction: Float) = start + (endInclusive - start) * fraction

private fun DrawScope.streaks(
    drops: List<Particle>,
    seconds: Float,
    fallSeconds: Float,
    alpha: Float,
    widthDp: Float,
    lengthDp: ClosedFloatingPointRange<Float>,
    slant: Float
) {
    val colour = Color.White.copy(alpha = alpha)
    for (drop in drops) {
        val length = lengthDp.at(drop.size).dp.toPx()
        val bottom = loop(drop.phase, seconds, fallSeconds) * (size.height + length)
        val x = drop.x * (size.width + slant * size.height) - slant * bottom
        drawLine(
            colour, Offset(x + slant * length, bottom - length), Offset(x, bottom),
            strokeWidth = widthDp.dp.toPx(), cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.flakes(
    flakes: List<Particle>,
    seconds: Float,
    fallSeconds: Float,
    radiusDp: ClosedFloatingPointRange<Float>,
    alpha: Float
) {
    for (flake in flakes) {
        val radius = radiusDp.at(flake.size).dp.toPx()
        val ownFall = fallSeconds * (0.8f + 0.4f * flake.speed)
        val y = loop(flake.phase, seconds, ownFall) * (size.height + 2 * radius) - radius
        val sway = sin((seconds / (2.5f + 2f * flake.speed) + flake.phase) * 2 * PI.toFloat()) * (6 + 8 * flake.size).dp.toPx()
        drawCircle(Color.White.copy(alpha = alpha), radius, Offset(flake.x * size.width + sway, y))
    }
}

private fun DrawScope.pellets(
    pellets: List<Particle>,
    seconds: Float,
    fallSeconds: Float,
    radiusDp: ClosedFloatingPointRange<Float>,
    alpha: Float
) {
    for (pellet in pellets) {
        val radius = radiusDp.at(pellet.size).dp.toPx()
        val y = loop(pellet.phase, seconds, fallSeconds) * (size.height + 2 * radius) - radius
        val x = pellet.x * (size.width + 0.12f * size.height) - 0.12f * y
        drawCircle(Color.White.copy(alpha = alpha), radius, Offset(x, y))
    }
}

// Two quick flashes every few seconds, like a far-off strike.
private fun DrawScope.lightning(seconds: Float) {
    val t = seconds % 5.5f
    val strength = when {
        t < 0.07f -> 1f
        t in 0.16f..0.24f -> 0.6f
        else -> return
    }
    drawRect(Color.White.copy(alpha = 0.28f * strength))
}

// Freezing rain: a cold sheen along the bottom and ice that catches the light.
private fun DrawScope.frost() {
    drawRect(
        Brush.verticalGradient(
            0.55f to Color.Transparent,
            1f to Color(0xFFE6F4FF).copy(alpha = 0.22f)
        )
    )
}

private fun DrawScope.glints(glints: List<Particle>, seconds: Float) {
    for (glint in glints) {
        val twinkle = sin((seconds / (1.8f + glint.speed * 1.6f) + glint.phase) * 2 * PI.toFloat()).coerceAtLeast(0f).pow(4)
        if (twinkle < 0.02f) continue
        val centre = Offset(glint.x * size.width, (0.45f + 0.55f * glint.y) * size.height)
        val arm = (3f + 4f * glint.size).dp.toPx() * twinkle
        val colour = Color.White.copy(alpha = 0.8f * twinkle)
        val width = 1.2.dp.toPx()
        drawLine(colour, centre - Offset(arm, 0f), centre + Offset(arm, 0f), width, StrokeCap.Round)
        drawLine(colour, centre - Offset(0f, arm), centre + Offset(0f, arm), width, StrokeCap.Round)
    }
}

private fun DrawScope.fog(banks: List<Particle>, seconds: Float) {
    drawRect(Color.White.copy(alpha = 0.06f))
    banks.forEachIndexed { i, bank ->
        val drift = sin((seconds / (18f + 14f * bank.speed) + bank.phase) * 2 * PI.toFloat())
        val centre = Offset(size.width * (0.2f + 0.6f * bank.x + 0.25f * drift), size.height * (i + 0.5f) / banks.size)
        val radius = size.width * (0.45f + 0.25f * bank.size)
        drawOval(
            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent), centre, radius),
            topLeft = Offset(centre.x - radius, centre.y - radius * 0.35f),
            size = Size(radius * 2, radius * 0.7f)
        )
    }
}

private fun DrawScope.sun(seconds: Float) {
    val centre = Offset(size.width * 0.93f, size.height * 0.05f)
    val pulse = 1f + 0.05f * sin(seconds / 4f * 2 * PI.toFloat())
    val glow = size.height * 0.95f * pulse
    rotate(seconds * 6f, centre) {
        for (ray in 0 until 12) {
            val angle = ray * 30f * PI.toFloat() / 180f
            val half = 4f * PI.toFloat() / 180f
            val reach = size.width * 1.2f
            val wedge = Path().apply {
                moveTo(centre.x, centre.y)
                lineTo(centre.x + reach * cos(angle - half), centre.y + reach * sin(angle - half))
                lineTo(centre.x + reach * cos(angle + half), centre.y + reach * sin(angle + half))
                close()
            }
            drawPath(wedge, Brush.radialGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent), centre, reach))
        }
    }
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFFFFF4D2).copy(alpha = 0.45f), Color.Transparent), centre, glow),
        glow, centre
    )
}

// Rings that open up and fade where heavy rain hits the bottom edge.
private fun DrawScope.splashes(splashes: List<Particle>, seconds: Float) {
    for (splash in splashes) {
        val age = loop(splash.phase, seconds, 0.6f + 0.5f * splash.speed)
        val width = (6f + 10f * splash.size).dp.toPx() * age
        val centre = Offset(splash.x * size.width, size.height - (2f + 10f * splash.y).dp.toPx())
        drawOval(
            Color.White.copy(alpha = 0.35f * (1 - age)),
            topLeft = Offset(centre.x - width / 2, centre.y - width / 6),
            size = Size(width, width / 3),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

// One path per cloud, so overlapping puffs do not show darker seams.
private fun DrawScope.clouds(clouds: List<Particle>, seconds: Float, alpha: Float, scale: Float) {
    clouds.forEachIndexed { i, cloud ->
        val width = size.width * (0.42f + 0.18f * cloud.size) * scale
        val left = loop(cloud.phase, seconds, 45f + 30f * cloud.speed) * (size.width + width) - width
        val baseY = size.height * (0.30f + 0.55f * i / clouds.size)
        val shape = Path().apply {
            addOval(Rect(Offset(left + width * 0.08f, baseY - width * 0.20f), Size(width * 0.40f, width * 0.32f)))
            addOval(Rect(Offset(left + width * 0.30f, baseY - width * 0.34f), Size(width * 0.46f, width * 0.46f)))
            addOval(Rect(Offset(left + width * 0.60f, baseY - width * 0.18f), Size(width * 0.34f, width * 0.28f)))
            addRoundRect(RoundRect(left, baseY - width * 0.06f, left + width, baseY + width * 0.10f, CornerRadius(width * 0.08f)))
        }
        drawPath(shape, Color.White.copy(alpha = alpha))
    }
}

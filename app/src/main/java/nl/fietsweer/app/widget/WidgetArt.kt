package nl.fietsweer.app.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import nl.fietsweer.app.R
import nl.fietsweer.app.domain.ChartSeries
import nl.fietsweer.app.domain.DepartureWindow
import nl.fietsweer.app.domain.RideAssessment
import nl.fietsweer.app.ui.components.charts.rainIntensity
import nl.fietsweer.app.ui.theme.Accents
import nl.fietsweer.app.ui.theme.BrandDark
import nl.fietsweer.app.ui.theme.BrandLight
import nl.fietsweer.app.ui.theme.DarkAccents
import nl.fietsweer.app.ui.theme.LightAccents
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

// The pictures in the widget, drawn here because a home-screen widget cannot run Compose.
object WidgetArt {

    private const val BADGE_DP = 38
    private const val RING_DP = 160
    private const val SPAN_MS = DepartureWindow.VISIBLE_MS

    enum class Picture(val fileName: String) { BADGE("badge"), RING("ring"), CHART("chart"), CHART_WIDE("chart_wide") }

    fun file(context: Context, picture: Picture) = File(context.filesDir, "widget/${picture.fileName}.png")

    fun load(context: Context, picture: Picture): Bitmap? =
        file(context, picture).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }

    fun clear(context: Context) = Picture.entries.forEach { file(context, it).delete() }

    // Follows the system theme, as the widget's own background drawable does.
    private fun palette(context: Context): Pair<Accents, ColorScheme> {
        val dark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        return if (dark) DarkAccents to BrandDark else LightAccents to BrandLight
    }

    fun save(bitmap: Bitmap, file: File) {
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    fun badge(context: Context, icon: ImageVector, accent: Int): Bitmap {
        val px = (BADGE_DP * context.resources.displayMetrics.density).roundToInt()
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawCircle(px / 2f, px / 2f, px / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })
        drawIcon(canvas, icon, px / 2f, px / 2f, px * 0.56f)
        return bitmap
    }

    private fun drawIcon(canvas: Canvas, icon: ImageVector, centreX: Float, centreY: Float, sizePx: Float) {
        canvas.save()
        canvas.translate(centreX - sizePx / 2, centreY - sizePx / 2)
        canvas.scale(sizePx / icon.viewportWidth, sizePx / icon.viewportHeight)
        drawGroup(canvas, icon.root, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE })
        canvas.restore()
    }

    private fun drawGroup(canvas: Canvas, group: VectorGroup, paint: Paint) {
        for (child in group) when (child) {
            is VectorPath -> canvas.drawPath(child.pathData.toPath().asAndroidPath(), paint)
            is VectorGroup -> drawGroup(canvas, child, paint)
        }
    }

    // The two hours around the next ride, as on the Today card: risk tint, best ride, rain, temperature.
    internal fun chart(context: Context, window: DepartureWindow, widthDp: Int, heightDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val width = (widthDp * density).roundToInt()
        val height = (heightDp * density).roundToInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val (accents, scheme) = palette(context)
        val centreMs = (window.plannedDepartureMs + window.plannedArrivalMs) / 2
        val chart = ChartPainter(canvas, width.toFloat(), height.toFloat(), density, centreMs - SPAN_MS / 2, accents, scheme)

        val corner = 8 * density
        val frame = Path().apply { addRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), corner, corner, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(frame)
        canvas.drawColor(scheme.surfaceContainerHighest.copy(alpha = 0.5f).toArgb())
        chart.riskTint(window.slots)
        chart.bestRide(window.best)
        chart.rain(window.series)
        chart.temperature(window.series)
        chart.plannedRide(window.plannedDepartureMs, window.plannedArrivalMs)
        canvas.restore()
        return bitmap
    }

    private class ChartPainter(
        val canvas: Canvas,
        val width: Float,
        val height: Float,
        val density: Float,
        val startMs: Long,
        val accents: Accents,
        val scheme: ColorScheme
    ) {
        fun xOf(timeMs: Long) = (timeMs - startMs).toFloat() / SPAN_MS * width

        fun riskTint(slots: List<RideAssessment>) {
            if (slots.size < 2) return
            val positions = FloatArray(slots.size) { (xOf(slots[it].slotCentreMs) / width).coerceIn(0f, 1f) }
            val colours = IntArray(slots.size) { accents.forRisk(slots[it].risk).copy(alpha = 0.18f).toArgb() }
            val sorted = positions.indices.sortedBy { positions[it] }
            canvas.drawRect(0f, 0f, width, height, Paint().apply {
                shader = LinearGradient(0f, 0f, width, 0f,
                    IntArray(sorted.size) { colours[sorted[it]] }, FloatArray(sorted.size) { positions[sorted[it]] }, Shader.TileMode.CLAMP)
            })
        }

        fun bestRide(best: RideAssessment) {
            val bestColour = accents.forRisk(best.risk)
            val left = xOf(best.departureMs)
            val right = xOf(best.arrivalMs)
            canvas.drawRoundRect(RectF(left, 0f, right, height), 4 * density, 4 * density,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bestColour.copy(alpha = 0.28f).toArgb() })
            canvas.drawRect(left, 0f, left + 2.5f * density, height,
                Paint().apply { color = bestColour.copy(alpha = 0.9f).toArgb() })
        }

        fun rain(series: ChartSeries) {
            val rain = Path()
            rain.moveTo(xOf(series.times.first()), height)
            for (i in series.times.indices) {
                rain.lineTo(xOf(series.times[i]), height * (1f - rainIntensity(series.rainMmPerHour[i]) * 0.85f))
            }
            rain.lineTo(xOf(series.times.last()), height)
            rain.close()
            canvas.drawPath(rain, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accents.rain.copy(alpha = 0.85f).toArgb() })
        }

        fun temperature(series: ChartSeries) {
            val visible = series.times.indices.filter { series.times[it] in startMs..startMs + SPAN_MS && !series.temperatureC[it].isNaN() }
            if (visible.size < 2) return
            val temps = visible.map { series.temperatureC[it] }
            val low = temps.min() - 1
            val span = max(2.0, temps.max() + 1 - low)
            val line = Path()
            visible.forEachIndexed { n, i ->
                val x = xOf(series.times[i])
                val y = height * (0.15f + (1f - ((series.temperatureC[i] - low) / span).toFloat()) * 0.45f)
                if (n == 0) line.moveTo(x, y) else line.lineTo(x, y)
            }
            canvas.drawPath(line, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 1.8f * density; strokeCap = Paint.Cap.ROUND
                color = scheme.tertiary.toArgb()
            })
        }

        fun plannedRide(departureMs: Long, arrivalMs: Long) {
            val outline = 1.2f * density
            canvas.drawRoundRect(
                RectF(xOf(departureMs) + outline / 2, outline / 2, xOf(arrivalMs) - outline / 2, height - outline / 2),
                4 * density, 4 * density,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE; strokeWidth = outline
                    color = scheme.onSurface.copy(alpha = 0.5f).toArgb()
                    pathEffect = DashPathEffect(floatArrayOf(4 * density, 3 * density), 0f)
                }
            )
        }
    }

    // Jacket icon in the advice colour, a ring that fills with the chance of rain, and the felt temperature.
    fun ring(context: Context, icon: ImageVector, accent: Int, riskPercent: Int, temperature: String): Bitmap {
        val px = (RING_DP * context.resources.displayMetrics.density).roundToInt()
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val (accents, scheme) = palette(context)
        val centre = px / 2f
        val stroke = px * 0.07f

        canvas.drawCircle(centre, centre, centre, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.widget_surface)
        })
        val arc = RectF(stroke * 1.4f, stroke * 1.4f, px - stroke * 1.4f, px - stroke * 1.4f)
        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = stroke; strokeCap = Paint.Cap.ROUND
            color = scheme.surfaceContainerHighest.toArgb()
        }
        canvas.drawArc(arc, 0f, 360f, false, track)
        if (riskPercent > 0) {
            canvas.drawArc(arc, -90f, 360f * riskPercent.coerceIn(0, 100) / 100f, false, Paint(track).apply {
                color = accents.rain.toArgb()
            })
        }

        val iconPx = px * 0.34f
        canvas.drawCircle(centre, px * 0.40f, iconPx * 0.82f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })
        drawIcon(canvas, icon, centre, px * 0.40f, iconPx)

        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = scheme.onSurface.toArgb(); textSize = px * 0.15f; textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText(temperature, centre, px * 0.78f, text)
        return bitmap
    }
}

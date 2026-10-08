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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import nl.fietsweer.app.ui.components.charts.rainIntensity
import nl.fietsweer.app.ui.screens.today.DepartureWindow
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
    private const val SPAN_MS = 2 * 60 * 60_000L
    private const val SLOT_MS = 15 * 60_000L

    enum class Picture(val fileName: String) { BADGE("badge"), RING("ring"), CHART("chart"), CHART_WIDE("chart_wide") }

    fun file(context: Context, picture: Picture) = File(context.filesDir, "widget/${picture.fileName}.png")

    fun load(context: Context, picture: Picture): Bitmap? =
        file(context, picture).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }

    fun clear(context: Context) = Picture.entries.forEach { file(context, it).delete() }

    fun isDark(context: Context): Boolean =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    fun save(bitmap: Bitmap, file: File) {
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    fun badge(context: Context, icon: ImageVector, accent: Int): Bitmap {
        val px = (BADGE_DP * context.resources.displayMetrics.density).roundToInt()
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawCircle(px / 2f, px / 2f, px / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })
        val iconPx = px * 0.56f
        canvas.save()
        canvas.translate((px - iconPx) / 2, (px - iconPx) / 2)
        canvas.scale(iconPx / icon.viewportWidth, iconPx / icon.viewportHeight)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
        drawGroup(canvas, icon.root, paint)
        canvas.restore()
        return bitmap
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
        val dark = isDark(context)
        val accents = if (dark) DarkAccents else LightAccents
        val scheme = if (dark) BrandDark else BrandLight

        val centreMs = (window.plannedDepartureMs + window.plannedArrivalMs) / 2
        val startMs = centreMs - SPAN_MS / 2
        fun xOf(timeMs: Long) = (timeMs - startMs).toFloat() / SPAN_MS * width
        val corner = 8 * density

        val frame = Path().apply { addRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), corner, corner, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(frame)
        canvas.drawColor(scheme.surfaceContainerHighest.copy(alpha = 0.5f).toArgb())

        val slots = window.slots
        if (slots.size >= 2) {
            val positions = FloatArray(slots.size) { (xOf(slots[it].departureMs + SLOT_MS / 2) / width).coerceIn(0f, 1f) }
            val colours = IntArray(slots.size) { accents.forRisk(slots[it].risk).copy(alpha = 0.18f).toArgb() }
            val sorted = positions.indices.sortedBy { positions[it] }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply {
                shader = LinearGradient(0f, 0f, width.toFloat(), 0f,
                    IntArray(sorted.size) { colours[sorted[it]] }, FloatArray(sorted.size) { positions[sorted[it]] }, Shader.TileMode.CLAMP)
            })
        }

        val best = window.best
        val bestColour = accents.forRisk(best.risk)
        val left = xOf(best.departureMs)
        val right = xOf(best.arrivalMs)
        canvas.drawRoundRect(RectF(left, 0f, right, height.toFloat()), 4 * density, 4 * density,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bestColour.copy(alpha = 0.28f).toArgb() })
        canvas.drawRect(left, 0f, left + 2.5f * density, height.toFloat(),
            Paint().apply { color = bestColour.copy(alpha = 0.9f).toArgb() })

        val series = window.series
        val rain = Path()
        rain.moveTo(xOf(series.times.first()), height.toFloat())
        for (i in series.times.indices) {
            rain.lineTo(xOf(series.times[i]), height * (1f - rainIntensity(series.rainMmPerHour[i]) * 0.85f))
        }
        rain.lineTo(xOf(series.times.last()), height.toFloat())
        rain.close()
        canvas.drawPath(rain, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accents.rain.copy(alpha = 0.85f).toArgb() })

        val visible = series.times.indices.filter { series.times[it] in startMs..startMs + SPAN_MS && !series.temperatureC[it].isNaN() }
        if (visible.size >= 2) {
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

        val outline = 1.2f * density
        canvas.drawRoundRect(
            RectF(xOf(window.plannedDepartureMs) + outline / 2, outline / 2, xOf(window.plannedArrivalMs) - outline / 2, height - outline / 2),
            4 * density, 4 * density,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = outline
                color = scheme.onSurface.copy(alpha = 0.5f).toArgb()
                pathEffect = DashPathEffect(floatArrayOf(4 * density, 3 * density), 0f)
            }
        )
        canvas.restore()
        return bitmap
    }

    // Jacket icon in the advice colour, a ring that fills with the chance of rain, and the felt temperature.
    fun ring(context: Context, icon: ImageVector, accent: Int, riskPercent: Int, temperature: String): Bitmap {
        val px = (RING_DP * context.resources.displayMetrics.density).roundToInt()
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val dark = isDark(context)
        val scheme = if (dark) BrandDark else BrandLight
        val accents = if (dark) DarkAccents else LightAccents
        val centre = px / 2f
        val stroke = px * 0.07f

        canvas.drawCircle(centre, centre, centre, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (dark) 0xFF141A1E.toInt() else 0xFFF7FAFC.toInt()
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
        canvas.save()
        canvas.translate(centre - iconPx / 2, px * 0.40f - iconPx / 2)
        canvas.scale(iconPx / icon.viewportWidth, iconPx / icon.viewportHeight)
        drawGroup(canvas, icon.root, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE })
        canvas.restore()

        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = scheme.onSurface.toArgb(); textSize = px * 0.15f; textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText(temperature, centre, px * 0.78f, text)
        return bitmap
    }
}

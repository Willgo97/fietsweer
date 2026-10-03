package nl.fietsweer.app.ui.screens.forecast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.data.RouteForecast
import nl.fietsweer.app.domain.Sky
import nl.fietsweer.app.domain.WeatherCode
import nl.fietsweer.app.ui.components.Caption
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.theme.AppTheme
import kotlin.math.roundToInt

@Composable
internal fun DailyOutlook(forecast: RouteForecast) {
    val strings = AppTheme.strings
    val format = AppTheme.format
    val accents = AppTheme.accents
    val times = forecast.dailyTimes
    if (times.isEmpty()) return
    val maxTemps = forecast.daily["temperature_2m_max"]
    val minTemps = forecast.daily["temperature_2m_min"]
    val precipitationSums = forecast.daily["precipitation_sum"]
    val rainChances = forecast.daily["precipitation_probability_max"]
    val weatherCodes = forecast.daily["weather_code"]

    val globalMin = minTemps?.filter { !it.isNaN() }?.minOrNull() ?: 0.0
    val globalMax = maxTemps?.filter { !it.isNaN() }?.maxOrNull() ?: 20.0
    val span = (globalMax - globalMin).coerceAtLeast(1.0)

    SectionCard(title = strings.dailyOutlook) {
        for (i in times.indices) {
            val low = minTemps?.getOrNull(i) ?: continue
            val high = maxTemps?.getOrNull(i) ?: continue
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (i == 0) strings.today.replaceFirstChar { it.uppercase() } else format.dayShort(times[i]),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(56.dp)
                )
                Text(
                    skyGlyph(WeatherCode.sky((weatherCodes?.getOrNull(i) ?: 0.0).toInt())),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(30.dp)
                )
                Caption(
                    "${(rainChances?.getOrNull(i) ?: 0.0).roundToInt()}%",
                    color = accents.rain,
                    modifier = Modifier.width(38.dp)
                )
                Text(
                    format.temp(low),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(34.dp),
                    textAlign = TextAlign.End
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .height(7.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                ) {
                    TempBar(
                        startFraction = ((low - globalMin) / span).toFloat().coerceIn(0f, 1f),
                        endFraction = ((high - globalMin) / span).toFloat().coerceIn(0f, 1f),
                        lowColor = accents.forTemperature(low),
                        highColor = accents.forTemperature(high)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    format.temp(high),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(34.dp)
                )
                val precipitation = precipitationSums?.getOrNull(i) ?: 0.0
                Caption(
                    if (precipitation > 0.05) "${format.millimetres(precipitation)}mm" else "",
                    modifier = Modifier.width(48.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun TempBar(startFraction: Float, endFraction: Float, lowColor: Color, highColor: Color) {
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        val left = size.width * startFraction
        val right = (size.width * endFraction).coerceAtLeast(left + size.height)
        drawRoundRect(
            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                listOf(lowColor, highColor), startX = left, endX = right
            ),
            topLeft = androidx.compose.ui.geometry.Offset(left, 0f),
            size = androidx.compose.ui.geometry.Size(right - left, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
        )
    }
}

private fun skyGlyph(sky: Sky): String = when (sky) {
    Sky.CLEAR -> "☀"
    Sky.PARTLY -> "⛅"
    Sky.CLOUDY -> "☁"
    Sky.FOG -> "🌫"
    Sky.DRIZZLE -> "🌦"
    Sky.RAIN -> "🌧"
    Sky.SHOWERS -> "🌦"
    Sky.SNOW -> "❄"
    Sky.THUNDER -> "⛈"
    Sky.UNKNOWN -> "·"
}

package nl.fietsweer.app.widget

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import nl.fietsweer.app.domain.SunTimes
import nl.fietsweer.app.ui.components.WeatherEffect

@Serializable
data class WidgetRide(
    val name: String,
    val time: String,
    val rain: String,
    val temperature: String,
    val riskColour: Int
)

@Serializable
data class WidgetNow(
    val temperature: String,
    val feelsLike: String,
    val windAndRain: String,
    val effect: WeatherEffect?,
    val sunrises: List<Long> = emptyList(),
    val sunsets: List<Long> = emptyList(),
    val isDay: Boolean = true
) {
    val sun: SunTimes get() = SunTimes(sunrises, sunsets, isDay)
}

@Serializable
data class WidgetSnapshot(
    val headline: String,
    val chipLine: String,
    val rides: List<WidgetRide>,
    val accent: Int,
    val jacket: Boolean,
    val now: WidgetNow? = null
) {
    val ridesInline: String get() = rides.joinToString(" · ") { "${it.time} ${it.rain}" }
}

object WidgetStore {

    private const val FILE = "widget_snapshot"
    private val json = Json { ignoreUnknownKeys = true }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun save(context: Context, snapshot: WidgetSnapshot) {
        preferences(context).edit().putString("snapshot", json.encodeToString(WidgetSnapshot.serializer(), snapshot)).apply()
    }

    fun load(context: Context): WidgetSnapshot? =
        preferences(context).getString("snapshot", null)
            ?.let { runCatching { json.decodeFromString(WidgetSnapshot.serializer(), it) }.getOrNull() }

    fun clear(context: Context) {
        preferences(context).edit().remove("snapshot").apply()
        WidgetArt.clear(context)
    }

    // Separate from the snapshot time, so a failing fetch cannot retrigger itself in a loop.
    fun lastAttempt(context: Context): Long = preferences(context).getLong("lastAttempt", 0L)

    fun markAttempt(context: Context, atMs: Long) {
        preferences(context).edit().putLong("lastAttempt", atMs).apply()
    }
}

package nl.fietsweer.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nl.fietsweer.app.widget.WidgetUpdater

data class ForecastState(
    val loading: Boolean = false,
    val forecast: RouteForecast? = null,
    val error: String? = null
)

object ForecastRepository {

    private const val FRESH_FOR_MS = 15 * 60 * 1000L

    private const val STALE = "stale"

    private val mutableState = MutableStateFlow(ForecastState())
    val state: StateFlow<ForecastState> = mutableState.asStateFlow()

    private val lock = Mutex()
    private var routeKey: String? = null

    private fun routeKeyOf(settings: Settings): String =
        "${settings.home?.lat},${settings.home?.lon}|${settings.work?.lat},${settings.work?.lon}|${settings.useRadar}"

    private fun isFresh(settings: Settings): Boolean {
        val forecast = mutableState.value.forecast ?: return false
        return routeKey == routeKeyOf(settings) && System.currentTimeMillis() - forecast.fetchedAt < FRESH_FOR_MS
    }

    suspend fun refresh(context: Context, force: Boolean = false) {
        val settings = SettingsStore.get(context).current
        val home = settings.home ?: return
        val work = settings.work ?: return
        if (!force && isFresh(settings)) return

        lock.withLock {
            if (!force && isFresh(settings)) return
            mutableState.value = mutableState.value.copy(loading = true, error = null)
            try {
                val forecast = WeatherApi.fetch(home, work, settings.useRadar)
                val sameRoute = routeKey == routeKeyOf(settings)
                val previous = mutableState.value.forecast
                if (forecast.hasModels || previous == null || !sameRoute) {
                    routeKey = routeKeyOf(settings)
                    mutableState.value = ForecastState(loading = false, forecast = forecast, error = null)
                    WidgetUpdater.publish(context, settings, forecast)
                } else {
                    mutableState.value = mutableState.value.copy(loading = false, error = STALE)
                }
            } catch (e: Throwable) {
                mutableState.value = mutableState.value.copy(
                    loading = false,
                    error = e.message ?: e::class.java.simpleName
                )
            }
        }
    }

    suspend fun fetchDirect(settings: Settings): RouteForecast? {
        val home = settings.home ?: return null
        val work = settings.work ?: return null
        val cached = mutableState.value.forecast
        if (cached != null && routeKey == routeKeyOf(settings) &&
            System.currentTimeMillis() - cached.fetchedAt < 10 * 60 * 1000L
        ) return cached
        return runCatching { WeatherApi.fetch(home, work, settings.useRadar) }
            .getOrNull()
            ?.let { fresh ->
                if (!fresh.hasModels && cached != null && routeKey == routeKeyOf(settings)) return cached
                routeKey = routeKeyOf(settings)
                mutableState.value = ForecastState(loading = false, forecast = fresh, error = null)
                fresh
            }
    }

    fun invalidate() {
        routeKey = null
        mutableState.value = ForecastState()
    }
}

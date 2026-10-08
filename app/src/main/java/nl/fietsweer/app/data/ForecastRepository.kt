package nl.fietsweer.app.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ForecastState(
    val loading: Boolean = false,
    val forecast: RouteForecast? = null,
    val failed: Boolean = false
)

object ForecastRepository {

    private const val FRESH_FOR_MS = 15 * 60 * 1000L
    private const val DIRECT_FRESH_FOR_MS = 10 * 60 * 1000L

    private val mutableState = MutableStateFlow(ForecastState())
    val state: StateFlow<ForecastState> = mutableState.asStateFlow()

    private val lock = Mutex()
    private var routeKey: String? = null

    private fun routeKeyOf(settings: Settings): String =
        "${settings.home?.lat},${settings.home?.lon}|${settings.work?.lat},${settings.work?.lon}|${settings.useRadar}"

    private fun isSameRoute(settings: Settings): Boolean = routeKey == routeKeyOf(settings)

    private fun isFresh(settings: Settings, maxAgeMs: Long = FRESH_FOR_MS): Boolean {
        val forecast = mutableState.value.forecast ?: return false
        return isSameRoute(settings) && System.currentTimeMillis() - forecast.fetchedAt < maxAgeMs
    }

    suspend fun refresh(
        context: Context,
        force: Boolean = false,
        onNewForecast: (Settings, RouteForecast) -> Unit
    ) {
        val settings = SettingsStore.get(context).current
        val home = settings.home ?: return
        val work = settings.work ?: return
        if (!force && isFresh(settings)) return

        lock.withLock {
            if (!force && isFresh(settings)) return
            mutableState.value = mutableState.value.copy(loading = true, failed = false)
            try {
                val forecast = WeatherApi.fetch(home, work, settings.useRadar)
                val previous = mutableState.value.forecast
                if (forecast.hasModels || previous == null || !isSameRoute(settings)) {
                    routeKey = routeKeyOf(settings)
                    mutableState.value = ForecastState(loading = false, forecast = forecast)
                    onNewForecast(settings, forecast)
                } else {
                    mutableState.value = mutableState.value.copy(loading = false, failed = true)
                }
            } catch (e: CancellationException) {
                mutableState.value = mutableState.value.copy(loading = false)
                throw e
            } catch (e: Throwable) {
                mutableState.value = mutableState.value.copy(loading = false, failed = true)
            }
        }
    }

    suspend fun fetchDirect(settings: Settings): RouteForecast? {
        val home = settings.home ?: return null
        val work = settings.work ?: return null
        val cached = mutableState.value.forecast
        if (isFresh(settings, DIRECT_FRESH_FOR_MS)) return cached
        return runCatching { WeatherApi.fetch(home, work, settings.useRadar) }
            .getOrNull()
            ?.let { fresh ->
                if (!fresh.hasModels && cached != null && isSameRoute(settings)) return cached
                routeKey = routeKeyOf(settings)
                mutableState.value = ForecastState(loading = false, forecast = fresh)
                fresh
            }
    }

    fun invalidate() {
        routeKey = null
        mutableState.value = ForecastState()
    }
}

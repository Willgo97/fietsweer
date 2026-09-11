package nl.fietsweer.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ForecastUi(
    val loading: Boolean = false,
    val forecast: RouteForecast? = null,
    val error: String? = null
)

/**
 * Owns the last forecast bundle. One refresh at a time, and a stale copy is
 * kept on screen while a new one is being fetched.
 */
object Repository {

    private const val FRESH_MS = 15 * 60 * 1000L

    /** Marks "the last refresh failed but what you see is still real". */
    const val STALE = "stale"

    private val _state = MutableStateFlow(ForecastUi())
    val state: StateFlow<ForecastUi> = _state.asStateFlow()

    private val lock = Mutex()
    private var routeKey: String? = null

    private fun keyOf(s: Settings): String =
        "${s.home?.lat},${s.home?.lon}|${s.work?.lat},${s.work?.lon}|${s.useRadar}"

    fun isFresh(s: Settings): Boolean {
        val f = _state.value.forecast ?: return false
        return routeKey == keyOf(s) && System.currentTimeMillis() - f.fetchedAt < FRESH_MS
    }

    suspend fun refresh(context: Context, force: Boolean = false) {
        val settings = Prefs.get(context).current
        val home = settings.home ?: return
        val work = settings.work ?: return
        if (!force && isFresh(settings)) return

        lock.withLock {
            if (!force && isFresh(settings)) return
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val fc = WeatherApi.fetch(home, work, settings.useRadar)
                val sameRoute = routeKey == keyOf(settings)
                val previous = _state.value.forecast
                if (fc.hasModels || previous == null || !sameRoute) {
                    routeKey = keyOf(settings)
                    _state.value = ForecastUi(loading = false, forecast = fc, error = null)
                    nl.fietsweer.app.widget.WidgetUpdater.publish(context, settings, fc)
                } else {
                    // A failed refresh should not throw away a perfectly good
                    // forecast; keep it on screen and mark it stale instead.
                    _state.value = _state.value.copy(loading = false, error = STALE)
                }
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = t.message ?: t::class.java.simpleName
                )
            }
        }
    }

    /** Used by the background worker, which wants a bundle without touching UI state. */
    suspend fun fetchDirect(settings: Settings): RouteForecast? {
        val home = settings.home ?: return null
        val work = settings.work ?: return null
        val cached = _state.value.forecast
        if (cached != null && routeKey == keyOf(settings) &&
            System.currentTimeMillis() - cached.fetchedAt < 10 * 60 * 1000L
        ) return cached
        return runCatching { WeatherApi.fetch(home, work, settings.useRadar) }
            .getOrNull()
            ?.let { fresh ->
                if (!fresh.hasModels && cached != null && routeKey == keyOf(settings)) return cached
                routeKey = keyOf(settings)
                _state.value = ForecastUi(loading = false, forecast = fresh, error = null)
                fresh
            }
    }

    fun invalidate() {
        routeKey = null
        _state.value = ForecastUi()
    }
}

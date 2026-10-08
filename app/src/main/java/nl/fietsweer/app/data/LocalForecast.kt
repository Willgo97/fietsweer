package nl.fietsweer.app.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import nl.fietsweer.app.data.DeviceLocation.toLatLon
import java.util.Locale
import kotlin.coroutines.resume

class LocalWeather(val placeName: String, val point: LatLon, val forecast: RouteForecast)

// Weather where the phone is, but only once it is away from the commute:
// near home, work or the route the route forecast already says it all.
object LocalForecast {

    private const val AWAY_KM = 10.0
    private const val MOVED_KM = 3.0
    private const val FRESH_MS = 15 * 60_000L
    private const val LOCATION_MAX_AGE_MS = 30 * 60_000L
    private const val FIX_TIMEOUT_MS = 20_000L

    private val mutableState = MutableStateFlow<LocalWeather?>(null)
    val state: StateFlow<LocalWeather?> = mutableState.asStateFlow()

    private val mutablePosition = MutableStateFlow<LatLon?>(null)
    val position: StateFlow<LatLon?> = mutablePosition.asStateFlow()

    private val lock = Mutex()

    suspend fun refresh(context: Context, settings: Settings, language: String) {
        val home = settings.home ?: return
        val work = settings.work ?: return
        if (!DeviceLocation.hasPermission(context)) {
            mutableState.value = null
            return
        }
        lock.withLock {
            val here = currentLocation(context)
            if (here != null) mutablePosition.value = here
            if (here == null || !isAway(here, home, work)) {
                mutableState.value = null
                return
            }
            val cached = mutableState.value
            if (cached != null && Geo.haversineKm(cached.point, here) < MOVED_KM &&
                System.currentTimeMillis() - cached.forecast.fetchedAt < FRESH_MS
            ) return

            val name = Geocoder.town(here, language)
                ?: "%.2f, %.2f".format(Locale.US, here.lat, here.lon)
            val place = Place(name, here.lat, here.lon)
            val forecast = runCatching { WeatherApi.fetch(place, place, settings.useRadar, pointCount = 1) }
                .getOrNull()
                ?.takeIf { it.hasModels }
                ?: return
            mutableState.value = LocalWeather(name, here, forecast)
        }
    }

    private fun isAway(here: LatLon, home: Place, work: Place): Boolean {
        val anchors = listOf(home.toLatLon(), work.toLatLon(), Geo.midpoint(home.toLatLon(), work.toLatLon()))
        return anchors.minOf { Geo.haversineKm(here, it) } > AWAY_KM
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(context: Context): LatLon? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        if (!LocationManagerCompat.isLocationEnabled(manager)) return null

        val last = DeviceLocation.lastKnown(manager)
        if (last != null && System.currentTimeMillis() - last.time < LOCATION_MAX_AGE_MS) return last.toLatLon()

        // Phones without Google services have no network location, so ask every provider
        // at once and take whichever answers first.
        val providers = manager.getProviders(true).filter { it != LocationManager.PASSIVE_PROVIDER }
        if (providers.isEmpty()) return last?.toLatLon()
        val fresh = withTimeoutOrNull(FIX_TIMEOUT_MS) {
            suspendCancellableCoroutine<Location> { continuation ->
                val cancels = providers.map { CancellationSignal() }
                continuation.invokeOnCancellation { cancels.forEach(CancellationSignal::cancel) }
                providers.forEachIndexed { i, provider ->
                    LocationManagerCompat.getCurrentLocation(
                        manager, provider, cancels[i], ContextCompat.getMainExecutor(context)
                    ) { location ->
                        if (location != null && continuation.isActive) {
                            cancels.forEach(CancellationSignal::cancel)
                            continuation.resume(location)
                        }
                    }
                }
            }
        }
        return (fresh ?: last)?.toLatLon()
    }
}

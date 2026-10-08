package nl.fietsweer.app.ui.map

import android.Manifest
import android.content.Context
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import nl.fietsweer.app.data.DeviceLocation
import nl.fietsweer.app.data.DeviceLocation.toLatLon
import nl.fietsweer.app.data.Geocoder
import nl.fietsweer.app.data.LatLon
import nl.fietsweer.app.data.Place
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.ui.components.ButtonLabel
import nl.fietsweer.app.ui.components.SectionLabel
import nl.fietsweer.app.ui.theme.AppTheme
import java.util.Locale

enum class RouteEnd { HOME, WORK }

private val CenterOfNetherlands = LatLon(52.1326, 5.2913)

private const val KNOWN_PLACE_ZOOM = 15f
private const val COUNTRY_ZOOM = 12f
private const val LOCATED_ZOOM = 16f
private const val FIRST_RESULT_ZOOM = 14f
private const val PICKED_RESULT_ZOOM = 14.5f

// Nominatim allows one request a second, so wait for the map to settle before asking.
private const val REVERSE_LOOKUP_DELAY_MS = 700L
private const val SEARCH_DELAY_MS = 280L
private const val MIN_QUERY_LENGTH = 2

@Composable
fun RouteEndPicker(
    end: RouteEnd,
    title: String,
    settings: Settings,
    mapTheme: MapTheme,
    onCancel: () -> Unit,
    onConfirm: (Place) -> Unit
) {
    val accents = AppTheme.accents
    val initial = if (end == RouteEnd.HOME) settings.home else settings.work
    LocationPickerScreen(
        title = title,
        initial = initial,
        fallback = (initial ?: settings.home)?.toLatLon() ?: CenterOfNetherlands,
        mapTheme = mapTheme,
        accent = if (end == RouteEnd.HOME) accents.dry else accents.rain,
        onCancel = onCancel,
        onConfirm = onConfirm
    )
}

@Composable
private fun LocationPickerScreen(
    title: String,
    initial: Place?,
    fallback: LatLon,
    mapTheme: MapTheme,
    accent: Color,
    onCancel: () -> Unit,
    onConfirm: (Place) -> Unit
) {
    val strings = AppTheme.strings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    val camera = rememberMapCamera(
        initial?.lat ?: fallback.lat,
        initial?.lon ?: fallback.lon,
        if (initial != null) KNOWN_PLACE_ZOOM else COUNTRY_ZOOM
    )

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }
    var resolved by remember { mutableStateOf(initial) }
    var resolving by remember { mutableStateOf(false) }
    var dragTick by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }

    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Restarts on every drag, so only the place where the map comes to rest is looked up.
    LaunchedEffect(dragTick) {
        if (dragTick == 0) return@LaunchedEffect
        resolving = true
        delay(REVERSE_LOOKUP_DELAY_MS)
        val place = Geocoder.reverse(camera.center, strings.locale.language)
        ensureActive()
        resolved = place ?: camera.centerPlace()
        resolving = false
    }

    fun jumpToLastLocation() {
        val location = (context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)
            ?.let(DeviceLocation::lastKnown)
        if (location != null) {
            camera.moveTo(location.toLatLon(), LOCATED_ZOOM)
            dragTick++
        } else message = strings.locationUnavailable
    }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) jumpToLastLocation() else message = strings.locationDenied
    }

    Box(Modifier.fillMaxSize()) {

        TileMap(
            camera = camera,
            source = mapTheme.source,
            darken = mapTheme.darken,
            modifier = Modifier.fillMaxSize(),
            onMoved = {
                dragTick++
                showResults = false
            }
        )

        Box(
            Modifier
                .align(Alignment.Center)
                .padding(bottom = 34.dp)
        ) {
            CenterPin(accent)
        }

        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(12.dp)
        ) {
            PlaceSearchField(
                query = query,
                searching = searching,
                onQueryChange = { text ->
                    query = text
                    searchJob?.cancel()
                    if (text.trim().length < MIN_QUERY_LENGTH) {
                        results = emptyList(); showResults = false
                    } else {
                        searchJob = scope.launch {
                            delay(SEARCH_DELAY_MS)
                            searching = true
                            try {
                                results = try {
                                    Geocoder.search(text, camera.center, strings.locale.language)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    emptyList()
                                }
                                showResults = true
                            } finally {
                                searching = false
                            }
                        }
                    }
                },
                onClear = { searchJob?.cancel(); query = ""; results = emptyList(); showResults = false },
                onSearch = {
                    keyboard?.hide()
                    results.firstOrNull()?.let {
                        camera.moveTo(it.toLatLon(), FIRST_RESULT_ZOOM)
                        resolved = it
                        showResults = false
                    }
                },
                onBack = onCancel
            )
            PlaceSearchResults(
                visible = showResults,
                results = results,
                origin = camera.center,
                onPick = { place ->
                    keyboard?.hide()
                    camera.moveTo(place.toLatLon(), PICKED_RESULT_ZOOM)
                    resolved = place
                    showResults = false
                    query = place.name
                }
            )
        }

        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MapButton(Icons.Rounded.Add) { camera.zoomBy(1) }
            MapButton(Icons.Rounded.Remove) { camera.zoomBy(-1) }
            MapButton(Icons.Rounded.MyLocation) {
                if (DeviceLocation.hasPermission(context)) {
                    jumpToLastLocation()
                } else {
                    locationPermission.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    )
                }
            }
        }

        Surface(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp
        ) {
            Column(Modifier.padding(20.dp)) {
                SectionLabel(title)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            resolved?.name ?: strings.dragMapHint,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1
                        )
                        Text(
                            message ?: resolved?.detail?.takeIf { it.isNotBlank() }
                            ?: camera.centerPlace().name,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (message != null) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    if (resolving) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        onConfirm((resolved ?: camera.centerPlace()).copy(lat = camera.lat, lon = camera.lon))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    ButtonLabel(strings.confirmLocation, Icons.Rounded.Check)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    mapTheme.attribution,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun MapCamera.centerPlace() =
    Place("%.4f, %.4f".format(Locale.US, lat, lon), lat, lon)

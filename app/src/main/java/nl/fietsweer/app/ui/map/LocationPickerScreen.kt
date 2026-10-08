package nl.fietsweer.app.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.fietsweer.app.data.CenterOfNetherlands
import nl.fietsweer.app.data.Geocoder
import nl.fietsweer.app.data.LatLon
import nl.fietsweer.app.data.Place
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.ui.components.ButtonLabel
import nl.fietsweer.app.ui.components.SectionLabel
import nl.fietsweer.app.ui.theme.AppTheme

enum class RouteEnd { HOME, WORK }

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
        if (initial != null) 15f else 12f
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

    // Restarts on every drag: a debounce, as Nominatim is rate limited.
    LaunchedEffect(dragTick) {
        if (dragTick == 0) return@LaunchedEffect
        resolving = true
        delay(700)
        val place = Geocoder.reverse(camera.center, strings.locale.language)
        resolved = place ?: camera.centerPlace()
        resolving = false
    }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) {
            useLastLocation(context) { location ->
                if (location != null) { camera.moveTo(location, 16f); dragTick++ }
                else message = strings.locationUnavailable
            }
        } else message = strings.locationDenied
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
                    if (text.trim().length < 2) {
                        results = emptyList(); showResults = false
                    } else {
                        searchJob = scope.launch {
                            delay(280)
                            searching = true
                            results = runCatching {
                                Geocoder.search(text, camera.center, strings.locale.language)
                            }.getOrDefault(emptyList())
                            searching = false
                            showResults = true
                        }
                    }
                },
                onClear = { query = ""; results = emptyList(); showResults = false },
                onSearch = {
                    keyboard?.hide()
                    results.firstOrNull()?.let {
                        camera.moveTo(it.toLatLon(), 14f)
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
                    camera.moveTo(place.toLatLon(), 14.5f)
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
            MapButton(Icons.Rounded.Add) {
                camera.zoom = (camera.zoom + 1f).coerceAtMost(MapCamera.MAX_ZOOM)
            }
            MapButton(Icons.Rounded.Remove) {
                camera.zoom = (camera.zoom - 1f).coerceAtLeast(MapCamera.MIN_ZOOM)
            }
            MapButton(Icons.Rounded.MyLocation) {
                val fine = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val coarse = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (fine || coarse) {
                    useLastLocation(context) { location ->
                        if (location != null) {
                            camera.moveTo(location, 16f); dragTick++
                        } else message = strings.locationUnavailable
                    }
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
    Place("%.4f, %.4f".format(java.util.Locale.US, lat, lon), lat, lon)

@SuppressLint("MissingPermission")
private fun useLastLocation(context: Context, onResult: (LatLon?) -> Unit) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (locationManager == null) { onResult(null); return }
    val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    )
    var newest: android.location.Location? = null
    for (provider in providers) {
        val location = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() ?: continue
        if (newest == null || location.time > newest!!.time) newest = location
    }
    onResult(newest?.let { LatLon(it.latitude, it.longitude) })
}

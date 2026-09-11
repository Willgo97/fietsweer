package nl.fietsweer.app.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.fietsweer.app.data.Geocoder
import nl.fietsweer.app.data.Place
import nl.fietsweer.app.domain.Geo
import nl.fietsweer.app.domain.LatLon
import nl.fietsweer.app.ui.theme.AppTheme
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

@Composable
fun LocationPickerScreen(
    title: String,
    initial: Place?,
    fallback: LatLon,
    mapTheme: MapTheme,
    accent: Color,
    onCancel: () -> Unit,
    onConfirm: (Place) -> Unit
) {
    val t = AppTheme.txt
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

    // Reverse geocode once the map has been still for a moment. Keying the
    // effect on the drag counter gives the debounce for free: every new drag
    // cancels the pending lookup.
    LaunchedEffect(dragTick) {
        if (dragTick == 0) return@LaunchedEffect
        resolving = true
        delay(700)
        val p = Geocoder.reverse(camera.center, t.locale.language)
        resolved = p ?: Place(
            name = "%.4f, %.4f".format(java.util.Locale.US, camera.lat, camera.lon),
            lat = camera.lat, lon = camera.lon
        )
        resolving = false
    }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) {
            useLastLocation(context) { p ->
                if (p != null) { camera.moveTo(p, 16f); dragTick++ }
                else message = t.locationUnavailable
            }
        } else message = t.locationDenied
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

        // centre pin, drawn as an overlay so it never lags behind the gesture
        Box(
            Modifier
                .align(Alignment.Center)
                .padding(bottom = 34.dp)
        ) {
            CenterPin(accent)
        }

        // ----------------------------------------------------------- top bar
        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, t.back)
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { v ->
                            query = v
                            searchJob?.cancel()
                            if (v.trim().length < 2) {
                                results = emptyList(); showResults = false
                            } else {
                                searchJob = scope.launch {
                                    delay(280)
                                    searching = true
                                    results = runCatching {
                                        Geocoder.search(v, camera.center, t.locale.language)
                                    }.getOrDefault(emptyList())
                                    searching = false
                                    showResults = true
                                }
                            }
                        },
                        placeholder = { Text(t.searchPlace) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboard?.hide()
                            results.firstOrNull()?.let {
                                camera.moveTo(it.toLatLon(), 14f)
                                resolved = it
                                showResults = false
                            }
                        })
                    )
                    if (searching) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 0.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                    } else if (query.isNotEmpty()) {
                        IconButton(onClick = { query = ""; results = emptyList(); showResults = false }) {
                            Icon(Icons.Rounded.Close, t.close)
                        }
                    } else {
                        Icon(
                            Icons.Rounded.Search, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(showResults, enter = fadeIn(), exit = fadeOut()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .heightIn(max = 280.dp)
                ) {
                    LazyColumn {
                        if (results.isEmpty()) {
                            item {
                                Text(
                                    t.searchNoResults,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
                                )
                            }
                        }
                        items(results) { p ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        keyboard?.hide()
                                        camera.moveTo(p.toLatLon(), 14.5f)
                                        resolved = p
                                        showResults = false
                                        query = p.name
                                    }
                                    .padding(horizontal = 18.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, style = MaterialTheme.typography.bodyLarge)
                                    if (p.detail.isNotBlank()) {
                                        Text(
                                            p.detail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    "${Geo.haversineKm(camera.center, p.toLatLon()).toInt()} km",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------- controls
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
                    useLastLocation(context) { p ->
                        if (p != null) {
                            camera.moveTo(p, 16f); dragTick++
                        } else message = t.locationUnavailable
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

        // ------------------------------------------------------------ bottom
        Surface(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            resolved?.name ?: t.dragMapHint,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1
                        )
                        Text(
                            message ?: resolved?.detail?.takeIf { it.isNotBlank() }
                            ?: "%.4f, %.4f".format(java.util.Locale.US, camera.lat, camera.lon),
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
                        val p = resolved ?: Place(
                            "%.4f, %.4f".format(java.util.Locale.US, camera.lat, camera.lon),
                            camera.lat, camera.lon
                        )
                        onConfirm(p.copy(lat = camera.lat, lon = camera.lon))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(t.confirmLocation)
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

@Composable
private fun MapButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = Modifier.size(44.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun CenterPin(accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(30.dp)
                .background(accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(11.dp)
                    .background(Color.White, CircleShape)
            )
        }
        Box(
            Modifier
                .width(3.dp)
                .height(20.dp)
                .background(accent)
        )
        Box(
            Modifier
                .size(7.dp)
                .background(Color.Black.copy(alpha = 0.28f), CircleShape)
        )
    }
}

@SuppressLint("MissingPermission")
private fun useLastLocation(context: Context, onResult: (LatLon?) -> Unit) {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (lm == null) { onResult(null); return }
    val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    )
    var best: android.location.Location? = null
    for (p in providers) {
        val loc = runCatching { lm.getLastKnownLocation(p) }.getOrNull() ?: continue
        if (best == null || loc.time > best!!.time) best = loc
    }
    onResult(best?.let { LatLon(it.latitude, it.longitude) })
}

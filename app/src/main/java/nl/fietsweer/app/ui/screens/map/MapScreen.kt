package nl.fietsweer.app.ui.screens.map

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch
import nl.fietsweer.app.data.Geo
import nl.fietsweer.app.data.LocalForecast
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.ui.components.FillScreen
import nl.fietsweer.app.ui.components.NoRouteState
import nl.fietsweer.app.ui.map.MapTheme
import nl.fietsweer.app.ui.theme.AppTheme

@Composable
fun MapScreen(
    settings: Settings,
    contentPadding: PaddingValues,
    mapTheme: MapTheme,
    onSetup: () -> Unit
) {
    val strings = AppTheme.strings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val here by LocalForecast.position.collectAsState()

    val home = settings.home
    val work = settings.work
    if (home == null || work == null) {
        NoRouteState(onSetup)
        return
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        scope.launch { LocalForecast.refresh(context, settings, strings.locale.language) }
    }

    val centre = here ?: Geo.midpoint(home.toLatLon(), work.toLatLon())
    FillScreen(contentPadding, stretchIndex = 0, stretchMin = 240.dp) {
        WeatherMap(centre, here, home.toLatLon(), work.toLatLon(), mapTheme)
    }
}

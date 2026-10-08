package nl.fietsweer.app.ui.screens.settings

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.data.Geo
import nl.fietsweer.app.data.Place
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.ui.components.NavigationRow
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.components.SoftDivider
import nl.fietsweer.app.ui.map.MapCamera
import nl.fietsweer.app.ui.map.MapLine
import nl.fietsweer.app.ui.map.MapMarker
import nl.fietsweer.app.ui.map.MapTheme
import nl.fietsweer.app.ui.map.TileMap
import nl.fietsweer.app.ui.map.zoomForPair
import nl.fietsweer.app.ui.theme.AppTheme

@Composable
internal fun RoutePage(
    settings: Settings,
    mapTheme: MapTheme,
    onPickHome: () -> Unit,
    onPickWork: () -> Unit
) {
    val strings = AppTheme.strings
    SectionCard {
        if (settings.home != null && settings.work != null) {
            RoutePreview(settings, mapTheme)
            Spacer(Modifier.height(12.dp))
        }
        NavigationRow(strings.home, Icons.Rounded.Home, settings.home?.summary() ?: strings.setHomeBody, onPickHome)
        SoftDivider()
        NavigationRow(strings.work, Icons.Rounded.Work, settings.work?.summary() ?: strings.setWorkBody, onPickWork)
    }
}

private fun Place.summary(): String =
    listOfNotNull(name, detail.takeIf { it.isNotBlank() }).joinToString(" · ")

@Composable
private fun RoutePreview(settings: Settings, mapTheme: MapTheme) {
    val home = settings.home ?: return
    val work = settings.work ?: return
    val accents = AppTheme.accents
    val density = LocalDensity.current.density

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val midpoint = Geo.midpoint(home.toLatLon(), work.toLatLon())
        val camera = remember(home, work, widthPx) {
            MapCamera(midpoint.lat, midpoint.lon, zoomForPair(home.toLatLon(), work.toLatLon(), widthPx, density))
        }
        TileMap(
            camera = camera,
            source = mapTheme.source,
            darken = mapTheme.darken,
            modifier = Modifier.fillMaxSize(),
            markers = listOf(
                MapMarker(home.toLatLon(), accents.dry),
                MapMarker(work.toLatLon(), accents.rain)
            ),
            lines = listOf(
                MapLine(
                    Geo.samplePoints(home.toLatLon(), work.toLatLon(), 12),
                    MaterialTheme.colorScheme.primary
                )
            ),
            interactive = false
        )
    }
}

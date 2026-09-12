package nl.fietsweer.app.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.FrameLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.data.Lang
import nl.fietsweer.app.data.MapStyle
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.data.ThemeMode
import nl.fietsweer.app.domain.Engine
import nl.fietsweer.app.domain.Geo
import nl.fietsweer.app.ui.components.LabeledSlider
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.components.SegmentedChoice
import nl.fietsweer.app.ui.components.SettingRow
import nl.fietsweer.app.ui.components.TimePickerDialog
import nl.fietsweer.app.ui.map.MapCamera
import nl.fietsweer.app.ui.map.MapLine
import nl.fietsweer.app.ui.map.MapMarker
import nl.fietsweer.app.ui.map.TileMap
import nl.fietsweer.app.ui.map.MapTheme
import nl.fietsweer.app.ui.map.zoomForPair
import nl.fietsweer.app.ui.theme.AppTheme
import nl.fietsweer.app.widget.JacketWidget
import nl.fietsweer.app.widget.WidgetRenderer
import kotlin.math.roundToInt
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

@Composable
fun SettingsScreen(
    settings: Settings,
    forecast: nl.fietsweer.app.data.RouteForecast?,
    versionName: String,
    mapTheme: MapTheme,
    contentPadding: PaddingValues,
    onUpdate: ((Settings) -> Settings) -> Unit,
    onPickHome: () -> Unit,
    onPickWork: () -> Unit,
    onResetSetup: () -> Unit
) {
    val t = AppTheme.txt
    val fmt = AppTheme.fmt
    val accents = AppTheme.accents

    var editingOutbound by remember { mutableStateOf(false) }
    var editingReturn by remember { mutableStateOf(false) }

    var speed by remember(settings.speedKmh) { mutableFloatStateOf(settings.speedKmh.toFloat()) }
    var rainPct by remember(settings.rainJacketPercent) {
        mutableFloatStateOf(settings.rainJacketPercent.toFloat())
    }
    var vestTemp by remember(settings.vestBelow) {
        mutableFloatStateOf(settings.vestBelow.toFloat())
    }
    var winterTemp by remember(settings.winterCoatBelow) {
        mutableFloatStateOf(settings.winterCoatBelow.toFloat())
    }

    val distance = remember(settings.home, settings.work) {
        val h = settings.home; val w = settings.work
        if (h == null || w == null) 0.0
        else Geo.haversineKm(h.toLatLon(), w.toLatLon()) * Geo.DETOUR_FACTOR
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text(t.tabSettings, style = MaterialTheme.typography.headlineMedium) }

        // ----------------------------------------------------------- route
        item {
            SectionCard(title = t.settingsRoute) {
                if (settings.home != null && settings.work != null) {
                    RoutePreview(settings, mapTheme)
                    Spacer(Modifier.height(12.dp))
                }
                SettingRow(
                    title = t.home,
                    subtitle = settings.home?.let {
                        listOfNotNull(it.name, it.detail.takeIf { d -> d.isNotBlank() })
                            .joinToString(" · ")
                    } ?: t.setHomeBody,
                    icon = Icons.Rounded.Home,
                    trailing = { Icon(Icons.Rounded.ChevronRight, null) },
                    onClick = onPickHome
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                SettingRow(
                    title = t.work,
                    subtitle = settings.work?.let {
                        listOfNotNull(it.name, it.detail.takeIf { d -> d.isNotBlank() })
                            .joinToString(" · ")
                    } ?: t.setWorkBody,
                    icon = Icons.Rounded.Work,
                    trailing = { Icon(Icons.Rounded.ChevronRight, null) },
                    onClick = onPickWork
                )
            }
        }

        // ------------------------------------------------------------ times
        item {
            SectionCard(title = t.settingsTimes) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimeTile(
                        t.outboundTime,
                        settings.outboundHour, settings.outboundMinute,
                        Modifier.weight(1f)
                    ) { editingOutbound = true }
                    TimeTile(
                        t.returnTime,
                        settings.returnHour, settings.returnMinute,
                        Modifier.weight(1f)
                    ) { editingReturn = true }
                }
            }
        }

        // ----------------------------------------------------------- riding
        item {
            SectionCard(title = t.settingsRiding) {
                LabeledSlider(
                    label = t.cyclingSpeed,
                    valueText = t.rideTimeIs(
                        fmt.km(distance),
                        Engine.rideDurationMin(distance, speed.toDouble())
                    ),
                    value = speed,
                    range = 10f..32f,
                    steps = 0,
                    onChange = { speed = it },
                    onChangeFinished = { onUpdate { s -> s.copy(speedKmh = speed.roundToInt()) } }
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${speed.roundToInt()} ${t.speedUnit} · ${t.cyclingSpeedBody}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                SettingRow(
                    title = t.windAdjust,
                    subtitle = t.windAdjustBody,
                    icon = Icons.Rounded.Air,
                    trailing = {
                        Switch(
                            checked = settings.windAdjustSpeed,
                            onCheckedChange = { v -> onUpdate { it.copy(windAdjustSpeed = v) } }
                        )
                    }
                )
            }
        }

        // ----------------------------------------------------------- advice
        item {
            SectionCard(title = t.settingsAdvice) {
                Text(t.whatIsWet, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                SegmentedChoice(
                    options = listOf(
                        0.05 to t.wetEveryDrop,
                        0.2 to t.wetDrizzle,
                        0.6 to t.wetShower
                    ),
                    selected = settings.wetThreshold,
                    onSelect = { v -> onUpdate { it.copy(wetThreshold = v) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(18.dp))
                LabeledSlider(
                    label = t.rainJacketFrom,
                    valueText = t.rainJacketFromValue(rainPct.roundToInt()),
                    value = rainPct,
                    range = 10f..70f,
                    steps = 0,
                    onChange = { rainPct = it },
                    onChangeFinished = {
                        onUpdate { it.copy(rainJacketPercent = rainPct.roundToInt()) }
                    }
                )
                Spacer(Modifier.height(10.dp))
                LabeledSlider(
                    label = t.vestBelowLabel,
                    valueText = t.feltOnBike(fmt.temp(vestTemp.toDouble())),
                    value = vestTemp,
                    range = 8f..26f,
                    steps = 0,
                    onChange = { vestTemp = it },
                    onChangeFinished = {
                        onUpdate {
                            // The ladder only makes sense in order.
                            val vest = vestTemp.toDouble()
                            it.copy(
                                vestBelow = vest,
                                winterCoatBelow = minOf(it.winterCoatBelow, vest - 2.0)
                            )
                        }
                    }
                )
                Spacer(Modifier.height(10.dp))
                LabeledSlider(
                    label = t.winterBelowLabel,
                    valueText = t.feltOnBike(fmt.temp(winterTemp.toDouble())),
                    value = winterTemp,
                    range = -6f..18f,
                    steps = 0,
                    onChange = { winterTemp = it },
                    onChangeFinished = {
                        onUpdate {
                            val winter = winterTemp.toDouble()
                            it.copy(
                                winterCoatBelow = winter,
                                vestBelow = maxOf(it.vestBelow, winter + 2.0)
                            )
                        }
                    }
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    t.layerLadder(fmt.temp(vestTemp.toDouble()), fmt.temp(winterTemp.toDouble())),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                SettingRow(
                    title = t.useRadarTitle,
                    subtitle = t.useRadarBody,
                    icon = Icons.Rounded.Radar,
                    trailing = {
                        Switch(
                            checked = settings.useRadar,
                            onCheckedChange = { v -> onUpdate { it.copy(useRadar = v) } }
                        )
                    }
                )
            }
        }

        // --------------------------------------------------------- appearance
        item {
            SectionCard(title = t.settingsLook) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.DarkMode, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(t.theme, style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(Modifier.height(8.dp))
                SegmentedChoice(
                    options = listOf(
                        ThemeMode.SYSTEM to t.themeSystem,
                        ThemeMode.LIGHT to t.themeLight,
                        ThemeMode.DARK to t.themeDark
                    ),
                    selected = settings.theme,
                    onSelect = { v -> onUpdate { it.copy(theme = v) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(6.dp))
                SettingRow(
                    title = t.dynamicColour,
                    icon = Icons.Rounded.Palette,
                    trailing = {
                        Switch(
                            checked = settings.dynamicColor,
                            onCheckedChange = { v -> onUpdate { it.copy(dynamicColor = v) } }
                        )
                    }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Language, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(t.language, style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(Modifier.height(8.dp))
                SegmentedChoice(
                    options = listOf(
                        Lang.SYSTEM to t.langSystem,
                        Lang.NL to t.langNl,
                        Lang.EN to t.langEn
                    ),
                    selected = settings.lang,
                    onSelect = { v -> onUpdate { it.copy(lang = v) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Map, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(t.mapStyleTitle, style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(Modifier.height(8.dp))
                SegmentedChoice(
                    options = listOf(
                        MapStyle.AUTO to t.mapAuto,
                        MapStyle.LIGHT to t.mapLight,
                        MapStyle.DARK to t.mapDark,
                        MapStyle.SOFT to t.mapSoft
                    ),
                    selected = settings.mapStyle,
                    onSelect = { v -> onUpdate { it.copy(mapStyle = v) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ----------------------------------------------------------- widget
        item {
            val context = LocalContext.current
            SectionCard(title = t.settingsWidget) {
                val snapshot = remember(settings, forecast) {
                    WidgetRenderer.snapshotFor(settings, forecast)
                }
                WidgetPreview(t.widgetSizeNormal, 132) {
                    WidgetRenderer.single(it, settings, snapshot, compact = false)
                }
                Spacer(Modifier.height(14.dp))
                WidgetPreview(t.widgetSizeSlim, 64) {
                    WidgetRenderer.single(it, settings, snapshot, compact = true)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    t.widgetResizeHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = {
                        val manager = AppWidgetManager.getInstance(context)
                        val provider = ComponentName(context, JacketWidget::class.java)
                        if (manager.isRequestPinAppWidgetSupported) {
                            manager.requestPinAppWidget(provider, null, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Widgets, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(t.widgetAdd)
                }
            }
        }

        // ------------------------------------------------------------ about
        item {
            SectionCard(title = t.settingsAbout) {
                Text(t.aboutBody, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                Text(
                    t.aboutData,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    t.version(versionName),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onResetSetup) {
                    Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(t.resetSetup)
                }
            }
        }
    }

    if (editingOutbound) {
        TimePickerDialog(
            settings.outboundHour, settings.outboundMinute, t.outboundTime,
            onDismiss = { editingOutbound = false },
            onConfirm = { h, m ->
                onUpdate { it.copy(outboundHour = h, outboundMinute = m) }
                editingOutbound = false
            }
        )
    }
    if (editingReturn) {
        TimePickerDialog(
            settings.returnHour, settings.returnMinute, t.returnTime,
            onDismiss = { editingReturn = false },
            onConfirm = { h, m ->
                onUpdate { it.copy(returnHour = h, returnMinute = m) }
                editingReturn = false
            }
        )
    }
}

@Composable
private fun WidgetPreview(
    label: String,
    height: Int,
    build: (android.content.Context) -> android.widget.RemoteViews
) {
    Text(
        label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(6.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .height(height.dp)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx -> FrameLayout(ctx) },
            update = { frame ->
                frame.removeAllViews()
                frame.addView(build(frame.context).apply(frame.context, frame))
            }
        )
    }
}

@Composable
private fun TimeTile(
    label: String,
    hour: Int,
    minute: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            Spacer(Modifier.height(6.dp))
            Text(
                String.format(java.util.Locale.ROOT, "%02d:%02d", hour, minute),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

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
        val mid = Geo.midpoint(home.toLatLon(), work.toLatLon())
        val camera = remember(home, work, widthPx) {
            MapCamera(mid.lat, mid.lon, zoomForPair(home.toLatLon(), work.toLatLon(), widthPx, density))
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

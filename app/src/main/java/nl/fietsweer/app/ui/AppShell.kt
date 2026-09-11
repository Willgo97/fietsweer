package nl.fietsweer.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.fietsweer.app.data.Alert
import nl.fietsweer.app.data.LatLonFallback
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.data.ThemeMode
import nl.fietsweer.app.domain.Txt
import nl.fietsweer.app.ui.map.LocationPickerScreen
import nl.fietsweer.app.ui.map.MapTheme
import nl.fietsweer.app.ui.map.mapThemeFor
import nl.fietsweer.app.ui.screens.AlertEditorScreen
import nl.fietsweer.app.ui.screens.AlertsScreen
import nl.fietsweer.app.ui.screens.ForecastScreen
import nl.fietsweer.app.ui.screens.OnboardingFlow
import nl.fietsweer.app.ui.screens.SettingsScreen
import nl.fietsweer.app.ui.screens.TodayScreen
import nl.fietsweer.app.ui.theme.AppTheme
import nl.fietsweer.app.ui.theme.FietsweerTheme
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

private sealed interface Overlay {
    data object None : Overlay
    data object PickHome : Overlay
    data object PickWork : Overlay
    data class EditAlert(val alert: Alert, val isNew: Boolean) : Overlay
}

private enum class Tab(val icon: ImageVector) {
    TODAY(Icons.Rounded.WbSunny),
    FORECAST(Icons.Rounded.Insights),
    ALERTS(Icons.Rounded.Notifications),
    SETTINGS(Icons.Rounded.Settings)
}

@Composable
fun FietsweerRoot(vm: AppViewModel, versionName: String) {
    val settings by vm.settings.collectAsState()
    val txt = remember(settings.lang) { Txt.of(settings.lang) }

    FietsweerTheme(
        themeMode = settings.theme,
        dynamicColor = settings.dynamicColor,
        txt = txt
    ) {
        val dark = when (settings.theme) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        val mapTheme = mapThemeFor(settings.mapStyle, dark)

        if (!settings.setupDone) {
            OnboardingFlow(
                settings = settings,
                mapTheme = mapTheme,
                onUpdate = { vm.update(it) },
                onFinish = { vm.refresh(force = true) }
            )
        } else {
            MainShell(vm, settings, versionName, mapTheme)
        }
    }
}

@Composable
private fun MainShell(
    vm: AppViewModel,
    settings: Settings,
    versionName: String,
    mapTheme: MapTheme
) {
    val t = AppTheme.txt
    val ui by vm.forecast.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var overlay by remember { mutableStateOf<Overlay>(Overlay.None) }
    var nowTick by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            nowTick = System.currentTimeMillis()
            delay(60_000)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        nowTick = System.currentTimeMillis()
        vm.refresh(force = false)
        vm.rescheduleAlarms()
    }

    BackHandler(enabled = overlay != Overlay.None) { overlay = Overlay.None }
    BackHandler(enabled = overlay == Overlay.None && tab != 0) { tab = 0 }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEachIndexed { index, entry ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { Icon(entry.icon, null) },
                            label = {
                                Text(
                                    when (entry) {
                                        Tab.TODAY -> t.tabToday
                                        Tab.FORECAST -> t.tabForecast
                                        Tab.ALERTS -> t.tabAlerts
                                        Tab.SETTINGS -> t.tabSettings
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            alwaysShowLabel = true
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { inner ->
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally { if (forward) it / 8 else -it / 8 } + fadeIn()) togetherWith
                        (slideOutHorizontally { if (forward) -it / 12 else it / 12 } + fadeOut())
                },
                label = "tabs"
            ) { current ->
                when (current) {
                    0 -> TodayScreen(
                        settings = settings,
                        ui = ui,
                        nowTick = nowTick,
                        contentPadding = inner,
                        onRefresh = { vm.refresh(force = true) },
                        onOpenForecast = { tab = 1 },
                        onSetup = { vm.update { s -> s.copy(setupDone = false) } }
                    )

                    1 -> ForecastScreen(
                        settings = settings,
                        ui = ui,
                        contentPadding = inner,
                        onRefresh = { vm.refresh(force = true) },
                        onSetup = { vm.update { s -> s.copy(setupDone = false) } }
                    )

                    2 -> AlertsScreen(
                        settings = settings,
                        contentPadding = inner,
                        onEdit = { overlay = Overlay.EditAlert(it, false) },
                        onToggle = { vm.saveAlert(it) },
                        onNew = { overlay = Overlay.EditAlert(vm.newAlertTemplate(), true) },
                        onTest = {
                            vm.sendTestNotification { ok ->
                                scope.launch {
                                    snackbar.showMessage(if (ok) t.done else t.updateFailedTitle)
                                }
                            }
                        }
                    )

                    else -> SettingsScreen(
                        settings = settings,
                        forecast = ui.forecast,
                        versionName = versionName,
                        mapTheme = mapTheme,
                        contentPadding = inner,
                        onUpdate = { vm.update(it) },
                        onPickHome = { overlay = Overlay.PickHome },
                        onPickWork = { overlay = Overlay.PickWork },
                        onResetSetup = { vm.update { s -> s.copy(setupDone = false) } }
                    )
                }
            }
        }

        when (val o = overlay) {
            Overlay.None -> Unit

            Overlay.PickHome -> LocationPickerScreen(
                title = t.home,
                initial = settings.home,
                fallback = settings.home?.toLatLon() ?: LatLonFallback,
                mapTheme = mapTheme,
                accent = AppTheme.accents.dry,
                onCancel = { overlay = Overlay.None },
                onConfirm = { p ->
                    vm.update { it.copy(home = p) }
                    overlay = Overlay.None
                }
            )

            Overlay.PickWork -> LocationPickerScreen(
                title = t.work,
                initial = settings.work,
                fallback = settings.work?.toLatLon() ?: settings.home?.toLatLon() ?: LatLonFallback,
                mapTheme = mapTheme,
                accent = AppTheme.accents.rain,
                onCancel = { overlay = Overlay.None },
                onConfirm = { p ->
                    vm.update { it.copy(work = p) }
                    overlay = Overlay.None
                }
            )

            is Overlay.EditAlert -> AlertEditorScreen(
                original = o.alert,
                isNew = o.isNew,
                onClose = { overlay = Overlay.None },
                onSave = { vm.saveAlert(it); overlay = Overlay.None },
                onDelete = { vm.deleteAlert(it); overlay = Overlay.None }
            )
        }
    }
}

private suspend fun SnackbarHostState.showMessage(text: String) {
    currentSnackbarData?.dismiss()
    showSnackbar(text)
}

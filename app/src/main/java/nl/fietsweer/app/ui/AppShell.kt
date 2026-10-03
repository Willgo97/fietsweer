package nl.fietsweer.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.fietsweer.app.data.Alert
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.domain.Strings
import nl.fietsweer.app.ui.map.MapTheme
import nl.fietsweer.app.ui.map.RouteEnd
import nl.fietsweer.app.ui.map.RouteEndPicker
import nl.fietsweer.app.ui.map.mapThemeFor
import nl.fietsweer.app.ui.screens.forecast.ForecastScreen
import nl.fietsweer.app.ui.screens.onboarding.OnboardingFlow
import nl.fietsweer.app.ui.screens.settings.AlertEditorScreen
import nl.fietsweer.app.ui.screens.settings.AlertsPage
import nl.fietsweer.app.ui.screens.settings.SettingsPage
import nl.fietsweer.app.ui.screens.settings.SettingsScreen
import nl.fietsweer.app.ui.screens.today.TodayScreen
import nl.fietsweer.app.ui.theme.AppTheme
import nl.fietsweer.app.ui.theme.FietsweerTheme
import nl.fietsweer.app.ui.theme.isDark

private sealed interface Overlay {
    data object None : Overlay
    data object PickHome : Overlay
    data object PickWork : Overlay
    data class EditAlert(val alert: Alert, val isNew: Boolean) : Overlay
}

private enum class Tab(val icon: ImageVector) {
    TODAY(Icons.Rounded.WbSunny),
    FORECAST(Icons.Rounded.Insights),
    SETTINGS(Icons.Rounded.Settings)
}

@Composable
fun FietsweerRoot(viewModel: AppViewModel, versionName: String) {
    val settings by viewModel.settings.collectAsState()
    val strings = remember(settings.language) { Strings.of(settings.language) }

    FietsweerTheme(
        themeMode = settings.theme,
        dynamicColor = settings.dynamicColor,
        strings = strings
    ) {
        val mapTheme = mapThemeFor(settings.mapStyle, settings.theme.isDark())

        if (!settings.setupDone) {
            OnboardingFlow(
                settings = settings,
                mapTheme = mapTheme,
                onUpdate = { viewModel.update(it) },
                onFinish = { viewModel.refresh(force = true) }
            )
        } else {
            MainShell(viewModel, settings, versionName, mapTheme)
        }
    }
}

@Composable
private fun MainShell(
    viewModel: AppViewModel,
    settings: Settings,
    versionName: String,
    mapTheme: MapTheme
) {
    val strings = AppTheme.strings
    val forecastState by viewModel.forecast.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var overlay by remember { mutableStateOf<Overlay>(Overlay.None) }
    var settingsPage by rememberSaveable { mutableStateOf(SettingsPage.MENU) }
    var nowTick by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            nowTick = System.currentTimeMillis()
            delay(60_000)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        nowTick = System.currentTimeMillis()
        viewModel.refresh(force = false)
        viewModel.rescheduleAlarms()
    }

    val inSubPage = selectedTab == 2 && settingsPage != SettingsPage.MENU
    BackHandler(enabled = overlay != Overlay.None) { overlay = Overlay.None }
    BackHandler(enabled = overlay == Overlay.None && inSubPage) { settingsPage = SettingsPage.MENU }
    BackHandler(enabled = overlay == Overlay.None && !inSubPage && selectedTab != 0) { selectedTab = 0 }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = {
                                if (index == 2 && selectedTab == 2) settingsPage = SettingsPage.MENU
                                selectedTab = index
                            },
                            icon = { Icon(tab.icon, null) },
                            label = {
                                Text(
                                    when (tab) {
                                        Tab.TODAY -> strings.tabToday
                                        Tab.FORECAST -> strings.tabForecast
                                        Tab.SETTINGS -> strings.tabSettings
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
        ) { innerPadding ->
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally { if (forward) it / 8 else -it / 8 } + fadeIn()) togetherWith
                        (slideOutHorizontally { if (forward) -it / 12 else it / 12 } + fadeOut())
                },
                label = "tabs"
            ) { tab ->
                when (tab) {
                    0 -> TodayScreen(
                        settings = settings,
                        forecastState = forecastState,
                        nowTick = nowTick,
                        contentPadding = innerPadding,
                        onRefresh = { viewModel.refresh(force = true) },
                        onOpenForecast = { selectedTab = 1 },
                        onSetup = { viewModel.update { it.copy(setupDone = false) } }
                    )

                    1 -> ForecastScreen(
                        settings = settings,
                        forecastState = forecastState,
                        contentPadding = innerPadding,
                        onRefresh = { viewModel.refresh(force = true) },
                        onSetup = { viewModel.update { it.copy(setupDone = false) } }
                    )

                    else -> if (settingsPage == SettingsPage.ALERTS) AlertsPage(
                        settings = settings,
                        contentPadding = innerPadding,
                        onEdit = { overlay = Overlay.EditAlert(it, false) },
                        onToggle = { viewModel.saveAlert(it) },
                        onNew = { overlay = Overlay.EditAlert(Alert.create(), true) },
                        onTest = {
                            viewModel.sendTestNotification { sent ->
                                scope.launch {
                                    snackbar.showMessage(if (sent) strings.done else strings.updateFailedTitle)
                                }
                            }
                        },
                        onBack = { settingsPage = SettingsPage.MENU }
                    ) else SettingsScreen(
                        page = settingsPage,
                        onPage = { settingsPage = it },
                        settings = settings,
                        forecast = forecastState.forecast,
                        versionName = versionName,
                        mapTheme = mapTheme,
                        contentPadding = innerPadding,
                        onUpdate = { viewModel.update(it) },
                        onPickHome = { overlay = Overlay.PickHome },
                        onPickWork = { overlay = Overlay.PickWork },
                        onResetSetup = { viewModel.update { it.copy(setupDone = false) } }
                    )
                }
            }
        }

        when (val current = overlay) {
            Overlay.None -> Unit

            Overlay.PickHome -> RouteEndPicker(
                end = RouteEnd.HOME,
                title = strings.home,
                settings = settings,
                mapTheme = mapTheme,
                onCancel = { overlay = Overlay.None },
                onConfirm = { place ->
                    viewModel.update { it.copy(home = place) }
                    overlay = Overlay.None
                }
            )

            Overlay.PickWork -> RouteEndPicker(
                end = RouteEnd.WORK,
                title = strings.work,
                settings = settings,
                mapTheme = mapTheme,
                onCancel = { overlay = Overlay.None },
                onConfirm = { place ->
                    viewModel.update { it.copy(work = place) }
                    overlay = Overlay.None
                }
            )

            is Overlay.EditAlert -> AlertEditorScreen(
                original = current.alert,
                isNew = current.isNew,
                onClose = { overlay = Overlay.None },
                onSave = { viewModel.saveAlert(it); overlay = Overlay.None },
                onDelete = { viewModel.deleteAlert(it); overlay = Overlay.None }
            )
        }
    }
}

private suspend fun SnackbarHostState.showMessage(text: String) {
    currentSnackbarData?.dismiss()
    showSnackbar(text)
}

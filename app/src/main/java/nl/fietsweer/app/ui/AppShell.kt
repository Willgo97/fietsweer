package nl.fietsweer.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.fietsweer.app.data.Alert
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.domain.Strings
import nl.fietsweer.app.ui.components.CARD_GAP
import nl.fietsweer.app.ui.components.CARD_SHAPE
import nl.fietsweer.app.ui.map.MapTheme
import nl.fietsweer.app.ui.map.RouteEnd
import nl.fietsweer.app.ui.map.RouteEndPicker
import nl.fietsweer.app.ui.map.mapThemeFor
import nl.fietsweer.app.ui.screens.forecast.ForecastScreen
import nl.fietsweer.app.ui.screens.map.MapScreen
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
    data class PickRouteEnd(val end: RouteEnd) : Overlay
    data class EditAlert(val alert: Alert, val isNew: Boolean) : Overlay
}

private enum class Tab(val icon: ImageVector) {
    TODAY(Icons.AutoMirrored.Rounded.DirectionsBike),
    FORECAST(Icons.Rounded.Insights),
    MAP(Icons.Rounded.Map),
    SETTINGS(Icons.Rounded.Settings)
}

@Composable
fun FietsweerRoot(viewModel: AppViewModel, versionName: String) {
    val settings by viewModel.settings.collectAsState()
    val strings = remember(settings.language) { Strings.of(settings.language) }

    FietsweerTheme(
        themeMode = settings.theme,
        accent = settings.accent,
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

    var selectedTab by rememberSaveable { mutableStateOf(Tab.TODAY) }
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
        viewModel.refresh(force = true)
        viewModel.rescheduleAlarms()
    }

    val refreshNow = { viewModel.refresh(force = true) }
    val inSubPage = selectedTab == Tab.SETTINGS && settingsPage != SettingsPage.MENU
    BackHandler(enabled = overlay != Overlay.None) { overlay = Overlay.None }
    BackHandler(enabled = overlay == Overlay.None && inSubPage) { settingsPage = SettingsPage.MENU }
    BackHandler(enabled = overlay == Overlay.None && !inSubPage && selectedTab != Tab.TODAY) { selectedTab = Tab.TODAY }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                TabBar(selectedTab) { tab ->
                    if (tab == Tab.SETTINGS && selectedTab == Tab.SETTINGS) settingsPage = SettingsPage.MENU
                    selectedTab = tab
                }
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { innerPadding ->
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    (slideInHorizontally { if (forward) it / 8 else -it / 8 } + fadeIn()) togetherWith
                        (slideOutHorizontally { if (forward) -it / 12 else it / 12 } + fadeOut())
                },
                label = "tabs"
            ) { tab ->
                when (tab) {
                    Tab.TODAY -> TodayScreen(
                        settings = settings,
                        forecastState = forecastState,
                        nowTick = nowTick,
                        contentPadding = innerPadding,
                        onRefresh = refreshNow,
                        onSetup = viewModel::restartSetup
                    )

                    Tab.FORECAST -> ForecastScreen(
                        settings = settings,
                        forecastState = forecastState,
                        contentPadding = innerPadding,
                        onRefresh = refreshNow,
                        onSetup = viewModel::restartSetup,
                        onAskedLocation = { viewModel.update { it.copy(askedLocation = true) } }
                    )

                    Tab.MAP -> MapScreen(
                        settings = settings,
                        contentPadding = innerPadding,
                        mapTheme = mapTheme,
                        onSetup = viewModel::restartSetup
                    )

                    Tab.SETTINGS -> if (settingsPage == SettingsPage.ALERTS) AlertsPage(
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
                        onPickHome = { overlay = Overlay.PickRouteEnd(RouteEnd.HOME) },
                        onPickWork = { overlay = Overlay.PickRouteEnd(RouteEnd.WORK) },
                        onResetSetup = viewModel::restartSetup
                    )
                }
            }
        }

        when (val current = overlay) {
            Overlay.None -> Unit

            is Overlay.PickRouteEnd -> RouteEndPicker(
                end = current.end,
                title = if (current.end == RouteEnd.HOME) strings.home else strings.work,
                settings = settings,
                mapTheme = mapTheme,
                onCancel = { overlay = Overlay.None },
                onConfirm = { place ->
                    viewModel.update { if (current.end == RouteEnd.HOME) it.copy(home = place) else it.copy(work = place) }
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

@Composable
private fun TabBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val strings = AppTheme.strings
    Card(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(CARD_GAP)
            .fillMaxWidth(),
        shape = CARD_SHAPE,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Tab.entries.forEach { tab ->
                val active = tab == selected
                Box(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                        .clickable { onSelect(tab) }
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        tab.icon,
                        when (tab) {
                            Tab.TODAY -> strings.tabToday
                            Tab.FORECAST -> strings.tabForecast
                            Tab.MAP -> strings.tabMap
                            Tab.SETTINGS -> strings.tabSettings
                        },
                        tint = if (active) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

package nl.fietsweer.app.ui.screens.today

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.data.Coverage
import nl.fietsweer.app.data.ForecastState
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.domain.Commute
import nl.fietsweer.app.domain.Engine
import nl.fietsweer.app.domain.Jacket
import nl.fietsweer.app.domain.departureWindow
import nl.fietsweer.app.ui.components.FillScreen
import nl.fietsweer.app.ui.components.InfoCard
import nl.fietsweer.app.ui.components.LoadingBlock
import nl.fietsweer.app.ui.components.NoForecastCard
import nl.fietsweer.app.ui.components.NoRouteState
import nl.fietsweer.app.ui.components.ScreenList
import nl.fietsweer.app.ui.theme.AppTheme

@Composable
fun TodayScreen(
    settings: Settings,
    forecastState: ForecastState,
    nowTick: Long,
    contentPadding: PaddingValues,
    onRefresh: () -> Unit,
    onSetup: () -> Unit
) {
    val strings = AppTheme.strings
    val format = AppTheme.format
    val accents = AppTheme.accents

    if (!settings.hasRoute) {
        NoRouteState(onSetup)
        return
    }

    val forecast = forecastState.forecast
    val engine = remember(forecast, settings) { forecast?.let { Engine(it, settings) } }
    val plannedRides = remember(settings, nowTick) {
        Commute.plannedRides(settings, Coverage.BOTH, nowTick, alertDaysOnly = true)
    }
    val rides = remember(engine, plannedRides) {
        engine?.let { plannedRides.map(it::assess) }.orEmpty()
    }
    val advice = remember(rides) { Jacket.forNextDay(rides, settings) }
    val nextWindow = remember(forecast, engine, plannedRides, nowTick) {
        val planned = plannedRides.firstOrNull()
        if (forecast == null || engine == null || planned == null) null
        else departureWindow(forecast, engine, planned, nowTick)
    }

    val errorCard: (@Composable () -> Unit)? = if (!forecastState.failed) null else {
        {
            InfoCard(
                title = strings.updateFailedTitle,
                body = if (forecast == null) strings.updateFailedBody else strings.staleNotice(format.time(forecast.fetchedAt)),
                actionLabel = strings.retry,
                accent = if (forecast == null) MaterialTheme.colorScheme.error else accents.uncertain,
                onAction = onRefresh
            )
        }
    }

    if (forecast == null || !forecast.hasModels) {
        ScreenList(contentPadding) {
            if (errorCard != null) item { errorCard() }
            if (forecast == null) item { LoadingBlock(strings.updating) }
            else item { NoForecastCard(forecast, onRefresh) }
        }
        return
    }

    val stretchIndex = if (nextWindow == null || rides.isEmpty()) -1 else if (errorCard != null) 2 else 1
    FillScreen(contentPadding, stretchIndex, stretchMin = 230.dp) {
        errorCard?.invoke()
        HeroCard(advice, forecast.current)
        rides.forEachIndexed { i, ride ->
            RideCard(ride, if (i == 0) nextWindow else null)
        }
    }
}

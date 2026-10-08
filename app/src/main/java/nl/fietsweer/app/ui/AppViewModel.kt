package nl.fietsweer.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nl.fietsweer.app.data.Alert
import nl.fietsweer.app.data.Coverage
import nl.fietsweer.app.data.ForecastRepository
import nl.fietsweer.app.data.ForecastState
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.data.SettingsStore
import nl.fietsweer.app.domain.Commute
import nl.fietsweer.app.domain.Engine
import nl.fietsweer.app.domain.Jacket
import nl.fietsweer.app.domain.Strings
import nl.fietsweer.app.notify.AlertScheduler
import nl.fietsweer.app.notify.Notifier
import nl.fietsweer.app.ui.screens.map.RadarAnimation
import nl.fietsweer.app.widget.WidgetUpdater

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SettingsStore.get(application)

    val settings: StateFlow<Settings> = store.state
    val forecast: StateFlow<ForecastState> = ForecastRepository.state

    fun update(change: (Settings) -> Settings) {
        val before = store.current
        store.update(change)
        val after = store.current
        if (before.home != after.home || before.work != after.work || before.useRadar != after.useRadar) {
            ForecastRepository.invalidate()
            refresh(force = true)
        }
        if (before.alerts != after.alerts) {
            AlertScheduler.rescheduleAll(getApplication())
        }
        if (before.widgetStyle != after.widgetStyle || before.language != after.language) {
            WidgetUpdater.redraw(getApplication())
        }
        if (before.language != after.language) {
            Notifier.ensureChannel(getApplication(), Strings.of(after.language))
        }
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            ForecastRepository.refresh(getApplication(), force)
            RadarAnimation.prepare()
        }
    }

    fun saveAlert(alert: Alert) = update { it.withAlert(alert) }

    fun deleteAlert(alertId: String) = update { settings ->
        settings.copy(alerts = settings.alerts.filterNot { it.id == alertId })
    }

    fun sendTestNotification(onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val settings = store.current
            val strings = Strings.of(settings.language)
            Notifier.ensureChannel(getApplication(), strings)
            val forecast = ForecastRepository.state.value.forecast ?: ForecastRepository.fetchDirect(settings)
            if (forecast == null || !forecast.hasModels) {
                Notifier.postProblem(getApplication(), strings, strings.updateFailedBody)
                onDone(false)
                return@launch
            }
            val engine = Engine(forecast, settings)
            val rides = Commute.plannedRides(settings, Coverage.BOTH).map(engine::assess)
            Notifier.postAdvice(getApplication(), null, Jacket.forNextDay(rides, settings), strings)
            onDone(true)
        }
    }

    fun rescheduleAlarms() = AlertScheduler.rescheduleAll(getApplication())
}

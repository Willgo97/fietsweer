package nl.fietsweer.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nl.fietsweer.app.data.Alert
import nl.fietsweer.app.data.ForecastUi
import nl.fietsweer.app.data.Prefs
import nl.fietsweer.app.data.Repository
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.domain.Engine
import nl.fietsweer.app.domain.Jacket
import nl.fietsweer.app.domain.Txt
import nl.fietsweer.app.notify.AlertScheduler
import nl.fietsweer.app.notify.Commute
import nl.fietsweer.app.notify.Notifier
import java.util.UUID

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs.get(app)

    val settings: StateFlow<Settings> = prefs.state
    val forecast: StateFlow<ForecastUi> = Repository.state

    fun update(block: (Settings) -> Settings) {
        val before = prefs.current
        prefs.update(block)
        val after = prefs.current
        if (before.home != after.home || before.work != after.work || before.useRadar != after.useRadar) {
            Repository.invalidate()
            refresh(force = true)
        }
        if (before.alerts != after.alerts) {
            AlertScheduler.rescheduleAll(getApplication())
        }
        if (before.lang != after.lang) {
            Notifier.ensureChannel(getApplication(), Txt.of(after.lang))
        }
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            Repository.refresh(getApplication(), force)
        }
    }

    fun saveAlert(alert: Alert) = update { s ->
        val list = s.alerts.toMutableList()
        val i = list.indexOfFirst { it.id == alert.id }
        if (i >= 0) list[i] = alert else list.add(alert)
        s.copy(alerts = list.sortedBy { it.minutesOfDay })
    }

    fun deleteAlert(id: String) = update { s ->
        s.copy(alerts = s.alerts.filterNot { it.id == id })
    }

    fun newAlertTemplate(): Alert = Alert(
        id = UUID.randomUUID().toString(),
        label = "",
        hour = 7,
        minute = 15,
        days = setOf(1, 2, 3, 4, 5)
    )

    /** Builds the verdict from whatever forecast is on screen and posts it. */
    fun sendTestNotification(onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val s = prefs.current
            val txt = Txt.of(s.lang)
            Notifier.ensureChannel(getApplication(), txt)
            val fc = Repository.state.value.forecast ?: Repository.fetchDirect(s)
            if (fc == null || !fc.hasModels) {
                Notifier.postProblem(getApplication(), txt, txt.updateFailedBody)
                onDone(false)
                return@launch
            }
            val engine = Engine(fc, s)
            val rides = Commute.plannedRides(s, nl.fietsweer.app.data.Coverage.BOTH)
                .map { (leg, at) -> engine.assess(at, leg) }
            Notifier.postAdvice(getApplication(), null, Jacket.forRides(rides, s), txt)
            onDone(true)
        }
    }

    fun rescheduleAlarms() = AlertScheduler.rescheduleAll(getApplication())
}

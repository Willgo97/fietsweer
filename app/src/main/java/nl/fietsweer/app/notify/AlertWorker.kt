package nl.fietsweer.app.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import nl.fietsweer.app.data.Coverage
import nl.fietsweer.app.data.Prefs
import nl.fietsweer.app.data.Repository
import nl.fietsweer.app.domain.Engine
import nl.fietsweer.app.domain.Jacket
import nl.fietsweer.app.domain.Txt

/** Fetches a fresh forecast and posts the jacket verdict for one alert. */
class AlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = Prefs.get(applicationContext)
        prefs.reload()
        val settings = prefs.current
        val txt = Txt.of(settings.lang)

        if (!settings.ready) return Result.success()

        val alertId = inputData.getString(AlertReceiver.EXTRA_ALERT_ID).orEmpty()
        val alert = settings.alerts.firstOrNull { it.id == alertId }
        val coverage = alert?.coverage ?: Coverage.BOTH

        val forecast = Repository.fetchDirect(settings)
        if (forecast == null || !forecast.hasModels) {
            return if (runAttemptCount < 3) Result.retry() else {
                Notifier.postProblem(applicationContext, txt, txt.updateFailedBody)
                Result.success()
            }
        }

        val engine = Engine(forecast, settings)
        val rides = Commute.plannedRides(settings, coverage).map { (leg, at) ->
            engine.assess(at, leg)
        }
        val advice = Jacket.forRides(rides, settings)

        if (alert?.onlyWhenNeeded == true && !advice.anythingNeeded) {
            return Result.success()
        }

        Notifier.postAdvice(applicationContext, alert, advice, txt)
        prefs.update { it.copy(lastNotifiedAt = System.currentTimeMillis()) }
        nl.fietsweer.app.widget.WidgetUpdater.redraw(applicationContext)
        return Result.success()
    }
}

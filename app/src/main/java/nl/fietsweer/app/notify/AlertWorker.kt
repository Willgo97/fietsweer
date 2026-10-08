package nl.fietsweer.app.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import nl.fietsweer.app.data.Coverage
import nl.fietsweer.app.data.ForecastRepository
import nl.fietsweer.app.data.SettingsStore
import nl.fietsweer.app.domain.Jacket
import nl.fietsweer.app.domain.Strings
import nl.fietsweer.app.widget.WidgetUpdater

class AlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsStore.get(applicationContext).current
        val strings = Strings.of(settings.language)

        if (!settings.hasRoute) return Result.success()

        val alertId = inputData.getString(AlertReceiver.EXTRA_ALERT_ID).orEmpty()
        val alert = settings.alerts.firstOrNull { it.id == alertId }
        val coverage = alert?.coverage ?: Coverage.BOTH

        val forecast = ForecastRepository.fetchDirect(settings)
        if (forecast == null || !forecast.hasModels) {
            return if (runAttemptCount < 3) Result.retry() else {
                Notifier.postProblem(applicationContext, strings, strings.updateFailedBody)
                Result.success()
            }
        }

        val advice = Jacket.forCommute(forecast, settings, coverage)

        if (alert?.onlyWhenNeeded == true && !advice.anythingNeeded) {
            return Result.success()
        }

        Notifier.postAdvice(applicationContext, alert, advice, strings)
        WidgetUpdater.publish(applicationContext, settings, forecast)
        return Result.success()
    }
}

package nl.fietsweer.app

import android.app.Application
import nl.fietsweer.app.data.SettingsStore
import nl.fietsweer.app.domain.Strings
import nl.fietsweer.app.notify.Notifier

class FietsweerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val settings = SettingsStore.get(this).current
        Notifier.ensureChannel(this, Strings.of(settings.language))
    }
}

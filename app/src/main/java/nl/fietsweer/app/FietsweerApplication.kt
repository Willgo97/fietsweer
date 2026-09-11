package nl.fietsweer.app

import android.app.Application
import nl.fietsweer.app.data.Prefs
import nl.fietsweer.app.domain.Txt
import nl.fietsweer.app.notify.Notifier

class FietsweerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val settings = Prefs.get(this).current
        Notifier.ensureChannel(this, Txt.of(settings.lang))
    }
}

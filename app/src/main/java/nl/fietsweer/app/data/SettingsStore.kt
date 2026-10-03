package nl.fietsweer.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

// One JSON blob, read synchronously by alarm receivers and workers.
class SettingsStore private constructor(context: Context) {

    private val preferences = context.applicationContext
        .getSharedPreferences("fietsweer", Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private val mutableState = MutableStateFlow(read())
    val state: StateFlow<Settings> = mutableState.asStateFlow()

    val current: Settings get() = mutableState.value

    private fun read(): Settings {
        val stored = preferences.getString(KEY, null) ?: return Settings()
        return runCatching { json.decodeFromString<Settings>(stored) }.getOrElse { Settings() }
    }

    fun update(change: (Settings) -> Settings) {
        val next = change(mutableState.value)
        preferences.edit().putString(KEY, json.encodeToString(next)).apply()
        mutableState.value = next
    }

    fun reload() {
        mutableState.value = read()
    }

    companion object {
        private const val KEY = "settings_v1"

        @Volatile
        private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context).also { instance = it }
            }
    }
}

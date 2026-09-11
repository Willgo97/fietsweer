package nl.fietsweer.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Settings live in a single JSON blob in SharedPreferences. That keeps reads
 * synchronous, which matters because alarm receivers and workers need the
 * configuration immediately, without a coroutine hop.
 */
class Prefs private constructor(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("fietsweer", Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private val _state = MutableStateFlow(read())
    val state: StateFlow<Settings> = _state.asStateFlow()

    val current: Settings get() = _state.value

    private fun read(): Settings {
        val raw = sp.getString(KEY, null) ?: return Settings()
        return runCatching { json.decodeFromString<Settings>(raw) }.getOrElse { Settings() }
    }

    fun update(block: (Settings) -> Settings) {
        val next = block(_state.value)
        sp.edit().putString(KEY, json.encodeToString(next)).apply()
        _state.value = next
    }

    /** Re-read from disk; used by background workers that may see other writes. */
    fun reload() {
        _state.value = read()
    }

    companion object {
        private const val KEY = "settings_v1"

        @Volatile
        private var instance: Prefs? = null

        fun get(context: Context): Prefs =
            instance ?: synchronized(this) {
                instance ?: Prefs(context).also { instance = it }
            }
    }
}

package nl.fietsweer.app.widget

import android.content.Context

data class WidgetSnapshot(
    val headline: String,
    val chipLine: String,
    val firstLegLine: String,
    val secondLegLine: String,
    val updatedTime: String,
    val accent: Int
)

object WidgetStore {

    private const val FILE = "widget_snapshot"

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun save(context: Context, snapshot: WidgetSnapshot) {
        preferences(context).edit()
            .putString("headline", snapshot.headline)
            .putString("chips", snapshot.chipLine)
            .putString("leg1", snapshot.firstLegLine)
            .putString("leg2", snapshot.secondLegLine)
            .putString("stamp", snapshot.updatedTime)
            .putInt("accent", snapshot.accent)
            .apply()
    }

    fun load(context: Context): WidgetSnapshot? {
        val stored = preferences(context)
        val headline = stored.getString("headline", null) ?: return null
        return WidgetSnapshot(
            headline = headline,
            chipLine = stored.getString("chips", "").orEmpty(),
            firstLegLine = stored.getString("leg1", "").orEmpty(),
            secondLegLine = stored.getString("leg2", "").orEmpty(),
            updatedTime = stored.getString("stamp", "").orEmpty(),
            accent = stored.getInt("accent", 0)
        )
    }

    fun clear(context: Context) = preferences(context).edit().clear().apply()

    // Separate from the snapshot time, so a failing fetch cannot retrigger itself in a loop.
    fun lastAttempt(context: Context): Long = preferences(context).getLong("lastAttempt", 0L)

    fun markAttempt(context: Context, atMs: Long) {
        preferences(context).edit().putLong("lastAttempt", atMs).apply()
    }
}

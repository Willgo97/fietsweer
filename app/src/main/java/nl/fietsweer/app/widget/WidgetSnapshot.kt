package nl.fietsweer.app.widget

import android.content.Context

/**
 * The handful of strings the widget draws, kept on disk.
 *
 * The forecast itself only lives in memory, so once Android reclaims the app
 * process the widget has nothing to draw. Without a snapshot it would fall back
 * to its "could not fetch" layout and then flip to real content a moment later
 * when the worker finishes — a visible flicker between two very different
 * layouts, and a pointless one, because the previous answer was still perfectly
 * good. Persisting the drawn result means a redraw is always cheap and always
 * shows something true.
 */
data class WidgetSnapshot(
    val headline: String,
    val chips: String,
    val leg1: String,
    val leg2: String,
    val stamp: String,
    val accent: Int,
    val savedAt: Long
)

object WidgetStore {

    private const val FILE = "widget_snapshot"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun save(context: Context, snapshot: WidgetSnapshot) {
        prefs(context).edit()
            .putString("headline", snapshot.headline)
            .putString("chips", snapshot.chips)
            .putString("leg1", snapshot.leg1)
            .putString("leg2", snapshot.leg2)
            .putString("stamp", snapshot.stamp)
            .putInt("accent", snapshot.accent)
            .putLong("savedAt", snapshot.savedAt)
            .apply()
    }

    fun load(context: Context): WidgetSnapshot? {
        val p = prefs(context)
        val headline = p.getString("headline", null) ?: return null
        return WidgetSnapshot(
            headline = headline,
            chips = p.getString("chips", "").orEmpty(),
            leg1 = p.getString("leg1", "").orEmpty(),
            leg2 = p.getString("leg2", "").orEmpty(),
            stamp = p.getString("stamp", "").orEmpty(),
            accent = p.getInt("accent", 0),
            savedAt = p.getLong("savedAt", 0L)
        )
    }

    fun clear(context: Context) = prefs(context).edit().clear().apply()

    /**
     * When we last *tried* to refresh, success or not. Separate from the
     * snapshot's own timestamp: a fetch that keeps failing must still not be
     * allowed to retrigger itself in a tight loop.
     */
    fun lastAttempt(context: Context): Long = prefs(context).getLong("lastAttempt", 0L)

    fun markAttempt(context: Context, at: Long) {
        prefs(context).edit().putLong("lastAttempt", at).apply()
    }
}

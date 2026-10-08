package nl.fietsweer.app.data

import org.json.JSONArray

internal fun JSONArray.toMillis(): LongArray =
    LongArray(length()) { optLong(it) * 1000L }

internal fun JSONArray.toDoubles(): DoubleArray =
    DoubleArray(length()) { if (isNull(it)) Double.NaN else optDouble(it, Double.NaN) }

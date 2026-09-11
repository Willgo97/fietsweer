package nl.fietsweer.app.data

import nl.fietsweer.app.domain.Geo
import nl.fietsweer.app.domain.LatLon
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Place lookup. Forward search uses the Open-Meteo geocoder (fast, no rate
 * limit worth worrying about); reverse lookup uses Nominatim, which is only
 * called when the user actually stops dragging the map.
 */
object Geocoder {

    suspend fun search(query: String, near: LatLon?, language: String): List<Place> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        val url = "https://geocoding-api.open-meteo.com/v1/search" +
            "?name=${URLEncoder.encode(q, "UTF-8")}&count=12&language=$language&format=json"
        val body = Net.getText(url, 12_000)
        val results = JSONObject(body).optJSONArray("results") ?: return emptyList()
        val out = ArrayList<Place>(results.length())
        for (i in 0 until results.length()) {
            val o = results.getJSONObject(i)
            val name = o.optString("name").ifBlank { continue }
            val lat = o.optDouble("latitude", Double.NaN)
            val lon = o.optDouble("longitude", Double.NaN)
            if (lat.isNaN() || lon.isNaN()) continue
            val admin2 = o.optString("admin2").takeIf { it.isNotBlank() && it != name }
            val admin1 = o.optString("admin1").takeIf { it.isNotBlank() && it != name }
            val country = o.optString("country_code").takeIf { it.isNotBlank() }
            val detail = listOfNotNull(admin2 ?: admin1, country).joinToString(", ")
            out += Place(name, lat, lon, detail)
        }
        return if (near == null) out else out.sortedWith(
            compareByDescending<Place> { it.detail.endsWith("NL") }
                .thenBy { Geo.haversineKm(near, it.toLatLon()) }
        )
    }

    /** Best-effort street level name for a dropped pin. */
    suspend fun reverse(point: LatLon, language: String): Place? = runCatching {
        val url = "https://nominatim.openstreetmap.org/reverse" +
            "?format=jsonv2&zoom=17&addressdetails=1" +
            "&lat=${"%.6f".format(java.util.Locale.US, point.lat)}" +
            "&lon=${"%.6f".format(java.util.Locale.US, point.lon)}" +
            "&accept-language=$language"
        val o = JSONObject(Net.getText(url, 12_000))
        val addr = o.optJSONObject("address")
        val road = addr?.optString("road")?.takeIf { it.isNotBlank() }
        val houseNumber = addr?.optString("house_number")?.takeIf { it.isNotBlank() }
        val place = listOf("city", "town", "village", "municipality", "suburb", "hamlet")
            .firstNotNullOfOrNull { addr?.optString(it)?.takeIf { v -> v.isNotBlank() } }
        val label = when {
            road != null && houseNumber != null -> "$road $houseNumber"
            road != null -> road
            o.optString("name").isNotBlank() -> o.optString("name")
            place != null -> place
            else -> null
        } ?: return@runCatching null
        Place(
            name = label,
            lat = point.lat,
            lon = point.lon,
            detail = listOfNotNull(place.takeIf { it != label }, addr?.optString("postcode")?.takeIf { it.isNotBlank() })
                .joinToString(" · ")
        )
    }.getOrNull()
}

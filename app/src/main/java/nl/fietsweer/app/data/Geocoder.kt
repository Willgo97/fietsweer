package nl.fietsweer.app.data

import nl.fietsweer.app.domain.Geo
import nl.fietsweer.app.domain.LatLon
import org.json.JSONObject
import java.net.URLEncoder

object Geocoder {

    suspend fun search(query: String, near: LatLon, language: String): List<Place> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()
        val url = "https://geocoding-api.open-meteo.com/v1/search" +
            "?name=${URLEncoder.encode(trimmed, "UTF-8")}&count=12&language=$language&format=json"
        val body = Net.getText(url, 12_000)
        val results = JSONObject(body).optJSONArray("results") ?: return emptyList()
        val places = ArrayList<Place>(results.length())
        for (i in 0 until results.length()) {
            val result = results.getJSONObject(i)
            val name = result.optString("name").ifBlank { continue }
            val lat = result.optDouble("latitude", Double.NaN)
            val lon = result.optDouble("longitude", Double.NaN)
            if (lat.isNaN() || lon.isNaN()) continue
            val district = result.optString("admin2").takeIf { it.isNotBlank() && it != name }
            val province = result.optString("admin1").takeIf { it.isNotBlank() && it != name }
            val country = result.optString("country_code").takeIf { it.isNotBlank() }
            val detail = listOfNotNull(district ?: province, country).joinToString(", ")
            places += Place(name, lat, lon, detail)
        }
        return places.sortedWith(
            compareByDescending<Place> { it.detail.endsWith("NL") }
                .thenBy { Geo.haversineKm(near, it.toLatLon()) }
        )
    }

    suspend fun reverse(point: LatLon, language: String): Place? = runCatching {
        val url = "https://nominatim.openstreetmap.org/reverse" +
            "?format=jsonv2&zoom=17&addressdetails=1" +
            "&lat=${"%.6f".format(java.util.Locale.US, point.lat)}" +
            "&lon=${"%.6f".format(java.util.Locale.US, point.lon)}" +
            "&accept-language=$language"
        val response = JSONObject(Net.getText(url, 12_000))
        val address = response.optJSONObject("address")
        val road = address?.optString("road")?.takeIf { it.isNotBlank() }
        val houseNumber = address?.optString("house_number")?.takeIf { it.isNotBlank() }
        val town = listOf("city", "town", "village", "municipality", "suburb", "hamlet")
            .firstNotNullOfOrNull { key -> address?.optString(key)?.takeIf { it.isNotBlank() } }
        val label = when {
            road != null && houseNumber != null -> "$road $houseNumber"
            road != null -> road
            response.optString("name").isNotBlank() -> response.optString("name")
            town != null -> town
            else -> null
        } ?: return@runCatching null
        Place(
            name = label,
            lat = point.lat,
            lon = point.lon,
            detail = listOfNotNull(
                town.takeIf { it != label },
                address?.optString("postcode")?.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
        )
    }.getOrNull()

    suspend fun town(point: LatLon, language: String): String? = runCatching {
        val url = "https://nominatim.openstreetmap.org/reverse" +
            "?format=jsonv2&zoom=10" +
            "&lat=${"%.4f".format(java.util.Locale.US, point.lat)}" +
            "&lon=${"%.4f".format(java.util.Locale.US, point.lon)}" +
            "&accept-language=$language"
        val address = JSONObject(Net.getText(url, 12_000)).optJSONObject("address")
        listOf("city", "town", "village", "municipality")
            .firstNotNullOfOrNull { key -> address?.optString(key)?.takeIf { it.isNotBlank() } }
    }.getOrNull()
}

package com.wskakuj.grabio.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Pogoda z Open-Meteo (darmowe, bez klucza API).
 * Używamy jej tylko po to, żeby w dni rowerowe podpowiedzieć, gdy ma padać.
 */
object Weather {

    data class Place(val name: String, val lat: Double, val lon: Double)

    /** Zamienia nazwę miasta na współrzędne. */
    suspend fun geocode(city: String): Place? = withContext(Dispatchers.IO) {
        try {
            val q = URLEncoder.encode(city, "UTF-8")
            val url = "https://geocoding-api.open-meteo.com/v1/search" +
                "?name=$q&count=1&language=pl&format=json"
            val body = get(url) ?: return@withContext null
            val results = JSONObject(body).optJSONArray("results") ?: return@withContext null
            if (results.length() == 0) return@withContext null
            val r = results.getJSONObject(0)
            Place(
                name = r.optString("name", city),
                lat = r.getDouble("latitude"),
                lon = r.getDouble("longitude")
            )
        } catch (e: Exception) {
            null
        }
    }

    /** Maksymalne prawdopodobieństwo opadów na dziś (0–100) albo null. */
    suspend fun precipitationToday(lat: Double, lon: Double): Int? =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lon" +
                    "&daily=precipitation_probability_max&forecast_days=1&timezone=auto"
                val body = get(url) ?: return@withContext null
                val daily = JSONObject(body).optJSONObject("daily") ?: return@withContext null
                val arr = daily.optJSONArray("precipitation_probability_max")
                    ?: return@withContext null
                if (arr.length() == 0) null else arr.optInt(0, -1).takeIf { it >= 0 }
            } catch (e: Exception) {
                null
            }
        }

    private fun get(url: String): String? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("User-Agent", "Grabio")
        }
        return try {
            if (conn.responseCode !in 200..299) {
                null
            } else {
                conn.inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }
}

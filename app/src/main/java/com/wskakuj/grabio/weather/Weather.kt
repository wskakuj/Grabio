package com.wskakuj.grabio.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.time.LocalDateTime
import java.util.Locale

/**
 * Pogoda: „teraz” z IMGW (oficjalne pomiary, bez klucza), a prognoza godzinowa
 * z Open-Meteo (bez klucza). Gdy IMGW nie odpowie albo nie ma stacji dla miasta,
 * „teraz” też bierzemy z Open-Meteo.
 */
object Weather {

    data class Place(val name: String, val lat: Double, val lon: Double)

    data class Current(
        val source: String,
        val place: String,
        val tempC: Double?,
        val precipMm: Double?,
        val windMs: Double?,
        val humidity: Int?,
        val code: Int?,
        val stamp: String
    )

    data class Hour(val time: String, val tempC: Double, val precipProb: Int, val code: Int)

    data class Result(val current: Current?, val hourly: List<Hour>)

    suspend fun load(city: String, lat: Double?, lon: Double?): Result =
        withContext(Dispatchers.IO) {
            if (city.isBlank()) return@withContext Result(null, emptyList())
            val place = if (lat != null && lon != null) Place(city, lat, lon) else geocode(city)
                ?: return@withContext Result(null, emptyList())

            val current = imgwCurrent(city) ?: openMeteoCurrent(place)
            val hourly = openMeteoHourly(place)
            Result(current, hourly)
        }

    /** Nazwa miasta → współrzędne (Open-Meteo geocoding). */
    suspend fun geocode(city: String): Place? = withContext(Dispatchers.IO) {
        try {
            val q = URLEncoder.encode(city, "UTF-8")
            val body = get("https://geocoding-api.open-meteo.com/v1/search" +
                "?name=$q&count=1&language=pl&format=json") ?: return@withContext null
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

    // --- IMGW: bieżące pomiary ze stacji synoptycznej ---

    private fun imgwCurrent(city: String): Current? {
        return try {
            val body = get("https://danepubliczne.imgw.pl/api/data/synop") ?: return null
            val arr = JSONArray(body)
            val target = normalize(city)
            var match: JSONObject? = null
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val name = normalize(o.optString("stacja"))
                if (name == target || name.startsWith(target) || target.startsWith(name)) {
                    match = o
                    break
                }
            }
            val m = match ?: return null
            Current(
                source = "IMGW",
                place = m.optString("stacja").lowercase(Locale.getDefault())
                    .replaceFirstChar { it.uppercase() },
                tempC = m.optString("temperatura").toDoubleOrNull(),
                precipMm = m.optString("suma_opadu").toDoubleOrNull(),
                windMs = m.optString("predkosc_wiatru").toDoubleOrNull(),
                humidity = m.optString("wilgotnosc_wzgledna").toDoubleOrNull()?.toInt(),
                code = null,
                stamp = m.optString("godzina_pomiaru") + ":00"
            )
        } catch (e: Exception) {
            null
        }
    }

    // --- Open-Meteo: teraz ---

    private fun openMeteoCurrent(place: Place): Current? {
        return try {
            val body = get("https://api.open-meteo.com/v1/forecast" +
                "?latitude=${place.lat}&longitude=${place.lon}" +
                "&current=temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m,weather_code" +
                "&timezone=auto") ?: return null
            val cur = JSONObject(body).optJSONObject("current") ?: return null
            Current(
                source = "Open-Meteo",
                place = place.name,
                tempC = cur.optDouble("temperature_2m").takeIf { !it.isNaN() },
                precipMm = cur.optDouble("precipitation").takeIf { !it.isNaN() },
                windMs = cur.optDouble("wind_speed_10m").takeIf { !it.isNaN() },
                humidity = cur.optInt("relative_humidity_2m", -1).takeIf { it >= 0 },
                code = cur.optInt("weather_code", -1).takeIf { it >= 0 },
                stamp = cur.optString("time").substringAfter("T")
            )
        } catch (e: Exception) {
            null
        }
    }

    // --- Open-Meteo: prognoza godzinowa (najbliższe 12 godzin) ---

    private fun openMeteoHourly(place: Place): List<Hour> {
        return try {
            val body = get("https://api.open-meteo.com/v1/forecast" +
                "?latitude=${place.lat}&longitude=${place.lon}" +
                "&hourly=temperature_2m,precipitation_probability,weather_code" +
                "&forecast_days=2&timezone=auto") ?: return emptyList()
            val hourly = JSONObject(body).optJSONObject("hourly") ?: return emptyList()
            val times = hourly.optJSONArray("time") ?: return emptyList()
            val temps = hourly.optJSONArray("temperature_2m")
            val probs = hourly.optJSONArray("precipitation_probability")
            val codes = hourly.optJSONArray("weather_code")

            val now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0)
            val out = mutableListOf<Hour>()
            for (i in 0 until times.length()) {
                val raw = times.optString(i)
                val dt = try {
                    LocalDateTime.parse(raw)
                } catch (e: Exception) {
                    null
                }
                if (dt != null && dt.isBefore(now)) continue
                out += Hour(
                    time = raw.substringAfter("T").take(5),
                    tempC = temps?.optDouble(i) ?: 0.0,
                    precipProb = probs?.optInt(i) ?: 0,
                    code = codes?.optInt(i) ?: 0
                )
                if (out.size >= 12) break
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- pomocnicze ---

    private fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase(Locale.getDefault())
            .trim()

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

/** Emoji dla kodu pogody WMO. */
fun weatherEmoji(code: Int?): String = when (code) {
    null -> "🌡️"
    0 -> "☀️"
    1 -> "🌤️"
    2 -> "⛅"
    3 -> "☁️"
    45, 48 -> "🌫️"
    51, 53, 55, 56, 57 -> "🌦️"
    61, 63, 65, 66, 67 -> "🌧️"
    71, 73, 75, 77 -> "🌨️"
    80, 81, 82 -> "🌦️"
    85, 86 -> "🌨️"
    95, 96, 99 -> "⛈️"
    else -> "🌡️"
}

/** Krótki opis po polsku. */
fun weatherLabel(code: Int?): String = when (code) {
    null -> "brak danych"
    0 -> "bezchmurnie"
    1 -> "prawie bezchmurnie"
    2 -> "częściowe zachmurzenie"
    3 -> "zachmurzenie"
    45, 48 -> "mgła"
    51, 53, 55 -> "mżawka"
    56, 57 -> "marznąca mżawka"
    61 -> "słaby deszcz"
    63 -> "deszcz"
    65 -> "silny deszcz"
    66, 67 -> "marznący deszcz"
    71, 73, 75 -> "śnieg"
    77 -> "krupy śnieżne"
    80, 81, 82 -> "przelotny deszcz"
    85, 86 -> "przelotny śnieg"
    95 -> "burza"
    96, 99 -> "burza z gradem"
    else -> "pogoda"
}

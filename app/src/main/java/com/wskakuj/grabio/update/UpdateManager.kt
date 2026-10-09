package com.wskakuj.grabio.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Auto-aktualizacja z GitHub Releases (schemat jak w forestly-go):
 *  1. sprawdza najnowszy release przez GitHub API,
 *  2. jeśli jest nowszy niż zainstalowany — pobiera plik APK,
 *  3. otwiera systemowy instalator (przez FileProvider).
 */
object UpdateManager {

    const val REPO = "wskakuj/Grabio"
    private const val API = "https://api.github.com/repos/$REPO/releases/latest"

    data class Info(val version: String, val apkUrl: String, val pageUrl: String)

    /** Zwraca informację o nowszej wersji albo null, gdy mamy najnowszą. */
    suspend fun check(currentVersion: String): Info? = withContext(Dispatchers.IO) {
        val conn = (URL(API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Grabio")
        }
        try {
            if (conn.responseCode != 200) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tag = json.optString("tag_name", "")
            val pageUrl = json.optString("html_url", "https://github.com/$REPO/releases/latest")

            var apkUrl = ""
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name", "").endsWith(".apk")) {
                        apkUrl = a.optString("browser_download_url", "")
                        break
                    }
                }
            }

            val latest = tag.removePrefix("v")
            if (latest.isEmpty() || apkUrl.isEmpty()) return@withContext null
            if (isNewer(latest, currentVersion)) Info(latest, apkUrl, pageUrl) else null
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    /** true, gdy wersja a jest nowsza niż b (porównanie x.y.z). */
    private fun isNewer(a: String, b: String): Boolean {
        val pa = a.split(".").mapNotNull { it.trim().toIntOrNull() }
        val pb = b.split(".").mapNotNull { it.trim().toIntOrNull() }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** Pobiera APK do pamięci podręcznej; onProgress dostaje procent (0–100). */
    suspend fun download(
        context: Context,
        info: Info,
        onProgress: (Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 30000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Grabio")
        }
        try {
            if (conn.responseCode !in 200..299) {
                error("serwer odpowiedział HTTP ${conn.responseCode}")
            }
            val total = conn.contentLengthLong
            val file = File(context.cacheDir, "Grabio-${info.version}.apk")
            conn.inputStream.use { input ->
                FileOutputStream(file).use { out ->
                    val buf = ByteArray(16384)
                    var done = 0L
                    var n = input.read(buf)
                    while (n > 0) {
                        out.write(buf, 0, n)
                        done += n
                        if (total > 0) onProgress((done * 100 / total).toInt())
                        n = input.read(buf)
                    }
                    out.flush()
                }
            }
            file
        } finally {
            conn.disconnect()
        }
    }

    /** Otwiera systemowy instalator na pobranym pliku. */
    fun install(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context, context.packageName + ".fileprovider", file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // najczęściej brak zgody „Instaluj nieznane aplikacje”
            val settings = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + context.packageName)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settings)
        }
    }
}

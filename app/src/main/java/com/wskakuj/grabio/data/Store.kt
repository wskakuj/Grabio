package com.wskakuj.grabio.data

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Prosty magazyn danych: cały stan aplikacji trzymamy jako jeden plik JSON
 * w prywatnym katalogu aplikacji.
 */
class Store(private val context: Context) {

    private val file = File(context.filesDir, "grabio.json")
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun load(): AppData = try {
        if (file.exists()) {
            json.decodeFromString(AppData.serializer(), file.readText())
        } else {
            AppData()
        }
    } catch (e: Exception) {
        AppData()
    }

    fun save(data: AppData) {
        try {
            file.writeText(json.encodeToString(AppData.serializer(), data))
        } catch (e: Exception) {
            // Zapis nieudany — stan trzyma się w pamięci.
        }
    }
}

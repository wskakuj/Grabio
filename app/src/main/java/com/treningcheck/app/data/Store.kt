package com.treningcheck.app.data

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Prosty magazyn danych: caly stan aplikacji trzymamy jako jeden plik JSON
 * w prywatnym katalogu aplikacji. Bez bazy danych — mniej ruchomych czesci.
 */
class Store(private val context: Context) {

    private val file = File(context.filesDir, "treningcheck.json")
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun load(): AppData = try {
        if (file.exists()) {
            json.decodeFromString(AppData.serializer(), file.readText())
        } else {
            seed()
        }
    } catch (e: Exception) {
        seed()
    }

    fun save(data: AppData) {
        try {
            file.writeText(json.encodeToString(AppData.serializer(), data))
        } catch (e: Exception) {
            // Zapis nieudany (np. brak miejsca) — ignorujemy, stan trzyma sie w pamieci.
        }
    }

    /** Przykladowe szablony na start, zeby aplikacja nie byla pusta. */
    private fun seed(): AppData = AppData(
        templates = listOf(
            Template(
                id = "t-silownia",
                name = "Silownia",
                icon = "\uD83C\uDFCB\uFE0F",
                items = listOf(
                    ChecklistItem("s1", "Buty na zmiane"),
                    ChecklistItem("s2", "Recznik"),
                    ChecklistItem("s3", "Bidon z woda"),
                    ChecklistItem("s4", "Karta czlonkowska"),
                    ChecklistItem("s5", "Sluchawki")
                )
            ),
            Template(
                id = "t-pilka",
                name = "Pilka nozna",
                icon = "\u26BD",
                items = listOf(
                    ChecklistItem("p1", "Korki"),
                    ChecklistItem("p2", "Ochraniacze"),
                    ChecklistItem("p3", "Bidon"),
                    ChecklistItem("p4", "Zmiana koszulki")
                )
            ),
            Template(
                id = "t-bieganie",
                name = "Bieganie",
                icon = "\uD83C\uDFC3",
                items = listOf(
                    ChecklistItem("b1", "Buty do biegania"),
                    ChecklistItem("b2", "Bidon"),
                    ChecklistItem("b3", "Opaska na telefon")
                )
            )
        )
    )
}

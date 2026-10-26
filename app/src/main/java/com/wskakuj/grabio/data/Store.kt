package com.wskakuj.grabio.data

import android.content.Context
import android.util.AtomicFile
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException

/**
 * Lokalny magazyn danych. AtomicFile chroni zapis przed przerwaniem procesu
 * i zachowuje poprzednią poprawną wersję podczas niezakończonego zapisu.
 */
class Store(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "grabio.json"))
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    /**
     * Domyślne dane zwracamy wyłącznie przy rzeczywistym pierwszym uruchomieniu.
     * Błąd odczytu istniejącego pliku musi być widoczny, żeby nie nadpisać
     * uszkodzonego zapisu pustą konfiguracją.
     */
    @Synchronized
    fun load(): AppData {
        if (!file.baseFile.exists() &&
            !File(file.baseFile.path + ".bak").exists() &&
            !File(file.baseFile.path + ".new").exists()
        ) return AppData()

        return try {
            val content = file.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
            json.decodeFromString(AppData.serializer(), content)
        } catch (e: FileNotFoundException) {
            throw IllegalStateException("Nie można odczytać zapisanych danych Grabio", e)
        } catch (e: Exception) {
            throw IllegalStateException("Zapisane dane Grabio są nieczytelne; nie zostały nadpisane", e)
        }
    }

    /** Wyjątek zapisu jest przekazywany wywołującemu, a nie ignorowany. */
    @Synchronized
    fun save(data: AppData) {
        val trimmed = if (data.days.size <= MAX_DNI) data
        else data.copy(days = data.days.sortedByDescending { it.date }.take(MAX_DNI))

        // Serializacja przed otwarciem pliku: błąd kodowania nie dotyka danych.
        val bytes = json.encodeToString(AppData.serializer(), trimmed).toByteArray(Charsets.UTF_8)
        val stream = file.startWrite()
        try {
            stream.write(bytes)
            file.finishWrite(stream)
        } catch (e: Exception) {
            file.failWrite(stream)
            throw IllegalStateException("Nie udało się zapisać danych Grabio", e)
        }
    }

    private companion object {
        const val MAX_DNI = 400
    }
}

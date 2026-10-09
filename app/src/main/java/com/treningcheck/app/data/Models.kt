package com.treningcheck.app.data

import kotlinx.serialization.Serializable

/** Pojedyncza pozycja w szablonie (np. "Bidon z woda"). */
@Serializable
data class ChecklistItem(
    val id: String,
    val name: String,
    val note: String = ""
)

/** Szablon treningu — zestaw rzeczy do zabrania. */
@Serializable
data class Template(
    val id: String,
    val name: String,
    val icon: String = "\uD83C\uDFCB\uFE0F",
    val items: List<ChecklistItem> = emptyList()
)

/** Pozycja na konkretny dzien (z odhaczeniem). */
@Serializable
data class DayItem(
    val id: String,
    val name: String,
    val note: String = "",
    val checked: Boolean = false
)

/** Zapis jednego dnia treningu. */
@Serializable
data class DayRecord(
    val date: String,
    val templateName: String = "",
    val items: List<DayItem> = emptyList()
) {
    val total: Int get() = items.size
    val done: Int get() = items.count { it.checked }
    val progress: Float get() = if (total == 0) 0f else done.toFloat() / total.toFloat()
}

/** Caly stan aplikacji zapisywany do pliku JSON. */
@Serializable
data class AppData(
    val templates: List<Template> = emptyList(),
    val days: List<DayRecord> = emptyList(),
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 17,
    val reminderMinute: Int = 0
)

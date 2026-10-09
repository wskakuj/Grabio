package com.wskakuj.grabio.data

import kotlinx.serialization.Serializable

/** Grupy pozycji na liście dnia. */
const val GROUP_TRAINING = "trening"
const val GROUP_ESSENTIAL = "dzienne"
const val GROUP_BIKE = "rower"
const val GROUP_CUSTOM = "wlasne"

/** Środek transportu wybrany na dany dzień. */
const val TRANSPORT_CAR = "car"
const val TRANSPORT_BIKE = "bike"

/** Co bierzemy na trening. */
const val BAG_BACKPACK = "plecak"
const val BAG_TOTE = "torba"

/** Domyślnie: torba w poniedziałek (1) i czwartek (4), w pozostałe dni plecak. */
fun defaultBag(weekday: Int): String =
    if (weekday == 1 || weekday == 4) BAG_TOTE else BAG_BACKPACK

/** Motyw aplikacji. */
const val THEME_SYSTEM = "system"
const val THEME_LIGHT = "light"
const val THEME_DARK = "dark"

/** Pojedyncza pozycja w planie / dodatkach. */
@Serializable
data class ChecklistItem(
    val id: String,
    val name: String,
    val note: String = ""
)

/** Pozycja na konkretny dzień (z odhaczeniem i grupą). */
@Serializable
data class DayItem(
    val id: String,
    val name: String,
    val checked: Boolean = false,
    val group: String = GROUP_TRAINING
)

/** Zapis jednego dnia. */
@Serializable
data class DayRecord(
    val date: String,
    val transport: String = "",
    val bag: String = "",
    val items: List<DayItem> = emptyList(),
    val restDay: Boolean = false
) {
    val total: Int get() = items.size
    val done: Int get() = items.count { it.checked }
    val progress: Float get() = if (total == 0) 0f else done.toFloat() / total.toFloat()
    val complete: Boolean get() = total > 0 && done == total
}

/** Cały stan aplikacji zapisywany do pliku JSON. */
@Serializable
data class AppData(
    val weekPlan: Map<Int, List<ChecklistItem>> = defaultWeekPlan(),
    val essentials: List<ChecklistItem> = defaultEssentials(),
    val bikeExtras: List<ChecklistItem> = defaultBikeExtras(),
    val days: List<DayRecord> = emptyList(),
    val reminderEnabled: Boolean = true,
    val weekdayHour: Int = 16,
    val weekdayMinute: Int = 20,
    val weekendHour: Int = 17,
    val weekendMinute: Int = 0,
    val themeMode: String = THEME_SYSTEM,
    val cityName: String = "",
    val regionName: String = "",
    val cityLat: Double? = null,
    val cityLon: Double? = null
)

/** Klucz: 1 = poniedziałek … 7 = niedziela. */
fun defaultWeekPlan(): Map<Int, List<ChecklistItem>> = mapOf(
    1 to listOf(
        ChecklistItem("pon1", "Buty do siadów"),
        ChecklistItem("pon2", "Buty na zmianę"),
        ChecklistItem("pon3", "Neopreny"),
        ChecklistItem("pon4", "Pas"),
        ChecklistItem("pon5", "Paski do allahów")
    ),
    2 to listOf(
        ChecklistItem("wt1", "Pomarańczowe paski"),
        ChecklistItem("wt2", "Paski na nadgarstek")
    ),
    3 to listOf(
        ChecklistItem("sr1", "Pas do podciągania"),
        ChecklistItem("sr2", "Paski 8"),
        ChecklistItem("sr3", "Paski pomarańczowe")
    ),
    4 to listOf(
        ChecklistItem("cz1", "Buty do siadów"),
        ChecklistItem("cz2", "Buty na zmianę"),
        ChecklistItem("cz3", "Neopreny"),
        ChecklistItem("cz4", "Paski pomarańczowe")
    ),
    5 to listOf(
        ChecklistItem("pt1", "Paski 8"),
        ChecklistItem("pt2", "Paski na nadgarstek")
    ),
    6 to listOf(
        ChecklistItem("sb1", "Pas do podciągania")
    ),
    7 to emptyList()
)

/** Dodatki przypominane każdego dnia. */
fun defaultEssentials(): List<ChecklistItem> = listOf(
    ChecklistItem("dz1", "Kłódka do szafki"),
    ChecklistItem("dz2", "Sprawdź baterię słuchawek"),
    ChecklistItem("dz3", "Inhalator w torbie"),
    ChecklistItem("dz4", "Miętówki / gumy do żucia")
)

/** Dodatki, gdy jedziesz rowerem. */
fun defaultBikeExtras(): List<ChecklistItem> = listOf(
    ChecklistItem("ro1", "Kluczyk od łańcucha"),
    ChecklistItem("ro2", "Światełka do roweru")
)

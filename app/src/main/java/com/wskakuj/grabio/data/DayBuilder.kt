package com.wskakuj.grabio.data

import java.util.UUID

/** Nowy, unikalny identyfikator pozycji. */
fun newItemId(): String = UUID.randomUUID().toString()

/**
 * Buduje listę rzeczy na dany dzień: plan tygodnia + codzienne dodatki
 * (+ dodatki rowerowe, gdy jedziesz rowerem) + własne pozycje.
 *
 * Trzymamy to w jednym miejscu, żeby aplikacja i widget budowały listę
 * dokładnie tak samo — inaczej wybór transportu w widgecie dawałby
 * inną listę niż wybór w aplikacji.
 *
 * @param checkedByName odhaczenia z poprzedniej wersji listy (zachowujemy po nazwie)
 */
fun buildDayItems(
    data: AppData,
    weekday: Int,
    transport: String,
    custom: List<DayItem> = emptyList(),
    checkedByName: Map<String, Boolean> = emptyMap()
): List<DayItem> {
    val out = mutableListOf<DayItem>()
    data.weekPlan[weekday].orEmpty().forEach {
        out += DayItem(newItemId(), it.name, group = GROUP_TRAINING)
    }
    data.essentials.forEach {
        out += DayItem(newItemId(), it.name, group = GROUP_ESSENTIAL)
    }
    if (transport == TRANSPORT_BIKE) {
        data.bikeExtras.forEach {
            out += DayItem(newItemId(), it.name, group = GROUP_BIKE)
        }
    }
    val merged = out.map { it.copy(checked = checkedByName[it.name] ?: false) }
    return merged + custom
}

package com.wskakuj.grabio.data

import java.util.UUID

/** Nowy, unikalny identyfikator pozycji dodawanych przez użytkownika. */
fun newItemId(): String = UUID.randomUUID().toString()

/**
 * Tworzy listę dnia ze stałymi ID odpowiadającymi elementom planu.
 *
 * Podczas pierwszej przebudowy starszej listy dopasowuje zapisane pozycje
 * po nazwie I grupie, jednokrotnie (bez mieszania dwóch duplikatów).
 * Przy kolejnych przebudowach priorytet ma już trwałe ID.
 *
 * checkedByName pozostaje dla zgodności ze starszymi wywołaniami (np. widget).
 */
fun buildDayItems(
    data: AppData,
    weekday: Int,
    transport: String,
    custom: List<DayItem> = emptyList(),
    checkedByName: Map<String, Boolean> = emptyMap(),
    previousItems: List<DayItem> = emptyList()
): List<DayItem> {
    val templates = mutableListOf<DayItem>()
    data.weekPlan[weekday].orEmpty().forEach {
        templates += DayItem(it.id, it.name, group = GROUP_TRAINING, priority = it.priority)
    }
    data.essentials.forEach {
        templates += DayItem(it.id, it.name, group = GROUP_ESSENTIAL, priority = it.priority)
    }
    if (transport == TRANSPORT_BIKE) {
        data.bikeExtras.forEach {
            templates += DayItem(it.id, it.name, group = GROUP_BIKE, priority = it.priority)
        }
    }

    // Wykorzystujemy pozycję starej listy najwyżej raz, aby duplikaty
    // nazw nie dziedziczyły przypadkowo wspólnego stanu.
    val remaining = previousItems.filter { it.group != GROUP_CUSTOM }.toMutableList()
    val merged = templates.map { template ->
        val byId = remaining.indexOfFirst {
            it.id == template.id && it.group == template.group
        }
        val byName = if (byId == -1) remaining.indexOfFirst {
            it.name == template.name && it.group == template.group
        } else -1
        val index = if (byId >= 0) byId else byName
        val checked = if (index >= 0) {
            remaining.removeAt(index).checked
        } else if (previousItems.isEmpty()) {
            checkedByName[template.name] ?: false
        } else {
            false
        }
        template.copy(checked = checked)
    }
    return merged + custom
}

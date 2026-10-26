package com.wskakuj.grabio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DayBuilderTest {
    @Test
    fun `new day items use stable source IDs`() {
        val data = AppData(
            weekPlan = mapOf(1 to listOf(ChecklistItem("training-1", "Buty"))),
            essentials = listOf(ChecklistItem("essential-1", "Woda")),
            bikeExtras = listOf(ChecklistItem("bike-1", "Kask"))
        )
        val first = buildDayItems(data, 1, TRANSPORT_BIKE)
        val second = buildDayItems(data, 1, TRANSPORT_BIKE)
        assertEquals(listOf("training-1", "essential-1", "bike-1"), first.map { it.id })
        assertEquals(first.map { it.id }, second.map { it.id })
    }

    @Test
    fun `renaming a template retains check by ID`() {
        val old = DayItem("training-1", "Stara nazwa", checked = true, group = GROUP_TRAINING)
        val data = AppData(
            weekPlan = mapOf(1 to listOf(ChecklistItem("training-1", "Nowa nazwa"))),
            essentials = emptyList(),
            bikeExtras = emptyList()
        )
        val result = buildDayItems(data, 1, TRANSPORT_CAR, previousItems = listOf(old))
        assertEquals("Nowa nazwa", result.single().name)
        assertTrue(result.single().checked)
    }

    @Test
    fun `same item name in separate groups keeps independent checks`() {
        val data = AppData(
            weekPlan = mapOf(1 to listOf(ChecklistItem("t", "Woda"))),
            essentials = listOf(ChecklistItem("e", "Woda")),
            bikeExtras = emptyList()
        )
        val previous = listOf(
            DayItem("t", "Woda", checked = true, group = GROUP_TRAINING),
            DayItem("e", "Woda", checked = false, group = GROUP_ESSENTIAL)
        )
        val result = buildDayItems(data, 1, TRANSPORT_CAR, previousItems = previous)
        assertTrue(result.single { it.id == "t" }.checked)
        assertFalse(result.single { it.id == "e" }.checked)
    }

    @Test
    fun `legacy random IDs migrate once by name and group`() {
        val data = AppData(
            weekPlan = mapOf(1 to listOf(ChecklistItem("t", "Pas"))),
            essentials = emptyList(),
            bikeExtras = emptyList()
        )
        val previous = listOf(DayItem("old-random-id", "Pas", checked = true, group = GROUP_TRAINING))
        val migrated = buildDayItems(data, 1, TRANSPORT_CAR, previousItems = previous)
        assertEquals("t", migrated.single().id)
        assertTrue(migrated.single().checked)
    }

    @Test
    fun `switching transport preserves common checked items and custom items`() {
        val data = AppData(
            weekPlan = mapOf(1 to listOf(ChecklistItem("t", "Pas"))),
            essentials = emptyList(),
            bikeExtras = listOf(ChecklistItem("b", "Kask"))
        )
        val custom = DayItem("custom", "Moja rzecz", checked = true, group = GROUP_CUSTOM)
        val bike = buildDayItems(data, 1, TRANSPORT_BIKE, custom = listOf(custom))
            .map { if (it.id == "t") it.copy(checked = true) else it }
        val car = buildDayItems(data, 1, TRANSPORT_CAR,
            custom = bike.filter { it.group == GROUP_CUSTOM }, previousItems = bike)
        assertEquals(listOf("t", "custom"), car.map { it.id })
        assertTrue(car.all { it.checked })
    }
}

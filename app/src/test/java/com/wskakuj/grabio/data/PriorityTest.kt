package com.wskakuj.grabio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PriorityTest {
    @Test
    fun `old data defaults to normal priority`() {
        assertEquals(0, ChecklistItem("a", "Pas").priority)
        assertEquals(0, DayItem("a", "Pas").priority)
    }

    @Test
    fun `priority survives list sync and rename`() {
        val data = AppData(
            weekPlan = mapOf(1 to listOf(ChecklistItem("t1", "Nowa nazwa", priority = 2))),
            essentials = listOf(ChecklistItem("e1", "Klucze", priority = 1)),
            bikeExtras = emptyList()
        )
        val old = listOf(DayItem("t1", "Stara nazwa", checked = true))
        val rebuilt = buildDayItems(data, 1, TRANSPORT_CAR, previousItems = old)
        assertEquals(2, rebuilt.single { it.id == "t1" }.priority)
        assertTrue(rebuilt.single { it.id == "t1" }.checked)
        assertEquals(1, rebuilt.single { it.id == "e1" }.priority)
    }

    @Test
    fun `priority IDs do not merge identical names`() {
        val data = AppData(
            weekPlan = mapOf(1 to listOf(ChecklistItem("t", "Woda", priority = 2))),
            essentials = listOf(ChecklistItem("e", "Woda", priority = 0)),
            bikeExtras = emptyList()
        )
        val items = buildDayItems(data, 1, TRANSPORT_CAR)
        assertEquals(2, items.first { it.id == "t" }.priority)
        assertEquals(0, items.first { it.id == "e" }.priority)
    }
}

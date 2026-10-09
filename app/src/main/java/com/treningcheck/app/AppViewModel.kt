package com.treningcheck.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.treningcheck.app.data.AppData
import com.treningcheck.app.data.DayItem
import com.treningcheck.app.data.DayRecord
import com.treningcheck.app.data.Store
import com.treningcheck.app.data.Template
import com.treningcheck.app.notify.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.util.UUID

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val store = Store(app)
    private val _data = MutableStateFlow(store.load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private fun update(block: (AppData) -> AppData) {
        val newData = block(_data.value)
        _data.value = newData
        store.save(newData)
    }

    fun todayKey(): String = LocalDate.now().toString()

    fun newId(): String = UUID.randomUUID().toString()

    /** Rozpoczyna dzisiejsza liste na podstawie szablonu. */
    fun startDayFromTemplate(templateId: String) {
        val tpl = _data.value.templates.find { it.id == templateId } ?: return
        val key = todayKey()
        val record = DayRecord(
            date = key,
            templateName = tpl.name,
            items = tpl.items.map { DayItem(id = newId(), name = it.name, note = it.note) }
        )
        update { d -> d.copy(days = d.days.filterNot { it.date == key } + record) }
    }

    fun toggleItem(date: String, itemId: String) {
        update { d ->
            d.copy(days = d.days.map { day ->
                if (day.date != date) day
                else day.copy(items = day.items.map {
                    if (it.id == itemId) it.copy(checked = !it.checked) else it
                })
            })
        }
    }

    fun addItemToDay(date: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        update { d ->
            val existing = d.days.find { it.date == date }
            if (existing == null) {
                d.copy(days = d.days + DayRecord(
                    date = date,
                    items = listOf(DayItem(id = newId(), name = trimmed))
                ))
            } else {
                d.copy(days = d.days.map {
                    if (it.date == date) it.copy(items = it.items + DayItem(id = newId(), name = trimmed))
                    else it
                })
            }
        }
    }

    fun removeItemFromDay(date: String, itemId: String) {
        update { d ->
            d.copy(days = d.days.map {
                if (it.date == date) it.copy(items = it.items.filterNot { i -> i.id == itemId })
                else it
            })
        }
    }

    fun clearDay(date: String) {
        update { d -> d.copy(days = d.days.filterNot { it.date == date }) }
    }

    // --- Szablony ---

    fun saveTemplate(template: Template) {
        update { d ->
            val exists = d.templates.any { it.id == template.id }
            d.copy(templates =
                if (exists) d.templates.map { if (it.id == template.id) template else it }
                else d.templates + template
            )
        }
    }

    fun deleteTemplate(id: String) {
        update { d -> d.copy(templates = d.templates.filterNot { it.id == id }) }
    }

    // --- Przypomnienie ---

    fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        update { d -> d.copy(reminderEnabled = enabled, reminderHour = hour, reminderMinute = minute) }
        ReminderScheduler.apply(getApplication(), enabled, hour, minute)
    }
}

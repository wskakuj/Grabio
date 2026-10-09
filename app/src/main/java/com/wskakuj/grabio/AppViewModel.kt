package com.wskakuj.grabio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wskakuj.grabio.data.AppData
import com.wskakuj.grabio.data.ChecklistItem
import com.wskakuj.grabio.data.DayItem
import com.wskakuj.grabio.data.DayRecord
import com.wskakuj.grabio.data.GROUP_BIKE
import com.wskakuj.grabio.data.GROUP_CUSTOM
import com.wskakuj.grabio.data.GROUP_ESSENTIAL
import com.wskakuj.grabio.data.GROUP_TRAINING
import com.wskakuj.grabio.data.Store
import com.wskakuj.grabio.data.TRANSPORT_BIKE
import com.wskakuj.grabio.notify.ReminderScheduler
import com.wskakuj.grabio.update.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/** Stan auto-aktualizacji pokazywany w interfejsie. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data class Available(val info: UpdateManager.Info) : UpdateState
    data class Downloading(val percent: Int) : UpdateState
    data object Ready : UpdateState
    data class Failed(val message: String) : UpdateState
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val store = Store(app)
    private val _data = MutableStateFlow(store.load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private fun update(block: (AppData) -> AppData) {
        val newData = block(_data.value)
        _data.value = newData
        store.save(newData)
    }

    fun newId(): String = UUID.randomUUID().toString()
    fun todayKey(): String = LocalDate.now().toString()
    fun weekday(): Int = LocalDate.now().dayOfWeek.value // 1 = poniedziałek … 7 = niedziela
    fun todayRecord(): DayRecord? = _data.value.days.find { it.date == todayKey() }

    // --- Lista dnia ---

    private fun buildItems(weekday: Int, transport: String): List<DayItem> {
        val out = mutableListOf<DayItem>()
        _data.value.weekPlan[weekday].orEmpty().forEach {
            out += DayItem(newId(), it.name, group = GROUP_TRAINING)
        }
        _data.value.essentials.forEach {
            out += DayItem(newId(), it.name, group = GROUP_ESSENTIAL)
        }
        if (transport == TRANSPORT_BIKE) {
            _data.value.bikeExtras.forEach {
                out += DayItem(newId(), it.name, group = GROUP_BIKE)
            }
        }
        return out
    }

    /** Buduje/odświeża dzisiejszą listę dla wybranego transportu (zachowuje odhaczenia). */
    fun setTransport(mode: String) {
        val key = todayKey()
        val existing = todayRecord()
        val prev = existing?.items?.associate { it.name to it.checked } ?: emptyMap()
        val fresh = buildItems(weekday(), mode).map { it.copy(checked = prev[it.name] ?: false) }
        val custom = existing?.items?.filter { it.group == GROUP_CUSTOM } ?: emptyList()
        val record = DayRecord(date = key, transport = mode, items = fresh + custom)
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
                    items = listOf(DayItem(newId(), trimmed, group = GROUP_CUSTOM))
                ))
            } else {
                d.copy(days = d.days.map {
                    if (it.date == date)
                        it.copy(items = it.items + DayItem(newId(), trimmed, group = GROUP_CUSTOM))
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

    // --- Plan tygodnia ---

    fun addWeekItem(day: Int, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        update { d ->
            val list = d.weekPlan[day].orEmpty()
            d.copy(weekPlan = d.weekPlan + (day to (list + ChecklistItem(newId(), trimmed))))
        }
    }

    fun removeWeekItem(day: Int, id: String) {
        update { d ->
            d.copy(weekPlan = d.weekPlan + (day to d.weekPlan[day].orEmpty().filterNot { it.id == id }))
        }
    }

    // --- Dodatki codzienne ---

    fun addEssential(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        update { d -> d.copy(essentials = d.essentials + ChecklistItem(newId(), trimmed)) }
    }

    fun removeEssential(id: String) {
        update { d -> d.copy(essentials = d.essentials.filterNot { it.id == id }) }
    }

    // --- Dodatki rowerowe ---

    fun addBikeExtra(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        update { d -> d.copy(bikeExtras = d.bikeExtras + ChecklistItem(newId(), trimmed)) }
    }

    fun removeBikeExtra(id: String) {
        update { d -> d.copy(bikeExtras = d.bikeExtras.filterNot { it.id == id }) }
    }

    // --- Przypomnienia ---

    fun setReminder(
        enabled: Boolean,
        weekdayHour: Int,
        weekdayMinute: Int,
        weekendHour: Int,
        weekendMinute: Int
    ) {
        update {
            it.copy(
                reminderEnabled = enabled,
                weekdayHour = weekdayHour,
                weekdayMinute = weekdayMinute,
                weekendHour = weekendHour,
                weekendMinute = weekendMinute
            )
        }
        ReminderScheduler.apply(
            getApplication(), enabled, weekdayHour, weekdayMinute, weekendHour, weekendMinute
        )
    }

    // --- Auto-aktualizacja ---

    fun checkForUpdate() {
        val state = _updateState.value
        if (state is UpdateState.Available || state is UpdateState.Downloading) return
        viewModelScope.launch {
            val info = try {
                UpdateManager.check(BuildConfig.VERSION_NAME)
            } catch (e: Exception) {
                null
            }
            _updateState.value = if (info != null) UpdateState.Available(info) else UpdateState.Idle
        }
    }

    fun downloadUpdate() {
        val info = (_updateState.value as? UpdateState.Available)?.info ?: return
        _updateState.value = UpdateState.Downloading(0)
        viewModelScope.launch {
            try {
                val file = UpdateManager.download(getApplication(), info) { p ->
                    _updateState.value = UpdateState.Downloading(p)
                }
                UpdateManager.install(getApplication(), file)
                _updateState.value = UpdateState.Ready
            } catch (e: Exception) {
                _updateState.value = UpdateState.Failed(e.message ?: "nie udało się pobrać")
            }
        }
    }

    fun dismissUpdate() {
        _updateState.value = UpdateState.Idle
    }
}

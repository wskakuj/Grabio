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
import com.wskakuj.grabio.data.defaultBag
import com.wskakuj.grabio.notify.ProgressNotifier
import com.wskakuj.grabio.notify.ReminderScheduler
import com.wskakuj.grabio.update.UpdateManager
import com.wskakuj.grabio.weather.Weather
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

    /** Dane pogodowe: „teraz” (IMGW / Open-Meteo) + prognoza godzinowa. */
    private val _weather = MutableStateFlow<Weather.Result?>(null)
    val weather: StateFlow<Weather.Result?> = _weather.asStateFlow()

    init {
        // odtworzenie wskaźnika postępu po ponownym uruchomieniu aplikacji
        ProgressNotifier.sync(app, _data.value.days.find { it.date == todayKey() })
    }

    private fun update(block: (AppData) -> AppData) {
        val newData = block(_data.value)
        _data.value = newData
        store.save(newData)
        ProgressNotifier.sync(getApplication(), newData.days.find { it.date == todayKey() })
    }

    fun newId(): String = UUID.randomUUID().toString()
    fun todayKey(): String = LocalDate.now().toString()
    fun weekday(): Int = LocalDate.now().dayOfWeek.value
    fun todayRecord(): DayRecord? = _data.value.days.find { it.date == todayKey() }

    /**
     * Uzgadnia dzisiejszą listę z planem i dodatkami: dorzuca nowe pozycje,
     * usuwa skasowane i odzwierciedla zmiany nazw — zachowując odhaczenia.
     * Ręcznie dodane „własne” pozycje zostają nietknięte.
     */
    private fun syncTodayWithPlan() {
        val key = todayKey()
        val weekday = LocalDate.now().dayOfWeek.value
        update { d ->
            val record = d.days.find { it.date == key }
            if (record == null || record.restDay) return@update d

            val checkedByName = record.items.associate { it.name to it.checked }
            val custom = record.items.filter { it.group == GROUP_CUSTOM }

            val fresh = mutableListOf<DayItem>()
            d.weekPlan[weekday].orEmpty().forEach {
                fresh += DayItem(newId(), it.name, group = GROUP_TRAINING)
            }
            d.essentials.forEach {
                fresh += DayItem(newId(), it.name, group = GROUP_ESSENTIAL)
            }
            if (record.transport == TRANSPORT_BIKE) {
                d.bikeExtras.forEach {
                    fresh += DayItem(newId(), it.name, group = GROUP_BIKE)
                }
            }
            val merged = fresh.map { it.copy(checked = checkedByName[it.name] ?: false) }
            d.copy(days = d.days.map {
                if (it.date == key) {
                    it.copy(items = merged + custom, bag = it.bag.ifEmpty { defaultBag(weekday) })
                } else {
                    it
                }
            })
        }
    }

    /** Ponowne wczytanie danych z pliku (np. gdy widget/powiadomienie coś zmieniło). */
    fun reload() {
        val fresh = store.load()
        if (fresh != _data.value) {
            _data.value = fresh
            ProgressNotifier.sync(getApplication(), fresh.days.find { it.date == todayKey() })
        }
    }

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

    fun setTransport(mode: String) {
        val key = todayKey()
        val existing = todayRecord()
        val prev = existing?.items?.associate { it.name to it.checked } ?: emptyMap()
        val fresh = buildItems(weekday(), mode).map { it.copy(checked = prev[it.name] ?: false) }
        val custom = existing?.items?.filter { it.group == GROUP_CUSTOM } ?: emptyList()
        val bag = existing?.bag?.ifEmpty { defaultBag(weekday()) } ?: defaultBag(weekday())
        val record = DayRecord(date = key, transport = mode, bag = bag, items = fresh + custom)
        update { d -> d.copy(days = d.days.filterNot { it.date == key } + record) }
    }

    /** Co dziś bierzemy: plecak czy torba (z domyślną wartością dla dnia tygodnia). */
    fun todayBag(): String =
        todayRecord()?.bag?.ifEmpty { defaultBag(weekday()) } ?: defaultBag(weekday())

    fun setBag(mode: String) {
        val key = todayKey()
        update { d ->
            d.copy(days = d.days.map {
                if (it.date == key) it.copy(bag = mode) else it
            })
        }
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

    fun markAllChecked(date: String = todayKey()) {
        update { d ->
            d.copy(days = d.days.map { day ->
                if (day.date == date) day.copy(items = day.items.map { it.copy(checked = true) })
                else day
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

    /** Dopisuje pozycję na stałe do planu tego dnia tygodnia. */
    fun addItemPermanently(date: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val day = try {
            LocalDate.parse(date).dayOfWeek.value
        } catch (e: Exception) {
            return
        }
        update { d ->
            val list = d.weekPlan[day].orEmpty()
            if (list.any { it.name.equals(trimmed, ignoreCase = true) }) {
                d
            } else {
                d.copy(weekPlan = d.weekPlan + (day to (list + ChecklistItem(newId(), trimmed))))
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

    /** Oznacza dzień jako wolny (bez treningu). */
    fun markRestDay(date: String = todayKey()) {
        update { d ->
            d.copy(
                days = d.days.filterNot { it.date == date } +
                    DayRecord(date = date, restDay = true, items = emptyList())
            )
        }
    }

    fun clearRestDay(date: String = todayKey()) {
        update { d -> d.copy(days = d.days.filterNot { it.date == date }) }
    }

    /** Zmiana nazwy pozycji na liście dnia. */
    fun renameDayItem(date: String, itemId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        update { d ->
            d.copy(days = d.days.map { day ->
                if (day.date != date) day
                else day.copy(items = day.items.map {
                    if (it.id == itemId) it.copy(name = trimmed) else it
                })
            })
        }
    }

    /** Zmiana kolejności w obrębie grupy na liście dnia. */
    fun moveDayItem(date: String, group: String, from: Int, to: Int) {
        update { d ->
            d.copy(days = d.days.map { day ->
                if (day.date != date) {
                    day
                } else {
                    val idx = day.items.indices.filter { day.items[it].group == group }
                    if (from !in idx.indices || to !in idx.indices) {
                        day
                    } else {
                        val list = day.items.toMutableList()
                        val moved = list.removeAt(idx[from])
                        list.add(idx[to], moved)
                        day.copy(items = list)
                    }
                }
            })
        }
    }

    // --- Plan tygodnia ---

    fun addWeekItem(day: Int, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        update { d ->
            val list = d.weekPlan[day].orEmpty()
            d.copy(weekPlan = d.weekPlan + (day to (list + ChecklistItem(newId(), trimmed))))
        }
        syncTodayWithPlan()
    }

    fun removeWeekItem(day: Int, id: String) {
        update { d ->
            d.copy(weekPlan = d.weekPlan + (day to d.weekPlan[day].orEmpty().filterNot { it.id == id }))
        }
        syncTodayWithPlan()
    }

    fun renameWeekItem(day: Int, id: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        update { d ->
            d.copy(weekPlan = d.weekPlan + (day to d.weekPlan[day].orEmpty().map {
                if (it.id == id) it.copy(name = trimmed) else it
            }))
        }
        syncTodayWithPlan()
    }

    fun moveWeekItem(day: Int, from: Int, to: Int) {
        update { d ->
            val list = d.weekPlan[day].orEmpty().toMutableList()
            if (from !in list.indices || to !in list.indices) {
                d
            } else {
                val moved = list.removeAt(from)
                list.add(to, moved)
                d.copy(weekPlan = d.weekPlan + (day to list.toList()))
            }
        }
        syncTodayWithPlan()
    }

    // --- Dodatki ---

    fun addEssential(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        update { d -> d.copy(essentials = d.essentials + ChecklistItem(newId(), trimmed)) }
        syncTodayWithPlan()
    }

    fun removeEssential(id: String) {
        update { d -> d.copy(essentials = d.essentials.filterNot { it.id == id }) }
        syncTodayWithPlan()
    }

    fun renameEssential(id: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        update { d ->
            d.copy(essentials = d.essentials.map { if (it.id == id) it.copy(name = trimmed) else it })
        }
        syncTodayWithPlan()
    }

    fun moveEssential(from: Int, to: Int) {
        update { d ->
            val list = d.essentials.toMutableList()
            if (from !in list.indices || to !in list.indices) d
            else {
                val moved = list.removeAt(from)
                list.add(to, moved)
                d.copy(essentials = list.toList())
            }
        }
        syncTodayWithPlan()
    }

    fun addBikeExtra(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        update { d -> d.copy(bikeExtras = d.bikeExtras + ChecklistItem(newId(), trimmed)) }
        syncTodayWithPlan()
    }

    fun removeBikeExtra(id: String) {
        update { d -> d.copy(bikeExtras = d.bikeExtras.filterNot { it.id == id }) }
        syncTodayWithPlan()
    }

    fun renameBikeExtra(id: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        update { d ->
            d.copy(bikeExtras = d.bikeExtras.map { if (it.id == id) it.copy(name = trimmed) else it })
        }
        syncTodayWithPlan()
    }

    fun moveBikeExtra(from: Int, to: Int) {
        update { d ->
            val list = d.bikeExtras.toMutableList()
            if (from !in list.indices || to !in list.indices) d
            else {
                val moved = list.removeAt(from)
                list.add(to, moved)
                d.copy(bikeExtras = list.toList())
            }
        }
        syncTodayWithPlan()
    }

    // --- Motyw ---

    fun setThemeMode(mode: String) {
        update { it.copy(themeMode = mode) }
    }

    // --- Pogoda ---

    fun setCity(name: String) {
        val trimmed = name.trim()
        update { it.copy(cityName = trimmed, regionName = "", cityLat = null, cityLon = null) }
        _weather.value = null
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val place = Weather.geocode(trimmed)
            if (place != null) {
                update {
                    it.copy(
                        cityName = place.name,
                        regionName = place.region,
                        cityLat = place.lat,
                        cityLon = place.lon
                    )
                }
            }
            refreshWeather()
        }
    }

    fun refreshWeather() {
        val d = _data.value
        if (d.cityName.isBlank()) {
            _weather.value = null
            return
        }
        viewModelScope.launch {
            _weather.value = Weather.load(d.cityName, d.regionName, d.cityLat, d.cityLon)
        }
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

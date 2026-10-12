package com.wskakuj.grabio.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.wskakuj.grabio.MainActivity
import com.wskakuj.grabio.R
import com.wskakuj.grabio.data.DayRecord
import com.wskakuj.grabio.data.GROUP_CUSTOM
import com.wskakuj.grabio.data.Store
import com.wskakuj.grabio.data.TRANSPORT_BIKE
import com.wskakuj.grabio.data.TRANSPORT_CAR
import com.wskakuj.grabio.data.buildDayItems
import com.wskakuj.grabio.data.defaultBag
import com.wskakuj.grabio.notify.ProgressNotifier
import java.time.LocalDate

/**
 * Widget na ekran główny: nagłówek z dniem i postępem, wybór transportu
 * (żeby nie trzeba było otwierać aplikacji) oraz przewijalna lista rzeczy
 * na dziś. Dotknięcie wiersza odhacza pozycję.
 */
class GrabioWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, build(context)) }
        manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH -> refreshAll(context)
            ACTION_SET_TRANSPORT -> {
                val mode = intent.getStringExtra(EXTRA_TRANSPORT)
                if (mode != null) {
                    setTransport(context, mode)
                    refreshAll(context)
                }
            }
            ACTION_TOGGLE -> {
                val itemId = intent.getStringExtra(EXTRA_ID)
                if (itemId != null) {
                    toggle(context, itemId)
                    refreshAll(context)
                }
            }
            ACTION_PACK_ALL -> {
                packAll(context)
                refreshAll(context)
            }
        }
    }

    private fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, GrabioWidget::class.java))
        if (ids.isEmpty()) return
        val views = build(context)
        ids.forEach { manager.updateAppWidget(it, views) }
        manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
    }

    /**
     * Wybór transportu wprost z widgetu: buduje dzisiejszą listę tak samo,
     * jak robi to aplikacja, i zapisuje wynik.
     */
    private fun setTransport(context: Context, mode: String) {
        val store = Store(context)
        val data = store.load()
        val key = LocalDate.now().toString()
        val weekday = LocalDate.now().dayOfWeek.value
        val existing = data.days.find { it.date == key }
        val prev = existing?.items?.associate { it.name to it.checked } ?: emptyMap()
        val custom = existing?.items?.filter { it.group == GROUP_CUSTOM } ?: emptyList()
        val bag = existing?.bag?.ifEmpty { defaultBag(weekday) } ?: defaultBag(weekday)
        val record = DayRecord(
            date = key,
            transport = mode,
            bag = bag,
            items = buildDayItems(data, weekday, mode, custom, prev)
        )
        store.save(data.copy(days = data.days.filterNot { it.date == key } + record))
        runCatching { ProgressNotifier.syncFromStore(context) }
    }

    private fun toggle(context: Context, itemId: String) {
        val store = Store(context)
        val data = store.load()
        val key = LocalDate.now().toString()
        val newDays = data.days.map { day ->
            if (day.date != key) {
                day
            } else {
                day.copy(items = day.items.map {
                    if (it.id == itemId) it.copy(checked = !it.checked) else it
                })
            }
        }
        store.save(data.copy(days = newDays))
        runCatching { ProgressNotifier.syncFromStore(context) }
    }

    private fun packAll(context: Context) {
        val store = Store(context)
        val data = store.load()
        val key = LocalDate.now().toString()
        val newDays = data.days.map { day ->
            if (day.date != key) day
            else day.copy(items = day.items.map { it.copy(checked = true) })
        }
        store.save(data.copy(days = newDays))
        runCatching { ProgressNotifier.syncFromStore(context) }
    }

    private fun build(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_grabio)
        val appData = Store(context).load()
        val key = LocalDate.now().toString()
        val record = appData.days.find { it.date == key }

        val dayName = listOf(
            "Poniedziałek", "Wtorek", "Środa", "Czwartek",
            "Piątek", "Sobota", "Niedziela"
        ).getOrElse(LocalDate.now().dayOfWeek.value - 1) { "" }
        views.setTextViewText(R.id.widget_day, dayName)

        // --- wybór transportu ---
        val transport = record?.transport ?: ""
        val carOn = transport == TRANSPORT_CAR
        val bikeOn = transport == TRANSPORT_BIKE

        views.setTextViewText(R.id.widget_car, "🚗  Samochód")
        views.setInt(
            R.id.widget_car, "setBackgroundResource",
            if (carOn) R.drawable.widget_choice_bg_on else R.drawable.widget_choice_bg
        )
        views.setTextColor(
            R.id.widget_car,
            if (carOn) 0xFFA3FF12.toInt() else 0xFFEDEDE6.toInt()
        )
        views.setOnClickPendingIntent(
            R.id.widget_car,
            transportPending(context, TRANSPORT_CAR)
        )

        views.setTextViewText(R.id.widget_bike, "🚲  Rower")
        views.setInt(
            R.id.widget_bike, "setBackgroundResource",
            if (bikeOn) R.drawable.widget_choice_bg_on else R.drawable.widget_choice_bg
        )
        views.setTextColor(
            R.id.widget_bike,
            if (bikeOn) 0xFFA3FF12.toInt() else 0xFFEDEDE6.toInt()
        )
        views.setOnClickPendingIntent(
            R.id.widget_bike,
            transportPending(context, TRANSPORT_BIKE)
        )

        // --- postęp ---
        if (record == null || record.items.isEmpty()) {
            views.setTextViewText(R.id.widget_count, "—")
            views.setViewVisibility(R.id.widget_progress, View.GONE)
        } else {
            views.setTextViewText(R.id.widget_count, "${record.done}/${record.total}")
            views.setViewVisibility(R.id.widget_progress, View.VISIBLE)
            views.setProgressBar(
                R.id.widget_progress,
                record.total.coerceAtLeast(1),
                record.done,
                false
            )
        }

        // --- komunikat, gdy lista jest pusta ---
        val emptyText = when {
            record?.restDay == true -> "🛋️  Dziś masz wolne"
            record == null || record.transport.isEmpty() -> "Wybierz, czym jedziesz 👆"
            else -> "Brak pozycji na dziś"
        }
        views.setTextViewText(R.id.widget_empty, emptyText)
        views.setEmptyView(R.id.widget_list, R.id.widget_empty)

        // --- przewijalna lista ---
        val serviceIntent = Intent(context, GrabioWidgetService::class.java).apply {
            setData(Uri.parse(toUri(Intent.URI_INTENT_SCHEME)))
        }
        views.setRemoteAdapter(R.id.widget_list, serviceIntent)

        val template = Intent(context, GrabioWidget::class.java).apply { action = ACTION_TOGGLE }
        val templatePending = PendingIntent.getBroadcast(
            context, 50, template,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.widget_list, templatePending)

        val open = Intent(context, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            context, 100, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_header, openPending)

        return views
    }

    /** Osobny PendingIntent dla każdego kafelka transportu. */
    private fun transportPending(context: Context, mode: String): PendingIntent {
        val intent = Intent(context, GrabioWidget::class.java).apply {
            action = ACTION_SET_TRANSPORT
            putExtra(EXTRA_TRANSPORT, mode)
        }
        val code = if (mode == TRANSPORT_CAR) 60 else 61
        return PendingIntent.getBroadcast(
            context, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_REFRESH = "com.wskakuj.grabio.action.WIDGET_REFRESH"
        const val ACTION_TOGGLE = "com.wskakuj.grabio.action.WIDGET_TOGGLE"
        const val ACTION_PACK_ALL = "com.wskakuj.grabio.action.WIDGET_PACK_ALL"
        const val ACTION_SET_TRANSPORT = "com.wskakuj.grabio.action.WIDGET_SET_TRANSPORT"
        const val EXTRA_ID = "item_id"
        const val EXTRA_TRANSPORT = "transport_mode"
    }
}

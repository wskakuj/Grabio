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
import com.wskakuj.grabio.data.Store
import java.time.LocalDate

/**
 * Widget na ekran główny: nagłówek z dniem i postępem oraz przewijalna
 * lista rzeczy na dziś. Dotknięcie wiersza odhacza pozycję.
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

        // przewijalna lista
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

    companion object {
        const val ACTION_REFRESH = "com.wskakuj.grabio.action.WIDGET_REFRESH"
        const val ACTION_TOGGLE = "com.wskakuj.grabio.action.WIDGET_TOGGLE"
        const val ACTION_PACK_ALL = "com.wskakuj.grabio.action.WIDGET_PACK_ALL"
        const val EXTRA_ID = "item_id"
    }
}

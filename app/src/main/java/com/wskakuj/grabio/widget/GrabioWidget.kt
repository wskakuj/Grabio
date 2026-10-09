package com.wskakuj.grabio.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.wskakuj.grabio.MainActivity
import com.wskakuj.grabio.R
import com.wskakuj.grabio.data.Store
import java.time.LocalDate

/**
 * Widget na ekran główny: dzisiejsza lista z odhaczaniem.
 * Przytrzymanie wiersza w aplikacji zmienia kolejność, tu można odhaczać.
 */
class GrabioWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, build(context)) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH -> refreshAll(context)
            ACTION_TOGGLE -> {
                val itemId = intent.getStringExtra(EXTRA_ID)
                if (itemId != null) {
                    setChecked(context, itemId, null)
                    refreshAll(context)
                }
            }
            ACTION_PACK_ALL -> {
                setChecked(context, null, true)
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
    }

    /** checked == null → przełącz; checked == true/false → ustaw. */
    private fun setChecked(context: Context, itemId: String?, checked: Boolean?) {
        val store = Store(context)
        val data = store.load()
        val key = LocalDate.now().toString()
        val newDays = data.days.map { day ->
            if (day.date != key) {
                day
            } else {
                day.copy(items = day.items.map { item ->
                    when {
                        itemId != null && item.id == itemId -> item.copy(checked = !item.checked)
                        itemId == null && checked != null -> item.copy(checked = checked)
                        else -> item
                    }
                })
            }
        }
        store.save(data.copy(days = newDays))
    }

    private fun build(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_grabio)
        val data = Store(context).load()
        val key = LocalDate.now().toString()
        val record = data.days.find { it.date == key }

        if (record == null || record.items.isEmpty()) {
            views.setTextViewText(R.id.widget_title, "Grabio")
            views.setTextViewText(R.id.widget_sub, "Brak listy na dziś")
            views.setViewVisibility(R.id.widget_progress, View.GONE)
            for (i in 0 until MAX_ROWS) {
                views.setViewVisibility(ROW_IDS[i], View.GONE)
            }
        } else {
            views.setTextViewText(R.id.widget_title, "Dziś")
            views.setTextViewText(R.id.widget_sub, "${record.done}/${record.total} spakowane")
            views.setViewVisibility(R.id.widget_progress, View.VISIBLE)
            views.setProgressBar(
                R.id.widget_progress,
                record.total.coerceAtLeast(1),
                record.done,
                false
            )
            for (i in 0 until MAX_ROWS) {
                if (i < record.items.size) {
                    val item = record.items[i]
                    views.setViewVisibility(ROW_IDS[i], View.VISIBLE)
                    views.setTextViewText(TEXT_IDS[i], item.name)
                    views.setImageViewResource(
                        CHECK_IDS[i],
                        if (item.checked) R.drawable.widget_check_on
                        else R.drawable.widget_check_off
                    )
                    val toggle = Intent(context, GrabioWidget::class.java).apply {
                        action = ACTION_TOGGLE
                        putExtra(EXTRA_ID, item.id)
                    }
                    val pi = PendingIntent.getBroadcast(
                        context, i, toggle,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(ROW_IDS[i], pi)
                } else {
                    views.setViewVisibility(ROW_IDS[i], View.GONE)
                }
            }
        }

        val open = Intent(context, MainActivity::class.java)
        val openPi = PendingIntent.getActivity(
            context, 100, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, openPi)
        views.setOnClickPendingIntent(R.id.widget_sub, openPi)

        return views
    }

    companion object {
        const val ACTION_REFRESH = "com.wskakuj.grabio.action.WIDGET_REFRESH"
        const val ACTION_TOGGLE = "com.wskakuj.grabio.action.WIDGET_TOGGLE"
        const val ACTION_PACK_ALL = "com.wskakuj.grabio.action.WIDGET_PACK_ALL"
        const val EXTRA_ID = "item_id"
        const val MAX_ROWS = 8

        private val ROW_IDS = intArrayOf(
            R.id.row1, R.id.row2, R.id.row3, R.id.row4,
            R.id.row5, R.id.row6, R.id.row7, R.id.row8
        )
        private val TEXT_IDS = intArrayOf(
            R.id.text1, R.id.text2, R.id.text3, R.id.text4,
            R.id.text5, R.id.text6, R.id.text7, R.id.text8
        )
        private val CHECK_IDS = intArrayOf(
            R.id.check1, R.id.check2, R.id.check3, R.id.check4,
            R.id.check5, R.id.check6, R.id.check7, R.id.check8
        )
    }
}

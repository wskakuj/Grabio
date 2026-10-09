package com.wskakuj.grabio.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.wskakuj.grabio.R
import com.wskakuj.grabio.data.DayItem
import com.wskakuj.grabio.data.Store
import java.time.LocalDate

/**
 * Dzięki temu widget ma przewijalną listę (widget kolekcji z ListView).
 */
class GrabioWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        GrabioWidgetFactory(applicationContext)
}

class GrabioWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<DayItem> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        val data = Store(context).load()
        val key = LocalDate.now().toString()
        items = data.days.find { it.date == key }?.items ?: emptyList()
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_row)
        val item = items[position]
        views.setTextViewText(R.id.row_text, item.name)
        views.setImageViewResource(
            R.id.row_check,
            if (item.checked) R.drawable.widget_check_on else R.drawable.widget_check_off
        )
        // odhaczone przygaszone, żeby wzrok od razu łapał, co jeszcze zostało
        views.setTextColor(
            R.id.row_text,
            if (item.checked) 0xFF8A948A.toInt() else 0xFFEDEDE6.toInt()
        )
        // Każdy wiersz dostaje WŁASNY PendingIntent (unikalny requestCode = id pozycji).
        // To pewniejsze niż "fill-in intent" z szablonu — kliknięcie zawsze trafia.
        val tap = Intent(context, GrabioWidget::class.java).apply {
            action = GrabioWidget.ACTION_TOGGLE
            putExtra(GrabioWidget.EXTRA_ID, item.id)
        }
        val tapPending = PendingIntent.getBroadcast(
            context,
            item.id.hashCode(),
            tap,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_row_root, tapPending)
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false
}

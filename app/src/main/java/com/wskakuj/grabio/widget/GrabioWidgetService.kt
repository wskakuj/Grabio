package com.wskakuj.grabio.widget

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
        // „doklejka” — widget dopisze do tego EXTRA_ID przy dotknięciu wiersza
        val fillIn = Intent().apply { putExtra(GrabioWidget.EXTRA_ID, item.id) }
        views.setOnClickFillInIntent(R.id.widget_row_root, fillIn)
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false
}

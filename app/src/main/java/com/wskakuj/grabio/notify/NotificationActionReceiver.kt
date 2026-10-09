package com.wskakuj.grabio.notify

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wskakuj.grabio.data.Store
import com.wskakuj.grabio.widget.WidgetRefresh
import java.time.LocalDate

/** Obsługuje przycisk „Spakowane” w powiadomieniu. */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PACKED) return

        val store = Store(context)
        val data = store.load()
        val key = LocalDate.now().toString()
        val newDays = data.days.map { day ->
            if (day.date != key) {
                day
            } else {
                day.copy(items = day.items.map { it.copy(checked = true) })
            }
        }
        store.save(data.copy(days = newDays))
        WidgetRefresh.update(context)
        ProgressNotifier.syncFromStore(context)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)
    }

    companion object {
        const val ACTION_PACKED = "com.wskakuj.grabio.action.PACKED"
        const val NOTIFICATION_ID = 1001
    }
}

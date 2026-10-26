package com.wskakuj.grabio.notify

import android.app.NotificationManager
import android.content.Context
import com.wskakuj.grabio.data.DayRecord

object PackingReminder {
    fun remaining(record: DayRecord?) =
        record?.takeUnless { it.restDay }?.items?.filterNot { it.checked }.orEmpty()
            .sortedWith(compareByDescending<com.wskakuj.grabio.data.DayItem> { it.priority }.thenBy { it.name })

    /** Cancel an active reminder when a day is completed or marked as a rest day. */
    fun cancelIfFinished(context: Context, record: DayRecord?) {
        if (record != null && (record.restDay || record.complete)) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(NotificationActionReceiver.NOTIFICATION_ID)
        }
    }
}

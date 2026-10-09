package com.treningcheck.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object Notifications {
    const val CHANNEL_ID = "treningcheck_reminder"

    /** Tworzy kanal powiadomien (wymagane na Androidzie 8+). */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Przypomnienia o treningu",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                channel.description = "Codzienne przypomnienie o spakowaniu rzeczy na trening."
                nm.createNotificationChannel(channel)
            }
        }
    }
}

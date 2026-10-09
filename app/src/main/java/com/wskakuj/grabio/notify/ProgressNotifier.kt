package com.wskakuj.grabio.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wskakuj.grabio.MainActivity
import com.wskakuj.grabio.R
import com.wskakuj.grabio.data.DayRecord
import com.wskakuj.grabio.data.Store
import java.time.LocalDate

/**
 * Stały wskaźnik w powiadomieniach: „ile z ilu już spakowane".
 * Pojawia się, gdy odhaczysz pierwszą rzecz, aktualizuje się przy każdej zmianie
 * i zostaje, aż użytkownik sam go zamknie (nie znika po kliknięciu).
 */
object ProgressNotifier {

    const val ID = 1002
    const val CHANNEL_ID = "grabio_progress"

    private var posted = false

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Postęp pakowania",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Cichy wskaźnik: ile rzeczy już spakowane."
            nm.createNotificationChannel(channel)
        }
    }

    /** Wygodne dla widgetu i akcji z powiadomienia — czyta stan z pliku. */
    fun syncFromStore(context: Context) {
        val data = Store(context).load()
        val key = LocalDate.now().toString()
        sync(context, data.days.find { it.date == key })
    }

    fun sync(context: Context, record: DayRecord?) {
        ensureChannel(context)
        val nm = NotificationManagerCompat.from(context)

        if (record == null || record.items.isEmpty()) {
            if (posted) {
                nm.cancel(ID)
                posted = false
            }
            return
        }

        // czekamy, aż coś odhaczysz — wtedy pokazujemy wskaźnik
        if (!posted && record.done == 0) return
        posted = true

        val remaining = record.items.filter { !it.checked }
        val text = when {
            record.complete -> "Wszystko spakowane! 🎉"
            else -> "Zostało: " + remaining.take(4).joinToString(", ") { it.name } +
                if (remaining.size > 4) "…" else ""
        }

        val open = PendingIntent.getActivity(
            context,
            200,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Spakowane: ${record.done} z ${record.total}")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setProgress(record.total, record.done, false)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(open)
            .build()

        try {
            nm.notify(ID, notification)
        } catch (e: SecurityException) {
            // brak zgody na powiadomienia
        }
    }
}

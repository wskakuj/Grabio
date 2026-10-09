package com.treningcheck.app.notify

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.treningcheck.app.MainActivity
import com.treningcheck.app.R

/** Pokazuje powiadomienie przypominajace o spakowaniu sie na trening. */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {

    override fun doWork(): Result {
        val ctx = applicationContext
        Notifications.ensureChannel(ctx)

        // Na Androidzie 13+ powiadomienia wymagaja zgody uzytkownika.
        val granted = ContextCompat.checkSelfPermission(
            ctx, android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            val intent = Intent(ctx, MainActivity::class.java)
            val pending = PendingIntent.getActivity(
                ctx, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notification = NotificationCompat.Builder(ctx, Notifications.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Nie zapomnij sie spakowac!")
                .setContentText("Zajrzyj do listy na dzisiejszy trening.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build()
            try {
                NotificationManagerCompat.from(ctx).notify(1001, notification)
            } catch (e: SecurityException) {
                // Brak zgody — pomijamy.
            }
        }

        return Result.success()
    }
}

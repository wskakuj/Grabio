package com.wskakuj.grabio.notify

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.wskakuj.grabio.MainActivity
import com.wskakuj.grabio.R
import java.time.LocalDate

/**
 * Pokazuje powiadomienie przypominające o spakowaniu się na trening,
 * z przyciskami „Spakowane” i „Otwórz listę”.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {

    override fun doWork(): Result {
        val ctx = applicationContext
        Notifications.ensureChannel(ctx)

        val mode = inputData.getString(KEY_MODE) ?: MODE_WEEKDAY
        val dow = LocalDate.now().dayOfWeek.value // 1 = poniedziałek … 7 = niedziela
        val shouldNotify = if (mode == MODE_WEEKDAY) dow in 1..5 else dow in 6..7
        if (!shouldNotify) return Result.success()

        val granted = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return Result.success()

        val openIntent = Intent(ctx, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            ctx, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val packedIntent = Intent(ctx, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_PACKED
        }
        val packedPending = PendingIntent.getBroadcast(
            ctx, 1, packedIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(ctx, Notifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Czas się spakować na trening")
            .setContentText("Otwórz Grabio i przejdź listę.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Otwórz Grabio i przejdź listę: rzeczy na trening, " +
                        "kłódka do szafki, bateria słuchawek, inhalator, miętówki."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(0, "Spakowane", packedPending)
            .addAction(0, "Otwórz listę", openPending)
            .build()

        try {
            NotificationManagerCompat.from(ctx)
                .notify(NotificationActionReceiver.NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // brak zgody — pomijamy
        }
        return Result.success()
    }

    companion object {
        const val KEY_MODE = "mode"
        const val MODE_WEEKDAY = "weekday"
        const val MODE_WEEKEND = "weekend"
    }
}

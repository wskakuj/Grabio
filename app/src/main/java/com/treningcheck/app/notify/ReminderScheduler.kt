package com.treningcheck.app.notify

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val UNIQUE_NAME = "daily_training_reminder"

    /** Wlacza/wylacza codzienne przypomnienie o wybranej godzinie. */
    fun apply(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(UNIQUE_NAME)
            return
        }

        val now = LocalDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        val delayMinutes = Duration.between(now, target).toMinutes()

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .build()

        wm.enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}

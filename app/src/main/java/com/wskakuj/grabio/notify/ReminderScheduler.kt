package com.wskakuj.grabio.notify

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val WEEKDAY_WORK = "grabio_reminder_weekday"
    private const val WEEKEND_WORK = "grabio_reminder_weekend"

    /**
     * Ustawia dwa przypomnienia:
     *  - dni robocze (pon–pt) o godzinie weekday…,
     *  - weekend (sb–nd) o godzinie weekend…
     */
    fun apply(
        context: Context,
        enabled: Boolean,
        weekdayHour: Int,
        weekdayMinute: Int,
        weekendHour: Int,
        weekendMinute: Int
    ) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(WEEKDAY_WORK)
            wm.cancelUniqueWork(WEEKEND_WORK)
            return
        }
        schedule(wm, WEEKDAY_WORK, weekdayHour, weekdayMinute, ReminderWorker.MODE_WEEKDAY)
        schedule(wm, WEEKEND_WORK, weekendHour, weekendMinute, ReminderWorker.MODE_WEEKEND)
    }

    private fun schedule(
        wm: WorkManager,
        name: String,
        hour: Int,
        minute: Int,
        mode: String
    ) {
        val now = LocalDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        val delayMinutes = Duration.between(now, target).toMinutes()

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(workDataOf(ReminderWorker.KEY_MODE to mode))
            .build()

        wm.enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}

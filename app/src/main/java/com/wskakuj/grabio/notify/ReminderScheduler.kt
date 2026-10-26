package com.wskakuj.grabio.notify

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val WEEKDAY_WORK = "grabio_reminder_weekday"
    private const val WEEKEND_WORK = "grabio_reminder_weekend"
    private const val TAG = "GrabioReminders"

    /** Call on a background thread at startup; never reset existing schedules. */
    fun ensureScheduled(
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
        try {
            if (!hasActiveWork(wm, WEEKDAY_WORK)) {
                schedule(wm, WEEKDAY_WORK, weekdayHour, weekdayMinute, ReminderWorker.MODE_WEEKDAY)
            }
            if (!hasActiveWork(wm, WEEKEND_WORK)) {
                schedule(wm, WEEKEND_WORK, weekendHour, weekendMinute, ReminderWorker.MODE_WEEKEND)
            }
        } catch (e: Exception) {
            // A transient WorkManager error must not destroy an existing schedule.
            Log.e(TAG, "Cannot verify reminder schedules", e)
        }
    }

    private fun hasActiveWork(wm: WorkManager, name: String): Boolean =
        wm.getWorkInfosForUniqueWork(name).get(10, TimeUnit.SECONDS)
            .any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.BLOCKED }

    /** Called when the user changes settings; replaces existing schedules. */
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

    private fun schedule(wm: WorkManager, name: String, hour: Int, minute: Int, mode: String) {
        require(hour in 0..23 && minute in 0..59) { "Invalid reminder time" }
        val now = LocalDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        val delayMillis = Duration.between(now, target).toMillis().coerceAtLeast(0L)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_MODE to mode))
            .build()
        wm.enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}

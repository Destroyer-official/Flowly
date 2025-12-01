package com.ledger.app.domain.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ledger.app.domain.model.Reminder
import com.ledger.app.domain.model.RepeatPattern
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for reminder notifications using WorkManager.
 * 
 * Requirements: 6.1, 6.3, 6.4
 * - Schedule one-time and repeating reminders
 * - Handle snooze (reschedule +24 hours)
 * - Cancel reminders
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedule a reminder notification.
     * Supports one-time and repeating reminders based on the repeat pattern.
     */
    fun scheduleReminder(reminder: Reminder) {
        val workName = getWorkName(reminder.id)

        when (reminder.repeatPattern) {
            RepeatPattern.NONE -> scheduleOneTimeReminder(reminder, workName)
            RepeatPattern.DAILY -> scheduleRepeatingReminder(reminder, workName, 1, TimeUnit.DAYS)
            RepeatPattern.WEEKLY -> scheduleRepeatingReminder(reminder, workName, 7, TimeUnit.DAYS)
            RepeatPattern.MONTHLY -> scheduleRepeatingReminder(reminder, workName, 30, TimeUnit.DAYS)
        }
    }

    /**
     * Schedule a one-time reminder.
     */
    private fun scheduleOneTimeReminder(reminder: Reminder, workName: String) {
        val delay = calculateDelay(reminder.dueDateTime)

        val inputData = Data.Builder()
            .putLong(ReminderWorker.KEY_REMINDER_ID, reminder.id)
            .putString(ReminderWorker.KEY_TITLE, reminder.title)
            .putString(ReminderWorker.KEY_DESCRIPTION, reminder.description)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Schedule a repeating reminder.
     */
    private fun scheduleRepeatingReminder(
        reminder: Reminder,
        workName: String,
        interval: Long,
        timeUnit: TimeUnit
    ) {
        val delay = calculateDelay(reminder.dueDateTime)

        val inputData = Data.Builder()
            .putLong(ReminderWorker.KEY_REMINDER_ID, reminder.id)
            .putString(ReminderWorker.KEY_TITLE, reminder.title)
            .putString(ReminderWorker.KEY_DESCRIPTION, reminder.description)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(interval, timeUnit)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniquePeriodicWork(
            workName,
            androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Cancel a scheduled reminder.
     */
    fun cancelReminder(reminderId: Long) {
        val workName = getWorkName(reminderId)
        workManager.cancelUniqueWork(workName)
    }

    /**
     * Reschedule a reminder (used for snooze functionality).
     * This is equivalent to scheduling a new reminder with updated time.
     */
    fun rescheduleReminder(reminder: Reminder) {
        // Cancel existing work and schedule new one
        cancelReminder(reminder.id)
        scheduleReminder(reminder)
    }

    /**
     * Calculate delay in milliseconds from now until the due date/time.
     */
    private fun calculateDelay(dueDateTime: LocalDateTime): Long {
        val now = LocalDateTime.now()
        val dueInstant = dueDateTime.atZone(ZoneId.systemDefault()).toInstant()
        val nowInstant = now.atZone(ZoneId.systemDefault()).toInstant()
        
        val delay = Duration.between(nowInstant, dueInstant).toMillis()
        
        // If the time has already passed, schedule immediately
        return if (delay < 0) 0L else delay
    }

    /**
     * Generate unique work name for a reminder.
     */
    private fun getWorkName(reminderId: Long): String {
        return "reminder_$reminderId"
    }
}

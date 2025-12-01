package com.ledger.app.domain.worker

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ledger.app.domain.model.Reminder
import com.ledger.app.domain.model.ReminderStatus
import com.ledger.app.domain.repository.ReminderRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * BroadcastReceiver that handles notification actions for reminders.
 * Handles "Mark Done" and "Snooze" actions from reminder notifications.
 */
@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderRepository: ReminderRepository

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderWorker.KEY_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        // Dismiss the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(reminderId.toInt())

        when (intent.action) {
            ReminderWorker.ACTION_MARK_DONE -> {
                scope.launch {
                    handleMarkDone(reminderId)
                }
            }
            ReminderWorker.ACTION_SNOOZE -> {
                scope.launch {
                    handleSnooze(reminderId)
                }
            }
        }
    }

    private suspend fun handleMarkDone(reminderId: Long) {
        // Get the reminder from the database
        val reminder = reminderRepository.getUpcoming().let { flow ->
            var result: Reminder? = null
            flow.collect { reminders ->
                result = reminders.find { it.id == reminderId }
            }
            result
        } ?: return

        // Update status to DONE
        val updatedReminder = reminder.copy(status = ReminderStatus.DONE)
        reminderRepository.update(updatedReminder)
    }

    private suspend fun handleSnooze(reminderId: Long) {
        // Get the reminder from the database
        val reminder = reminderRepository.getUpcoming().let { flow ->
            var result: Reminder? = null
            flow.collect { reminders ->
                result = reminders.find { it.id == reminderId }
            }
            result
        } ?: return

        // Update status to SNOOZED and reschedule for +24 hours
        val updatedReminder = reminder.copy(
            status = ReminderStatus.SNOOZED,
            dueDateTime = LocalDateTime.now().plusDays(1)
        )
        reminderRepository.update(updatedReminder)

        // Reschedule the reminder
        reminderScheduler.scheduleReminder(updatedReminder)
    }
}

package com.ledger.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.app.domain.model.Reminder
import com.ledger.app.domain.model.ReminderStatus
import com.ledger.app.domain.model.ReminderTargetType
import com.ledger.app.domain.model.RepeatPattern
import com.ledger.app.domain.repository.ReminderRepository
import com.ledger.app.domain.worker.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for managing reminders.
 * 
 * Requirements: 6.1, 6.5
 * - List upcoming reminders
 * - Create new reminder
 * - Mark done, snooze actions
 */
@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    // State for upcoming reminders
    val upcomingReminders: StateFlow<List<Reminder>> = reminderRepository.getUpcoming()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // State for create reminder dialog
    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    // State for new reminder form
    private val _newReminderTitle = MutableStateFlow("")
    val newReminderTitle: StateFlow<String> = _newReminderTitle.asStateFlow()

    private val _newReminderDescription = MutableStateFlow("")
    val newReminderDescription: StateFlow<String> = _newReminderDescription.asStateFlow()

    private val _newReminderDateTime = MutableStateFlow(LocalDateTime.now().plusHours(1))
    val newReminderDateTime: StateFlow<LocalDateTime> = _newReminderDateTime.asStateFlow()

    private val _newReminderRepeatPattern = MutableStateFlow(RepeatPattern.NONE)
    val newReminderRepeatPattern: StateFlow<RepeatPattern> = _newReminderRepeatPattern.asStateFlow()

    /**
     * Show the create reminder dialog.
     */
    fun showCreateDialog() {
        _showCreateDialog.value = true
    }

    /**
     * Hide the create reminder dialog and reset form.
     */
    fun hideCreateDialog() {
        _showCreateDialog.value = false
        resetForm()
    }

    /**
     * Update new reminder title.
     */
    fun updateTitle(title: String) {
        _newReminderTitle.value = title
    }

    /**
     * Update new reminder description.
     */
    fun updateDescription(description: String) {
        _newReminderDescription.value = description
    }

    /**
     * Update new reminder date/time.
     */
    fun updateDateTime(dateTime: LocalDateTime) {
        _newReminderDateTime.value = dateTime
    }

    /**
     * Update new reminder repeat pattern.
     */
    fun updateRepeatPattern(pattern: RepeatPattern) {
        _newReminderRepeatPattern.value = pattern
    }

    /**
     * Create a new reminder.
     */
    fun createReminder() {
        viewModelScope.launch {
            val reminder = Reminder(
                targetType = ReminderTargetType.GENERIC,
                targetId = null,
                title = _newReminderTitle.value,
                description = _newReminderDescription.value.ifBlank { null },
                dueDateTime = _newReminderDateTime.value,
                repeatPattern = _newReminderRepeatPattern.value,
                status = ReminderStatus.UPCOMING
            )

            val reminderId = reminderRepository.insert(reminder)
            val savedReminder = reminder.copy(id = reminderId)
            
            // Schedule the reminder notification
            reminderScheduler.scheduleReminder(savedReminder)

            hideCreateDialog()
        }
    }

    /**
     * Mark a reminder as done.
     */
    fun markDone(reminder: Reminder) {
        viewModelScope.launch {
            val updatedReminder = reminder.copy(status = ReminderStatus.DONE)
            reminderRepository.update(updatedReminder)
            
            // Cancel the scheduled notification
            reminderScheduler.cancelReminder(reminder.id)
        }
    }

    /**
     * Snooze a reminder for 24 hours.
     */
    fun snooze(reminder: Reminder) {
        viewModelScope.launch {
            val updatedReminder = reminder.copy(
                status = ReminderStatus.SNOOZED,
                dueDateTime = LocalDateTime.now().plusDays(1)
            )
            reminderRepository.update(updatedReminder)
            
            // Reschedule the notification
            reminderScheduler.rescheduleReminder(updatedReminder)
        }
    }

    /**
     * Delete a reminder.
     */
    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderRepository.delete(reminder.id)
            
            // Cancel the scheduled notification
            reminderScheduler.cancelReminder(reminder.id)
        }
    }

    /**
     * Reset the form fields.
     */
    private fun resetForm() {
        _newReminderTitle.value = ""
        _newReminderDescription.value = ""
        _newReminderDateTime.value = LocalDateTime.now().plusHours(1)
        _newReminderRepeatPattern.value = RepeatPattern.NONE
    }
}

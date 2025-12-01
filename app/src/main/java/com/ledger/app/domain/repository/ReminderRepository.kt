package com.ledger.app.domain.repository

import com.ledger.app.domain.model.Reminder
import com.ledger.app.domain.model.ReminderTargetType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for reminder data access.
 */
interface ReminderRepository {
    suspend fun insert(reminder: Reminder): Long
    suspend fun update(reminder: Reminder)
    suspend fun delete(reminderId: Long)
    suspend fun getById(reminderId: Long): Reminder?
    fun getUpcoming(): Flow<List<Reminder>>
    suspend fun getUpcomingCount(): Int
    
    /**
     * Clear all reminders associated with a transaction.
     * Used when a transaction is settled.
     * Requirements: 8.5
     */
    suspend fun clearRemindersForTransaction(transactionId: Long)
    
    /**
     * Snooze a reminder by rescheduling it to a new date/time.
     */
    suspend fun snoozeReminder(reminderId: Long, newDueDateTime: java.time.LocalDateTime)
    
    /**
     * Reschedule an ignored reminder to the next day and increment ignoredCount.
     * Requirements: 8.4
     */
    suspend fun rescheduleIgnoredReminder(reminderId: Long)
    
    /**
     * Get all overdue reminders that need to be auto-rescheduled.
     */
    suspend fun getOverdueReminders(): List<Reminder>
    
    /**
     * Reschedule all ignored/overdue reminders to the next day.
     * Requirements: 8.4
     */
    suspend fun rescheduleAllIgnoredReminders()
    
    /**
     * Get reminders by target type.
     */
    fun getByTargetType(targetType: ReminderTargetType): Flow<List<Reminder>>
    
    /**
     * Get reminders for a specific transaction.
     */
    suspend fun getRemindersForTransaction(transactionId: Long): List<Reminder>
}

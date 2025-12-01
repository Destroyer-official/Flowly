package com.ledger.app.data.repository

import com.ledger.app.data.local.dao.ReminderDao
import com.ledger.app.data.local.entity.ReminderEntity
import com.ledger.app.domain.model.Reminder
import com.ledger.app.domain.model.ReminderStatus
import com.ledger.app.domain.model.ReminderTargetType
import com.ledger.app.domain.model.RepeatPattern
import com.ledger.app.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao
) : ReminderRepository {

    override suspend fun insert(reminder: Reminder): Long {
        return reminderDao.insert(reminder.toEntity())
    }

    override suspend fun update(reminder: Reminder) {
        reminderDao.update(reminder.toEntity())
    }

    override suspend fun delete(reminderId: Long) {
        reminderDao.delete(reminderId)
    }

    override fun getUpcoming(): Flow<List<Reminder>> {
        return reminderDao.getUpcoming().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getUpcomingCount(): Int {
        return reminderDao.getUpcomingCount()
    }

    override suspend fun getById(reminderId: Long): Reminder? {
        return reminderDao.getById(reminderId)?.toDomain()
    }

    /**
     * Clear all reminders associated with a transaction.
     * Used when a transaction is settled (Property 16: Reminder Auto-Clear).
     * Requirements: 8.5
     */
    override suspend fun clearRemindersForTransaction(transactionId: Long) {
        reminderDao.clearForTransaction(transactionId)
    }

    /**
     * Snooze a reminder by rescheduling it to a new date/time.
     */
    override suspend fun snoozeReminder(reminderId: Long, newDueDateTime: LocalDateTime) {
        val newDateTimeMillis = newDueDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        reminderDao.snooze(reminderId, newDateTimeMillis)
    }

    /**
     * Reschedule an ignored reminder to the next day and increment ignoredCount.
     * Requirements: 8.4
     */
    override suspend fun rescheduleIgnoredReminder(reminderId: Long) {
        val reminder = reminderDao.getById(reminderId) ?: return
        val currentDueDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(reminder.dueDateTime),
            ZoneId.systemDefault()
        )
        // Reschedule to next day at the same time
        val newDueDateTime = currentDueDateTime.plusDays(1)
        val newDateTimeMillis = newDueDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        reminderDao.rescheduleIgnored(reminderId, newDateTimeMillis)
    }

    /**
     * Get all overdue reminders that need to be auto-rescheduled.
     */
    override suspend fun getOverdueReminders(): List<Reminder> {
        val currentTimeMillis = System.currentTimeMillis()
        return reminderDao.getOverdueReminders(currentTimeMillis).map { it.toDomain() }
    }

    /**
     * Reschedule all ignored/overdue reminders to the next day.
     * Requirements: 8.4
     */
    override suspend fun rescheduleAllIgnoredReminders() {
        val overdueReminders = getOverdueReminders()
        for (reminder in overdueReminders) {
            rescheduleIgnoredReminder(reminder.id)
        }
    }

    /**
     * Get reminders by target type.
     */
    override fun getByTargetType(targetType: ReminderTargetType): Flow<List<Reminder>> {
        return reminderDao.getByTargetType(targetType.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Get reminders for a specific transaction.
     */
    override suspend fun getRemindersForTransaction(transactionId: Long): List<Reminder> {
        return reminderDao.getForTransaction(transactionId).map { it.toDomain() }
    }

    private fun Reminder.toEntity(): ReminderEntity {
        return ReminderEntity(
            id = id,
            targetType = targetType.name,
            targetId = targetId,
            title = title,
            description = description,
            dueDateTime = dueDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            repeatPattern = repeatPattern.name,
            status = status.name,
            ignoredCount = ignoredCount
        )
    }

    private fun ReminderEntity.toDomain(): Reminder {
        return Reminder(
            id = id,
            targetType = ReminderTargetType.valueOf(targetType),
            targetId = targetId,
            title = title,
            description = description,
            dueDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(dueDateTime),
                ZoneId.systemDefault()
            ),
            repeatPattern = RepeatPattern.valueOf(repeatPattern),
            status = ReminderStatus.valueOf(status),
            ignoredCount = ignoredCount
        )
    }
}

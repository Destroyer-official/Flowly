package com.ledger.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ledger.app.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE status = 'UPCOMING' ORDER BY dueDateTime")
    fun getUpcoming(): Flow<List<ReminderEntity>>

    @Query("SELECT COUNT(*) FROM reminders WHERE status = 'UPCOMING'")
    suspend fun getUpcomingCount(): Int

    @Query("DELETE FROM reminders WHERE targetType = 'TRANSACTION' AND targetId = :transactionId")
    suspend fun clearForTransaction(transactionId: Long)

    @Query("UPDATE reminders SET status = 'SNOOZED', dueDateTime = :newDateTime WHERE id = :id")
    suspend fun snooze(id: Long, newDateTime: Long)

    /**
     * Reschedule an ignored reminder to the next day and increment ignoredCount.
     */
    @Query("UPDATE reminders SET dueDateTime = :newDateTime, ignoredCount = ignoredCount + 1 WHERE id = :id")
    suspend fun rescheduleIgnored(id: Long, newDateTime: Long)

    /**
     * Get all overdue reminders that are still UPCOMING (past due date).
     */
    @Query("SELECT * FROM reminders WHERE status = 'UPCOMING' AND dueDateTime < :currentTime")
    suspend fun getOverdueReminders(currentTime: Long): List<ReminderEntity>

    /**
     * Get reminders by target type.
     */
    @Query("SELECT * FROM reminders WHERE targetType = :targetType AND status = 'UPCOMING' ORDER BY dueDateTime")
    fun getByTargetType(targetType: String): Flow<List<ReminderEntity>>

    /**
     * Get reminders for a specific transaction.
     */
    @Query("SELECT * FROM reminders WHERE targetType = 'TRANSACTION' AND targetId = :transactionId AND status = 'UPCOMING'")
    suspend fun getForTransaction(transactionId: Long): List<ReminderEntity>

    // Backup/Restore operations
    @Query("SELECT * FROM reminders")
    suspend fun getAllForBackup(): List<ReminderEntity>

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(reminders: List<ReminderEntity>)
}

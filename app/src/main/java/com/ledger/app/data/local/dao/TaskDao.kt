package com.ledger.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ledger.app.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Task operations.
 */
@Dao
interface TaskDao {
    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'PENDING' ORDER BY priority DESC, CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END, dueDate ASC, createdAt DESC")
    fun getPendingTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY createdAt DESC")
    fun getByStatus(status: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE priority = :priority AND status = 'PENDING' ORDER BY CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END, dueDate ASC, createdAt DESC")
    fun getByPriority(priority: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate IS NOT NULL AND status = 'PENDING' ORDER BY dueDate ASC")
    fun getTasksWithDueDate(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate <= :dateMillis AND status = 'PENDING' ORDER BY dueDate ASC")
    fun getTasksDueBefore(dateMillis: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE linkedCounterpartyId = :counterpartyId ORDER BY createdAt DESC")
    fun getByCounterparty(counterpartyId: Long): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET status = 'COMPLETED', completedAt = :completedAt WHERE id = :id")
    suspend fun markCompleted(id: Long, completedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET status = 'CANCELLED' WHERE id = :id")
    suspend fun markCancelled(id: Long)

    @Query("UPDATE tasks SET linkedTransactionId = :transactionId WHERE id = :id")
    suspend fun linkTransaction(id: Long, transactionId: Long)

    // Backup/Restore operations
    @Query("SELECT * FROM tasks")
    suspend fun getAllForBackup(): List<TaskEntity>

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(tasks: List<TaskEntity>)
}

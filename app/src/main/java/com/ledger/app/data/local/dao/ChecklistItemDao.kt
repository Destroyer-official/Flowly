package com.ledger.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ledger.app.data.local.entity.ChecklistItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for ChecklistItem operations.
 * Note: Items are automatically deleted when parent task is deleted (CASCADE).
 */
@Dao
interface ChecklistItemDao {
    @Insert
    suspend fun insert(item: ChecklistItemEntity): Long

    @Insert
    suspend fun insertAll(items: List<ChecklistItemEntity>)

    @Update
    suspend fun update(item: ChecklistItemEntity)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM checklist_items WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: Long)

    @Query("SELECT * FROM checklist_items WHERE id = :id")
    suspend fun getById(id: Long): ChecklistItemEntity?

    @Query("SELECT * FROM checklist_items WHERE taskId = :taskId ORDER BY sortOrder ASC")
    fun getByTaskId(taskId: Long): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM checklist_items WHERE taskId = :taskId ORDER BY sortOrder ASC")
    suspend fun getByTaskIdSync(taskId: Long): List<ChecklistItemEntity>

    @Query("UPDATE checklist_items SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun setCompleted(id: Long, isCompleted: Boolean)

    @Query("UPDATE checklist_items SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Query("SELECT COUNT(*) FROM checklist_items WHERE taskId = :taskId")
    suspend fun getCountByTaskId(taskId: Long): Int

    @Query("SELECT COUNT(*) FROM checklist_items WHERE taskId = :taskId AND isCompleted = 1")
    suspend fun getCompletedCountByTaskId(taskId: Long): Int

    // Backup/Restore operations
    @Query("SELECT * FROM checklist_items")
    suspend fun getAllForBackup(): List<ChecklistItemEntity>

    @Query("DELETE FROM checklist_items")
    suspend fun deleteAll()
}

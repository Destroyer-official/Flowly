package com.ledger.app.domain.repository

import com.ledger.app.domain.model.ChecklistItem
import com.ledger.app.domain.model.Task
import com.ledger.app.domain.model.TaskPriority
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Repository interface for task data access.
 */
interface TaskRepository {
    // Task CRUD operations
    suspend fun insert(task: Task): Long
    suspend fun update(task: Task)
    suspend fun delete(taskId: Long)
    suspend fun getById(id: Long): Task?
    fun getAll(): Flow<List<Task>>
    
    // Task queries by status
    fun getPendingTasks(): Flow<List<Task>>
    fun getCompletedTasks(): Flow<List<Task>>
    
    // Task queries by priority and due date
    fun getTasksByPriority(priority: TaskPriority): Flow<List<Task>>
    fun getTasksWithDueDate(): Flow<List<Task>>
    fun getTasksDueBefore(date: LocalDateTime): Flow<List<Task>>
    
    // Task queries by linked entities
    fun getTasksByCounterparty(counterpartyId: Long): Flow<List<Task>>
    
    // Task status operations
    suspend fun markCompleted(taskId: Long)
    suspend fun markCancelled(taskId: Long)
    suspend fun linkTransaction(taskId: Long, transactionId: Long)
    
    // Checklist operations
    suspend fun addChecklistItem(item: ChecklistItem): Long
    suspend fun updateChecklistItem(item: ChecklistItem)
    suspend fun deleteChecklistItem(itemId: Long)
    suspend fun setChecklistItemCompleted(itemId: Long, isCompleted: Boolean)
    fun getChecklistItems(taskId: Long): Flow<List<ChecklistItem>>
}

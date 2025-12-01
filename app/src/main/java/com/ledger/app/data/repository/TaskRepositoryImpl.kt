package com.ledger.app.data.repository

import com.ledger.app.data.local.dao.ChecklistItemDao
import com.ledger.app.data.local.dao.TaskDao
import com.ledger.app.data.local.entity.ChecklistItemEntity
import com.ledger.app.data.local.entity.TaskEntity
import com.ledger.app.domain.model.ChecklistItem
import com.ledger.app.domain.model.Task
import com.ledger.app.domain.model.TaskPriority
import com.ledger.app.domain.model.TaskStatus
import com.ledger.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val checklistItemDao: ChecklistItemDao
) : TaskRepository {

    override suspend fun insert(task: Task): Long {
        val taskId = taskDao.insert(task.toEntity())
        // Insert checklist items if any
        if (task.checklistItems.isNotEmpty()) {
            val items = task.checklistItems.map { it.toEntity(taskId) }
            checklistItemDao.insertAll(items)
        }
        return taskId
    }

    override suspend fun update(task: Task) {
        taskDao.update(task.toEntity())
    }

    override suspend fun delete(taskId: Long) {
        // Checklist items are automatically deleted via CASCADE
        taskDao.delete(taskId)
    }

    override suspend fun getById(id: Long): Task? {
        val entity = taskDao.getById(id) ?: return null
        val checklistItems = checklistItemDao.getByTaskIdSync(id)
        return entity.toDomain(checklistItems)
    }

    override fun getAll(): Flow<List<Task>> {
        return taskDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPendingTasks(): Flow<List<Task>> {
        return taskDao.getPendingTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCompletedTasks(): Flow<List<Task>> {
        return taskDao.getCompletedTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTasksByPriority(priority: TaskPriority): Flow<List<Task>> {
        return taskDao.getByPriority(priority.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTasksWithDueDate(): Flow<List<Task>> {
        return taskDao.getTasksWithDueDate().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTasksDueBefore(date: LocalDateTime): Flow<List<Task>> {
        val dateMillis = date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return taskDao.getTasksDueBefore(dateMillis).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTasksByCounterparty(counterpartyId: Long): Flow<List<Task>> {
        return taskDao.getByCounterparty(counterpartyId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun markCompleted(taskId: Long) {
        taskDao.markCompleted(taskId)
    }

    override suspend fun markCancelled(taskId: Long) {
        taskDao.markCancelled(taskId)
    }

    override suspend fun linkTransaction(taskId: Long, transactionId: Long) {
        taskDao.linkTransaction(taskId, transactionId)
    }

    // Checklist operations
    override suspend fun addChecklistItem(item: ChecklistItem): Long {
        return checklistItemDao.insert(item.toEntity(item.taskId))
    }

    override suspend fun updateChecklistItem(item: ChecklistItem) {
        checklistItemDao.update(item.toEntity(item.taskId))
    }

    override suspend fun deleteChecklistItem(itemId: Long) {
        checklistItemDao.delete(itemId)
    }

    override suspend fun setChecklistItemCompleted(itemId: Long, isCompleted: Boolean) {
        checklistItemDao.setCompleted(itemId, isCompleted)
    }

    override fun getChecklistItems(taskId: Long): Flow<List<ChecklistItem>> {
        return checklistItemDao.getByTaskId(taskId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // Extension functions for entity-domain mapping
    private fun Task.toEntity(): TaskEntity {
        return TaskEntity(
            id = id,
            title = title,
            description = description,
            priority = priority.name,
            dueDate = dueDate?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            status = status.name,
            linkedCounterpartyId = linkedCounterpartyId,
            linkedTransactionId = linkedTransactionId,
            createdAt = createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            completedAt = completedAt?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )
    }

    private fun TaskEntity.toDomain(checklistItems: List<ChecklistItemEntity> = emptyList()): Task {
        return Task(
            id = id,
            title = title,
            description = description,
            priority = TaskPriority.valueOf(priority),
            dueDate = dueDate?.let {
                LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
            },
            status = TaskStatus.valueOf(status),
            linkedCounterpartyId = linkedCounterpartyId,
            linkedTransactionId = linkedTransactionId,
            createdAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(createdAt),
                ZoneId.systemDefault()
            ),
            completedAt = completedAt?.let {
                LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
            },
            checklistItems = checklistItems.map { it.toDomain() }
        )
    }

    private fun ChecklistItem.toEntity(taskId: Long): ChecklistItemEntity {
        return ChecklistItemEntity(
            id = id,
            taskId = taskId,
            text = text,
            isCompleted = isCompleted,
            sortOrder = sortOrder
        )
    }

    private fun ChecklistItemEntity.toDomain(): ChecklistItem {
        return ChecklistItem(
            id = id,
            taskId = taskId,
            text = text,
            isCompleted = isCompleted,
            sortOrder = sortOrder
        )
    }
}

package com.ledger.app.domain.model

import java.time.LocalDateTime

/**
 * Domain model representing a task/todo item.
 */
data class Task(
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDate: LocalDateTime? = null,
    val status: TaskStatus = TaskStatus.PENDING,
    val linkedCounterpartyId: Long? = null,
    val linkedTransactionId: Long? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val completedAt: LocalDateTime? = null,
    val checklistItems: List<ChecklistItem> = emptyList()
)

/**
 * Domain model representing a checklist item within a task.
 */
data class ChecklistItem(
    val id: Long = 0,
    val taskId: Long,
    val text: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0
)

/**
 * Priority level for tasks.
 */
enum class TaskPriority {
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Status of a task.
 */
enum class TaskStatus {
    PENDING,
    COMPLETED,
    CANCELLED
}

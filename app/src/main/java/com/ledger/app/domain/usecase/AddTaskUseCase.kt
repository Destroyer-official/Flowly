package com.ledger.app.domain.usecase

import com.ledger.app.domain.model.ChecklistItem
import com.ledger.app.domain.model.Task
import com.ledger.app.domain.model.TaskPriority
import com.ledger.app.domain.model.TaskStatus
import com.ledger.app.domain.repository.TaskRepository
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Use case for adding a new task.
 * 
 * Validates required fields (title), sets default priority if not specified,
 * and persists to repository.
 * 
 * Requirements: 14.2
 */
class AddTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    /**
     * Result of adding a task.
     */
    sealed class Result {
        data class Success(val taskId: Long) : Result()
        data class ValidationError(val message: String) : Result()
    }

    /**
     * Adds a new task after validating required fields.
     * 
     * @param title The task title (required, cannot be blank)
     * @param description Optional task description
     * @param priority Task priority (defaults to MEDIUM if not specified)
     * @param dueDate Optional due date for the task
     * @param linkedCounterpartyId Optional counterparty ID for financial tasks
     * @param checklistItems Optional list of checklist items
     * @return Result indicating success with task ID or validation error
     */
    suspend operator fun invoke(
        title: String,
        description: String? = null,
        priority: TaskPriority? = null,
        dueDate: LocalDateTime? = null,
        linkedCounterpartyId: Long? = null,
        checklistItems: List<ChecklistItem> = emptyList()
    ): Result {
        // Validate title is not blank
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) {
            return Result.ValidationError("Task title is required")
        }

        // Set default priority if not specified
        val taskPriority = priority ?: TaskPriority.MEDIUM

        // Create task with validated data
        val task = Task(
            id = 0,
            title = trimmedTitle,
            description = description?.trim()?.takeIf { it.isNotBlank() },
            priority = taskPriority,
            dueDate = dueDate,
            status = TaskStatus.PENDING,
            linkedCounterpartyId = linkedCounterpartyId,
            linkedTransactionId = null,
            createdAt = LocalDateTime.now(),
            completedAt = null,
            checklistItems = checklistItems
        )

        // Persist to repository
        val taskId = taskRepository.insert(task)

        return Result.Success(taskId)
    }
}

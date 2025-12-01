package com.ledger.app.domain.usecase

import com.ledger.app.domain.model.Task
import com.ledger.app.domain.model.TaskStatus
import com.ledger.app.domain.repository.TaskRepository
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Use case for marking a task as completed.
 * 
 * Marks task as completed, sets completedAt timestamp, and triggers
 * completion animation flag.
 * 
 * Requirements: 14.4
 */
class CompleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    /**
     * Result of completing a task.
     */
    sealed class Result {
        data class Success(
            val task: Task,
            val shouldShowAnimation: Boolean = true
        ) : Result()
        data class TaskNotFound(val taskId: Long) : Result()
        data class AlreadyCompleted(val taskId: Long) : Result()
        data class TaskCancelled(val taskId: Long) : Result()
    }

    /**
     * Marks a task as completed.
     * 
     * Property 18: Task Completion Status
     * For any task that is marked as completed:
     * - The task status should be COMPLETED
     * - The completedAt timestamp should be set
     * - The task should appear in the completed tasks list (not pending)
     * 
     * @param taskId The ID of the task to complete
     * @return Result indicating success with updated task or error
     */
    suspend operator fun invoke(taskId: Long): Result {
        // Get the task
        val task = taskRepository.getById(taskId)
            ?: return Result.TaskNotFound(taskId)

        // Check if already completed
        if (task.status == TaskStatus.COMPLETED) {
            return Result.AlreadyCompleted(taskId)
        }

        // Check if cancelled
        if (task.status == TaskStatus.CANCELLED) {
            return Result.TaskCancelled(taskId)
        }

        // Mark as completed with timestamp
        val completedAt = LocalDateTime.now()
        val completedTask = task.copy(
            status = TaskStatus.COMPLETED,
            completedAt = completedAt
        )

        // Update in repository
        taskRepository.update(completedTask)

        // Return success with animation flag
        return Result.Success(
            task = completedTask,
            shouldShowAnimation = true
        )
    }
}

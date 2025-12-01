package com.ledger.app.domain.usecase

import com.ledger.app.domain.model.BillCategory
import com.ledger.app.domain.model.Task
import com.ledger.app.domain.model.TaskStatus
import com.ledger.app.domain.model.Transaction
import com.ledger.app.domain.model.TransactionDirection
import com.ledger.app.domain.model.TransactionStatus
import com.ledger.app.domain.model.TransactionType
import com.ledger.app.domain.repository.TaskRepository
import com.ledger.app.domain.repository.TransactionRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Use case for converting a task to a transaction.
 * 
 * Creates a transaction from task data, pre-fills note with task title,
 * pre-selects counterparty if linked, marks task as completed after
 * transaction is saved, and links transaction back to task.
 * 
 * Requirements: 15.1, 15.2, 15.3, 15.4
 */
class ConvertTaskToTransactionUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val transactionRepository: TransactionRepository
) {
    /**
     * Result of converting a task to a transaction.
     */
    sealed class Result {
        data class Success(
            val transactionId: Long,
            val completedTask: Task
        ) : Result()
        data class TaskNotFound(val taskId: Long) : Result()
        data class TaskAlreadyCompleted(val taskId: Long) : Result()
        data class TaskCancelled(val taskId: Long) : Result()
        data class ValidationError(val message: String) : Result()
    }

    /**
     * Converts a task to a transaction.
     * 
     * Property 19: Task-to-Transaction Linking
     * For any task that is converted to a transaction:
     * - The resulting transaction should have linkedTaskId set to the original task ID
     * - The original task should be marked as COMPLETED
     * 
     * @param taskId The ID of the task to convert
     * @param direction The direction of the transaction (GAVE/RECEIVED)
     * @param type The type of transaction
     * @param amount The transaction amount (must be positive)
     * @param accountId The account ID used for the transaction
     * @param categoryId The category ID for the transaction
     * @param transactionDateTime The date/time of the transaction (defaults to now)
     * @param consumerId Optional consumer ID for bill payments
     * @param billCategory Optional bill category for bill payments
     * @param isForSelf True if bill paid for self
     * @return Result indicating success or error
     */
    suspend operator fun invoke(
        taskId: Long,
        direction: TransactionDirection,
        type: TransactionType,
        amount: BigDecimal,
        accountId: Long,
        categoryId: Long,
        transactionDateTime: LocalDateTime = LocalDateTime.now(),
        consumerId: String? = null,
        billCategory: BillCategory? = null,
        isForSelf: Boolean = false
    ): Result {
        // Get the task
        val task = taskRepository.getById(taskId)
            ?: return Result.TaskNotFound(taskId)

        // Check if already completed
        if (task.status == TaskStatus.COMPLETED) {
            return Result.TaskAlreadyCompleted(taskId)
        }

        // Check if cancelled
        if (task.status == TaskStatus.CANCELLED) {
            return Result.TaskCancelled(taskId)
        }

        // Validate amount is positive
        if (amount <= BigDecimal.ZERO) {
            return Result.ValidationError("Amount must be positive")
        }

        // Validate accountId is valid
        if (accountId <= 0) {
            return Result.ValidationError("Account is required")
        }

        // Validate categoryId is valid
        if (categoryId <= 0) {
            return Result.ValidationError("Category is required")
        }

        // Pre-fill note with task title (Requirements 15.2)
        val notes = task.title

        // Pre-select counterparty if linked (Requirements 15.4)
        val counterpartyId = task.linkedCounterpartyId

        // Calculate remainingDue and status based on isForSelf
        val remainingDue = if (isForSelf) BigDecimal.ZERO else amount
        val status = if (isForSelf) TransactionStatus.SETTLED else TransactionStatus.PENDING

        // Create transaction with task title as note and link back to task (Requirements 15.5)
        val transaction = Transaction(
            id = 0,
            direction = direction,
            type = type,
            amount = amount,
            accountId = accountId,
            counterpartyId = counterpartyId,
            categoryId = categoryId,
            transactionDateTime = transactionDateTime,
            status = status,
            notes = notes,
            remainingDue = remainingDue,
            consumerId = consumerId,
            billCategory = billCategory,
            isForSelf = isForSelf,
            linkedTaskId = taskId
        )

        // Persist transaction to repository
        val transactionId = transactionRepository.insert(transaction)

        // Link transaction back to task (Requirements 15.3, 15.5)
        taskRepository.linkTransaction(taskId, transactionId)

        // Mark task as completed (Requirements 15.3)
        val completedAt = LocalDateTime.now()
        val completedTask = task.copy(
            status = TaskStatus.COMPLETED,
            completedAt = completedAt,
            linkedTransactionId = transactionId
        )
        taskRepository.update(completedTask)

        return Result.Success(
            transactionId = transactionId,
            completedTask = completedTask
        )
    }
}

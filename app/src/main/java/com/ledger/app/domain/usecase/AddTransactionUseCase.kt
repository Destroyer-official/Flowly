package com.ledger.app.domain.usecase

import com.ledger.app.domain.model.AuditAction
import com.ledger.app.domain.model.AuditEntityType
import com.ledger.app.domain.model.Transaction
import com.ledger.app.domain.model.TransactionDirection
import com.ledger.app.domain.model.TransactionStatus
import com.ledger.app.domain.model.TransactionType
import com.ledger.app.domain.repository.AuditLogRepository
import com.ledger.app.domain.repository.TransactionRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Use case for adding a new transaction.
 * 
 * Validates required fields (amount, direction, account, category),
 * sets initial remainingDue equal to amount, and persists to repository.
 * Logs CREATE action to audit log.
 * 
 * Requirements: 1.3, 7.3
 */
class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val auditLogRepository: AuditLogRepository
) {
    /**
     * Result of adding a transaction.
     */
    sealed class Result {
        data class Success(val transactionId: Long) : Result()
        data class ValidationError(val message: String) : Result()
    }

    /**
     * Adds a new transaction after validating required fields.
     * 
     * @param direction The direction of the transaction (GAVE/RECEIVED)
     * @param type The type of transaction
     * @param amount The transaction amount (must be positive)
     * @param accountId The account ID used for the transaction
     * @param categoryId The category ID for the transaction
     * @param counterpartyId Optional counterparty ID
     * @param transactionDateTime The date/time of the transaction
     * @param notes Optional notes
     * @param consumerId Optional consumer ID for bill payments (phone number or account number)
     * @param billCategory Optional bill category for bill payments
     * @param isForSelf True if bill paid for self (no debt), false if for others (adds to debt)
     * @param linkedTaskId Optional ID of the task this transaction was created from (Requirements: 15.5)
     * @return Result indicating success with transaction ID or validation error
     */
    suspend operator fun invoke(
        direction: TransactionDirection,
        type: TransactionType,
        amount: BigDecimal,
        accountId: Long,
        categoryId: Long,
        counterpartyId: Long? = null,
        transactionDateTime: LocalDateTime = LocalDateTime.now(),
        notes: String? = null,
        consumerId: String? = null,
        billCategory: com.ledger.app.domain.model.BillCategory? = null,
        isForSelf: Boolean = false,
        linkedTaskId: Long? = null
    ): Result {
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

        // Create transaction with remainingDue equal to amount
        // For self-paid bills, remainingDue is 0 (no debt to track)
        val remainingDue = if (isForSelf) BigDecimal.ZERO else amount
        val status = if (isForSelf) TransactionStatus.SETTLED else TransactionStatus.PENDING
        
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
            linkedTaskId = linkedTaskId
        )

        // Persist to repository
        val transactionId = transactionRepository.insert(transaction)
        
        // Log CREATE action to audit log
        auditLogRepository.logAction(
            action = AuditAction.CREATE,
            entityType = AuditEntityType.TRANSACTION,
            entityId = transactionId,
            oldValue = null,
            newValue = transactionToJson(transaction.copy(id = transactionId)),
            details = "Created transaction: ${direction.name} ${amount}"
        )
        
        return Result.Success(transactionId)
    }
    
    private fun transactionToJson(transaction: Transaction): String {
        // Build JSON manually to avoid Android JSONObject issues in unit tests
        val parts = mutableListOf<String>()
        parts.add("\"id\":${transaction.id}")
        parts.add("\"direction\":\"${transaction.direction.name}\"")
        parts.add("\"type\":\"${transaction.type.name}\"")
        parts.add("\"amount\":\"${transaction.amount.toPlainString()}\"")
        parts.add("\"accountId\":${transaction.accountId}")
        parts.add("\"counterpartyId\":${transaction.counterpartyId ?: "null"}")
        parts.add("\"categoryId\":${transaction.categoryId}")
        parts.add("\"status\":\"${transaction.status.name}\"")
        parts.add("\"notes\":${transaction.notes?.let { "\"$it\"" } ?: "null"}")
        parts.add("\"remainingDue\":\"${transaction.remainingDue.toPlainString()}\"")
        parts.add("\"consumerId\":${transaction.consumerId?.let { "\"$it\"" } ?: "null"}")
        parts.add("\"billCategory\":${transaction.billCategory?.name?.let { "\"$it\"" } ?: "null"}")
        parts.add("\"isForSelf\":${transaction.isForSelf}")
        parts.add("\"linkedTaskId\":${transaction.linkedTaskId ?: "null"}")
        return "{${parts.joinToString(",")}}"
    }
}

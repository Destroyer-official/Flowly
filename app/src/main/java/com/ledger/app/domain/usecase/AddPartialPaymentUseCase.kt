package com.ledger.app.domain.usecase

import com.ledger.app.domain.model.AuditAction
import com.ledger.app.domain.model.AuditEntityType
import com.ledger.app.domain.model.PartialPayment
import com.ledger.app.domain.model.PaymentDirection
import com.ledger.app.domain.model.PaymentMethod
import com.ledger.app.domain.model.Transaction
import com.ledger.app.domain.model.TransactionStatus
import com.ledger.app.domain.repository.AuditLogRepository
import com.ledger.app.domain.repository.PartialPaymentRepository
import com.ledger.app.domain.repository.ReminderRepository
import com.ledger.app.domain.repository.TransactionRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Use case for adding a partial payment to a transaction.
 * 
 * Handles:
 * - Adding payment to repository
 * - Recalculating remainingDue = baseAmount - sum(payments)
 * - Updating transaction status based on remainingDue
 * - Detecting surplus (overpayment) condition
 * - Logging PARTIAL_PAYMENT and UPDATE actions to audit log
 * - Auto-clearing reminders when transaction is settled (Property 16)
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.5, 7.3, 8.5
 */
class AddPartialPaymentUseCase @Inject constructor(
    private val partialPaymentRepository: PartialPaymentRepository,
    private val transactionRepository: TransactionRepository,
    private val auditLogRepository: AuditLogRepository,
    private val reminderRepository: ReminderRepository
) {
    /**
     * Result of adding a partial payment.
     */
    sealed class Result {
        data class Success(
            val paymentId: Long,
            val newRemainingDue: BigDecimal,
            val newStatus: TransactionStatus
        ) : Result()
        
        data class Surplus(
            val paymentId: Long,
            val surplusAmount: BigDecimal,
            val newStatus: TransactionStatus
        ) : Result()
        
        data class ValidationError(val message: String) : Result()
        data class TransactionNotFound(val transactionId: Long) : Result()
    }


    /**
     * Adds a partial payment to a transaction.
     * 
     * @param transactionId The ID of the parent transaction
     * @param amount The payment amount (must be positive)
     * @param direction The direction of the payment
     * @param method The payment method used
     * @param dateTime The date/time of the payment
     * @param notes Optional notes
     * @return Result indicating success, surplus, or error
     */
    suspend operator fun invoke(
        transactionId: Long,
        amount: BigDecimal,
        direction: PaymentDirection,
        method: PaymentMethod,
        dateTime: LocalDateTime = LocalDateTime.now(),
        notes: String? = null
    ): Result {
        // Validate amount is positive
        if (amount <= BigDecimal.ZERO) {
            return Result.ValidationError("Payment amount must be positive")
        }

        // Get the parent transaction
        val transaction = transactionRepository.getById(transactionId)
            ?: return Result.TransactionNotFound(transactionId)

        // Cannot add payments to cancelled transactions
        if (transaction.status == TransactionStatus.CANCELLED) {
            return Result.ValidationError("Cannot add payment to cancelled transaction")
        }

        // Store old transaction state for audit
        val oldTransactionJson = transactionToJson(transaction)

        // Create and insert the partial payment
        val payment = PartialPayment(
            id = 0,
            parentTransactionId = transactionId,
            amount = amount,
            direction = direction,
            dateTime = dateTime,
            method = method,
            notes = notes
        )
        val paymentId = partialPaymentRepository.insert(payment)

        // Log PARTIAL_PAYMENT action
        auditLogRepository.logAction(
            action = AuditAction.PARTIAL_PAYMENT,
            entityType = AuditEntityType.PARTIAL_PAYMENT,
            entityId = paymentId,
            oldValue = null,
            newValue = paymentToJson(payment.copy(id = paymentId)),
            details = "Added partial payment of ${amount} to transaction $transactionId"
        )

        // Calculate new remaining due
        val totalPaid = partialPaymentRepository.getTotalPaidForTransaction(transactionId)
        val newRemainingDue = transaction.amount - totalPaid

        // Determine new status based on remaining due
        val newStatus = calculateStatus(newRemainingDue, transaction.amount)

        // Update the transaction with new remainingDue and status
        val updatedTransaction = transaction.copy(
            remainingDue = newRemainingDue,
            status = newStatus
        )
        transactionRepository.update(updatedTransaction)

        // Log UPDATE action for transaction
        auditLogRepository.logAction(
            action = AuditAction.UPDATE,
            entityType = AuditEntityType.TRANSACTION,
            entityId = transactionId,
            oldValue = oldTransactionJson,
            newValue = transactionToJson(updatedTransaction),
            details = "Updated transaction after partial payment: remainingDue=${newRemainingDue}, status=${newStatus}"
        )

        // Property 16: Reminder Auto-Clear
        // When a transaction becomes SETTLED, automatically clear any pending reminders
        // Requirements: 8.5
        if (newStatus == TransactionStatus.SETTLED) {
            reminderRepository.clearRemindersForTransaction(transactionId)
        }

        // Check for surplus (overpayment)
        return if (newRemainingDue < BigDecimal.ZERO) {
            Result.Surplus(
                paymentId = paymentId,
                surplusAmount = newRemainingDue.abs(),
                newStatus = newStatus
            )
        } else {
            Result.Success(
                paymentId = paymentId,
                newRemainingDue = newRemainingDue,
                newStatus = newStatus
            )
        }
    }


    /**
     * Calculates the transaction status based on remaining due.
     * 
     * Property 3: Transaction Status Consistency
     * - SETTLED when remaining_due equals zero
     * - PARTIALLY_SETTLED when between zero and base_amount
     * - PENDING when equals base_amount
     */
    internal fun calculateStatus(remainingDue: BigDecimal, baseAmount: BigDecimal): TransactionStatus {
        return when {
            remainingDue.compareTo(BigDecimal.ZERO) == 0 -> TransactionStatus.SETTLED
            remainingDue.compareTo(BigDecimal.ZERO) < 0 -> TransactionStatus.SETTLED // Overpaid
            remainingDue.compareTo(baseAmount) == 0 -> TransactionStatus.PENDING
            else -> TransactionStatus.PARTIALLY_SETTLED
        }
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
        return "{${parts.joinToString(",")}}"
    }

    private fun paymentToJson(payment: PartialPayment): String {
        // Build JSON manually to avoid Android JSONObject issues in unit tests
        val parts = mutableListOf<String>()
        parts.add("\"id\":${payment.id}")
        parts.add("\"parentTransactionId\":${payment.parentTransactionId}")
        parts.add("\"amount\":\"${payment.amount.toPlainString()}\"")
        parts.add("\"direction\":\"${payment.direction.name}\"")
        parts.add("\"method\":\"${payment.method.name}\"")
        parts.add("\"notes\":${payment.notes?.let { "\"$it\"" } ?: "null"}")
        return "{${parts.joinToString(",")}}"
    }

    companion object {
        /**
         * Calculates remaining due from base amount and total payments.
         * 
         * Property 2: Remaining Due Calculation
         * remaining_due = base_amount - sum(all partial payments)
         */
        fun calculateRemainingDue(baseAmount: BigDecimal, totalPayments: BigDecimal): BigDecimal {
            return baseAmount - totalPayments
        }

        /**
         * Detects if there is a surplus (overpayment).
         * 
         * Property 6: Partial Payment Surplus Detection
         * Surplus exists when sum of partial payments exceeds base amount.
         */
        fun detectSurplus(remainingDue: BigDecimal): Boolean {
            return remainingDue < BigDecimal.ZERO
        }
    }
}

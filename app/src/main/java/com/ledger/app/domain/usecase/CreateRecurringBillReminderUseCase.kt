package com.ledger.app.domain.usecase

import com.ledger.app.domain.model.BillCategory
import com.ledger.app.domain.model.Reminder
import com.ledger.app.domain.model.ReminderStatus
import com.ledger.app.domain.model.ReminderTargetType
import com.ledger.app.domain.model.RepeatPattern
import com.ledger.app.domain.model.Transaction
import com.ledger.app.domain.model.TransactionType
import com.ledger.app.domain.repository.ReminderRepository
import com.ledger.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Use case for creating recurring bill reminders based on previous payment dates.
 * 
 * Analyzes past bill payments to predict when the next payment is due
 * and creates a reminder for it.
 * 
 * Requirements: 8.2
 */
class CreateRecurringBillReminderUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val reminderRepository: ReminderRepository
) {
    /**
     * Result of creating a recurring bill reminder.
     */
    sealed class Result {
        data class Success(val reminderId: Long, val dueDate: LocalDateTime) : Result()
        data class InsufficientData(val message: String) : Result()
        data class Error(val message: String) : Result()
    }

    /**
     * Creates a recurring bill reminder for a specific bill category.
     * 
     * @param billCategory The bill category (Electricity, TV, Mobile, Internet)
     * @param isForSelf Whether the bill is for self or others
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(
        billCategory: BillCategory,
        isForSelf: Boolean = true
    ): Result {
        // Get past bill payments for this category
        val pastPayments = transactionRepository
            .getBillPaymentsByCategoryAndForSelf(billCategory.name, isForSelf)
            .first()
            .filter { it.type == TransactionType.BILL_PAYMENT || it.type == TransactionType.RECHARGE }
            .sortedByDescending { it.transactionDateTime }

        if (pastPayments.size < 2) {
            return Result.InsufficientData(
                "Need at least 2 past payments to predict next due date for ${billCategory.name}"
            )
        }

        // Calculate average interval between payments
        val predictedDueDate = predictNextDueDate(pastPayments)
            ?: return Result.Error("Could not predict next due date")

        // Create the reminder
        val reminder = Reminder(
            id = 0,
            targetType = ReminderTargetType.BILL,
            targetId = null,
            title = "${billCategory.name} Bill Due",
            description = "Recurring ${billCategory.name.lowercase()} bill payment reminder based on your payment history",
            dueDateTime = predictedDueDate,
            repeatPattern = RepeatPattern.MONTHLY,
            status = ReminderStatus.UPCOMING,
            ignoredCount = 0
        )

        val reminderId = reminderRepository.insert(reminder)
        return Result.Success(reminderId, predictedDueDate)
    }

    /**
     * Creates reminders for all supported bill categories based on payment history.
     * 
     * @return Map of bill category to result
     */
    suspend fun createAllBillReminders(): Map<BillCategory, Result> {
        val results = mutableMapOf<BillCategory, Result>()
        
        for (category in listOf(BillCategory.ELECTRICITY, BillCategory.TV, BillCategory.MOBILE, BillCategory.INTERNET)) {
            results[category] = invoke(category, isForSelf = true)
        }
        
        return results
    }

    /**
     * Predicts the next due date based on past payment dates.
     * Uses the average interval between payments.
     */
    internal fun predictNextDueDate(pastPayments: List<Transaction>): LocalDateTime? {
        if (pastPayments.size < 2) return null

        // Calculate intervals between consecutive payments
        val intervals = mutableListOf<Long>()
        for (i in 0 until pastPayments.size - 1) {
            val interval = ChronoUnit.DAYS.between(
                pastPayments[i + 1].transactionDateTime,
                pastPayments[i].transactionDateTime
            )
            if (interval > 0) {
                intervals.add(interval)
            }
        }

        if (intervals.isEmpty()) return null

        // Calculate average interval
        val averageInterval = intervals.average().toLong()
        
        // Predict next due date from the most recent payment
        val mostRecentPayment = pastPayments.first()
        return mostRecentPayment.transactionDateTime.plusDays(averageInterval)
    }

    /**
     * Gets the suggested reminder date for a bill category.
     * Returns null if insufficient data.
     */
    suspend fun getSuggestedReminderDate(
        billCategory: BillCategory,
        isForSelf: Boolean = true
    ): LocalDateTime? {
        val pastPayments = transactionRepository
            .getBillPaymentsByCategoryAndForSelf(billCategory.name, isForSelf)
            .first()
            .filter { it.type == TransactionType.BILL_PAYMENT || it.type == TransactionType.RECHARGE }
            .sortedByDescending { it.transactionDateTime }

        return predictNextDueDate(pastPayments)
    }
}

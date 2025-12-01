package com.ledger.app.domain.usecase

import com.ledger.app.domain.model.BillCategory
import com.ledger.app.domain.model.DashboardSummary
import com.ledger.app.domain.model.TransactionType
import com.ledger.app.domain.repository.CounterpartyRepository
import com.ledger.app.domain.repository.ReminderRepository
import com.ledger.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

/**
 * Use case for getting the dashboard summary.
 * 
 * Retrieves:
 * - Total owed to user (sum of remaining_due for GAVE transactions)
 * - Total user owes (sum of remaining_due for RECEIVED transactions)
 * - Upcoming reminders count
 * - Recent transactions
 * - Top debtors (people who owe the most)
 * - Monthly bill summary by category
 * 
 * Requirements: 9.1, 9.2, 9.3
 */
class GetDashboardSummaryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val counterpartyRepository: CounterpartyRepository,
    private val reminderRepository: ReminderRepository
) {
    /**
     * Gets the dashboard summary data.
     * 
     * @param recentTransactionsLimit Maximum number of recent transactions to include
     * @param topDebtorsLimit Maximum number of top debtors to include
     * @return DashboardSummary containing all dashboard data
     */
    suspend operator fun invoke(
        recentTransactionsLimit: Int = 10,
        topDebtorsLimit: Int = 5
    ): DashboardSummary {
        val totalOwedToUser = transactionRepository.getTotalOwedToUser()
        val totalUserOwes = transactionRepository.getTotalUserOwes()
        val upcomingRemindersCount = reminderRepository.getUpcomingCount()
        val recentTransactions = transactionRepository
            .getRecentTransactions(recentTransactionsLimit)
            .first()

        // Get top debtors (people who owe the most, positive balance)
        val allWithBalances = counterpartyRepository.getAllWithBalances().first()
        val topDebtors = allWithBalances
            .filter { it.netBalance > BigDecimal.ZERO }
            .sortedByDescending { it.netBalance }
            .take(topDebtorsLimit)

        // Get this month's bill payments by category
        val startOfMonth = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN)
        val endOfMonth = LocalDateTime.now()
        val monthlyTransactions = transactionRepository
            .getByDateRange(startOfMonth, endOfMonth)
            .first()
        
        val monthlyBillSummary = monthlyTransactions
            .filter { it.type == TransactionType.BILL_PAYMENT || it.type == TransactionType.RECHARGE }
            .groupBy { it.billCategory ?: BillCategory.OTHER }
            .mapValues { (_, transactions) -> 
                transactions.sumOf { it.amount }
            }

        return DashboardSummary(
            totalOwedToUser = totalOwedToUser,
            totalUserOwes = totalUserOwes,
            upcomingRemindersCount = upcomingRemindersCount,
            recentTransactions = recentTransactions,
            topDebtors = topDebtors,
            monthlyBillSummary = monthlyBillSummary
        )
    }
}

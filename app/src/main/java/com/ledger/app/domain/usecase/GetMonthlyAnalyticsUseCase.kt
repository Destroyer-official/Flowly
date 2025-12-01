package com.ledger.app.domain.usecase

import com.ledger.app.domain.model.Category
import com.ledger.app.domain.model.Counterparty
import com.ledger.app.domain.model.MonthlySummary
import com.ledger.app.domain.model.Transaction
import com.ledger.app.domain.model.TransactionDirection
import com.ledger.app.domain.model.TransactionStatus
import com.ledger.app.domain.repository.CategoryRepository
import com.ledger.app.domain.repository.CounterpartyRepository
import com.ledger.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

/**
 * Use case for getting monthly analytics data.
 * 
 * Retrieves:
 * - Monthly summary (outflow, inflow, net)
 * - Category breakdown
 * - Top debtors and creditors
 * - Unrecovered amount for last 12 months
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4
 */
class GetMonthlyAnalyticsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val counterpartyRepository: CounterpartyRepository
) {
    /**
     * Gets analytics data for a specific month.
     * 
     * @param year The year
     * @param month The month (1-12)
     * @return AnalyticsData containing all analytics information
     */
    suspend operator fun invoke(year: Int, month: Int): AnalyticsData {
        // Get date range for the month
        val startDate = LocalDate.of(year, month, 1)
        val endDate = startDate.plusMonths(1)
        val startDateTime = LocalDateTime.of(startDate, LocalTime.MIN)
        val endDateTime = LocalDateTime.of(endDate, LocalTime.MIN)

        // Get transactions for the month
        val transactions = transactionRepository
            .getByDateRange(startDateTime, endDateTime)
            .first()
            .filter { it.status != TransactionStatus.CANCELLED }

        // Calculate monthly summary
        val monthlySummary = calculateMonthlySummary(year, month, transactions)

        // Get category breakdown
        val categories = categoryRepository.getAll().first()
        val categoryBreakdown = calculateCategoryBreakdown(transactions, categories)

        // Get top debtors and creditors (uses ALL outstanding transactions for accurate totals)
        val counterparties = counterpartyRepository.getAll().first()
        val topDebtors = calculateTopDebtors(counterparties)
        val topCreditors = calculateTopCreditors(counterparties)

        // Calculate unrecovered amount for last 12 months
        val unrecoveredAmount = calculateUnrecoveredAmount()

        return AnalyticsData(
            monthlySummary = monthlySummary,
            categoryBreakdown = categoryBreakdown,
            topDebtors = topDebtors,
            topCreditors = topCreditors,
            unrecoveredAmount = unrecoveredAmount
        )
    }

    private fun calculateMonthlySummary(
        year: Int,
        month: Int,
        transactions: List<Transaction>
    ): MonthlySummary {
        val totalOutflow = transactions
            .filter { it.direction == TransactionDirection.GAVE }
            .sumOf { it.amount }

        val totalInflow = transactions
            .filter { it.direction == TransactionDirection.RECEIVED }
            .sumOf { it.amount }

        val netBalance = totalInflow - totalOutflow

        return MonthlySummary(
            year = year,
            month = month,
            totalOutflow = totalOutflow,
            totalInflow = totalInflow,
            netBalance = netBalance
        )
    }

    private fun calculateCategoryBreakdown(
        transactions: List<Transaction>,
        categories: List<Category>
    ): List<CategoryBreakdown> {
        val categoryMap = categories.associateBy { it.id }
        
        return transactions
            .groupBy { it.categoryId }
            .mapNotNull { (categoryId, txns) ->
                val category = categoryMap[categoryId] ?: return@mapNotNull null
                val total = txns.sumOf { it.amount }
                CategoryBreakdown(
                    category = category,
                    total = total
                )
            }
            .sortedByDescending { it.total }
    }

    /**
     * Calculates top debtors - people who owe the user money.
     * Uses ALL outstanding transactions (not just from selected month) for accurate totals.
     */
    private suspend fun calculateTopDebtors(
        counterparties: List<Counterparty>
    ): List<CounterpartyAmount> {
        // Get ALL outstanding transactions to calculate accurate balances
        val allTransactions = transactionRepository.getOutstandingObligations().first()
        val counterpartyMap = counterparties.associateBy { it.id }
        
        return allTransactions
            .filter { it.direction == TransactionDirection.GAVE && it.counterpartyId != null }
            .groupBy { it.counterpartyId!! }
            .mapNotNull { (counterpartyId, txns) ->
                val counterparty = counterpartyMap[counterpartyId] ?: return@mapNotNull null
                val total = txns.sumOf { it.remainingDue }
                if (total > BigDecimal.ZERO) {
                    CounterpartyAmount(
                        counterparty = counterparty,
                        amount = total
                    )
                } else null
            }
            .sortedByDescending { it.amount }
            .take(5)
    }

    /**
     * Calculates top creditors - people the user owes money to.
     * Uses ALL outstanding transactions (not just from selected month) for accurate totals.
     */
    private suspend fun calculateTopCreditors(
        counterparties: List<Counterparty>
    ): List<CounterpartyAmount> {
        // Get ALL outstanding transactions to calculate accurate balances
        val allTransactions = transactionRepository.getOutstandingObligations().first()
        val counterpartyMap = counterparties.associateBy { it.id }
        
        return allTransactions
            .filter { it.direction == TransactionDirection.RECEIVED && it.counterpartyId != null }
            .groupBy { it.counterpartyId!! }
            .mapNotNull { (counterpartyId, txns) ->
                val counterparty = counterpartyMap[counterpartyId] ?: return@mapNotNull null
                val total = txns.sumOf { it.remainingDue }
                if (total > BigDecimal.ZERO) {
                    CounterpartyAmount(
                        counterparty = counterparty,
                        amount = total
                    )
                } else null
            }
            .sortedByDescending { it.amount }
            .take(5)
    }

    private suspend fun calculateUnrecoveredAmount(): BigDecimal {
        // Get date range for last 12 months
        val endDate = LocalDate.now().plusMonths(1).withDayOfMonth(1)
        val startDate = endDate.minusMonths(12)
        val startDateTime = LocalDateTime.of(startDate, LocalTime.MIN)
        val endDateTime = LocalDateTime.of(endDate, LocalTime.MIN)

        val transactions = transactionRepository
            .getByDateRange(startDateTime, endDateTime)
            .first()
            .filter { it.status != TransactionStatus.CANCELLED && it.status != TransactionStatus.SETTLED }

        return transactions
            .filter { it.direction == TransactionDirection.GAVE }
            .sumOf { it.remainingDue }
    }
}

/**
 * Data class containing all analytics information.
 */
data class AnalyticsData(
    val monthlySummary: MonthlySummary,
    val categoryBreakdown: List<CategoryBreakdown>,
    val topDebtors: List<CounterpartyAmount>,
    val topCreditors: List<CounterpartyAmount>,
    val unrecoveredAmount: BigDecimal
)

/**
 * Category breakdown with total amount.
 */
data class CategoryBreakdown(
    val category: Category,
    val total: BigDecimal
)

/**
 * Counterparty with amount owed/owing.
 */
data class CounterpartyAmount(
    val counterparty: Counterparty,
    val amount: BigDecimal
)

package com.ledger.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.app.domain.model.BillCategory
import com.ledger.app.domain.model.CounterpartyWithBalance
import com.ledger.app.domain.model.DashboardSummary
import com.ledger.app.domain.model.Transaction
import com.ledger.app.domain.model.TransactionDirection
import com.ledger.app.domain.model.TransactionStatus
import com.ledger.app.domain.model.TransactionType
import com.ledger.app.domain.repository.CounterpartyRepository
import com.ledger.app.domain.repository.TransactionRepository
import com.ledger.app.domain.usecase.GetDashboardSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

/**
 * ViewModel for the Home/Dashboard screen.
 * 
 * Responsibilities:
 * - Expose dashboard summary (total owed to user, total user owes, reminders count)
 * - Expose recent transactions with filtering
 * - Handle filter selection (Today, This Week, This Month)
 * - Expose top debtors (people who owe the most)
 * - Expose monthly bill summary by category
 * 
 * Requirements: 9.1, 9.2, 9.3
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase,
    private val transactionRepository: TransactionRepository,
    private val counterpartyRepository: CounterpartyRepository
) : ViewModel() {

    private val _dashboardSummary = MutableStateFlow(
        DashboardSummary(
            totalOwedToUser = BigDecimal.ZERO,
            totalUserOwes = BigDecimal.ZERO,
            upcomingRemindersCount = 0,
            recentTransactions = emptyList(),
            topDebtors = emptyList(),
            monthlyBillSummary = emptyMap()
        )
    )
    val dashboardSummary: StateFlow<DashboardSummary> = _dashboardSummary.asStateFlow()
    
    // Expose top debtors separately for easier access
    private val _topDebtors = MutableStateFlow<List<CounterpartyWithBalance>>(emptyList())
    val topDebtors: StateFlow<List<CounterpartyWithBalance>> = _topDebtors.asStateFlow()
    
    // Expose monthly bill summary separately for easier access
    private val _monthlyBillSummary = MutableStateFlow<Map<BillCategory, BigDecimal>>(emptyMap())
    val monthlyBillSummary: StateFlow<Map<BillCategory, BigDecimal>> = _monthlyBillSummary.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TimeFilter.TODAY)
    val selectedFilter: StateFlow<TimeFilter> = _selectedFilter.asStateFlow()

    private val _recentTransactions = MutableStateFlow<List<TransactionWithCounterparty>>(emptyList())
    val recentTransactions: StateFlow<List<TransactionWithCounterparty>> = _recentTransactions.asStateFlow()
    
    // Cache for counterparty names
    private val counterpartyNameCache = mutableMapOf<Long, String>()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadDashboardData()
        observeTransactionsRealTime()
        observeFilteredTransactions()
    }

    /**
     * Loads the dashboard summary data.
     */
    private fun loadDashboardData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val summary = getDashboardSummaryUseCase(
                    recentTransactionsLimit = 50,
                    topDebtorsLimit = 5
                )
                _dashboardSummary.value = summary
                _topDebtors.value = summary.topDebtors
                _monthlyBillSummary.value = summary.monthlyBillSummary
            } catch (e: Exception) {
                // Handle error - in production, emit error state
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Observes transactions in real-time from the database.
     * This ensures the UI updates immediately when transactions change.
     */
    private fun observeTransactionsRealTime() {
        viewModelScope.launch {
            transactionRepository.getRecentTransactions(50)
                .catch { e ->
                    e.printStackTrace()
                }
                .collect { transactions ->
                    // Update dashboard summary with fresh data
                    val totalOwed = transactionRepository.getTotalOwedToUser()
                    val totalOwes = transactionRepository.getTotalUserOwes()
                    _dashboardSummary.value = _dashboardSummary.value.copy(
                        totalOwedToUser = totalOwed,
                        totalUserOwes = totalOwes,
                        recentTransactions = transactions
                    )
                }
        }
    }

    /**
     * Observes transactions and applies the selected filter.
     */
    private fun observeFilteredTransactions() {
        viewModelScope.launch {
            combine(
                _selectedFilter,
                _dashboardSummary
            ) { filter, summary ->
                filterTransactions(summary.recentTransactions, filter)
            }
                .catch { e ->
                    e.printStackTrace()
                    emit(emptyList())
                }
                .collect { filtered ->
                    // Enrich transactions with counterparty names
                    val enriched = filtered.map { transaction ->
                        val counterpartyName = transaction.counterpartyId?.let { id ->
                            counterpartyNameCache.getOrPut(id) {
                                counterpartyRepository.getById(id)?.displayName ?: "Unknown"
                            }
                        }
                        TransactionWithCounterparty(transaction, counterpartyName)
                    }
                    _recentTransactions.value = enriched
                }
        }
    }

    /**
     * Filters transactions based on the selected time filter.
     */
    private fun filterTransactions(
        transactions: List<Transaction>,
        filter: TimeFilter
    ): List<Transaction> {
        val now = LocalDateTime.now()
        val filterStart = when (filter) {
            TimeFilter.TODAY -> LocalDateTime.of(LocalDate.now(), LocalTime.MIN)
            TimeFilter.THIS_WEEK -> {
                val today = LocalDate.now()
                val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
                LocalDateTime.of(startOfWeek, LocalTime.MIN)
            }
            TimeFilter.THIS_MONTH -> {
                val today = LocalDate.now()
                val startOfMonth = today.withDayOfMonth(1)
                LocalDateTime.of(startOfMonth, LocalTime.MIN)
            }
        }

        return transactions
            .filter { it.transactionDateTime >= filterStart && it.transactionDateTime <= now }
            .sortedByDescending { it.transactionDateTime }
    }

    /**
     * Updates the selected time filter.
     */
    fun selectFilter(filter: TimeFilter) {
        _selectedFilter.value = filter
    }

    /**
     * Refreshes the dashboard data.
     */
    fun refresh() {
        loadDashboardData()
    }

    /**
     * Undoes a transaction by soft-deleting it.
     * 
     * Requirements: 1.4
     */
    fun undoTransaction(transactionId: Long) {
        viewModelScope.launch {
            try {
                transactionRepository.softDelete(transactionId)
                refresh()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

/**
 * Time filter options for the home screen.
 */
enum class TimeFilter {
    TODAY,
    THIS_WEEK,
    THIS_MONTH
}

/**
 * Transaction with counterparty name for display.
 */
data class TransactionWithCounterparty(
    val transaction: Transaction,
    val counterpartyName: String?
) {
    val id: Long get() = transaction.id
    val direction: TransactionDirection get() = transaction.direction
    val type: TransactionType get() = transaction.type
    val amount: BigDecimal get() = transaction.amount
    val transactionDateTime: LocalDateTime get() = transaction.transactionDateTime
    val status: TransactionStatus get() = transaction.status
    val remainingDue: BigDecimal get() = transaction.remainingDue
}

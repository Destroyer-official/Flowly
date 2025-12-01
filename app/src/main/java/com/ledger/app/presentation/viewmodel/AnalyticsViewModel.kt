package com.ledger.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.app.domain.repository.TransactionRepository
import com.ledger.app.domain.usecase.AnalyticsData
import com.ledger.app.domain.usecase.GetMonthlyAnalyticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel for the Analytics screen.
 * 
 * Responsibilities:
 * - Load monthly summary for selected month
 * - Load category breakdown
 * - Load top debtors and creditors
 * - Calculate unrecovered amount
 * - Auto-refresh when transactions change
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getMonthlyAnalyticsUseCase: GetMonthlyAnalyticsUseCase,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _selectedYear = MutableStateFlow(LocalDate.now().year)
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(LocalDate.now().monthValue)
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _analyticsData = MutableStateFlow<AnalyticsData?>(null)
    val analyticsData: StateFlow<AnalyticsData?> = _analyticsData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadAnalytics()
        observeTransactionChanges()
    }

    /**
     * Observes transaction changes and auto-refreshes analytics.
     * Uses debounce to avoid excessive refreshes.
     */
    @OptIn(FlowPreview::class)
    private fun observeTransactionChanges() {
        viewModelScope.launch {
            transactionRepository.getAll()
                .debounce(300) // Wait 300ms after last change before refreshing
                .catch { e -> 
                    e.printStackTrace()
                }
                .collectLatest {
                    // Refresh analytics when transactions change
                    loadAnalyticsInternal()
                }
        }
    }

    /**
     * Loads analytics data for the currently selected month.
     */
    fun loadAnalytics() {
        viewModelScope.launch {
            _isLoading.value = true
            loadAnalyticsInternal()
            _isLoading.value = false
        }
    }

    /**
     * Internal method to load analytics without showing loading indicator.
     */
    private suspend fun loadAnalyticsInternal() {
        _error.value = null
        try {
            val data = getMonthlyAnalyticsUseCase(
                year = _selectedYear.value,
                month = _selectedMonth.value
            )
            _analyticsData.value = data
        } catch (e: Exception) {
            _error.value = "Failed to load analytics: ${e.message}"
            e.printStackTrace()
        }
    }

    /**
     * Selects a different month and reloads analytics.
     * 
     * @param year The year to select
     * @param month The month to select (1-12)
     */
    fun selectMonth(year: Int, month: Int) {
        _selectedYear.value = year
        _selectedMonth.value = month
        loadAnalytics()
    }

    /**
     * Moves to the previous month.
     */
    fun previousMonth() {
        val currentDate = LocalDate.of(_selectedYear.value, _selectedMonth.value, 1)
        val previousDate = currentDate.minusMonths(1)
        selectMonth(previousDate.year, previousDate.monthValue)
    }

    /**
     * Moves to the next month.
     */
    fun nextMonth() {
        val currentDate = LocalDate.of(_selectedYear.value, _selectedMonth.value, 1)
        val nextDate = currentDate.plusMonths(1)
        selectMonth(nextDate.year, nextDate.monthValue)
    }

    /**
     * Clears any error message.
     */
    fun clearError() {
        _error.value = null
    }
}

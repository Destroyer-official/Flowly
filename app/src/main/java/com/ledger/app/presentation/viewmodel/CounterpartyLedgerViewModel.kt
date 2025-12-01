package com.ledger.app.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.app.domain.model.Counterparty
import com.ledger.app.domain.model.Transaction
import com.ledger.app.domain.model.TransactionStatus
import com.ledger.app.domain.model.TransactionType
import com.ledger.app.domain.usecase.ExportLedgerUseCase
import com.ledger.app.domain.usecase.GetCounterpartyLedgerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/**
 * ViewModel for the Counterparty Ledger screen.
 * 
 * Responsibilities:
 * - Load counterparty details and net balance
 * - Load transactions with filters
 * - Handle filter selection (All, Loans, Bills, Outstanding)
 * 
 * Requirements: 4.1, 4.2, 4.3
 */
@HiltViewModel
class CounterpartyLedgerViewModel @Inject constructor(
    private val getCounterpartyLedgerUseCase: GetCounterpartyLedgerUseCase,
    private val exportLedgerUseCase: ExportLedgerUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val counterpartyId: Long = savedStateHandle.get<Long>("counterpartyId") ?: 0L

    private val _counterparty = MutableStateFlow<Counterparty?>(null)
    val counterparty: StateFlow<Counterparty?> = _counterparty.asStateFlow()

    private val _netBalance = MutableStateFlow(BigDecimal.ZERO)
    val netBalance: StateFlow<BigDecimal> = _netBalance.asStateFlow()

    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    
    private val _filteredTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val filteredTransactions: StateFlow<List<Transaction>> = _filteredTransactions.asStateFlow()

    private val _selectedFilter = MutableStateFlow(FilterType.ALL)
    val selectedFilter: StateFlow<FilterType> = _selectedFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _exportResult = MutableStateFlow<ExportLedgerUseCase.ExportResult?>(null)
    val exportResult: StateFlow<ExportLedgerUseCase.ExportResult?> = _exportResult.asStateFlow()

    init {
        loadCounterpartyLedger()
    }

    /**
     * Loads the counterparty ledger data.
     * 
     * Requirements: 4.1, 4.2
     */
    private fun loadCounterpartyLedger() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                getCounterpartyLedgerUseCase.observeLedger(counterpartyId)
                    .catch { e ->
                        e.printStackTrace()
                        emit(null)
                    }
                    .collect { ledger ->
                        if (ledger != null) {
                            _counterparty.value = ledger.counterparty
                            _netBalance.value = ledger.netBalance
                            _allTransactions.value = ledger.transactions
                            applyFilter(_selectedFilter.value)
                        }
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }

    /**
     * Sets the filter type and applies it to the transactions.
     * 
     * Requirements: 4.3
     */
    fun setFilter(filterType: FilterType) {
        _selectedFilter.value = filterType
        applyFilter(filterType)
    }

    /**
     * Applies the selected filter to the transactions.
     * 
     * Filter types (Requirements 4.3):
     * - ALL: Show all transactions
     * - LOANS: Show only LOAN type transactions
     * - BILLS: Show only BILL_PAYMENT and RECHARGE type transactions
     * - OUTSTANDING: Show only transactions with status PENDING or PARTIALLY_SETTLED
     */
    private fun applyFilter(filterType: FilterType) {
        val filtered = when (filterType) {
            FilterType.ALL -> _allTransactions.value
            FilterType.LOANS -> _allTransactions.value.filter { it.type == TransactionType.LOAN }
            FilterType.BILLS -> _allTransactions.value.filter { 
                it.type == TransactionType.BILL_PAYMENT || it.type == TransactionType.RECHARGE 
            }
            FilterType.OUTSTANDING -> _allTransactions.value.filter { 
                it.status == TransactionStatus.PENDING || it.status == TransactionStatus.PARTIALLY_SETTLED 
            }
        }
        _filteredTransactions.value = filtered
    }

    /**
     * Exports the counterparty ledger to shareable text format.
     * 
     * Requirements: 4.3
     */
    fun exportLedger() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = exportLedgerUseCase.exportCounterpartyLedger(counterpartyId)
                _exportResult.value = result
            } catch (e: Exception) {
                _exportResult.value = ExportLedgerUseCase.ExportResult.Error(
                    e.message ?: "Failed to export ledger"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clears the export result after it has been consumed.
     */
    fun clearExportResult() {
        _exportResult.value = null
    }

    /**
     * Filter types for the counterparty ledger.
     */
    enum class FilterType {
        ALL,
        LOANS,
        BILLS,
        OUTSTANDING
    }
}

package com.ledger.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.app.domain.model.BillCategory
import com.ledger.app.domain.model.Transaction
import com.ledger.app.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Bill History screen.
 * 
 * Responsibilities:
 * - Load all bill payments and recharges
 * - Filter by category (Electricity, TV, Mobile, Internet)
 * - Filter by for self vs for others
 * 
 * Requirements: 3.5
 */
@HiltViewModel
class BillHistoryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    // All bill transactions
    private val _allBillTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    
    // Filtered transactions
    private val _filteredTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val filteredTransactions: StateFlow<List<Transaction>> = _filteredTransactions.asStateFlow()

    // Selected category filter (null = all)
    private val _selectedCategory = MutableStateFlow<BillCategory?>(null)
    val selectedCategory: StateFlow<BillCategory?> = _selectedCategory.asStateFlow()

    // For self filter (null = all, true = self only, false = others only)
    private val _forSelfFilter = MutableStateFlow<Boolean?>(null)
    val forSelfFilter: StateFlow<Boolean?> = _forSelfFilter.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadBillTransactions()
    }

    /**
     * Loads all bill payments and recharges.
     */
    private fun loadBillTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            transactionRepository.getAllBillPaymentsAndRecharges().collectLatest { transactions ->
                _allBillTransactions.value = transactions
                applyFilters()
                _isLoading.value = false
            }
        }
    }

    /**
     * Sets the category filter.
     */
    fun setCategory(category: BillCategory?) {
        _selectedCategory.value = category
        applyFilters()
    }

    /**
     * Sets the for self filter.
     */
    fun setForSelfFilter(forSelf: Boolean?) {
        _forSelfFilter.value = forSelf
        applyFilters()
    }

    /**
     * Applies all active filters to the transaction list.
     */
    private fun applyFilters() {
        var filtered = _allBillTransactions.value

        // Filter by category
        _selectedCategory.value?.let { category ->
            filtered = filtered.filter { it.billCategory == category }
        }

        // Filter by for self
        _forSelfFilter.value?.let { forSelf ->
            filtered = filtered.filter { it.isForSelf == forSelf }
        }

        _filteredTransactions.value = filtered
    }

    /**
     * Clears all filters.
     */
    fun clearFilters() {
        _selectedCategory.value = null
        _forSelfFilter.value = null
        applyFilters()
    }
}

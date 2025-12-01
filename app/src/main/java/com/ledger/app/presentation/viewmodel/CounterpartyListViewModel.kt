package com.ledger.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.app.domain.model.CounterpartyWithBalance
import com.ledger.app.domain.repository.CounterpartyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Counterparty List screen.
 * 
 * Responsibilities:
 * - Load all counterparties with balances
 * - Handle search functionality
 * - Sort favorites first
 * 
 * Requirements: 11.2, 11.4
 */
@HiltViewModel
class CounterpartyListViewModel @Inject constructor(
    private val counterpartyRepository: CounterpartyRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _counterparties = MutableStateFlow<List<CounterpartyWithBalance>>(emptyList())
    val counterparties: StateFlow<List<CounterpartyWithBalance>> = _counterparties.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeCounterparties()
    }

    /**
     * Observes counterparties and applies search filtering.
     * Favorites are shown first, followed by alphabetical order.
     * 
     * Requirements: 11.2, 11.4
     */
    private fun observeCounterparties() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                combine(
                    counterpartyRepository.getAllWithBalances(),
                    _searchQuery
                ) { allCounterparties, query ->
                    filterAndSortCounterparties(allCounterparties, query)
                }
                    .catch { e ->
                        e.printStackTrace()
                        emit(emptyList())
                    }
                    .collect { filtered ->
                        _counterparties.value = filtered
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }

    /**
     * Filters counterparties by search query and sorts them.
     * 
     * Filtering logic (Requirements 11.2):
     * - Matches display name or phone number (case-insensitive)
     * 
     * Sorting logic (Requirements 11.4):
     * - Favorites shown first
     * - Then alphabetically by display name
     */
    private fun filterAndSortCounterparties(
        counterparties: List<CounterpartyWithBalance>,
        query: String
    ): List<CounterpartyWithBalance> {
        val filtered = if (query.isBlank()) {
            counterparties
        } else {
            val lowerQuery = query.lowercase()
            counterparties.filter { cwb ->
                cwb.counterparty.displayName.lowercase().contains(lowerQuery) ||
                    cwb.counterparty.phoneNumber?.lowercase()?.contains(lowerQuery) == true
            }
        }

        return filtered.sortedWith(
            compareByDescending<CounterpartyWithBalance> { it.counterparty.isFavorite }
                .thenBy { it.counterparty.displayName }
        )
    }

    /**
     * Updates the search query.
     * 
     * Requirements: 11.2
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Clears the search query.
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }
}

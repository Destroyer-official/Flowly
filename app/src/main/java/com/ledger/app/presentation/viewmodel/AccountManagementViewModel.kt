package com.ledger.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.app.domain.model.Account
import com.ledger.app.domain.model.AccountType
import com.ledger.app.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Account Management screen.
 * Manages account creation, updates, and deactivation.
 * 
 * Requirements: 12.1, 12.2, 12.4
 */
@HiltViewModel
class AccountManagementViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    val activeAccounts: StateFlow<List<Account>> = accountRepository.getActive()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun addAccount(name: String, type: AccountType) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                if (name.isBlank()) {
                    _error.value = "Account name cannot be empty"
                    return@launch
                }
                
                val account = Account(
                    name = name.trim(),
                    type = type,
                    isActive = true
                )
                
                accountRepository.insert(account)
            } catch (e: Exception) {
                _error.value = "Failed to add account: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deactivateAccount(accountId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                accountRepository.deactivate(accountId)
            } catch (e: Exception) {
                _error.value = "Failed to deactivate account: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

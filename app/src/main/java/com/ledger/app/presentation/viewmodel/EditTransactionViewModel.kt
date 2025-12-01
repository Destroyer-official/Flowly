package com.ledger.app.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.app.domain.model.Account
import com.ledger.app.domain.model.Category
import com.ledger.app.domain.model.Counterparty
import com.ledger.app.domain.model.Transaction
import com.ledger.app.domain.model.TransactionDirection
import com.ledger.app.domain.repository.AccountRepository
import com.ledger.app.domain.repository.CategoryRepository
import com.ledger.app.domain.repository.CounterpartyRepository
import com.ledger.app.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for editing existing transactions.
 * Loads transaction data and allows modification.
 */
@HiltViewModel
class EditTransactionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val counterpartyRepository: CounterpartyRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val transactionId: Long = savedStateHandle.get<Long>("transactionId") ?: 0L

    // Form state
    private val _amountString = MutableStateFlow("0")
    val amountString: StateFlow<String> = _amountString.asStateFlow()

    private val _direction = MutableStateFlow(TransactionDirection.GAVE)
    val direction: StateFlow<TransactionDirection> = _direction.asStateFlow()

    private val _selectedCounterparty = MutableStateFlow<Counterparty?>(null)
    val selectedCounterparty: StateFlow<Counterparty?> = _selectedCounterparty.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _selectedAccount = MutableStateFlow<Account?>(null)
    val selectedAccount: StateFlow<Account?> = _selectedAccount.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private val _transactionDateTime = MutableStateFlow(LocalDateTime.now())
    val transactionDateTime: StateFlow<LocalDateTime> = _transactionDateTime.asStateFlow()

    // Available options
    private val _counterparties = MutableStateFlow<List<Counterparty>>(emptyList())
    val counterparties: StateFlow<List<Counterparty>> = _counterparties.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    // UI state
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveResult = MutableStateFlow<EditResult?>(null)
    val saveResult: StateFlow<EditResult?> = _saveResult.asStateFlow()

    private val _originalTransaction = MutableStateFlow<Transaction?>(null)

    init {
        loadData()
        loadTransaction()
    }

    private fun loadData() {
        viewModelScope.launch {
            counterpartyRepository.getAll().collect { list ->
                _counterparties.value = list
            }
        }
        viewModelScope.launch {
            categoryRepository.getAll().collect { list ->
                _categories.value = list
            }
        }
        viewModelScope.launch {
            accountRepository.getActive().collect { list ->
                _accounts.value = list
            }
        }
    }

    private fun loadTransaction() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val transaction = transactionRepository.getById(transactionId)
                if (transaction != null) {
                    _originalTransaction.value = transaction
                    _amountString.value = transaction.amount.toPlainString()
                    _direction.value = transaction.direction
                    _note.value = transaction.notes ?: ""
                    _transactionDateTime.value = transaction.transactionDateTime

                    // Load related entities
                    transaction.counterpartyId?.let { id ->
                        _selectedCounterparty.value = counterpartyRepository.getById(id)
                    }
                    
                    val categories = categoryRepository.getAll().first()
                    _selectedCategory.value = categories.find { it.id == transaction.categoryId }
                    
                    val accounts = accountRepository.getActive().first()
                    _selectedAccount.value = accounts.find { it.id == transaction.accountId }
                }
            } catch (e: Exception) {
                _saveResult.value = EditResult.Error("Failed to load transaction: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onDigitInput(digit: String) {
        val current = _amountString.value
        _amountString.value = if (current == "0") digit else current + digit
    }

    fun onDecimalInput() {
        val current = _amountString.value
        if (!current.contains(".")) {
            _amountString.value = current + "."
        }
    }

    fun onBackspace() {
        val current = _amountString.value
        _amountString.value = if (current.length > 1) current.dropLast(1) else "0"
    }

    fun setDirection(direction: TransactionDirection) {
        _direction.value = direction
    }

    fun selectCounterparty(counterparty: Counterparty?) {
        _selectedCounterparty.value = counterparty
    }

    fun selectCategory(category: Category) {
        _selectedCategory.value = category
    }

    fun selectAccount(account: Account) {
        _selectedAccount.value = account
    }

    fun setNote(note: String) {
        _note.value = note
    }

    fun setDateTime(dateTime: LocalDateTime) {
        _transactionDateTime.value = dateTime
    }

    fun updateTransaction() {
        viewModelScope.launch {
            _isSaving.value = true
            _saveResult.value = null

            try {
                val amount = try {
                    BigDecimal(_amountString.value)
                } catch (e: Exception) {
                    _saveResult.value = EditResult.Error("Invalid amount")
                    _isSaving.value = false
                    return@launch
                }

                val category = _selectedCategory.value
                if (category == null) {
                    _saveResult.value = EditResult.Error("Please select a category")
                    _isSaving.value = false
                    return@launch
                }

                val account = _selectedAccount.value
                if (account == null) {
                    _saveResult.value = EditResult.Error("Please select an account")
                    _isSaving.value = false
                    return@launch
                }

                val original = _originalTransaction.value
                if (original == null) {
                    _saveResult.value = EditResult.Error("Transaction not found")
                    _isSaving.value = false
                    return@launch
                }

                val updatedTransaction = original.copy(
                    amount = amount,
                    direction = _direction.value,
                    counterpartyId = _selectedCounterparty.value?.id,
                    categoryId = category.id,
                    accountId = account.id,
                    notes = _note.value.ifBlank { null },
                    transactionDateTime = _transactionDateTime.value,
                    remainingDue = amount // Reset remaining due on edit
                )

                transactionRepository.update(updatedTransaction)
                _saveResult.value = EditResult.Success
            } catch (e: Exception) {
                _saveResult.value = EditResult.Error("Failed to update: ${e.message}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }
}

sealed class EditResult {
    data object Success : EditResult()
    data class Error(val message: String) : EditResult()
}

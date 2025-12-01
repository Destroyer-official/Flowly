package com.ledger.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.app.domain.model.Account
import com.ledger.app.domain.model.Category
import com.ledger.app.domain.model.Counterparty
import com.ledger.app.domain.model.TransactionDirection
import com.ledger.app.domain.model.TransactionType
import com.ledger.app.domain.repository.AccountRepository
import com.ledger.app.domain.repository.CategoryRepository
import com.ledger.app.domain.repository.CounterpartyRepository
import com.ledger.app.domain.repository.TransactionRepository
import com.ledger.app.domain.usecase.AddTransactionUseCase
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
 * ViewModel for the Quick Add bottom sheet.
 * 
 * Responsibilities:
 * - Manage form state (amount, direction, counterparty, category, account, note)
 * - Load counterparties, categories, accounts
 * - Suggest recent category/account for selected counterparty
 * - Handle save transaction
 * 
 * Requirements: 1.2, 1.5
 */
@HiltViewModel
class QuickAddViewModel @Inject constructor(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val counterpartyRepository: CounterpartyRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val preferencesManager: com.ledger.app.data.local.PreferencesManager
) : ViewModel() {

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

    private val _linkedTaskId = MutableStateFlow<Long?>(null)
    val linkedTaskId: StateFlow<Long?> = _linkedTaskId.asStateFlow()

    private val _transactionDateTime = MutableStateFlow(LocalDateTime.now())
    val transactionDateTime: StateFlow<LocalDateTime> = _transactionDateTime.asStateFlow()

    // Bill payment specific fields
    private val _isBillPayment = MutableStateFlow(false)
    val isBillPayment: StateFlow<Boolean> = _isBillPayment.asStateFlow()

    private val _selectedBillCategory = MutableStateFlow<com.ledger.app.domain.model.BillCategory?>(null)
    val selectedBillCategory: StateFlow<com.ledger.app.domain.model.BillCategory?> = _selectedBillCategory.asStateFlow()

    private val _consumerId = MutableStateFlow("")
    val consumerId: StateFlow<String> = _consumerId.asStateFlow()

    private val _isForSelf = MutableStateFlow(false)
    val isForSelf: StateFlow<Boolean> = _isForSelf.asStateFlow()

    // Available options
    private val _counterparties = MutableStateFlow<List<Counterparty>>(emptyList())
    val counterparties: StateFlow<List<Counterparty>> = _counterparties.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    // UI state
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveResult = MutableStateFlow<SaveResult?>(null)
    val saveResult: StateFlow<SaveResult?> = _saveResult.asStateFlow()

    init {
        loadData()
    }

    /**
     * Loads counterparties, categories, and accounts.
     */
    private fun loadData() {
        viewModelScope.launch {
            // Load counterparties
            counterpartyRepository.getAll().collect { list ->
                _counterparties.value = list
            }
        }

        viewModelScope.launch {
            // Load categories
            categoryRepository.getAll().collect { list ->
                _categories.value = list
                // Auto-select first category if none selected
                if (_selectedCategory.value == null && list.isNotEmpty()) {
                    _selectedCategory.value = list.first()
                }
            }
        }

        viewModelScope.launch {
            // Load accounts
            accountRepository.getActive().collect { list ->
                _accounts.value = list
                // Auto-select default account or first account if none selected
                if (_selectedAccount.value == null && list.isNotEmpty()) {
                    val defaultAccountId = preferencesManager.getDefaultAccountId()
                    val defaultAccount = defaultAccountId?.let { id ->
                        list.find { it.id == id }
                    }
                    _selectedAccount.value = defaultAccount ?: list.first()
                }
            }
        }
    }

    /**
     * Handles digit input for amount.
     */
    fun onDigitInput(digit: String) {
        val current = _amountString.value
        // Remove leading zero
        val newValue = if (current == "0") {
            digit
        } else {
            current + digit
        }
        _amountString.value = newValue
    }

    /**
     * Handles decimal point input.
     */
    fun onDecimalInput() {
        val current = _amountString.value
        // Only add decimal if not already present
        if (!current.contains(".")) {
            _amountString.value = current + "."
        }
    }

    /**
     * Handles backspace input.
     */
    fun onBackspace() {
        val current = _amountString.value
        _amountString.value = if (current.length > 1) {
            current.dropLast(1)
        } else {
            "0"
        }
    }

    /**
     * Sets the transaction direction.
     */
    fun setDirection(direction: TransactionDirection) {
        _direction.value = direction
    }

    /**
     * Selects a counterparty and suggests recent category/account.
     * 
     * Requirements: 1.5
     */
    fun selectCounterparty(counterparty: Counterparty?) {
        _selectedCounterparty.value = counterparty
        
        // Suggest recent category and account for this counterparty
        if (counterparty != null) {
            viewModelScope.launch {
                suggestRecentCategoryAndAccount(counterparty.id)
            }
        }
    }

    /**
     * Suggests the most recently used category and account for a counterparty.
     */
    private suspend fun suggestRecentCategoryAndAccount(counterpartyId: Long) {
        try {
            val recentTransactions = transactionRepository
                .getByCounterparty(counterpartyId)
                .first()
            
            if (recentTransactions.isNotEmpty()) {
                val mostRecent = recentTransactions.first()
                
                // Suggest category
                val category = _categories.value.find { it.id == mostRecent.categoryId }
                if (category != null) {
                    _selectedCategory.value = category
                }
                
                // Suggest account
                val account = _accounts.value.find { it.id == mostRecent.accountId }
                if (account != null) {
                    _selectedAccount.value = account
                }
            }
        } catch (e: Exception) {
            // If suggestion fails, keep current selections
            e.printStackTrace()
        }
    }

    /**
     * Selects a category.
     */
    fun selectCategory(category: Category) {
        _selectedCategory.value = category
    }

    /**
     * Selects an account.
     */
    fun selectAccount(account: Account) {
        _selectedAccount.value = account
    }

    /**
     * Updates the note field.
     */
    fun setNote(note: String) {
        _note.value = note
    }

    /**
     * Sets the linked task ID (Requirements: 15.5).
     */
    fun setLinkedTaskId(taskId: Long?) {
        _linkedTaskId.value = taskId
    }

    /**
     * Sets the transaction date/time.
     */
    fun setTransactionDateTime(dateTime: LocalDateTime) {
        _transactionDateTime.value = dateTime
    }

    /**
     * Toggles bill payment mode.
     */
    fun setBillPaymentMode(isBill: Boolean) {
        _isBillPayment.value = isBill
        if (isBill) {
            _direction.value = TransactionDirection.GAVE
            // Default to first bill category
            if (_selectedBillCategory.value == null) {
                _selectedBillCategory.value = com.ledger.app.domain.model.BillCategory.ELECTRICITY
            }
        }
    }

    /**
     * Sets the bill category.
     */
    fun setBillCategory(category: com.ledger.app.domain.model.BillCategory) {
        _selectedBillCategory.value = category
    }

    /**
     * Sets the consumer ID (phone number or account number for bills).
     */
    fun setConsumerId(id: String) {
        _consumerId.value = id
    }

    /**
     * Sets whether the bill is for self or for others.
     * When for self, no debt is tracked.
     * When for others, the amount is added to their debt.
     */
    fun setIsForSelf(forSelf: Boolean) {
        _isForSelf.value = forSelf
        // If for self, clear counterparty selection
        if (forSelf) {
            _selectedCounterparty.value = null
        }
    }

    /**
     * Saves the transaction.
     */
    fun saveTransaction() {
        viewModelScope.launch {
            _isSaving.value = true
            _saveResult.value = null

            try {
                // Parse amount
                val amount = try {
                    BigDecimal(_amountString.value)
                } catch (e: Exception) {
                    _saveResult.value = SaveResult.Error("Invalid amount")
                    _isSaving.value = false
                    return@launch
                }

                // Validate required fields
                val category = _selectedCategory.value
                if (category == null) {
                    _saveResult.value = SaveResult.Error("Please select a category")
                    _isSaving.value = false
                    return@launch
                }

                val account = _selectedAccount.value
                if (account == null) {
                    _saveResult.value = SaveResult.Error("Please select an account")
                    _isSaving.value = false
                    return@launch
                }

                // Determine transaction type from category name
                val type = when (category.name.lowercase()) {
                    "loan" -> TransactionType.LOAN
                    "bill payment", "bill" -> TransactionType.BILL_PAYMENT
                    "recharge" -> TransactionType.RECHARGE
                    else -> TransactionType.OTHER
                }

                // Determine transaction type
                val finalType = if (_isBillPayment.value) {
                    TransactionType.BILL_PAYMENT
                } else {
                    type
                }

                // Save transaction
                val result = addTransactionUseCase(
                    direction = _direction.value,
                    type = finalType,
                    amount = amount,
                    accountId = account.id,
                    categoryId = category.id,
                    counterpartyId = _selectedCounterparty.value?.id,
                    transactionDateTime = _transactionDateTime.value,
                    notes = _note.value.ifBlank { null },
                    consumerId = _consumerId.value.ifBlank { null },
                    billCategory = if (_isBillPayment.value) _selectedBillCategory.value else null,
                    isForSelf = _isForSelf.value,
                    linkedTaskId = _linkedTaskId.value
                )

                when (result) {
                    is AddTransactionUseCase.Result.Success -> {
                        _saveResult.value = SaveResult.Success(result.transactionId)
                        resetForm()
                    }
                    is AddTransactionUseCase.Result.ValidationError -> {
                        _saveResult.value = SaveResult.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                _saveResult.value = SaveResult.Error("Failed to save transaction: ${e.message}")
                e.printStackTrace()
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Resets the form to initial state.
     */
    fun resetForm() {
        _amountString.value = "0"
        _direction.value = TransactionDirection.GAVE
        _selectedCounterparty.value = null
        _note.value = ""
        _linkedTaskId.value = null
        _transactionDateTime.value = LocalDateTime.now()
        _isBillPayment.value = false
        _selectedBillCategory.value = null
        _consumerId.value = ""
        _isForSelf.value = false
        // Keep category and account selections for convenience
    }

    /**
     * Clears the save result.
     */
    fun clearSaveResult() {
        _saveResult.value = null
    }

    /**
     * Pre-fills the counterparty (used when opening from counterparty ledger).
     */
    fun prefillCounterparty(counterpartyId: Long) {
        viewModelScope.launch {
            val counterparty = counterpartyRepository.getById(counterpartyId)
            if (counterparty != null) {
                selectCounterparty(counterparty)
            }
        }
    }

    /**
     * Creates a new counterparty with the given name and selects it.
     */
    fun createAndSelectCounterparty(name: String) {
        if (name.isBlank()) return
        
        viewModelScope.launch {
            try {
                val newCounterparty = Counterparty(
                    id = 0,
                    displayName = name.trim(),
                    phoneNumber = null,
                    notes = null
                )
                val id = counterpartyRepository.insert(newCounterparty)
                val created = counterpartyRepository.getById(id)
                if (created != null) {
                    _selectedCounterparty.value = created
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

/**
 * Result of saving a transaction.
 */
sealed class SaveResult {
    data class Success(val transactionId: Long) : SaveResult()
    data class Error(val message: String) : SaveResult()
}

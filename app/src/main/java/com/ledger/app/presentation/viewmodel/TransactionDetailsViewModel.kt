package com.ledger.app.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.app.domain.model.Account
import com.ledger.app.domain.model.AuditAction
import com.ledger.app.domain.model.AuditEntityType
import com.ledger.app.domain.model.Category
import com.ledger.app.domain.model.Counterparty
import com.ledger.app.domain.model.PartialPayment
import com.ledger.app.domain.model.PaymentDirection
import com.ledger.app.domain.model.PaymentMethod
import com.ledger.app.domain.model.Transaction
import com.ledger.app.domain.model.TransactionStatus
import com.ledger.app.domain.repository.AccountRepository
import com.ledger.app.domain.repository.AuditLogRepository
import com.ledger.app.domain.repository.CategoryRepository
import com.ledger.app.domain.repository.CounterpartyRepository
import com.ledger.app.domain.repository.PartialPaymentRepository
import com.ledger.app.domain.repository.TransactionRepository
import com.ledger.app.domain.usecase.AddPartialPaymentUseCase
import com.ledger.app.domain.usecase.ExportLedgerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for the Transaction Details screen.
 * 
 * Responsibilities:
 * - Load transaction with partial payments
 * - Calculate remaining due
 * - Handle add partial payment, edit, cancel, duplicate actions
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5
 */
@HiltViewModel
class TransactionDetailsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val partialPaymentRepository: PartialPaymentRepository,
    private val counterpartyRepository: CounterpartyRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val auditLogRepository: AuditLogRepository,
    private val addPartialPaymentUseCase: AddPartialPaymentUseCase,
    private val exportLedgerUseCase: ExportLedgerUseCase,
    private val taskRepository: com.ledger.app.domain.repository.TaskRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: Long = savedStateHandle.get<Long>("transactionId") ?: 0L

    private val _uiState = MutableStateFlow<TransactionDetailsUiState>(TransactionDetailsUiState.Loading)
    val uiState: StateFlow<TransactionDetailsUiState> = _uiState.asStateFlow()

    private val _showAddPaymentDialog = MutableStateFlow(false)
    val showAddPaymentDialog: StateFlow<Boolean> = _showAddPaymentDialog.asStateFlow()

    private val _exportResult = MutableStateFlow<ExportLedgerUseCase.ExportResult?>(null)
    val exportResult: StateFlow<ExportLedgerUseCase.ExportResult?> = _exportResult.asStateFlow()

    init {
        loadTransactionDetails()
    }

    /**
     * Loads the transaction details with all related data.
     * 
     * Requirements: 2.1, 2.2
     */
    private fun loadTransactionDetails() {
        viewModelScope.launch {
            try {
                val transaction = transactionRepository.getById(transactionId)
                if (transaction == null) {
                    _uiState.value = TransactionDetailsUiState.Error("Transaction not found")
                    return@launch
                }

                // Load related data
                partialPaymentRepository.getByTransaction(transactionId)
                    .catch { e ->
                        _uiState.value = TransactionDetailsUiState.Error(e.message ?: "Unknown error")
                    }
                    .collect { payments ->
                        // Load counterparty if exists
                        val counterparty = transaction.counterpartyId?.let {
                            counterpartyRepository.getById(it)
                        }

                        // Load account
                        val account = accountRepository.getById(transaction.accountId)

                        // Load category
                        val category = categoryRepository.getById(transaction.categoryId)

                        // Load linked task if exists (Requirements: 15.5)
                        val linkedTask = transaction.linkedTaskId?.let {
                            taskRepository.getById(it)
                        }

                        _uiState.value = TransactionDetailsUiState.Success(
                            transaction = transaction,
                            partialPayments = payments,
                            counterparty = counterparty,
                            account = account,
                            category = category,
                            linkedTask = linkedTask
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = TransactionDetailsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Shows the add partial payment dialog.
     */
    fun showAddPaymentDialog() {
        _showAddPaymentDialog.value = true
    }

    /**
     * Hides the add partial payment dialog.
     */
    fun hideAddPaymentDialog() {
        _showAddPaymentDialog.value = false
    }

    /**
     * Adds a partial payment to the transaction.
     * 
     * Requirements: 3.1, 3.2, 3.3
     */
    fun addPartialPayment(
        amount: BigDecimal,
        direction: PaymentDirection,
        method: PaymentMethod,
        notes: String?
    ) {
        viewModelScope.launch {
            val result = addPartialPaymentUseCase(
                transactionId = transactionId,
                amount = amount,
                direction = direction,
                method = method,
                dateTime = LocalDateTime.now(),
                notes = notes
            )

            when (result) {
                is AddPartialPaymentUseCase.Result.Success -> {
                    hideAddPaymentDialog()
                    loadTransactionDetails()
                }
                is AddPartialPaymentUseCase.Result.Surplus -> {
                    // Update UI state to show surplus warning
                    val currentState = _uiState.value
                    if (currentState is TransactionDetailsUiState.Success) {
                        _uiState.value = currentState.copy(
                            surplusAmount = result.surplusAmount
                        )
                    }
                    hideAddPaymentDialog()
                    loadTransactionDetails()
                }
                is AddPartialPaymentUseCase.Result.ValidationError -> {
                    val currentState = _uiState.value
                    if (currentState is TransactionDetailsUiState.Success) {
                        _uiState.value = currentState.copy(
                            errorMessage = result.message
                        )
                    }
                }
                is AddPartialPaymentUseCase.Result.TransactionNotFound -> {
                    _uiState.value = TransactionDetailsUiState.Error("Transaction not found")
                }
            }
        }
    }

    /**
     * Cancels the transaction.
     * 
     * Requirements: 2.4, 7.3
     */
    fun cancelTransaction() {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is TransactionDetailsUiState.Success) {
                    val oldTransaction = currentState.transaction
                    val updatedTransaction = oldTransaction.copy(
                        status = TransactionStatus.CANCELLED
                    )
                    transactionRepository.update(updatedTransaction)
                    
                    // Log UPDATE action for cancellation
                    auditLogRepository.logAction(
                        action = AuditAction.UPDATE,
                        entityType = AuditEntityType.TRANSACTION,
                        entityId = transactionId,
                        oldValue = transactionToJson(oldTransaction),
                        newValue = transactionToJson(updatedTransaction),
                        details = "Cancelled transaction"
                    )
                    
                    loadTransactionDetails()
                }
            } catch (e: Exception) {
                val currentState = _uiState.value
                if (currentState is TransactionDetailsUiState.Success) {
                    _uiState.value = currentState.copy(
                        errorMessage = e.message ?: "Failed to cancel transaction"
                    )
                }
            }
        }
    }

    /**
     * Duplicates the transaction with current date.
     * 
     * Requirements: 2.5, 7.3
     */
    fun duplicateTransaction(): Long? {
        var newTransactionId: Long? = null
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is TransactionDetailsUiState.Success) {
                    val duplicatedTransaction = currentState.transaction.copy(
                        id = 0,
                        transactionDateTime = LocalDateTime.now(),
                        status = TransactionStatus.PENDING,
                        remainingDue = currentState.transaction.amount
                    )
                    newTransactionId = transactionRepository.insert(duplicatedTransaction)
                    
                    // Log CREATE action for duplicated transaction
                    newTransactionId?.let { id ->
                        auditLogRepository.logAction(
                            action = AuditAction.CREATE,
                            entityType = AuditEntityType.TRANSACTION,
                            entityId = id,
                            oldValue = null,
                            newValue = transactionToJson(duplicatedTransaction.copy(id = id)),
                            details = "Duplicated from transaction $transactionId"
                        )
                    }
                }
            } catch (e: Exception) {
                val currentState = _uiState.value
                if (currentState is TransactionDetailsUiState.Success) {
                    _uiState.value = currentState.copy(
                        errorMessage = e.message ?: "Failed to duplicate transaction"
                    )
                }
            }
        }
        return newTransactionId
    }

    /**
     * Clears error message.
     */
    fun clearError() {
        val currentState = _uiState.value
        if (currentState is TransactionDetailsUiState.Success) {
            _uiState.value = currentState.copy(errorMessage = null)
        }
    }

    /**
     * Clears surplus warning.
     */
    fun clearSurplus() {
        val currentState = _uiState.value
        if (currentState is TransactionDetailsUiState.Success) {
            _uiState.value = currentState.copy(surplusAmount = null)
        }
    }

    /**
     * Exports the transaction to shareable text format.
     * 
     * Requirements: 4.3
     */
    fun exportTransaction() {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is TransactionDetailsUiState.Success) {
                    val result = exportLedgerUseCase.exportTransaction(
                        transactionId = transactionId,
                        counterpartyName = currentState.counterparty?.displayName,
                        accountName = currentState.account?.name,
                        categoryName = currentState.category?.name
                    )
                    _exportResult.value = result
                }
            } catch (e: Exception) {
                _exportResult.value = ExportLedgerUseCase.ExportResult.Error(
                    e.message ?: "Failed to export transaction"
                )
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
     * Deletes the transaction (soft delete).
     * 
     * Requirements: 13.2, 7.3
     */
    fun deleteTransaction(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is TransactionDetailsUiState.Success) {
                    val oldTransaction = currentState.transaction
                    
                    transactionRepository.softDelete(transactionId)
                    
                    // Log DELETE action
                    auditLogRepository.logAction(
                        action = AuditAction.DELETE,
                        entityType = AuditEntityType.TRANSACTION,
                        entityId = transactionId,
                        oldValue = transactionToJson(oldTransaction),
                        newValue = null,
                        details = "Soft deleted transaction"
                    )
                    
                    onDeleted()
                }
            } catch (e: Exception) {
                val currentState = _uiState.value
                if (currentState is TransactionDetailsUiState.Success) {
                    _uiState.value = currentState.copy(
                        errorMessage = e.message ?: "Failed to delete transaction"
                    )
                }
            }
        }
    }
    
    private fun transactionToJson(transaction: Transaction): String {
        return JSONObject().apply {
            put("id", transaction.id)
            put("direction", transaction.direction.name)
            put("type", transaction.type.name)
            put("amount", transaction.amount.toPlainString())
            put("accountId", transaction.accountId)
            put("counterpartyId", transaction.counterpartyId)
            put("categoryId", transaction.categoryId)
            put("status", transaction.status.name)
            put("notes", transaction.notes)
            put("remainingDue", transaction.remainingDue.toPlainString())
        }.toString()
    }
}

/**
 * UI state for the Transaction Details screen.
 */
sealed class TransactionDetailsUiState {
    object Loading : TransactionDetailsUiState()
    
    data class Success(
        val transaction: Transaction,
        val partialPayments: List<PartialPayment>,
        val counterparty: Counterparty?,
        val account: Account?,
        val category: Category?,
        val linkedTask: com.ledger.app.domain.model.Task? = null,
        val errorMessage: String? = null,
        val surplusAmount: BigDecimal? = null
    ) : TransactionDetailsUiState()
    
    data class Error(val message: String) : TransactionDetailsUiState()
}


package com.ledger.app.domain.usecase

import com.ledger.app.domain.model.AuditAction
import com.ledger.app.domain.model.AuditEntityType
import com.ledger.app.domain.model.BillCategory
import com.ledger.app.domain.model.TransactionDirection
import com.ledger.app.domain.model.TransactionType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Property-based tests for audit log completeness.
 * 
 * **Feature: offline-ledger-app, Property 14: Audit Log Completeness**
 * 
 * For any create, update, or delete operation on transactions, 
 * a corresponding audit log entry should be created with timestamp and details.
 * 
 * **Validates: Requirements 7.3**
 */
class AuditLogCompletenessPropertyTest : FunSpec({

    /**
     * Generator for valid positive amounts (in cents, converted to BigDecimal).
     */
    fun arbPositiveAmount(): Arb<BigDecimal> = Arb.long(1L, 1_000_000_00L).map { cents ->
        BigDecimal(cents).divide(BigDecimal(100))
    }

    /**
     * Generator for valid account IDs.
     */
    fun arbAccountId(): Arb<Long> = Arb.long(1L, 100L)

    /**
     * Generator for valid category IDs.
     */
    fun arbCategoryId(): Arb<Long> = Arb.long(1L, 20L)

    /**
     * Generator for optional counterparty IDs.
     */
    fun arbCounterpartyId(): Arb<Long?> = Arb.long(1L, 100L).orNull()

    /**
     * Generator for optional notes.
     */
    fun arbNotes(): Arb<String?> = Arb.string(0, 100).orNull()


    /**
     * **Feature: offline-ledger-app, Property 14: Audit Log Completeness**
     * 
     * For any CREATE operation on transactions, a corresponding audit log entry 
     * should be created with action=CREATE, entityType=TRANSACTION, and the new value.
     * 
     * **Validates: Requirements 7.3**
     */
    test("Property 14 - CREATE operation generates audit log entry") {
        checkAll(
            100,
            Arb.enum<TransactionDirection>(),
            Arb.enum<TransactionType>(),
            arbPositiveAmount(),
            arbAccountId(),
            arbCategoryId(),
            arbCounterpartyId(),
            arbNotes()
        ) { direction, type, amount, accountId, categoryId, counterpartyId, notes ->
            // Setup
            val transactionRepository = FakeTransactionRepository()
            val auditLogRepository = FakeAuditLogRepository()
            
            val useCase = AddTransactionUseCase(
                transactionRepository = transactionRepository,
                auditLogRepository = auditLogRepository
            )
            
            // Execute
            val result = useCase(
                direction = direction,
                type = type,
                amount = amount,
                accountId = accountId,
                categoryId = categoryId,
                counterpartyId = counterpartyId,
                transactionDateTime = LocalDateTime.now(),
                notes = notes
            )
            
            // Verify
            when (result) {
                is AddTransactionUseCase.Result.Success -> {
                    val transactionId = result.transactionId
                    val logs = auditLogRepository.getLogsForEntity(AuditEntityType.TRANSACTION, transactionId)
                    
                    // Should have exactly one CREATE log
                    logs shouldHaveSize 1
                    
                    val log = logs.first()
                    log.action shouldBe AuditAction.CREATE
                    log.entityType shouldBe AuditEntityType.TRANSACTION
                    log.entityId shouldBe transactionId
                    log.oldValue shouldBe null // CREATE has no old value
                    log.newValue shouldNotBe null // CREATE should have new value
                    log.timestamp shouldNotBe null
                }
                is AddTransactionUseCase.Result.ValidationError -> {
                    // Validation errors don't create audit logs - this is expected
                }
            }
        }
    }

    /**
     * **Feature: offline-ledger-app, Property 14: Audit Log Completeness**
     * 
     * For any UPDATE operation on transactions (via partial payment), 
     * a corresponding audit log entry should be created with action=UPDATE,
     * entityType=TRANSACTION, and both old and new values.
     * 
     * **Validates: Requirements 7.3**
     */
    test("Property 14 - UPDATE operation generates audit log entry with old and new values") {
        checkAll(
            100,
            arbPositiveAmount(),
            arbPositiveAmount()
        ) { baseAmount, paymentAmount ->
            // Only test when payment is less than or equal to base amount
            if (paymentAmount <= baseAmount) {
                // Setup
                val transactionRepository = FakeTransactionRepository()
                val partialPaymentRepository = FakePartialPaymentRepository()
                val auditLogRepository = FakeAuditLogRepository()
                
                // Create a transaction first
                val addTransactionUseCase = AddTransactionUseCase(
                    transactionRepository = transactionRepository,
                    auditLogRepository = auditLogRepository
                )
                
                val createResult = addTransactionUseCase(
                    direction = TransactionDirection.GAVE,
                    type = TransactionType.LOAN,
                    amount = baseAmount,
                    accountId = 1L,
                    categoryId = 1L,
                    counterpartyId = 1L,
                    transactionDateTime = LocalDateTime.now()
                )
                
                if (createResult is AddTransactionUseCase.Result.Success) {
                    val transactionId = createResult.transactionId
                    
                    // Clear the CREATE log to focus on UPDATE
                    auditLogRepository.clear()
                    
                    // Add partial payment (which triggers UPDATE)
                    val addPaymentUseCase = AddPartialPaymentUseCase(
                        partialPaymentRepository = partialPaymentRepository,
                        transactionRepository = transactionRepository,
                        auditLogRepository = auditLogRepository,
                        reminderRepository = FakeReminderRepository()
                    )
                    
                    val paymentResult = addPaymentUseCase(
                        transactionId = transactionId,
                        amount = paymentAmount,
                        direction = com.ledger.app.domain.model.PaymentDirection.FROM_COUNTERPARTY,
                        method = com.ledger.app.domain.model.PaymentMethod.CASH
                    )
                    
                    when (paymentResult) {
                        is AddPartialPaymentUseCase.Result.Success,
                        is AddPartialPaymentUseCase.Result.Surplus -> {
                            // Should have UPDATE log for transaction
                            val updateLogs = auditLogRepository.getAllLogs()
                                .filter { it.action == AuditAction.UPDATE && it.entityType == AuditEntityType.TRANSACTION }
                            
                            updateLogs shouldHaveSize 1
                            
                            val updateLog = updateLogs.first()
                            updateLog.oldValue shouldNotBe null // UPDATE should have old value
                            updateLog.newValue shouldNotBe null // UPDATE should have new value
                            updateLog.entityId shouldBe transactionId
                        }
                        else -> {
                            // Validation errors or not found - no audit log expected
                        }
                    }
                }
            }
        }
    }


    /**
     * **Feature: offline-ledger-app, Property 14: Audit Log Completeness**
     * 
     * For any PARTIAL_PAYMENT operation, a corresponding audit log entry 
     * should be created with action=PARTIAL_PAYMENT and the payment details.
     * 
     * **Validates: Requirements 7.3**
     */
    test("Property 14 - PARTIAL_PAYMENT operation generates audit log entry") {
        checkAll(
            100,
            arbPositiveAmount(),
            arbPositiveAmount()
        ) { baseAmount, paymentAmount ->
            // Only test when payment is less than or equal to base amount
            if (paymentAmount <= baseAmount) {
                // Setup
                val transactionRepository = FakeTransactionRepository()
                val partialPaymentRepository = FakePartialPaymentRepository()
                val auditLogRepository = FakeAuditLogRepository()
                
                // Create a transaction first
                val addTransactionUseCase = AddTransactionUseCase(
                    transactionRepository = transactionRepository,
                    auditLogRepository = auditLogRepository
                )
                
                val createResult = addTransactionUseCase(
                    direction = TransactionDirection.GAVE,
                    type = TransactionType.LOAN,
                    amount = baseAmount,
                    accountId = 1L,
                    categoryId = 1L,
                    counterpartyId = 1L,
                    transactionDateTime = LocalDateTime.now()
                )
                
                if (createResult is AddTransactionUseCase.Result.Success) {
                    val transactionId = createResult.transactionId
                    
                    // Clear the CREATE log to focus on PARTIAL_PAYMENT
                    auditLogRepository.clear()
                    
                    // Add partial payment
                    val addPaymentUseCase = AddPartialPaymentUseCase(
                        partialPaymentRepository = partialPaymentRepository,
                        transactionRepository = transactionRepository,
                        auditLogRepository = auditLogRepository,
                        reminderRepository = FakeReminderRepository()
                    )
                    
                    val paymentResult = addPaymentUseCase(
                        transactionId = transactionId,
                        amount = paymentAmount,
                        direction = com.ledger.app.domain.model.PaymentDirection.FROM_COUNTERPARTY,
                        method = com.ledger.app.domain.model.PaymentMethod.CASH
                    )
                    
                    when (paymentResult) {
                        is AddPartialPaymentUseCase.Result.Success,
                        is AddPartialPaymentUseCase.Result.Surplus -> {
                            // Should have PARTIAL_PAYMENT log
                            val paymentLogs = auditLogRepository.getAllLogs()
                                .filter { it.action == AuditAction.PARTIAL_PAYMENT }
                            
                            paymentLogs shouldHaveSize 1
                            
                            val paymentLog = paymentLogs.first()
                            paymentLog.entityType shouldBe AuditEntityType.PARTIAL_PAYMENT
                            paymentLog.newValue shouldNotBe null // Should have payment details
                            paymentLog.oldValue shouldBe null // New payment has no old value
                        }
                        else -> {
                            // Validation errors or not found - no audit log expected
                        }
                    }
                }
            }
        }
    }

    /**
     * **Feature: offline-ledger-app, Property 14: Audit Log Completeness**
     * 
     * Audit log entries should contain meaningful details about the operation.
     * 
     * **Validates: Requirements 7.3**
     */
    test("Property 14 - Audit log entries contain meaningful details") {
        checkAll(
            100,
            Arb.enum<TransactionDirection>(),
            arbPositiveAmount()
        ) { direction, amount ->
            // Setup
            val transactionRepository = FakeTransactionRepository()
            val auditLogRepository = FakeAuditLogRepository()
            
            val useCase = AddTransactionUseCase(
                transactionRepository = transactionRepository,
                auditLogRepository = auditLogRepository
            )
            
            // Execute
            val result = useCase(
                direction = direction,
                type = TransactionType.LOAN,
                amount = amount,
                accountId = 1L,
                categoryId = 1L,
                counterpartyId = 1L,
                transactionDateTime = LocalDateTime.now()
            )
            
            // Verify
            if (result is AddTransactionUseCase.Result.Success) {
                val logs = auditLogRepository.getAllLogs()
                logs shouldHaveSize 1
                
                val log = logs.first()
                // Details should contain meaningful information
                log.details shouldNotBe null
                log.details!!.contains(direction.name) shouldBe true
                log.details!!.contains(amount.toString()) shouldBe true
            }
        }
    }

    /**
     * **Feature: offline-ledger-app, Property 14: Audit Log Completeness**
     * 
     * Audit log newValue should contain JSON representation of the entity.
     * 
     * **Validates: Requirements 7.3**
     */
    test("Property 14 - Audit log newValue contains JSON representation") {
        checkAll(
            100,
            arbPositiveAmount(),
            arbAccountId(),
            arbCategoryId()
        ) { amount, accountId, categoryId ->
            // Setup
            val transactionRepository = FakeTransactionRepository()
            val auditLogRepository = FakeAuditLogRepository()
            
            val useCase = AddTransactionUseCase(
                transactionRepository = transactionRepository,
                auditLogRepository = auditLogRepository
            )
            
            // Execute
            val result = useCase(
                direction = TransactionDirection.GAVE,
                type = TransactionType.LOAN,
                amount = amount,
                accountId = accountId,
                categoryId = categoryId,
                counterpartyId = 1L,
                transactionDateTime = LocalDateTime.now()
            )
            
            // Verify
            if (result is AddTransactionUseCase.Result.Success) {
                val logs = auditLogRepository.getAllLogs()
                logs shouldHaveSize 1
                
                val log = logs.first()
                val newValue = log.newValue!!
                
                // Should be valid JSON containing key fields
                newValue.contains("\"id\"") shouldBe true
                newValue.contains("\"direction\"") shouldBe true
                newValue.contains("\"amount\"") shouldBe true
                newValue.contains("\"accountId\"") shouldBe true
                newValue.contains("\"categoryId\"") shouldBe true
            }
        }
    }
})

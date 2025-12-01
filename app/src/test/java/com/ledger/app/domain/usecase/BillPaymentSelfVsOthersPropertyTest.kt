package com.ledger.app.domain.usecase

import com.ledger.app.domain.model.BillCategory
import com.ledger.app.domain.model.TransactionDirection
import com.ledger.app.domain.model.TransactionStatus
import com.ledger.app.domain.model.TransactionType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Property-based tests for bill payment self vs others behavior.
 * 
 * **Feature: offline-ledger-app, Property 13: Bill Payment Self vs Others**
 * 
 * For any bill payment marked as "for self", it should NOT affect any counterparty's 
 * debt balance. For others, it should add to their debt.
 * 
 * **Validates: Requirements 3.3, 3.4**
 */
class BillPaymentSelfVsOthersPropertyTest : FunSpec({

    /**
     * Generator for valid positive amounts (in cents, converted to BigDecimal).
     */
    fun arbPositiveAmount(): Arb<BigDecimal> = Arb.long(1L, 1_000_000_00L).map { cents ->
        BigDecimal(cents).divide(BigDecimal(100))
    }

    /**
     * Generator for bill categories.
     */
    fun arbBillCategory(): Arb<BillCategory> = Arb.element(BillCategory.values().toList())

    /**
     * **Feature: offline-ledger-app, Property 13: Bill Payment Self vs Others**
     * 
     * For any bill payment marked as "for self", it should NOT affect any counterparty's 
     * debt balance. For others, it should add to their debt.
     * 
     * **Validates: Requirements 3.3, 3.4**
     */
    test("Property 13 - Bill payment for self has zero remaining due (no debt tracked)") {
        val repository = FakeTransactionRepository()
        val auditLogRepository = FakeAuditLogRepository()
        val addTransactionUseCase = AddTransactionUseCase(repository, auditLogRepository)

        checkAll(100, arbPositiveAmount(), arbBillCategory()) { amount, billCategory ->
            // Add a bill payment for self
            val result = addTransactionUseCase(
                direction = TransactionDirection.GAVE,
                type = TransactionType.BILL_PAYMENT,
                amount = amount,
                accountId = 1L,
                categoryId = 1L,
                counterpartyId = null, // No counterparty for self
                transactionDateTime = LocalDateTime.now(),
                notes = "Test bill payment for self",
                consumerId = "1234567890",
                billCategory = billCategory,
                isForSelf = true
            )

            // Verify transaction was created
            result.shouldBeInstanceOf<AddTransactionUseCase.Result.Success>()

            // Get the transaction
            val transactionId = (result as AddTransactionUseCase.Result.Success).transactionId
            val transaction = repository.getById(transactionId)

            // For self payments should have zero remaining due
            transaction?.remainingDue?.compareTo(BigDecimal.ZERO) shouldBe 0
            
            // For self payments should be marked as SETTLED
            transaction?.status shouldBe TransactionStatus.SETTLED
            
            // isForSelf flag should be true
            transaction?.isForSelf shouldBe true
        }
    }

    test("Property 13 - Bill payment for others has remaining due equal to amount (debt tracked)") {
        val repository = FakeTransactionRepository()
        val auditLogRepository = FakeAuditLogRepository()
        val addTransactionUseCase = AddTransactionUseCase(repository, auditLogRepository)

        checkAll(100, arbPositiveAmount(), arbBillCategory()) { amount, billCategory ->
            // Add a bill payment for others
            val result = addTransactionUseCase(
                direction = TransactionDirection.GAVE,
                type = TransactionType.BILL_PAYMENT,
                amount = amount,
                accountId = 1L,
                categoryId = 1L,
                counterpartyId = 1L, // Has counterparty for others
                transactionDateTime = LocalDateTime.now(),
                notes = "Test bill payment for others",
                consumerId = "1234567890",
                billCategory = billCategory,
                isForSelf = false
            )

            // Verify transaction was created
            result.shouldBeInstanceOf<AddTransactionUseCase.Result.Success>()

            // Get the transaction
            val transactionId = (result as AddTransactionUseCase.Result.Success).transactionId
            val transaction = repository.getById(transactionId)

            // For others payments should have remaining due equal to amount
            transaction?.remainingDue?.compareTo(amount) shouldBe 0
            
            // For others payments should be marked as PENDING
            transaction?.status shouldBe TransactionStatus.PENDING
            
            // isForSelf flag should be false
            transaction?.isForSelf shouldBe false
        }
    }

    test("Property 13 - Self payments do not affect total owed to user") {
        checkAll(50, arbPositiveAmount(), arbPositiveAmount()) { selfAmount, othersAmount ->
            // Clear repository for each test
            val freshRepository = FakeTransactionRepository()
            val freshAuditLogRepository = FakeAuditLogRepository()
            val freshUseCase = AddTransactionUseCase(freshRepository, freshAuditLogRepository)

            // Add a bill payment for self
            freshUseCase(
                direction = TransactionDirection.GAVE,
                type = TransactionType.BILL_PAYMENT,
                amount = selfAmount,
                accountId = 1L,
                categoryId = 1L,
                counterpartyId = null,
                transactionDateTime = LocalDateTime.now(),
                notes = "Self payment",
                consumerId = "1111111111",
                billCategory = BillCategory.ELECTRICITY,
                isForSelf = true
            )

            // Add a bill payment for others
            freshUseCase(
                direction = TransactionDirection.GAVE,
                type = TransactionType.BILL_PAYMENT,
                amount = othersAmount,
                accountId = 1L,
                categoryId = 1L,
                counterpartyId = 1L,
                transactionDateTime = LocalDateTime.now(),
                notes = "Others payment",
                consumerId = "2222222222",
                billCategory = BillCategory.MOBILE,
                isForSelf = false
            )

            // Total owed to user should only include the "for others" payment
            val totalOwed = freshRepository.getTotalOwedToUser()
            
            // Self payments have remainingDue = 0, so they don't contribute
            // Others payments have remainingDue = amount, so they contribute
            totalOwed.compareTo(othersAmount) shouldBe 0
        }
    }

    test("Property 13 - Bill payments for self are filtered correctly") {
        checkAll(30, arbPositiveAmount(), arbPositiveAmount()) { selfAmount, othersAmount ->
            val freshRepository = FakeTransactionRepository()
            val freshAuditLogRepository = FakeAuditLogRepository()
            val freshUseCase = AddTransactionUseCase(freshRepository, freshAuditLogRepository)

            // Add bill payment for self
            freshUseCase(
                direction = TransactionDirection.GAVE,
                type = TransactionType.BILL_PAYMENT,
                amount = selfAmount,
                accountId = 1L,
                categoryId = 1L,
                counterpartyId = null,
                transactionDateTime = LocalDateTime.now(),
                consumerId = "1111111111",
                billCategory = BillCategory.TV,
                isForSelf = true
            )

            // Add bill payment for others
            freshUseCase(
                direction = TransactionDirection.GAVE,
                type = TransactionType.BILL_PAYMENT,
                amount = othersAmount,
                accountId = 1L,
                categoryId = 1L,
                counterpartyId = 1L,
                transactionDateTime = LocalDateTime.now(),
                consumerId = "2222222222",
                billCategory = BillCategory.INTERNET,
                isForSelf = false
            )

            // Filter for self payments
            val selfPayments = freshRepository.getBillPaymentsByForSelf(true).first()
            selfPayments.size shouldBe 1
            selfPayments.all { it.isForSelf } shouldBe true

            // Filter for others payments
            val othersPayments = freshRepository.getBillPaymentsByForSelf(false).first()
            othersPayments.size shouldBe 1
            othersPayments.all { !it.isForSelf } shouldBe true
        }
    }

    test("Property 13 - isForSelf flag is preserved through repository operations") {
        checkAll(100, arbPositiveAmount(), Arb.boolean(), arbBillCategory()) { amount, isForSelf, billCategory ->
            val repository = FakeTransactionRepository()
            val auditLogRepository = FakeAuditLogRepository()
            val addTransactionUseCase = AddTransactionUseCase(repository, auditLogRepository)

            val result = addTransactionUseCase(
                direction = TransactionDirection.GAVE,
                type = TransactionType.BILL_PAYMENT,
                amount = amount,
                accountId = 1L,
                categoryId = 1L,
                counterpartyId = if (isForSelf) null else 1L,
                transactionDateTime = LocalDateTime.now(),
                consumerId = "9876543210",
                billCategory = billCategory,
                isForSelf = isForSelf
            )

            result.shouldBeInstanceOf<AddTransactionUseCase.Result.Success>()
            val transactionId = (result as AddTransactionUseCase.Result.Success).transactionId
            val retrieved = repository.getById(transactionId)

            // isForSelf flag should be preserved
            retrieved?.isForSelf shouldBe isForSelf
            
            // Remaining due should be consistent with isForSelf
            if (isForSelf) {
                retrieved?.remainingDue?.compareTo(BigDecimal.ZERO) shouldBe 0
            } else {
                retrieved?.remainingDue?.compareTo(amount) shouldBe 0
            }
        }
    }
})

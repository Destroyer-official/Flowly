package com.ledger.app.domain.usecase

import com.ledger.app.domain.model.TransactionStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import java.math.BigDecimal

/**
 * Property-based tests for partial payment calculations.
 * 
 * Tests Properties 2, 3, and 6 from the design document.
 */
class PartialPaymentPropertyTest : FunSpec({

    /**
     * Generator for valid positive amounts (in cents, converted to BigDecimal).
     */
    fun arbPositiveAmount(): Arb<BigDecimal> = Arb.long(1L, 1_000_000_00L).map { cents ->
        BigDecimal(cents).divide(BigDecimal(100))
    }

    /**
     * Generator for non-negative amounts (including zero).
     */
    fun arbNonNegativeAmount(): Arb<BigDecimal> = Arb.long(0L, 1_000_000_00L).map { cents ->
        BigDecimal(cents).divide(BigDecimal(100))
    }

    /**
     * **Feature: offline-ledger-app, Property 2: Remaining Due Calculation**
     * 
     * For any transaction with a set of partial payments, the remaining_due 
     * should equal base_amount minus the sum of all partial payments.
     * This must hold after any sequence of payment additions.
     * 
     * **Validates: Requirements 3.2**
     */
    test("Property 2 - Remaining due equals base amount minus sum of payments") {
        checkAll(100, arbPositiveAmount(), Arb.list(arbNonNegativeAmount(), 0..10)) { baseAmount, payments ->
            val totalPayments = payments.fold(BigDecimal.ZERO) { acc, payment -> acc + payment }
            
            val remainingDue = AddPartialPaymentUseCase.calculateRemainingDue(baseAmount, totalPayments)
            
            remainingDue shouldBe (baseAmount - totalPayments)
        }
    }

    test("Property 2 - Remaining due with no payments equals base amount") {
        checkAll(100, arbPositiveAmount()) { baseAmount ->
            val remainingDue = AddPartialPaymentUseCase.calculateRemainingDue(baseAmount, BigDecimal.ZERO)
            
            remainingDue shouldBe baseAmount
        }
    }

    test("Property 2 - Remaining due with exact payment equals zero") {
        checkAll(100, arbPositiveAmount()) { baseAmount ->
            val remainingDue = AddPartialPaymentUseCase.calculateRemainingDue(baseAmount, baseAmount)
            
            remainingDue.compareTo(BigDecimal.ZERO) shouldBe 0
        }
    }

    test("Property 2 - Sequential payments accumulate correctly") {
        checkAll(100, arbPositiveAmount(), Arb.list(arbNonNegativeAmount(), 1..5)) { baseAmount, payments ->
            var runningTotal = BigDecimal.ZERO
            
            payments.forEach { payment ->
                runningTotal += payment
                val remainingDue = AddPartialPaymentUseCase.calculateRemainingDue(baseAmount, runningTotal)
                
                remainingDue shouldBe (baseAmount - runningTotal)
            }
        }
    }

    /**
     * **Feature: offline-ledger-app, Property 3: Transaction Status Consistency**
     * 
     * For any transaction, the status should be consistent with remaining_due:
     * - SETTLED when remaining_due equals zero
     * - PARTIALLY_SETTLED when between zero and base_amount
     * - PENDING when equals base_amount
     * 
     * **Validates: Requirements 3.5**
     */
    test("Property 3 - Status is SETTLED when remaining due is zero") {
        checkAll(100, arbPositiveAmount()) { baseAmount ->
            val useCase = AddPartialPaymentUseCase(
                partialPaymentRepository = FakePartialPaymentRepository(),
                transactionRepository = FakeTransactionRepository(),
                auditLogRepository = FakeAuditLogRepository(),
                reminderRepository = FakeReminderRepository()
            )
            
            val status = useCase.calculateStatus(BigDecimal.ZERO, baseAmount)
            
            status shouldBe TransactionStatus.SETTLED
        }
    }

    test("Property 3 - Status is PENDING when remaining due equals base amount") {
        checkAll(100, arbPositiveAmount()) { baseAmount ->
            val useCase = AddPartialPaymentUseCase(
                partialPaymentRepository = FakePartialPaymentRepository(),
                transactionRepository = FakeTransactionRepository(),
                auditLogRepository = FakeAuditLogRepository(),
                reminderRepository = FakeReminderRepository()
            )
            
            val status = useCase.calculateStatus(baseAmount, baseAmount)
            
            status shouldBe TransactionStatus.PENDING
        }
    }

    test("Property 3 - Status is PARTIALLY_SETTLED when remaining due is between zero and base amount") {
        // Generate base amount and a partial payment that leaves some remaining
        checkAll(100, arbPositiveAmount(), arbPositiveAmount()) { baseAmount, partialPayment ->
            // Only test when partial payment is less than base amount
            if (partialPayment < baseAmount) {
                val remainingDue = baseAmount - partialPayment
                
                val useCase = AddPartialPaymentUseCase(
                    partialPaymentRepository = FakePartialPaymentRepository(),
                    transactionRepository = FakeTransactionRepository(),
                    auditLogRepository = FakeAuditLogRepository(),
                    reminderRepository = FakeReminderRepository()
                )
                
                val status = useCase.calculateStatus(remainingDue, baseAmount)
                
                status shouldBe TransactionStatus.PARTIALLY_SETTLED
            }
        }
    }

    test("Property 3 - Status is SETTLED when overpaid (negative remaining due)") {
        checkAll(100, arbPositiveAmount()) { overpayment ->
            val negativeRemainingDue = overpayment.negate()
            val baseAmount = BigDecimal("100.00")
            
            val useCase = AddPartialPaymentUseCase(
                partialPaymentRepository = FakePartialPaymentRepository(),
                transactionRepository = FakeTransactionRepository(),
                auditLogRepository = FakeAuditLogRepository(),
                reminderRepository = FakeReminderRepository()
            )
            
            val status = useCase.calculateStatus(negativeRemainingDue, baseAmount)
            
            status shouldBe TransactionStatus.SETTLED
        }
    }

    /**
     * **Feature: offline-ledger-app, Property 6: Partial Payment Surplus Detection**
     * 
     * For any transaction where sum of partial payments exceeds base amount,
     * the system should detect this (remaining_due becomes negative).
     * 
     * **Validates: Requirements 3.3**
     */
    test("Property 6 - Surplus detected when payments exceed base amount") {
        checkAll(100, arbPositiveAmount(), arbPositiveAmount()) { baseAmount, extraPayment ->
            // Total payments = baseAmount + extraPayment (always exceeds base)
            val totalPayments = baseAmount + extraPayment
            val remainingDue = AddPartialPaymentUseCase.calculateRemainingDue(baseAmount, totalPayments)
            
            val hasSurplus = AddPartialPaymentUseCase.detectSurplus(remainingDue)
            
            hasSurplus shouldBe true
        }
    }

    test("Property 6 - No surplus when payments equal base amount") {
        checkAll(100, arbPositiveAmount()) { baseAmount ->
            val remainingDue = AddPartialPaymentUseCase.calculateRemainingDue(baseAmount, baseAmount)
            
            val hasSurplus = AddPartialPaymentUseCase.detectSurplus(remainingDue)
            
            hasSurplus shouldBe false
        }
    }

    test("Property 6 - No surplus when payments less than base amount") {
        checkAll(100, arbPositiveAmount(), arbPositiveAmount()) { baseAmount, partialPayment ->
            // Only test when partial payment is less than base amount
            if (partialPayment < baseAmount) {
                val remainingDue = AddPartialPaymentUseCase.calculateRemainingDue(baseAmount, partialPayment)
                
                val hasSurplus = AddPartialPaymentUseCase.detectSurplus(remainingDue)
                
                hasSurplus shouldBe false
            }
        }
    }

    test("Property 6 - Surplus amount equals overpayment") {
        checkAll(100, arbPositiveAmount(), arbPositiveAmount()) { baseAmount, extraPayment ->
            val totalPayments = baseAmount + extraPayment
            val remainingDue = AddPartialPaymentUseCase.calculateRemainingDue(baseAmount, totalPayments)
            
            // Surplus amount should equal the extra payment (use compareTo for BigDecimal)
            val surplusAmount = remainingDue.abs()
            surplusAmount.compareTo(extraPayment) shouldBe 0
        }
    }
})

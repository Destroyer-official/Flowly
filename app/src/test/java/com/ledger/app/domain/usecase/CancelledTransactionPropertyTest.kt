package com.ledger.app.domain.usecase

import com.ledger.app.domain.model.Transaction
import com.ledger.app.domain.model.TransactionDirection
import com.ledger.app.domain.model.TransactionStatus
import com.ledger.app.domain.model.TransactionType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Property-based tests for cancelled transaction exclusion.
 * 
 * Tests Property 5 from the design document.
 */
class CancelledTransactionPropertyTest : FunSpec({

    /**
     * Generator for valid positive amounts (in cents, converted to BigDecimal).
     */
    fun arbPositiveAmount(): Arb<BigDecimal> = Arb.long(1L, 1_000_000_00L).map { cents ->
        BigDecimal(cents).divide(BigDecimal(100))
    }

    /**
     * Generator for transactions with any status.
     */
    fun arbTransaction(): Arb<Transaction> = Arb.bind(
        arbPositiveAmount(),
        Arb.enum<TransactionDirection>(),
        Arb.enum<TransactionType>(),
        Arb.enum<TransactionStatus>()
    ) { amount, direction, type, status ->
        Transaction(
            id = 0,
            direction = direction,
            type = type,
            amount = amount,
            accountId = 1,
            counterpartyId = 1,
            categoryId = 1,
            transactionDateTime = LocalDateTime.now(),
            status = status,
            notes = null,
            remainingDue = if (status == TransactionStatus.SETTLED) BigDecimal.ZERO else amount
        )
    }

    /**
     * Generator for cancelled transactions.
     */
    fun arbCancelledTransaction(): Arb<Transaction> = Arb.bind(
        arbPositiveAmount(),
        Arb.enum<TransactionDirection>(),
        Arb.enum<TransactionType>()
    ) { amount, direction, type ->
        Transaction(
            id = 0,
            direction = direction,
            type = type,
            amount = amount,
            accountId = 1,
            counterpartyId = 1,
            categoryId = 1,
            transactionDateTime = LocalDateTime.now(),
            status = TransactionStatus.CANCELLED,
            notes = null,
            remainingDue = amount
        )
    }

    /**
     * **Feature: offline-ledger-app, Property 5: Cancelled Transaction Exclusion**
     * 
     * For any cancelled transaction, it should not appear in balance calculations 
     * or outstanding obligations, but should remain retrievable for audit.
     * 
     * **Validates: Requirements 2.4**
     */
    test("Property 5 - Cancelled transactions excluded from outstanding obligations") {
        checkAll(100, Arb.list(arbTransaction(), 1..20)) { transactions ->
            val repository = FakeTransactionRepository()
            
            // Insert all transactions
            transactions.forEach { transaction ->
                repository.insert(transaction)
            }
            
            // Get outstanding obligations
            val outstanding = repository.getOutstandingObligations().first()
            
            // Verify no cancelled transactions in outstanding
            val cancelledTransactions = transactions.filter { it.status == TransactionStatus.CANCELLED }
            cancelledTransactions.forEach { cancelled ->
                outstanding.none { 
                    it.amount == cancelled.amount && 
                    it.direction == cancelled.direction &&
                    it.status == TransactionStatus.CANCELLED
                } shouldBe true
            }
        }
    }

    test("Property 5 - Cancelled transactions excluded from total owed to user") {
        checkAll(100, arbCancelledTransaction()) { cancelledTransaction ->
            val repository = FakeTransactionRepository()
            
            // Insert a cancelled GAVE transaction
            val gaveTransaction = cancelledTransaction.copy(
                direction = TransactionDirection.GAVE,
                status = TransactionStatus.CANCELLED
            )
            repository.insert(gaveTransaction)
            
            // Total owed to user should be zero
            val totalOwed = repository.getTotalOwedToUser()
            
            totalOwed.compareTo(BigDecimal.ZERO) shouldBe 0
        }
    }

    test("Property 5 - Cancelled transactions excluded from total user owes") {
        checkAll(100, arbCancelledTransaction()) { cancelledTransaction ->
            val repository = FakeTransactionRepository()
            
            // Insert a cancelled RECEIVED transaction
            val receivedTransaction = cancelledTransaction.copy(
                direction = TransactionDirection.RECEIVED,
                status = TransactionStatus.CANCELLED
            )
            repository.insert(receivedTransaction)
            
            // Total user owes should be zero
            val totalOwes = repository.getTotalUserOwes()
            
            totalOwes.compareTo(BigDecimal.ZERO) shouldBe 0
        }
    }

    test("Property 5 - Cancelled transactions still retrievable by ID") {
        checkAll(100, arbCancelledTransaction()) { cancelledTransaction ->
            val repository = FakeTransactionRepository()
            
            // Insert cancelled transaction
            val id = repository.insert(cancelledTransaction)
            
            // Should be retrievable by ID
            val retrieved = repository.getById(id)
            
            retrieved shouldBe cancelledTransaction.copy(id = id)
        }
    }

    test("Property 5 - Cancelled transactions appear in getAll but not in calculations") {
        checkAll(100, arbPositiveAmount()) { amount ->
            val repository = FakeTransactionRepository()
            
            // Insert one active and one cancelled transaction
            val activeTransaction = Transaction(
                id = 0,
                direction = TransactionDirection.GAVE,
                type = TransactionType.LOAN,
                amount = amount,
                accountId = 1,
                counterpartyId = 1,
                categoryId = 1,
                transactionDateTime = LocalDateTime.now(),
                status = TransactionStatus.PENDING,
                notes = null,
                remainingDue = amount
            )
            
            val cancelledTransaction = Transaction(
                id = 0,
                direction = TransactionDirection.GAVE,
                type = TransactionType.LOAN,
                amount = amount,
                accountId = 1,
                counterpartyId = 1,
                categoryId = 1,
                transactionDateTime = LocalDateTime.now(),
                status = TransactionStatus.CANCELLED,
                notes = null,
                remainingDue = amount
            )
            
            repository.insert(activeTransaction)
            repository.insert(cancelledTransaction)
            
            // Both should appear in getAll
            val allTransactions = repository.getAll().first()
            allTransactions.size shouldBe 2
            
            // Only active should appear in outstanding
            val outstanding = repository.getOutstandingObligations().first()
            outstanding.size shouldBe 1
            outstanding.first().status shouldBe TransactionStatus.PENDING
            
            // Only active should contribute to total
            val totalOwed = repository.getTotalOwedToUser()
            totalOwed.compareTo(amount) shouldBe 0
        }
    }

    test("Property 5 - Cancelling transaction removes it from balance calculations") {
        checkAll(100, arbPositiveAmount()) { amount ->
            val repository = FakeTransactionRepository()
            
            // Insert active transaction
            val transaction = Transaction(
                id = 0,
                direction = TransactionDirection.GAVE,
                type = TransactionType.LOAN,
                amount = amount,
                accountId = 1,
                counterpartyId = 1,
                categoryId = 1,
                transactionDateTime = LocalDateTime.now(),
                status = TransactionStatus.PENDING,
                notes = null,
                remainingDue = amount
            )
            
            val id = repository.insert(transaction)
            
            // Verify it contributes to balance
            val totalBefore = repository.getTotalOwedToUser()
            totalBefore.compareTo(amount) shouldBe 0
            
            // Cancel the transaction
            val cancelledTransaction = transaction.copy(
                id = id,
                status = TransactionStatus.CANCELLED
            )
            repository.update(cancelledTransaction)
            
            // Verify it no longer contributes to balance
            val totalAfter = repository.getTotalOwedToUser()
            totalAfter.compareTo(BigDecimal.ZERO) shouldBe 0
            
            // But still retrievable
            val retrieved = repository.getById(id)
            retrieved?.status shouldBe TransactionStatus.CANCELLED
        }
    }

    test("Property 5 - Mixed cancelled and active transactions calculated correctly") {
        checkAll(100, Arb.list(arbTransaction(), 2..20)) { transactions ->
            val repository = FakeTransactionRepository()
            
            // Insert all transactions
            transactions.forEach { transaction ->
                repository.insert(transaction)
            }
            
            // Calculate expected total (excluding cancelled)
            val expectedGaveTotal = transactions
                .filter { 
                    it.direction == TransactionDirection.GAVE && 
                    it.status != TransactionStatus.CANCELLED 
                }
                .fold(BigDecimal.ZERO) { acc, t -> acc + t.remainingDue }
            
            val expectedReceivedTotal = transactions
                .filter { 
                    it.direction == TransactionDirection.RECEIVED && 
                    it.status != TransactionStatus.CANCELLED 
                }
                .fold(BigDecimal.ZERO) { acc, t -> acc + t.remainingDue }
            
            // Verify actual totals match expected
            val actualGaveTotal = repository.getTotalOwedToUser()
            val actualReceivedTotal = repository.getTotalUserOwes()
            
            actualGaveTotal.compareTo(expectedGaveTotal) shouldBe 0
            actualReceivedTotal.compareTo(expectedReceivedTotal) shouldBe 0
        }
    }
})


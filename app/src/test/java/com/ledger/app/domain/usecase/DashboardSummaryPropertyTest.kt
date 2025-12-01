package com.ledger.app.domain.usecase

import com.ledger.app.domain.model.Transaction
import com.ledger.app.domain.model.TransactionDirection
import com.ledger.app.domain.model.TransactionStatus
import com.ledger.app.domain.model.TransactionType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Property-based tests for dashboard summary calculations.
 * 
 * Tests Property 7 from the design document.
 */
class DashboardSummaryPropertyTest : FunSpec({

    /**
     * Generator for valid positive amounts (in cents, converted to BigDecimal).
     */
    fun arbPositiveAmount(): Arb<BigDecimal> = Arb.long(1L, 1_000_000_00L).map { cents ->
        BigDecimal(cents).divide(BigDecimal(100))
    }

    /**
     * Generator for transactions with specific direction.
     */
    fun arbTransaction(direction: TransactionDirection): Arb<Transaction> = Arb.bind(
        arbPositiveAmount(),
        Arb.enum<TransactionType>(),
        Arb.enum<TransactionStatus>()
    ) { amount, type, status ->
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
     * **Feature: offline-ledger-app, Property 7: Dashboard Summary Consistency**
     * 
     * For any database state, "Total owed to user" should equal sum of remaining_due 
     * for all non-cancelled GAVE transactions, and "Total user owes" should equal 
     * sum for RECEIVED transactions.
     * 
     * **Validates: Requirements 8.1**
     */
    test("Property 7 - Total owed to user equals sum of GAVE transactions remaining due") {
        checkAll(100, Arb.list(arbTransaction(TransactionDirection.GAVE), 0..20)) { transactions ->
            runTest {
                val fakeRepo = FakeTransactionRepository()
                
                // Insert all transactions
                transactions.forEach { transaction ->
                    fakeRepo.insert(transaction)
                }
                
                // Calculate expected total (excluding cancelled)
                val expectedTotal = transactions
                    .filter { it.status != TransactionStatus.CANCELLED }
                    .fold(BigDecimal.ZERO) { acc, t -> acc + t.remainingDue }
                
                val actualTotal = fakeRepo.getTotalOwedToUser()
                
                actualTotal.compareTo(expectedTotal) shouldBe 0
            }
        }
    }

    test("Property 7 - Total user owes equals sum of RECEIVED transactions remaining due") {
        checkAll(100, Arb.list(arbTransaction(TransactionDirection.RECEIVED), 0..20)) { transactions ->
            runTest {
                val fakeRepo = FakeTransactionRepository()
                
                // Insert all transactions
                transactions.forEach { transaction ->
                    fakeRepo.insert(transaction)
                }
                
                // Calculate expected total (excluding cancelled)
                val expectedTotal = transactions
                    .filter { it.status != TransactionStatus.CANCELLED }
                    .fold(BigDecimal.ZERO) { acc, t -> acc + t.remainingDue }
                
                val actualTotal = fakeRepo.getTotalUserOwes()
                
                actualTotal.compareTo(expectedTotal) shouldBe 0
            }
        }
    }

    test("Property 7 - Cancelled transactions excluded from totals") {
        checkAll(100, arbPositiveAmount()) { amount ->
            runTest {
                val fakeRepo = FakeTransactionRepository()
                
                // Insert a cancelled GAVE transaction
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
                fakeRepo.insert(cancelledTransaction)
                
                val totalOwed = fakeRepo.getTotalOwedToUser()
                
                totalOwed.compareTo(BigDecimal.ZERO) shouldBe 0
            }
        }
    }

    test("Property 7 - Mixed transactions calculate correctly") {
        checkAll(
            100,
            Arb.list(arbTransaction(TransactionDirection.GAVE), 0..10),
            Arb.list(arbTransaction(TransactionDirection.RECEIVED), 0..10)
        ) { gaveTransactions, receivedTransactions ->
            runTest {
                val fakeRepo = FakeTransactionRepository()
                
                // Insert all transactions
                gaveTransactions.forEach { fakeRepo.insert(it) }
                receivedTransactions.forEach { fakeRepo.insert(it) }
                
                // Calculate expected totals
                val expectedOwedToUser = gaveTransactions
                    .filter { it.status != TransactionStatus.CANCELLED }
                    .fold(BigDecimal.ZERO) { acc, t -> acc + t.remainingDue }
                
                val expectedUserOwes = receivedTransactions
                    .filter { it.status != TransactionStatus.CANCELLED }
                    .fold(BigDecimal.ZERO) { acc, t -> acc + t.remainingDue }
                
                val actualOwedToUser = fakeRepo.getTotalOwedToUser()
                val actualUserOwes = fakeRepo.getTotalUserOwes()
                
                actualOwedToUser.compareTo(expectedOwedToUser) shouldBe 0
                actualUserOwes.compareTo(expectedUserOwes) shouldBe 0
            }
        }
    }
})

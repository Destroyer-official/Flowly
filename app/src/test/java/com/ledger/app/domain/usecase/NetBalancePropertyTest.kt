package com.ledger.app.domain.usecase

import com.ledger.app.domain.model.Transaction
import com.ledger.app.domain.model.TransactionDirection
import com.ledger.app.domain.model.TransactionStatus
import com.ledger.app.domain.model.TransactionType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Property-based tests for net balance calculations.
 * 
 * Tests Property 4 from the design document.
 */
class NetBalancePropertyTest : FunSpec({

    /**
     * Generator for valid positive amounts (in cents, converted to BigDecimal).
     */
    fun arbPositiveAmount(): Arb<BigDecimal> = Arb.long(1L, 1_000_000_00L).map { cents ->
        BigDecimal(cents).divide(BigDecimal(100))
    }

    /**
     * Generator for transactions with any direction.
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
     * Generator for GAVE transactions only.
     */
    fun arbGaveTransaction(): Arb<Transaction> = Arb.bind(
        arbPositiveAmount(),
        Arb.enum<TransactionType>()
    ) { amount, type ->
        Transaction(
            id = 0,
            direction = TransactionDirection.GAVE,
            type = type,
            amount = amount,
            accountId = 1,
            counterpartyId = 1,
            categoryId = 1,
            transactionDateTime = LocalDateTime.now(),
            status = TransactionStatus.PENDING,
            notes = null,
            remainingDue = amount
        )
    }

    /**
     * Generator for RECEIVED transactions only.
     */
    fun arbReceivedTransaction(): Arb<Transaction> = Arb.bind(
        arbPositiveAmount(),
        Arb.enum<TransactionType>()
    ) { amount, type ->
        Transaction(
            id = 0,
            direction = TransactionDirection.RECEIVED,
            type = type,
            amount = amount,
            accountId = 1,
            counterpartyId = 1,
            categoryId = 1,
            transactionDateTime = LocalDateTime.now(),
            status = TransactionStatus.PENDING,
            notes = null,
            remainingDue = amount
        )
    }

    /**
     * **Feature: offline-ledger-app, Property 4: Net Balance Calculation**
     * 
     * For any counterparty, the net balance should equal 
     * sum(GAVE transactions remaining_due) minus sum(RECEIVED transactions remaining_due).
     * Positive means they owe user, negative means user owes them.
     * 
     * **Validates: Requirements 4.1, 13.3**
     */
    test("Property 4 - Net balance equals GAVE minus RECEIVED remaining due") {
        checkAll(100, Arb.list(arbTransaction(), 0..20)) { transactions ->
            val activeTransactions = transactions.filter { it.status != TransactionStatus.CANCELLED }
            
            val expectedGaveTotal = activeTransactions
                .filter { it.direction == TransactionDirection.GAVE }
                .fold(BigDecimal.ZERO) { acc, t -> acc + t.remainingDue }
            
            val expectedReceivedTotal = activeTransactions
                .filter { it.direction == TransactionDirection.RECEIVED }
                .fold(BigDecimal.ZERO) { acc, t -> acc + t.remainingDue }
            
            val expectedNetBalance = expectedGaveTotal - expectedReceivedTotal
            
            val actualNetBalance = GetCounterpartyLedgerUseCase.calculateNetBalanceStatic(transactions)
            
            actualNetBalance.compareTo(expectedNetBalance) shouldBe 0
        }
    }

    test("Property 4 - Positive balance when only GAVE transactions exist") {
        checkAll(100, Arb.list(arbGaveTransaction(), 1..10)) { transactions ->
            val netBalance = GetCounterpartyLedgerUseCase.calculateNetBalanceStatic(transactions)
            
            netBalance shouldBeGreaterThan BigDecimal.ZERO
        }
    }

    test("Property 4 - Negative balance when only RECEIVED transactions exist") {
        checkAll(100, Arb.list(arbReceivedTransaction(), 1..10)) { transactions ->
            val netBalance = GetCounterpartyLedgerUseCase.calculateNetBalanceStatic(transactions)
            
            netBalance shouldBeLessThan BigDecimal.ZERO
        }
    }

    test("Property 4 - Zero balance when no transactions") {
        val netBalance = GetCounterpartyLedgerUseCase.calculateNetBalanceStatic(emptyList())
        
        netBalance.compareTo(BigDecimal.ZERO) shouldBe 0
    }

    test("Property 4 - Cancelled transactions excluded from balance") {
        checkAll(100, arbPositiveAmount()) { amount ->
            val cancelledGave = Transaction(
                id = 1,
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
            
            val cancelledReceived = Transaction(
                id = 2,
                direction = TransactionDirection.RECEIVED,
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
            
            val netBalance = GetCounterpartyLedgerUseCase.calculateNetBalanceStatic(
                listOf(cancelledGave, cancelledReceived)
            )
            
            netBalance.compareTo(BigDecimal.ZERO) shouldBe 0
        }
    }

    test("Property 4 - Settled transactions contribute zero to balance") {
        checkAll(100, arbPositiveAmount()) { amount ->
            val settledGave = Transaction(
                id = 1,
                direction = TransactionDirection.GAVE,
                type = TransactionType.LOAN,
                amount = amount,
                accountId = 1,
                counterpartyId = 1,
                categoryId = 1,
                transactionDateTime = LocalDateTime.now(),
                status = TransactionStatus.SETTLED,
                notes = null,
                remainingDue = BigDecimal.ZERO // Settled means remainingDue is zero
            )
            
            val netBalance = GetCounterpartyLedgerUseCase.calculateNetBalanceStatic(listOf(settledGave))
            
            netBalance.compareTo(BigDecimal.ZERO) shouldBe 0
        }
    }

    test("Property 4 - Balance is commutative with transaction order") {
        checkAll(100, Arb.list(arbTransaction(), 2..10)) { transactions ->
            val balance1 = GetCounterpartyLedgerUseCase.calculateNetBalanceStatic(transactions)
            val balance2 = GetCounterpartyLedgerUseCase.calculateNetBalanceStatic(transactions.reversed())
            
            balance1.compareTo(balance2) shouldBe 0
        }
    }
})

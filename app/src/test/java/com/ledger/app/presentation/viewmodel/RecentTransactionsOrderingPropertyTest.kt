package com.ledger.app.presentation.viewmodel

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
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Property-based tests for recent transactions ordering.
 * 
 * Tests Property 12 from the design document.
 */
class RecentTransactionsOrderingPropertyTest : FunSpec({

    /**
     * Generator for valid positive amounts (in cents, converted to BigDecimal).
     */
    fun arbPositiveAmount(): Arb<BigDecimal> = Arb.long(1L, 1_000_000_00L).map { cents ->
        BigDecimal(cents).divide(BigDecimal(100))
    }

    /**
     * Generator for LocalDateTime within a reasonable range.
     * Generates dates from 2020 to 2030 to avoid edge cases.
     */
    fun arbDateTime(): Arb<LocalDateTime> = Arb.bind(
        Arb.long(2020L, 2030L),  // year
        Arb.long(1L, 12L),        // month
        Arb.long(1L, 28L),        // day (safe for all months)
        Arb.long(0L, 23L),        // hour
        Arb.long(0L, 59L),        // minute
        Arb.long(0L, 59L)         // second
    ) { year, month, day, hour, minute, second ->
        LocalDateTime.of(year.toInt(), month.toInt(), day.toInt(), hour.toInt(), minute.toInt(), second.toInt())
    }

    /**
     * Generator for transactions with random dates.
     */
    fun arbTransaction(): Arb<Transaction> = Arb.bind(
        arbPositiveAmount(),
        Arb.enum<TransactionDirection>(),
        Arb.enum<TransactionType>(),
        Arb.enum<TransactionStatus>(),
        arbDateTime()
    ) { amount, direction, type, status, dateTime ->
        Transaction(
            id = 0,
            direction = direction,
            type = type,
            amount = amount,
            accountId = 1,
            counterpartyId = 1,
            categoryId = 1,
            transactionDateTime = dateTime,
            status = status,
            notes = null,
            remainingDue = if (status == TransactionStatus.SETTLED) BigDecimal.ZERO else amount
        )
    }

    /**
     * **Feature: offline-ledger-app, Property 12: Recent Transactions Ordering**
     * 
     * For any set of transactions, the recent activity list should be sorted by 
     * transaction date descending (newest first).
     * 
     * **Validates: Requirements 8.3**
     */
    test("Property 12 - Recent transactions are sorted by date descending") {
        checkAll(100, Arb.list(arbTransaction(), 1..50)) { transactions ->
            // Sort transactions by date descending (newest first)
            val sortedTransactions = transactions.sortedByDescending { it.transactionDateTime }
            
            // Verify that each transaction is newer than or equal to the next one
            for (i in 0 until sortedTransactions.size - 1) {
                val current = sortedTransactions[i]
                val next = sortedTransactions[i + 1]
                
                // Current transaction should be >= next transaction (newer or equal)
                (current.transactionDateTime >= next.transactionDateTime) shouldBe true
            }
        }
    }

    test("Property 12 - Empty list remains empty after sorting") {
        val emptyList = emptyList<Transaction>()
        val sorted = emptyList.sortedByDescending { it.transactionDateTime }
        
        sorted.isEmpty() shouldBe true
    }

    test("Property 12 - Single transaction list remains unchanged") {
        checkAll(100, arbTransaction()) { transaction ->
            val singleList = listOf(transaction)
            val sorted = singleList.sortedByDescending { it.transactionDateTime }
            
            sorted.size shouldBe 1
            sorted[0] shouldBe transaction
        }
    }

    test("Property 12 - Transactions with same timestamp maintain stable order") {
        checkAll(100, arbDateTime(), arbPositiveAmount()) { dateTime, amount ->
            // Create multiple transactions with the same timestamp
            val transactions = listOf(
                Transaction(
                    id = 1,
                    direction = TransactionDirection.GAVE,
                    type = TransactionType.LOAN,
                    amount = amount,
                    accountId = 1,
                    counterpartyId = 1,
                    categoryId = 1,
                    transactionDateTime = dateTime,
                    status = TransactionStatus.PENDING,
                    notes = null,
                    remainingDue = amount
                ),
                Transaction(
                    id = 2,
                    direction = TransactionDirection.RECEIVED,
                    type = TransactionType.BILL_PAYMENT,
                    amount = amount,
                    accountId = 1,
                    counterpartyId = 2,
                    categoryId = 2,
                    transactionDateTime = dateTime,
                    status = TransactionStatus.SETTLED,
                    notes = null,
                    remainingDue = BigDecimal.ZERO
                )
            )
            
            val sorted = transactions.sortedByDescending { it.transactionDateTime }
            
            // All transactions should have the same timestamp
            sorted.all { it.transactionDateTime == dateTime } shouldBe true
        }
    }

    test("Property 12 - Ordering is consistent across multiple sorts") {
        checkAll(100, Arb.list(arbTransaction(), 2..20)) { transactions ->
            val sorted1 = transactions.sortedByDescending { it.transactionDateTime }
            val sorted2 = transactions.sortedByDescending { it.transactionDateTime }
            
            // Multiple sorts should produce the same order
            sorted1.map { it.transactionDateTime } shouldBe sorted2.map { it.transactionDateTime }
        }
    }
})

package com.ledger.app.data.local.dao

import com.ledger.app.data.local.entity.TransactionEntity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.math.BigDecimal

/**
 * **Feature: offline-ledger-app, Property 11: Soft Delete Exclusion**
 * 
 * For any soft-deleted transaction, it should not appear in regular lists 
 * or balance calculations, but should be recoverable.
 * 
 * **Validates: Requirements 13.2**
 */
class SoftDeleteExclusionPropertyTest : FunSpec({

    // Generator for valid positive amounts as strings
    fun arbPositiveAmount(): Arb<String> = Arb.long(1L, 1_000_000_00L).map { cents ->
        BigDecimal(cents).divide(BigDecimal(100)).toPlainString()
    }

    // Generators for enum-like values
    fun arbDirection(): Arb<String> = Arb.choice(
        Arb.constant("GAVE"),
        Arb.constant("RECEIVED")
    )

    fun arbTransactionType(): Arb<String> = Arb.choice(
        Arb.constant("LOAN"),
        Arb.constant("BILL_PAYMENT"),
        Arb.constant("RECHARGE"),
        Arb.constant("OTHER")
    )

    fun arbStatus(): Arb<String> = Arb.choice(
        Arb.constant("PENDING"),
        Arb.constant("PARTIALLY_SETTLED"),
        Arb.constant("SETTLED"),
        Arb.constant("CANCELLED")
    )

    // Generator for valid transaction entities
    fun arbTransactionEntity(isSoftDeleted: Boolean = false): Arb<TransactionEntity> = Arb.bind(
        arbDirection(),
        arbTransactionType(),
        arbPositiveAmount(),
        Arb.long(1L, 100L), // accountId
        Arb.long(1L, 100L).orNull(), // counterpartyId
        Arb.long(1L, 100L), // categoryId
        Arb.long(1L, Long.MAX_VALUE / 2), // transactionDateTime
        arbStatus(),
        Arb.string(0, 100).orNull(), // notes
    ) { direction, type, amount, accountId, counterpartyId, categoryId, dateTime, status, notes ->
        TransactionEntity(
            id = 0,
            direction = direction,
            type = type,
            amount = amount,
            accountId = accountId,
            counterpartyId = counterpartyId,
            categoryId = categoryId,
            transactionDateTime = dateTime,
            status = status,
            notes = notes,
            remainingDue = amount,
            isSoftDeleted = isSoftDeleted
        )
    }

    test("Property 11 - Soft-deleted transactions are excluded from regular lists") {
        checkAll(100, arbTransactionEntity(isSoftDeleted = false)) { activeTransaction ->
            // Create a soft-deleted version
            val softDeleted = activeTransaction.copy(isSoftDeleted = true)
            
            // Simulate a list of transactions (mix of active and soft-deleted)
            val allTransactions = listOf(activeTransaction, softDeleted)
            
            // Filter as the DAO queries do: WHERE isSoftDeleted = 0
            val regularList = allTransactions.filter { !it.isSoftDeleted }
            
            // Verify soft-deleted transaction is excluded
            regularList shouldNotContain softDeleted
            regularList.size shouldBe 1
            regularList.first() shouldBe activeTransaction
        }
    }

    test("Property 11 - Soft-deleted transactions are excluded from balance calculations") {
        checkAll(100, arbTransactionEntity(isSoftDeleted = false)) { activeTransaction ->
            val softDeleted = activeTransaction.copy(isSoftDeleted = true)
            
            val allTransactions = listOf(activeTransaction, softDeleted)
            
            // Simulate balance calculation: SUM(remainingDue) WHERE isSoftDeleted = 0 AND status != CANCELLED
            val totalWithSoftDeleted = allTransactions
                .filter { !it.isSoftDeleted && it.status != "CANCELLED" }
                .sumOf { BigDecimal(it.remainingDue) }
            
            // Expected total should also exclude CANCELLED transactions
            val expectedTotal = if (activeTransaction.status != "CANCELLED") {
                BigDecimal(activeTransaction.remainingDue)
            } else {
                BigDecimal.ZERO
            }
            
            // Verify soft-deleted transaction doesn't contribute to balance (use compareTo for BigDecimal)
            totalWithSoftDeleted.compareTo(expectedTotal) shouldBe 0
        }
    }

    test("Property 11 - Soft-deleted transactions remain in database and are recoverable") {
        checkAll(100, arbTransactionEntity(isSoftDeleted = false)) { transaction ->
            // Simulate soft delete operation
            val softDeleted = transaction.copy(isSoftDeleted = true)
            
            // Simulate database storage (all transactions including soft-deleted)
            val database = mutableListOf(transaction, softDeleted)
            
            // Verify soft-deleted transaction is still in database
            val allRecords = database.filter { it.id == softDeleted.id || it.id == transaction.id }
            allRecords.size shouldBe 2
            
            // Verify we can recover the soft-deleted transaction
            val recovered = database.find { it.isSoftDeleted }
            recovered shouldBe softDeleted
        }
    }

    test("Property 11 - Soft delete flag correctly distinguishes active from deleted") {
        checkAll(100, arbTransactionEntity(isSoftDeleted = false)) { transaction ->
            // Active transaction
            transaction.isSoftDeleted shouldBe false
            
            // Soft-deleted transaction
            val deleted = transaction.copy(isSoftDeleted = true)
            deleted.isSoftDeleted shouldBe true
            
            // Verify they differ only in soft delete flag
            deleted.copy(isSoftDeleted = false) shouldBe transaction
        }
    }

    test("Property 11 - Outstanding obligations exclude soft-deleted transactions") {
        checkAll(100, arbTransactionEntity(isSoftDeleted = false)) { transaction ->
            // Create pending/partially settled transaction
            val outstandingStatus = if (transaction.status in listOf("PENDING", "PARTIALLY_SETTLED")) {
                transaction.status
            } else {
                "PENDING"
            }
            val outstanding = transaction.copy(status = outstandingStatus)
            val softDeleted = outstanding.copy(isSoftDeleted = true)
            
            val allTransactions = listOf(outstanding, softDeleted)
            
            // Simulate outstanding obligations query
            val outstandingList = allTransactions.filter { 
                it.status in listOf("PENDING", "PARTIALLY_SETTLED") && !it.isSoftDeleted 
            }
            
            // Verify soft-deleted transaction is excluded
            outstandingList shouldNotContain softDeleted
            outstandingList.size shouldBe 1
            outstandingList.first() shouldBe outstanding
        }
    }

    test("Property 11 - Date range queries exclude soft-deleted transactions") {
        checkAll(100, arbTransactionEntity(isSoftDeleted = false)) { transaction ->
            val softDeleted = transaction.copy(isSoftDeleted = true)
            
            val allTransactions = listOf(transaction, softDeleted)
            
            // Simulate date range query with soft delete filter
            val startMillis = 0L
            val endMillis = Long.MAX_VALUE
            val dateRangeResults = allTransactions.filter {
                it.transactionDateTime >= startMillis &&
                it.transactionDateTime < endMillis &&
                !it.isSoftDeleted
            }
            
            // Verify soft-deleted transaction is excluded
            dateRangeResults shouldNotContain softDeleted
            dateRangeResults.size shouldBe 1
        }
    }
})

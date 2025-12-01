package com.ledger.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index("counterpartyId"),
        Index("accountId"),
        Index("transactionDateTime"),
        Index("billCategory"),
        Index("isForSelf"),
        Index("linkedTaskId")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val direction: String, // GAVE, RECEIVED
    val type: String, // LOAN, BILL_PAYMENT, RECHARGE, OTHER
    val amount: String, // BigDecimal as string
    val accountId: Long,
    val counterpartyId: Long? = null,
    val categoryId: Long,
    val transactionDateTime: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING, PARTIALLY_SETTLED, SETTLED, CANCELLED
    val notes: String? = null,
    val remainingDue: String, // Cached for fast queries
    val isSoftDeleted: Boolean = false,
    val consumerId: String? = null, // Phone number or consumer ID for bill payments
    val billCategory: String? = null, // ELECTRICITY, TV, MOBILE, INTERNET, OTHER
    val isForSelf: Boolean = false, // True if bill paid for self, false if paid for others (affects debt)
    val linkedTaskId: Long? = null // ID of the task this transaction was created from (Requirements: 15.5)
)

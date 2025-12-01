package com.ledger.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "partial_payments",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentTransactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("parentTransactionId")]
)
data class PartialPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentTransactionId: Long,
    val amount: String, // BigDecimal as string
    val direction: String, // FROM_COUNTERPARTY, TO_COUNTERPARTY
    val dateTime: Long,
    val method: String, // CASH, UPI, BANK, OTHER
    val notes: String? = null
)

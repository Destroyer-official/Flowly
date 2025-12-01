package com.ledger.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a task/todo item.
 * Supports linking to counterparties and transactions for financial tasks.
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index("status"),
        Index("priority"),
        Index("dueDate"),
        Index("linkedCounterpartyId"),
        Index("linkedTransactionId")
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val priority: String = "MEDIUM", // HIGH, MEDIUM, LOW
    val dueDate: Long? = null, // Optional due date as epoch millis
    val status: String = "PENDING", // PENDING, COMPLETED, CANCELLED
    val linkedCounterpartyId: Long? = null, // Optional link to counterparty for financial tasks
    val linkedTransactionId: Long? = null, // Optional link to transaction created from this task
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null // Timestamp when task was completed
)

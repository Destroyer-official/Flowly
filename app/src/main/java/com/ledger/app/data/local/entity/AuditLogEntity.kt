package com.ledger.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing audit logs of all data modifications.
 * Tracks CREATE, UPDATE, DELETE, PARTIAL_PAYMENT, BACKUP, RESTORE actions.
 * 
 * **Feature: offline-ledger-app, Property 14: Audit Log Completeness**
 * **Validates: Requirements 7.3**
 */
@Entity(
    tableName = "audit_logs",
    indices = [
        Index("timestamp"),
        Index("entityType"),
        Index("action")
    ]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String, // CREATE, UPDATE, DELETE, PARTIAL_PAYMENT, BACKUP, RESTORE
    val entityType: String, // TRANSACTION, COUNTERPARTY, ACCOUNT, REMINDER, PARTIAL_PAYMENT
    val entityId: Long,
    val oldValue: String? = null, // JSON of old state (null for CREATE)
    val newValue: String? = null, // JSON of new state (null for DELETE)
    val timestamp: Long = System.currentTimeMillis(),
    val details: String? = null // Additional context or description
)

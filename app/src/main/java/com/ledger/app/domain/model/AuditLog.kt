package com.ledger.app.domain.model

import java.time.LocalDateTime

/**
 * Domain model representing an audit log entry.
 * 
 * **Feature: offline-ledger-app, Property 14: Audit Log Completeness**
 * **Validates: Requirements 7.3**
 */
data class AuditLog(
    val id: Long = 0,
    val action: AuditAction,
    val entityType: String,
    val entityId: Long,
    val oldValue: String? = null, // JSON of old state (null for CREATE)
    val newValue: String? = null, // JSON of new state (null for DELETE)
    val timestamp: LocalDateTime,
    val details: String? = null // Additional context or description
)

/**
 * Actions that can be logged in the audit system.
 */
enum class AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    PARTIAL_PAYMENT,
    BACKUP,
    RESTORE
}

/**
 * Entity types that can be audited.
 */
object AuditEntityType {
    const val TRANSACTION = "TRANSACTION"
    const val COUNTERPARTY = "COUNTERPARTY"
    const val ACCOUNT = "ACCOUNT"
    const val REMINDER = "REMINDER"
    const val PARTIAL_PAYMENT = "PARTIAL_PAYMENT"
    const val CATEGORY = "CATEGORY"
}

package com.ledger.app.data.local.backup

import com.ledger.app.data.local.entity.AccountEntity
import com.ledger.app.data.local.entity.AuditLogEntity
import com.ledger.app.data.local.entity.CategoryEntity
import com.ledger.app.data.local.entity.CounterpartyEntity
import com.ledger.app.data.local.entity.PartialPaymentEntity
import com.ledger.app.data.local.entity.ReminderEntity
import com.ledger.app.data.local.entity.TransactionEntity
import kotlinx.serialization.Serializable

/**
 * Data class representing a complete backup of all ledger data.
 * Used for JSON serialization/deserialization during backup and restore operations.
 * 
 * **Feature: offline-ledger-app, Property 15: Backup Integrity**
 * **Validates: Requirements 13.1, 13.2**
 */
@Serializable
data class BackupData(
    val version: Int = CURRENT_VERSION,
    val createdAt: Long = System.currentTimeMillis(),
    val transactions: List<TransactionBackup>,
    val partialPayments: List<PartialPaymentBackup>,
    val counterparties: List<CounterpartyBackup>,
    val accounts: List<AccountBackup>,
    val categories: List<CategoryBackup>,
    val reminders: List<ReminderBackup>,
    val auditLogs: List<AuditLogBackup> = emptyList()
) {
    /**
     * Returns the total number of records in this backup.
     */
    fun totalRecords(): Int = transactions.size + partialPayments.size + 
        counterparties.size + accounts.size + categories.size + 
        reminders.size + auditLogs.size

    companion object {
        const val CURRENT_VERSION = 1
    }
}

/**
 * Serializable backup representation of TransactionEntity.
 */
@Serializable
data class TransactionBackup(
    val id: Long,
    val direction: String,
    val type: String,
    val amount: String,
    val accountId: Long,
    val counterpartyId: Long?,
    val categoryId: Long,
    val transactionDateTime: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val status: String,
    val notes: String?,
    val remainingDue: String,
    val isSoftDeleted: Boolean,
    val consumerId: String?,
    val billCategory: String?,
    val isForSelf: Boolean
)


/**
 * Serializable backup representation of PartialPaymentEntity.
 */
@Serializable
data class PartialPaymentBackup(
    val id: Long,
    val parentTransactionId: Long,
    val amount: String,
    val direction: String,
    val dateTime: Long,
    val method: String,
    val notes: String?
)

/**
 * Serializable backup representation of CounterpartyEntity.
 */
@Serializable
data class CounterpartyBackup(
    val id: Long,
    val displayName: String,
    val phoneNumber: String?,
    val notes: String?,
    val isFavorite: Boolean,
    val createdAt: Long
)

/**
 * Serializable backup representation of AccountEntity.
 */
@Serializable
data class AccountBackup(
    val id: Long,
    val name: String,
    val type: String,
    val isActive: Boolean
)

/**
 * Serializable backup representation of CategoryEntity.
 */
@Serializable
data class CategoryBackup(
    val id: Long,
    val name: String,
    val iconName: String,
    val colorKey: String,
    val isBillCategory: Boolean
)

/**
 * Serializable backup representation of ReminderEntity.
 */
@Serializable
data class ReminderBackup(
    val id: Long,
    val targetType: String,
    val targetId: Long?,
    val title: String,
    val description: String?,
    val dueDateTime: Long,
    val repeatPattern: String,
    val status: String,
    val ignoredCount: Int
)

/**
 * Serializable backup representation of AuditLogEntity.
 */
@Serializable
data class AuditLogBackup(
    val id: Long,
    val action: String,
    val entityType: String,
    val entityId: Long,
    val oldValue: String?,
    val newValue: String?,
    val timestamp: Long,
    val details: String?
)

// Extension functions to convert entities to backup models

fun TransactionEntity.toBackup() = TransactionBackup(
    id = id,
    direction = direction,
    type = type,
    amount = amount,
    accountId = accountId,
    counterpartyId = counterpartyId,
    categoryId = categoryId,
    transactionDateTime = transactionDateTime,
    createdAt = createdAt,
    updatedAt = updatedAt,
    status = status,
    notes = notes,
    remainingDue = remainingDue,
    isSoftDeleted = isSoftDeleted,
    consumerId = consumerId,
    billCategory = billCategory,
    isForSelf = isForSelf
)

fun TransactionBackup.toEntity() = TransactionEntity(
    id = id,
    direction = direction,
    type = type,
    amount = amount,
    accountId = accountId,
    counterpartyId = counterpartyId,
    categoryId = categoryId,
    transactionDateTime = transactionDateTime,
    createdAt = createdAt,
    updatedAt = updatedAt,
    status = status,
    notes = notes,
    remainingDue = remainingDue,
    isSoftDeleted = isSoftDeleted,
    consumerId = consumerId,
    billCategory = billCategory,
    isForSelf = isForSelf
)

fun PartialPaymentEntity.toBackup() = PartialPaymentBackup(
    id = id,
    parentTransactionId = parentTransactionId,
    amount = amount,
    direction = direction,
    dateTime = dateTime,
    method = method,
    notes = notes
)

fun PartialPaymentBackup.toEntity() = PartialPaymentEntity(
    id = id,
    parentTransactionId = parentTransactionId,
    amount = amount,
    direction = direction,
    dateTime = dateTime,
    method = method,
    notes = notes
)

fun CounterpartyEntity.toBackup() = CounterpartyBackup(
    id = id,
    displayName = displayName,
    phoneNumber = phoneNumber,
    notes = notes,
    isFavorite = isFavorite,
    createdAt = createdAt
)

fun CounterpartyBackup.toEntity() = CounterpartyEntity(
    id = id,
    displayName = displayName,
    phoneNumber = phoneNumber,
    notes = notes,
    isFavorite = isFavorite,
    createdAt = createdAt
)

fun AccountEntity.toBackup() = AccountBackup(
    id = id,
    name = name,
    type = type,
    isActive = isActive
)

fun AccountBackup.toEntity() = AccountEntity(
    id = id,
    name = name,
    type = type,
    isActive = isActive
)

fun CategoryEntity.toBackup() = CategoryBackup(
    id = id,
    name = name,
    iconName = iconName,
    colorKey = colorKey,
    isBillCategory = isBillCategory
)

fun CategoryBackup.toEntity() = CategoryEntity(
    id = id,
    name = name,
    iconName = iconName,
    colorKey = colorKey,
    isBillCategory = isBillCategory
)

fun ReminderEntity.toBackup() = ReminderBackup(
    id = id,
    targetType = targetType,
    targetId = targetId,
    title = title,
    description = description,
    dueDateTime = dueDateTime,
    repeatPattern = repeatPattern,
    status = status,
    ignoredCount = ignoredCount
)

fun ReminderBackup.toEntity() = ReminderEntity(
    id = id,
    targetType = targetType,
    targetId = targetId,
    title = title,
    description = description,
    dueDateTime = dueDateTime,
    repeatPattern = repeatPattern,
    status = status,
    ignoredCount = ignoredCount
)

fun AuditLogEntity.toBackup() = AuditLogBackup(
    id = id,
    action = action,
    entityType = entityType,
    entityId = entityId,
    oldValue = oldValue,
    newValue = newValue,
    timestamp = timestamp,
    details = details
)

fun AuditLogBackup.toEntity() = AuditLogEntity(
    id = id,
    action = action,
    entityType = entityType,
    entityId = entityId,
    oldValue = oldValue,
    newValue = newValue,
    timestamp = timestamp,
    details = details
)

package com.ledger.app.domain.repository

import com.ledger.app.domain.model.AuditAction
import com.ledger.app.domain.model.AuditLog
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Repository interface for audit log operations.
 * 
 * **Feature: offline-ledger-app, Property 14: Audit Log Completeness**
 * **Validates: Requirements 7.3, 7.4**
 */
interface AuditLogRepository {
    /**
     * Log an action with old and new values as JSON.
     */
    suspend fun logAction(
        action: AuditAction,
        entityType: String,
        entityId: Long,
        oldValue: String? = null,
        newValue: String? = null,
        details: String? = null
    )
    
    /**
     * Get all audit logs ordered by timestamp descending.
     */
    fun getAuditLogs(): Flow<List<AuditLog>>
    
    /**
     * Get recent audit logs with a limit.
     */
    fun getRecent(limit: Int): Flow<List<AuditLog>>
    
    /**
     * Search logs by query string (searches in details, oldValue, newValue, entityType).
     */
    fun searchLogs(query: String): Flow<List<AuditLog>>
    
    /**
     * Search logs synchronously for export.
     */
    suspend fun searchLogsSync(query: String): List<AuditLog>
    
    /**
     * Export all audit logs to a file.
     */
    suspend fun exportAuditLog(): File
    
    /**
     * Get total count of audit logs.
     */
    suspend fun getCount(): Int
    
    /**
     * Get logs by date range.
     */
    fun getByDateRange(startMillis: Long, endMillis: Long): Flow<List<AuditLog>>
    
    /**
     * Get logs by entity type.
     */
    fun getByEntityType(entityType: String): Flow<List<AuditLog>>
    
    /**
     * Get logs for a specific entity.
     */
    fun getByEntity(entityType: String, entityId: Long): Flow<List<AuditLog>>
}

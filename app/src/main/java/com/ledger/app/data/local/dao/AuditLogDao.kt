package com.ledger.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ledger.app.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for audit log operations.
 * Provides methods to log and query all data modifications.
 * 
 * **Feature: offline-ledger-app, Property 14: Audit Log Completeness**
 * **Validates: Requirements 7.3, 7.4**
 */
@Dao
interface AuditLogDao {
    @Insert
    suspend fun insert(auditLog: AuditLogEntity): Long

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAll(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    suspend fun getAllSync(): List<AuditLogEntity>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<AuditLogEntity>>

    @Query("""
        SELECT * FROM audit_logs 
        WHERE details LIKE '%' || :query || '%'
           OR oldValue LIKE '%' || :query || '%'
           OR newValue LIKE '%' || :query || '%'
           OR entityType LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchLogs(query: String): Flow<List<AuditLogEntity>>

    @Query("""
        SELECT * FROM audit_logs 
        WHERE details LIKE '%' || :query || '%'
           OR oldValue LIKE '%' || :query || '%'
           OR newValue LIKE '%' || :query || '%'
           OR entityType LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    suspend fun searchLogsSync(query: String): List<AuditLogEntity>

    @Query("""
        SELECT * FROM audit_logs 
        WHERE timestamp >= :startMillis AND timestamp < :endMillis
        ORDER BY timestamp DESC
    """)
    fun getByDateRange(startMillis: Long, endMillis: Long): Flow<List<AuditLogEntity>>

    @Query("""
        SELECT * FROM audit_logs 
        WHERE entityType = :entityType
        ORDER BY timestamp DESC
    """)
    fun getByEntityType(entityType: String): Flow<List<AuditLogEntity>>

    @Query("""
        SELECT * FROM audit_logs 
        WHERE entityType = :entityType AND entityId = :entityId
        ORDER BY timestamp DESC
    """)
    fun getByEntity(entityType: String, entityId: Long): Flow<List<AuditLogEntity>>

    @Query("SELECT COUNT(*) FROM audit_logs")
    suspend fun getCount(): Int

    @Query("SELECT * FROM audit_logs WHERE action = :action ORDER BY timestamp DESC")
    fun getByAction(action: String): Flow<List<AuditLogEntity>>

    // Backup/Restore operations
    @Query("SELECT * FROM audit_logs")
    suspend fun getAllForBackup(): List<AuditLogEntity>

    @Query("DELETE FROM audit_logs")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(auditLogs: List<AuditLogEntity>)
}

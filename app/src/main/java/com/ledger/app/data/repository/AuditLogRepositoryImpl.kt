package com.ledger.app.data.repository

import android.content.Context
import com.ledger.app.data.local.dao.AuditLogDao
import com.ledger.app.data.local.entity.AuditLogEntity
import com.ledger.app.domain.model.AuditAction
import com.ledger.app.domain.model.AuditLog
import com.ledger.app.domain.repository.AuditLogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Implementation of AuditLogRepository.
 * 
 * **Feature: offline-ledger-app, Property 14: Audit Log Completeness**
 * **Validates: Requirements 7.3, 7.4**
 */
class AuditLogRepositoryImpl @Inject constructor(
    private val auditLogDao: AuditLogDao,
    @ApplicationContext private val context: Context
) : AuditLogRepository {

    private val exportDir: File
        get() = File(context.filesDir, "exports").apply { mkdirs() }

    override suspend fun logAction(
        action: AuditAction,
        entityType: String,
        entityId: Long,
        oldValue: String?,
        newValue: String?,
        details: String?
    ) {
        val entity = AuditLogEntity(
            action = action.name,
            entityType = entityType,
            entityId = entityId,
            oldValue = oldValue,
            newValue = newValue,
            details = details
        )
        auditLogDao.insert(entity)
    }

    override fun getAuditLogs(): Flow<List<AuditLog>> {
        return auditLogDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }


    override fun getRecent(limit: Int): Flow<List<AuditLog>> {
        return auditLogDao.getRecent(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchLogs(query: String): Flow<List<AuditLog>> {
        return auditLogDao.searchLogs(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun searchLogsSync(query: String): List<AuditLog> {
        return auditLogDao.searchLogsSync(query).map { it.toDomain() }
    }

    override suspend fun exportAuditLog(): File = withContext(Dispatchers.IO) {
        val logs = auditLogDao.getAllSync()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val exportFile = File(exportDir, "audit_log_$timestamp.txt")
        
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        
        exportFile.bufferedWriter().use { writer ->
            writer.write("=== LEDGER APP AUDIT LOG ===\n")
            writer.write("Exported: ${LocalDateTime.now().format(dateFormatter)}\n")
            writer.write("Total Entries: ${logs.size}\n")
            writer.write("=" .repeat(50) + "\n\n")
            
            logs.forEach { log ->
                val logTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(log.timestamp),
                    ZoneId.systemDefault()
                )
                writer.write("[${ logTime.format(dateFormatter)}] ${log.action}\n")
                writer.write("  Entity: ${log.entityType} (ID: ${log.entityId})\n")
                log.oldValue?.let { writer.write("  Old Value: $it\n") }
                log.newValue?.let { writer.write("  New Value: $it\n") }
                log.details?.let { writer.write("  Details: $it\n") }
                writer.write("-".repeat(50) + "\n")
            }
        }
        
        exportFile
    }

    override suspend fun getCount(): Int {
        return auditLogDao.getCount()
    }

    override fun getByDateRange(startMillis: Long, endMillis: Long): Flow<List<AuditLog>> {
        return auditLogDao.getByDateRange(startMillis, endMillis).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getByEntityType(entityType: String): Flow<List<AuditLog>> {
        return auditLogDao.getByEntityType(entityType).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getByEntity(entityType: String, entityId: Long): Flow<List<AuditLog>> {
        return auditLogDao.getByEntity(entityType, entityId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private fun AuditLogEntity.toDomain(): AuditLog {
        return AuditLog(
            id = id,
            action = AuditAction.valueOf(action),
            entityType = entityType,
            entityId = entityId,
            oldValue = oldValue,
            newValue = newValue,
            timestamp = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
            ),
            details = details
        )
    }
}

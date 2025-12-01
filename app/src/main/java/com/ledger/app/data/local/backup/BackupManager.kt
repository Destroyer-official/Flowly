package com.ledger.app.data.local.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.ledger.app.data.local.db.LedgerDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages backup and restore operations for the ledger database.
 * Exports database to JSON format and restores from backup files.
 * 
 * **Feature: offline-ledger-app, Property 15: Backup Integrity**
 * **Validates: Requirements 13.1, 13.2**
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: LedgerDatabase
) {
    private val backupDir: File
        get() = File(context.filesDir, "backups").apply { mkdirs() }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Creates a JSON backup of all database data.
     * Saves to Downloads folder for user accessibility.
     * @return BackupResult with success status and file path
     */
    suspend fun createBackup(): BackupResult = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "ledger_backup_$timestamp.json"

            // Collect all data from database
            val backupData = BackupData(
                transactions = database.transactionDao().getAllForBackup().map { it.toBackup() },
                partialPayments = database.partialPaymentDao().getAllForBackup().map { it.toBackup() },
                counterparties = database.counterpartyDao().getAllForBackup().map { it.toBackup() },
                accounts = database.accountDao().getAllForBackup().map { it.toBackup() },
                categories = database.categoryDao().getAllForBackup().map { it.toBackup() },
                reminders = database.reminderDao().getAllForBackup().map { it.toBackup() },
                auditLogs = database.auditLogDao().getAllForBackup().map { it.toBackup() }
            )

            // Serialize to JSON
            val jsonContent = json.encodeToString(backupData)
            
            // Save to Downloads folder using MediaStore for Android 10+
            val savedPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToDownloadsMediaStore(fileName, jsonContent)
            } else {
                saveToDownloadsLegacy(fileName, jsonContent)
            }

            // Also save to internal storage for quick access
            val internalBackupFile = File(backupDir, fileName)
            internalBackupFile.writeText(jsonContent)
            
            // Create checksum
            val checksum = calculateChecksum(internalBackupFile)
            File(backupDir, "ledger_backup_$timestamp.checksum").writeText(checksum)

            BackupResult(
                success = true,
                filePath = savedPath,
                timestamp = Date(),
                checksum = checksum,
                recordCount = backupData.totalRecords()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            BackupResult(
                success = false,
                errorMessage = "Backup failed: ${e.message}"
            )
        }
    }

    /**
     * Saves backup to Downloads folder using MediaStore (Android 10+).
     */
    private fun saveToDownloadsMediaStore(fileName: String, content: String): String {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/LedgerBackups")
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw Exception("Failed to create backup file in Downloads")

        resolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(content.toByteArray())
        } ?: throw Exception("Failed to write backup file")

        return "Downloads/LedgerBackups/$fileName"
    }

    /**
     * Saves backup to Downloads folder for older Android versions.
     */
    @Suppress("DEPRECATION")
    private fun saveToDownloadsLegacy(fileName: String, content: String): String {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val backupDir = File(downloadsDir, "LedgerBackups").apply { mkdirs() }
        val backupFile = File(backupDir, fileName)
        backupFile.writeText(content)
        return backupFile.absolutePath
    }

    /**
     * Creates a backup and writes to a user-selected URI (via SAF).
     */
    suspend fun createBackupToUri(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            // Collect all data from database
            val backupData = BackupData(
                transactions = database.transactionDao().getAllForBackup().map { it.toBackup() },
                partialPayments = database.partialPaymentDao().getAllForBackup().map { it.toBackup() },
                counterparties = database.counterpartyDao().getAllForBackup().map { it.toBackup() },
                accounts = database.accountDao().getAllForBackup().map { it.toBackup() },
                categories = database.categoryDao().getAllForBackup().map { it.toBackup() },
                reminders = database.reminderDao().getAllForBackup().map { it.toBackup() },
                auditLogs = database.auditLogDao().getAllForBackup().map { it.toBackup() }
            )

            // Serialize to JSON
            val jsonContent = json.encodeToString(backupData)

            // Write to the selected URI
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonContent.toByteArray())
            } ?: throw Exception("Failed to write to selected location")

            // Also save to internal storage
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val internalBackupFile = File(backupDir, "ledger_backup_$timestamp.json")
            internalBackupFile.writeText(jsonContent)

            BackupResult(
                success = true,
                filePath = uri.toString(),
                timestamp = Date(),
                recordCount = backupData.totalRecords()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            BackupResult(
                success = false,
                errorMessage = "Backup failed: ${e.message}"
            )
        }
    }

    /**
     * Restores database from a JSON backup file.
     * @param backupFile The backup file to restore from
     * @return RestoreResult with success status
     */
    suspend fun restoreBackup(backupFile: File): RestoreResult = withContext(Dispatchers.IO) {
        try {
            if (!backupFile.exists()) {
                return@withContext RestoreResult(
                    success = false,
                    errorMessage = "Backup file not found"
                )
            }

            // Validate backup file
            if (!validateBackupFile(backupFile)) {
                return@withContext RestoreResult(
                    success = false,
                    errorMessage = "Invalid or corrupted backup file"
                )
            }

            // Parse JSON backup
            val jsonContent = backupFile.readText()
            val backupData = json.decodeFromString<BackupData>(jsonContent)

            // Restore data - clear existing data first (order matters due to foreign keys)
            database.partialPaymentDao().deleteAll()
            database.transactionDao().deleteAll()
            database.reminderDao().deleteAll()
            database.counterpartyDao().deleteAll()
            database.accountDao().deleteAll()
            database.categoryDao().deleteAll()
            database.auditLogDao().deleteAll()

            // Restore data (order matters due to foreign keys)
            database.categoryDao().insertAll(backupData.categories.map { it.toEntity() })
            database.accountDao().insertAll(backupData.accounts.map { it.toEntity() })
            database.counterpartyDao().insertAll(backupData.counterparties.map { it.toEntity() })
            database.transactionDao().insertAll(backupData.transactions.map { it.toEntity() })
            database.partialPaymentDao().insertAll(backupData.partialPayments.map { it.toEntity() })
            database.reminderDao().insertAll(backupData.reminders.map { it.toEntity() })
            database.auditLogDao().insertAll(backupData.auditLogs.map { it.toEntity() })

            RestoreResult(
                success = true,
                recordsRestored = backupData.totalRecords()
            )
        } catch (e: Exception) {
            RestoreResult(
                success = false,
                errorMessage = "Restore failed: ${e.message}"
            )
        }
    }

    /**
     * Restores database from a content URI (for file picker).
     */
    suspend fun restoreBackup(uri: Uri): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext RestoreResult(
                    success = false,
                    errorMessage = "Cannot open backup file"
                )

            val tempFile = File(context.cacheDir, "temp_restore.json")
            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            val result = restoreBackup(tempFile)
            tempFile.delete()
            result
        } catch (e: Exception) {
            RestoreResult(
                success = false,
                errorMessage = "Restore failed: ${e.message}"
            )
        }
    }

    /**
     * Validates a backup file's integrity.
     */
    suspend fun validateBackupFile(backupFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!backupFile.exists()) return@withContext false
            
            val jsonContent = backupFile.readText()
            
            // Try to parse as BackupData
            val backupData = json.decodeFromString<BackupData>(jsonContent)
            
            // Basic validation
            backupData.version > 0 && backupData.createdAt > 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets list of available backup files.
     */
    fun getBackupFiles(): List<BackupFileInfo> {
        return backupDir.listFiles { file -> 
            file.extension == "json" && file.name.startsWith("ledger_backup_")
        }?.sortedByDescending { it.lastModified() }?.map { file ->
            BackupFileInfo(
                file = file,
                name = file.name,
                size = file.length(),
                lastModified = Date(file.lastModified())
            )
        } ?: emptyList()
    }

    /**
     * Gets the most recent backup file info.
     */
    fun getLastBackupInfo(): BackupFileInfo? {
        return getBackupFiles().firstOrNull()
    }

    /**
     * Deletes a backup file.
     */
    fun deleteBackup(file: File): Boolean {
        val checksumFile = File(file.parent, file.nameWithoutExtension + ".checksum")
        checksumFile.delete()
        return file.delete()
    }

    private fun calculateChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

/**
 * Information about a backup file.
 */
data class BackupFileInfo(
    val file: File,
    val name: String,
    val size: Long,
    val lastModified: Date
)

/**
 * Result of a backup operation.
 */
data class BackupResult(
    val success: Boolean,
    val filePath: String? = null,
    val timestamp: Date? = null,
    val checksum: String? = null,
    val recordCount: Int = 0,
    val errorMessage: String? = null
)

/**
 * Result of a restore operation.
 */
data class RestoreResult(
    val success: Boolean,
    val recordsRestored: Int = 0,
    val errorMessage: String? = null
)

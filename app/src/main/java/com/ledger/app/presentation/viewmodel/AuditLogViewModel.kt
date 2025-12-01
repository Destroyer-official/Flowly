package com.ledger.app.presentation.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.app.domain.model.AuditLog
import com.ledger.app.domain.repository.AuditLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for the Audit Log screen.
 * 
 * Features:
 * - List all audit logs
 * - Search by date, amount, or note text
 * - Export audit log to file
 * 
 * Requirements: 7.3, 7.4
 */
@HiltViewModel
class AuditLogViewModel @Inject constructor(
    private val auditLogRepository: AuditLogRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuditLogUiState>(AuditLogUiState.Loading)
    val uiState: StateFlow<AuditLogUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    init {
        loadAuditLogs()
    }

    /**
     * Loads all audit logs.
     */
    private fun loadAuditLogs() {
        viewModelScope.launch {
            auditLogRepository.getAuditLogs()
                .catch { e ->
                    _uiState.value = AuditLogUiState.Error(e.message ?: "Unknown error")
                }
                .collect { logs ->
                    _uiState.value = AuditLogUiState.Success(logs)
                }
        }
    }


    /**
     * Updates the search query and filters logs.
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            loadAuditLogs()
        } else {
            searchLogs(query)
        }
    }

    /**
     * Searches logs by query string.
     */
    private fun searchLogs(query: String) {
        viewModelScope.launch {
            auditLogRepository.searchLogs(query)
                .catch { e ->
                    _uiState.value = AuditLogUiState.Error(e.message ?: "Search failed")
                }
                .collect { logs ->
                    _uiState.value = AuditLogUiState.Success(logs)
                }
        }
    }

    /**
     * Exports audit logs to a file and shares it.
     */
    fun exportAuditLog() {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting
            try {
                val file = auditLogRepository.exportAuditLog()
                _exportState.value = ExportState.Success(file)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Export failed")
            }
        }
    }

    /**
     * Shares the exported file.
     */
    fun shareExportedFile(file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Clears the export state.
     */
    fun clearExportState() {
        _exportState.value = ExportState.Idle
    }

    /**
     * Clears the search query.
     */
    fun clearSearch() {
        _searchQuery.value = ""
        loadAuditLogs()
    }
}

/**
 * UI state for the Audit Log screen.
 */
sealed class AuditLogUiState {
    object Loading : AuditLogUiState()
    data class Success(val logs: List<AuditLog>) : AuditLogUiState()
    data class Error(val message: String) : AuditLogUiState()
}

/**
 * Export state for audit log export.
 */
sealed class ExportState {
    object Idle : ExportState()
    object Exporting : ExportState()
    data class Success(val file: File) : ExportState()
    data class Error(val message: String) : ExportState()
}

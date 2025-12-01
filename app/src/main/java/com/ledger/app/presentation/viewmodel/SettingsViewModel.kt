package com.ledger.app.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.app.data.local.PreferencesManager
import com.ledger.app.data.local.backup.BackupFileInfo
import com.ledger.app.data.local.backup.BackupManager
import com.ledger.app.domain.model.Account
import com.ledger.app.domain.repository.AccountRepository
import com.ledger.app.presentation.theme.DesignSkin
import com.ledger.app.presentation.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Settings screen.
 * Manages theme mode, design skin, default account preferences, and backup/restore operations.
 * 
 * Requirements: 9.1, 9.2, 12.3, 13.1, 13.2
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val accountRepository: AccountRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = preferencesManager.themeMode

    val designSkin: StateFlow<DesignSkin> = preferencesManager.designSkin

    private val defaultAccountId: StateFlow<Long?> = preferencesManager.defaultAccountId

    val activeAccounts: StateFlow<List<Account>> = accountRepository.getActive()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val defaultAccount: StateFlow<Account?> = combine(
        defaultAccountId,
        activeAccounts
    ) { accountId, accounts ->
        accountId?.let { id ->
            accounts.find { it.id == id }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Backup state
    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    private val _lastBackupInfo = MutableStateFlow<BackupFileInfo?>(null)
    val lastBackupInfo: StateFlow<BackupFileInfo?> = _lastBackupInfo.asStateFlow()

    init {
        refreshLastBackupInfo()
    }

    fun setThemeMode(mode: ThemeMode) {
        preferencesManager.setThemeMode(mode)
    }

    fun setDesignSkin(skin: DesignSkin) {
        preferencesManager.setDesignSkin(skin)
    }

    fun setDefaultAccount(accountId: Long?) {
        preferencesManager.setDefaultAccountId(accountId)
    }

    /**
     * Creates a backup of all database data to internal storage.
     */
    fun createBackup() {
        viewModelScope.launch {
            _backupState.value = BackupState.InProgress("Creating backup...")
            val result = backupManager.createBackup()
            if (result.success) {
                _backupState.value = BackupState.Success(
                    message = "Backup saved to: ${result.filePath}",
                    recordCount = result.recordCount
                )
                refreshLastBackupInfo()
            } else {
                _backupState.value = BackupState.Error(
                    message = result.errorMessage ?: "Backup failed"
                )
            }
        }
    }

    /**
     * Creates a backup to a user-selected location via SAF.
     */
    fun createBackupToUri(uri: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupState.InProgress("Creating backup...")
            val result = backupManager.createBackupToUri(uri)
            if (result.success) {
                _backupState.value = BackupState.Success(
                    message = "Backup saved successfully!",
                    recordCount = result.recordCount
                )
                refreshLastBackupInfo()
            } else {
                _backupState.value = BackupState.Error(
                    message = result.errorMessage ?: "Backup failed"
                )
            }
        }
    }

    /**
     * Restores database from a backup file URI.
     */
    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupState.InProgress("Restoring backup...")
            val result = backupManager.restoreBackup(uri)
            if (result.success) {
                _backupState.value = BackupState.Success(
                    message = "Backup restored successfully",
                    recordCount = result.recordsRestored
                )
            } else {
                _backupState.value = BackupState.Error(
                    message = result.errorMessage ?: "Restore failed"
                )
            }
        }
    }

    /**
     * Clears the backup state after user acknowledges.
     */
    fun clearBackupState() {
        _backupState.value = BackupState.Idle
    }

    private fun refreshLastBackupInfo() {
        _lastBackupInfo.value = backupManager.getLastBackupInfo()
    }
}

/**
 * Represents the state of backup/restore operations.
 */
sealed class BackupState {
    data object Idle : BackupState()
    data class InProgress(val message: String) : BackupState()
    data class Success(val message: String, val recordCount: Int) : BackupState()
    data class Error(val message: String) : BackupState()
}

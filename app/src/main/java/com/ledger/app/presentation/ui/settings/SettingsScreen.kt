package com.ledger.app.presentation.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ledger.app.data.local.backup.BackupFileInfo
import com.ledger.app.presentation.theme.DesignSkin
import com.ledger.app.presentation.theme.LocalSkinColors
import com.ledger.app.presentation.theme.LocalSkinShapes
import com.ledger.app.presentation.theme.ThemeMode
import com.ledger.app.presentation.viewmodel.BackupState
import com.ledger.app.presentation.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Settings screen for managing app preferences.
 * 
 * Features:
 * - Theme mode selector (Light, Dark, System)
 * - Design skin selector with preview
 * - Default account selector
 * - Manage accounts navigation
 * 
 * Requirements: 9.1, 9.2, 12.3
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToAccountManagement: () -> Unit = {},
    onNavigateToAuditLog: () -> Unit = {}
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val designSkin by viewModel.designSkin.collectAsState()
    val defaultAccount by viewModel.defaultAccount.collectAsState()
    val activeAccounts by viewModel.activeAccounts.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    val lastBackupInfo by viewModel.lastBackupInfo.collectAsState()

    // File picker for restore
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.restoreBackup(it) }
    }

    // File creator for backup (SAF - no permissions needed)
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.createBackupToUri(it) }
    }

    // Show dialog for backup state
    BackupStateDialog(
        state = backupState,
        onDismiss = { viewModel.clearBackupState() }
    )
    
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Text(
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = LocalSkinColors.current.textPrimary
            )
            
            // Appearance Section
            SettingsSection(title = "Appearance") {
                // Theme Mode
                ThemeModeSelector(
                    currentMode = themeMode,
                    onModeSelected = { viewModel.setThemeMode(it) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Design Skin
                DesignSkinSelector(
                    currentSkin = designSkin,
                    onSkinSelected = { viewModel.setDesignSkin(it) }
                )
            }
            
            // Defaults Section
            SettingsSection(title = "Defaults") {
                // Default Account
                DefaultAccountSelector(
                    currentAccount = defaultAccount,
                    accounts = activeAccounts,
                    onAccountSelected = { viewModel.setDefaultAccount(it?.id) }
                )
            }
            
            // Data Section
            SettingsSection(title = "Data") {
                ManageAccountsButton(onClick = onNavigateToAccountManagement)
                Spacer(modifier = Modifier.height(12.dp))
                ViewAuditLogsButton(onClick = onNavigateToAuditLog)
            }

            // Backup & Restore Section
            SettingsSection(title = "Backup & Restore") {
                BackupSection(
                    lastBackupInfo = lastBackupInfo,
                    isLoading = backupState is BackupState.InProgress,
                    onCreateBackup = { 
                        // Use SAF to let user choose where to save
                        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                        backupLauncher.launch("ledger_backup_$timestamp.json")
                    },
                    onRestoreBackup = { restoreLauncher.launch(arrayOf("application/json")) }
                )
            }
        }
    }
}

/**
 * Settings section with title and content.
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = LocalSkinColors.current.textPrimary
        )
        content()
    }
}

/**
 * Theme mode selector (Light, Dark, System).
 */
@Composable
private fun ThemeModeSelector(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (skinShapes.elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = skinShapes.elevation,
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (skinShapes.borderWidth > 0.dp) {
                    Modifier.border(
                        width = skinShapes.borderWidth,
                        color = skinColors.textSecondary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(skinShapes.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = skinColors.cardBackground
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Theme Mode",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = skinColors.textPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ThemeMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onModeSelected(mode) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 14.sp,
                        color = skinColors.textPrimary
                    )
                    
                    RadioButton(
                        selected = currentMode == mode,
                        onClick = { onModeSelected(mode) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = skinColors.primary,
                            unselectedColor = skinColors.textSecondary
                        )
                    )
                }
            }
        }
    }
}

/**
 * Design skin selector with all 5 skins.
 */
@Composable
private fun DesignSkinSelector(
    currentSkin: DesignSkin,
    onSkinSelected: (DesignSkin) -> Unit
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (skinShapes.elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = skinShapes.elevation,
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (skinShapes.borderWidth > 0.dp) {
                    Modifier.border(
                        width = skinShapes.borderWidth,
                        color = skinColors.textSecondary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(skinShapes.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = skinColors.cardBackground
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Design Skin",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = skinColors.textPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val skinDescriptions = mapOf(
                DesignSkin.NEO_MINIMAL to "Neo-Minimalism",
                DesignSkin.GLASS to "Glassmorphism",
                DesignSkin.RETRO_FUTURISM to "Retrofuturism",
                DesignSkin.NEO_BRUTAL to "Neobrutalism",
                DesignSkin.HYPER_BLOOM to "Hyper-Bloom"
            )
            
            DesignSkin.entries.forEach { skin ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSkinSelected(skin) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = skinDescriptions[skin] ?: skin.name,
                        fontSize = 14.sp,
                        color = skinColors.textPrimary
                    )
                    
                    RadioButton(
                        selected = currentSkin == skin,
                        onClick = { onSkinSelected(skin) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = skinColors.primary,
                            unselectedColor = skinColors.textSecondary
                        )
                    )
                }
            }
        }
    }
}

/**
 * Default account selector dropdown.
 */
@Composable
private fun DefaultAccountSelector(
    currentAccount: com.ledger.app.domain.model.Account?,
    accounts: List<com.ledger.app.domain.model.Account>,
    onAccountSelected: (com.ledger.app.domain.model.Account?) -> Unit
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (skinShapes.elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = skinShapes.elevation,
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (skinShapes.borderWidth > 0.dp) {
                    Modifier.border(
                        width = skinShapes.borderWidth,
                        color = skinColors.textSecondary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                    )
                } else {
                    Modifier
                }
            )
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(skinShapes.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = skinColors.cardBackground
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Default Account",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = skinColors.textPrimary
                    )
                    Text(
                        text = currentAccount?.name ?: "None",
                        fontSize = 12.sp,
                        color = skinColors.textSecondary
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Select account",
                    tint = skinColors.textSecondary
                )
            }
            
            if (expanded && accounts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = skinColors.textSecondary.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))
                
                accounts.forEach { account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAccountSelected(account)
                                expanded = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = account.name,
                                fontSize = 14.sp,
                                color = skinColors.textPrimary
                            )
                            Text(
                                text = account.type.name,
                                fontSize = 12.sp,
                                color = skinColors.textSecondary
                            )
                        }
                        
                        if (currentAccount?.id == account.id) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = skinColors.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Manage accounts navigation button.
 */
@Composable
private fun ManageAccountsButton(onClick: () -> Unit) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (skinShapes.elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = skinShapes.elevation,
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (skinShapes.borderWidth > 0.dp) {
                    Modifier.border(
                        width = skinShapes.borderWidth,
                        color = skinColors.textSecondary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(skinShapes.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = skinColors.cardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Manage Accounts",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = skinColors.textPrimary
            )
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Navigate",
                tint = skinColors.textSecondary
            )
        }
    }
}

/**
 * View audit logs navigation button.
 */
@Composable
private fun ViewAuditLogsButton(onClick: () -> Unit) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (skinShapes.elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = skinShapes.elevation,
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (skinShapes.borderWidth > 0.dp) {
                    Modifier.border(
                        width = skinShapes.borderWidth,
                        color = skinColors.textSecondary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(skinShapes.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = skinColors.cardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "View Audit Logs",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = skinColors.textPrimary
            )
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Navigate",
                tint = skinColors.textSecondary
            )
        }
    }
}

/**
 * Backup and restore section with buttons and last backup info.
 * Requirements: 13.1, 13.2
 */
@Composable
private fun BackupSection(
    lastBackupInfo: BackupFileInfo?,
    isLoading: Boolean,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (skinShapes.elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = skinShapes.elevation,
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (skinShapes.borderWidth > 0.dp) {
                    Modifier.border(
                        width = skinShapes.borderWidth,
                        color = skinColors.textSecondary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(skinShapes.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = skinColors.cardBackground
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Last backup info
            if (lastBackupInfo != null) {
                Column {
                    Text(
                        text = "Last Backup",
                        fontSize = 12.sp,
                        color = skinColors.textSecondary
                    )
                    Text(
                        text = dateFormat.format(lastBackupInfo.lastModified),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = skinColors.textPrimary
                    )
                }
            } else {
                Text(
                    text = "No backups yet",
                    fontSize = 14.sp,
                    color = skinColors.textSecondary
                )
            }

            HorizontalDivider(color = skinColors.textSecondary.copy(alpha = 0.2f))

            // Create Backup Button
            Button(
                onClick = onCreateBackup,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = skinColors.primary,
                    contentColor = skinColors.background
                ),
                shape = RoundedCornerShape(skinShapes.buttonCornerRadius)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = skinColors.background,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Create Backup",
                    fontWeight = FontWeight.Medium
                )
            }

            // Restore Backup Button
            Button(
                onClick = onRestoreBackup,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = skinColors.secondary,
                    contentColor = skinColors.background
                ),
                shape = RoundedCornerShape(skinShapes.buttonCornerRadius)
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Restore from Backup",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Dialog to show backup/restore operation status.
 */
@Composable
private fun BackupStateDialog(
    state: BackupState,
    onDismiss: () -> Unit
) {
    val skinColors = LocalSkinColors.current

    when (state) {
        is BackupState.InProgress -> {
            AlertDialog(
                onDismissRequest = { /* Cannot dismiss while in progress */ },
                title = { Text("Please Wait") },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Text(state.message)
                    }
                },
                confirmButton = { }
            )
        }
        is BackupState.Success -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Success") },
                text = {
                    Text("${state.message}\n${state.recordCount} records processed.")
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("OK", color = skinColors.primary)
                    }
                }
            )
        }
        is BackupState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Error") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("OK", color = skinColors.primary)
                    }
                }
            )
        }
        BackupState.Idle -> { /* No dialog */ }
    }
}

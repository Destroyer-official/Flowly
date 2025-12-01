package com.ledger.app.presentation.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.ledger.app.domain.model.Account
import com.ledger.app.domain.model.AccountType
import com.ledger.app.presentation.theme.LocalSkinColors
import com.ledger.app.presentation.theme.LocalSkinShapes
import com.ledger.app.presentation.viewmodel.AccountManagementViewModel

/**
 * Account Management screen for creating and managing payment methods.
 * 
 * Features:
 * - List accounts grouped by type
 * - Add new account with type selection
 * - Deactivate account with confirmation
 * 
 * Requirements: 12.1, 12.2, 12.4
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(
    viewModel: AccountManagementViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    val activeAccounts by viewModel.activeAccounts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var accountToDeactivate by remember { mutableStateOf<Account?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error in snackbar
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Payment Methods",
                        color = skinColors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = skinColors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = skinColors.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = skinColors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(32.dp),
                    color = skinColors.primary
                )
            }
            
            if (activeAccounts.isEmpty() && !isLoading) {
                EmptyAccountsMessage()
            } else {
                // Group accounts by type
                val groupedAccounts = activeAccounts.groupBy { it.type }
                
                AccountType.entries.forEach { type ->
                    val accountsOfType = groupedAccounts[type] ?: emptyList()
                    if (accountsOfType.isNotEmpty()) {
                        AccountTypeSection(
                            type = type,
                            accounts = accountsOfType,
                            onDeactivate = { accountToDeactivate = it }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Add Account Button
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = skinColors.primary
                ),
                shape = RoundedCornerShape(skinShapes.buttonCornerRadius)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Payment Method",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    
    // Add Account Dialog
    if (showAddDialog) {
        AddAccountDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type ->
                viewModel.addAccount(name, type)
                showAddDialog = false
            }
        )
    }
    
    // Deactivate Confirmation Dialog
    accountToDeactivate?.let { account ->
        DeactivateAccountDialog(
            account = account,
            onDismiss = { accountToDeactivate = null },
            onConfirm = {
                viewModel.deactivateAccount(account.id)
                accountToDeactivate = null
            }
        )
    }
}

/**
 * Section header and accounts for a specific account type.
 */
@Composable
private fun AccountTypeSection(
    type: AccountType,
    accounts: List<Account>,
    onDeactivate: (Account) -> Unit
) {
    val skinColors = LocalSkinColors.current
    
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Section header
        Text(
            text = getAccountTypeDisplayName(type),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = skinColors.textSecondary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        // Accounts list
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            accounts.forEach { account ->
                AccountItem(
                    account = account,
                    onDeactivate = { onDeactivate(account) }
                )
            }
        }
    }
}

/**
 * Get display name for account type.
 */
private fun getAccountTypeDisplayName(type: AccountType): String {
    return when (type) {
        AccountType.CASH -> "💵 Cash"
        AccountType.BANK -> "🏦 Bank Accounts"
        AccountType.UPI -> "📱 UPI"
        AccountType.CARD -> "💳 Cards"
        AccountType.OTHER -> "📋 Other"
    }
}

/**
 * Empty state message when no accounts exist.
 */
@Composable
private fun EmptyAccountsMessage() {
    val skinColors = LocalSkinColors.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "No accounts yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = skinColors.textPrimary
        )
        Text(
            text = "Tap the + button to add your first account",
            fontSize = 14.sp,
            color = skinColors.textSecondary
        )
    }
}

/**
 * Individual account item card with clean design.
 */
@Composable
private fun AccountItem(
    account: Account,
    onDeactivate: () -> Unit
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (skinShapes.elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = skinShapes.elevation / 2,
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                    )
                } else {
                    Modifier
                }
            )
            .border(
                width = 1.dp,
                color = skinColors.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(skinShapes.cardCornerRadius)
            ),
        shape = RoundedCornerShape(skinShapes.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = skinColors.cardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = account.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = skinColors.textPrimary
            )
            
            IconButton(
                onClick = onDeactivate,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = skinColors.gaveColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Dialog for adding a new payment method.
 */
@Composable
private fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, AccountType) -> Unit
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    var accountName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.CASH) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Payment Method",
                fontWeight = FontWeight.Bold,
                color = skinColors.textPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("Name") },
                    placeholder = { Text("e.g., HDFC Bank, Paytm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = skinColors.primary,
                        unfocusedBorderColor = skinColors.textSecondary.copy(alpha = 0.3f),
                        focusedLabelColor = skinColors.primary
                    )
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Type",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = skinColors.textSecondary
                    )
                    
                    // Type selection as chips
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AccountType.entries.forEach { type ->
                            val isSelected = selectedType == type
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedType = type }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) skinColors.primary else skinColors.textSecondary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) 
                                        skinColors.primary.copy(alpha = 0.1f) 
                                    else 
                                        skinColors.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = getAccountTypeIcon(type),
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = getAccountTypeLabel(type),
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) skinColors.primary else skinColors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(accountName, selectedType) },
                enabled = accountName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = skinColors.primary,
                    contentColor = skinColors.onPrimary
                ),
                shape = RoundedCornerShape(skinShapes.buttonCornerRadius)
            ) {
                Text("Add", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = skinColors.textSecondary
                )
            }
        },
        containerColor = skinColors.cardBackground,
        shape = RoundedCornerShape(skinShapes.cardCornerRadius)
    )
}

/**
 * Get icon for account type.
 */
private fun getAccountTypeIcon(type: AccountType): String {
    return when (type) {
        AccountType.CASH -> "💵"
        AccountType.BANK -> "🏦"
        AccountType.UPI -> "📱"
        AccountType.CARD -> "💳"
        AccountType.OTHER -> "📋"
    }
}

/**
 * Get label for account type.
 */
private fun getAccountTypeLabel(type: AccountType): String {
    return when (type) {
        AccountType.CASH -> "Cash"
        AccountType.BANK -> "Bank Account"
        AccountType.UPI -> "UPI"
        AccountType.CARD -> "Credit/Debit Card"
        AccountType.OTHER -> "Other"
    }
}

/**
 * Confirmation dialog for deactivating an account.
 */
@Composable
private fun DeactivateAccountDialog(
    account: Account,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val skinColors = LocalSkinColors.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Deactivate Account?",
                color = skinColors.textPrimary
            )
        },
        text = {
            Text(
                text = "Are you sure you want to deactivate \"${account.name}\"? Historical transactions will be preserved, but this account will no longer appear in selection lists.",
                color = skinColors.textPrimary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = skinColors.gaveColor,
                    contentColor = skinColors.onPrimary
                )
            ) {
                Text("Deactivate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = skinColors.textSecondary
                )
            }
        },
        containerColor = skinColors.cardBackground
    )
}

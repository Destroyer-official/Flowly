package com.ledger.app.presentation.ui.transaction

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ledger.app.domain.model.PartialPayment
import com.ledger.app.domain.model.PaymentDirection
import com.ledger.app.domain.model.PaymentMethod
import com.ledger.app.domain.model.Transaction
import com.ledger.app.domain.model.TransactionDirection
import com.ledger.app.domain.model.TransactionStatus
import com.ledger.app.domain.usecase.ExportLedgerUseCase
import com.ledger.app.presentation.theme.LocalSkinColors
import com.ledger.app.presentation.theme.LocalSkinShapes
import com.ledger.app.presentation.ui.components.AmountDisplay
import com.ledger.app.presentation.viewmodel.TransactionDetailsUiState
import com.ledger.app.presentation.viewmodel.TransactionDetailsViewModel
import java.math.BigDecimal
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter

/**
 * Transaction Details screen showing full transaction information.
 * 
 * Features:
 * - Transaction header (amount, direction, counterparty, date)
 * - Status and remaining due display
 * - Payment timeline visualization
 * - Action buttons: Add Payment, Edit, Cancel, Duplicate
 * 
 * Requirements: 2.1, 2.2, 3.4
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsScreen(
    onNavigateBack: () -> Unit,
    onEditTransaction: (Long) -> Unit = {},
    viewModel: TransactionDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showAddPaymentDialog by viewModel.showAddPaymentDialog.collectAsState()
    val exportResult by viewModel.exportResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val skinColors = LocalSkinColors.current
    val context = LocalContext.current

    // Handle export result - launch share intent
    LaunchedEffect(exportResult) {
        when (val result = exportResult) {
            is ExportLedgerUseCase.ExportResult.Success -> {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, result.title)
                    putExtra(Intent.EXTRA_TEXT, result.content)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Transaction"))
                viewModel.clearExportResult()
            }
            is ExportLedgerUseCase.ExportResult.Error -> {
                snackbarHostState.showSnackbar(result.message)
                viewModel.clearExportResult()
            }
            null -> { /* No action */ }
        }
    }

    // Show error messages
    LaunchedEffect(uiState) {
        if (uiState is TransactionDetailsUiState.Success) {
            val state = uiState as TransactionDetailsUiState.Success
            state.errorMessage?.let { message ->
                snackbarHostState.showSnackbar(message)
                viewModel.clearError()
            }
            state.surplusAmount?.let { surplus ->
                snackbarHostState.showSnackbar(
                    "Warning: Overpayment of ₹${DecimalFormat("#,##0.00").format(surplus)}"
                )
                viewModel.clearSurplus()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Export button (Requirements: 4.3)
                    IconButton(onClick = { viewModel.exportTransaction() }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export transaction",
                            tint = skinColors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = skinColors.background,
                    titleContentColor = skinColors.textPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = skinColors.background
    ) { paddingValues ->
        when (val state = uiState) {
            is TransactionDetailsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = skinColors.primary)
                }
            }

            is TransactionDetailsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = skinColors.error,
                        fontSize = 16.sp
                    )
                }
            }

            is TransactionDetailsUiState.Success -> {
                TransactionDetailsContent(
                    transaction = state.transaction,
                    partialPayments = state.partialPayments,
                    counterpartyName = state.counterparty?.displayName,
                    accountName = state.account?.name,
                    categoryName = state.category?.name,
                    linkedTask = state.linkedTask,
                    onAddPayment = { viewModel.showAddPaymentDialog() },
                    onEdit = { onEditTransaction(state.transaction.id) },
                    onCancel = { viewModel.cancelTransaction() },
                    onDuplicate = { viewModel.duplicateTransaction() },
                    onDelete = { viewModel.deleteTransaction { onNavigateBack() } },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Add Payment Dialog
        if (showAddPaymentDialog && uiState is TransactionDetailsUiState.Success) {
            val state = uiState as TransactionDetailsUiState.Success
            AddPartialPaymentDialog(
                transaction = state.transaction,
                onDismiss = { viewModel.hideAddPaymentDialog() },
                onConfirm = { amount, direction, method, notes ->
                    viewModel.addPartialPayment(amount, direction, method, notes)
                }
            )
        }
    }
}

@Composable
private fun TransactionDetailsContent(
    transaction: Transaction,
    partialPayments: List<PartialPayment>,
    counterpartyName: String?,
    accountName: String?,
    categoryName: String?,
    linkedTask: com.ledger.app.domain.model.Task?,
    onAddPayment: () -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Transaction Header Card
        item {
            TransactionHeaderCard(
                transaction = transaction,
                counterpartyName = counterpartyName,
                accountName = accountName,
                categoryName = categoryName
            )
        }

        // Linked Task Card (Requirements: 15.5)
        if (linkedTask != null) {
            item {
                LinkedTaskCard(
                    task = linkedTask
                )
            }
        }

        // Status and Remaining Due Card
        item {
            StatusCard(
                status = transaction.status,
                remainingDue = transaction.remainingDue,
                baseAmount = transaction.amount
            )
        }

        // Payment Timeline
        item {
            Text(
                text = "Payment Timeline",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = skinColors.textPrimary
            )
        }

        // Original transaction
        item {
            PaymentTimelineItem(
                label = "Original",
                amount = transaction.amount,
                date = transaction.transactionDateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                isOriginal = true
            )
        }

        // Partial payments
        items(partialPayments) { payment ->
            PaymentTimelineItem(
                label = "Payment",
                amount = payment.amount,
                date = payment.dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                isOriginal = false,
                method = payment.method.name
            )
        }

        // Remaining
        if (transaction.remainingDue > BigDecimal.ZERO) {
            item {
                PaymentTimelineItem(
                    label = "Remaining",
                    amount = transaction.remainingDue,
                    date = null,
                    isOriginal = false,
                    isPending = true
                )
            }
        }

        // Action Buttons
        item {
            Spacer(modifier = Modifier.height(8.dp))
            ActionButtons(
                canAddPayment = transaction.status != TransactionStatus.CANCELLED && 
                               transaction.status != TransactionStatus.SETTLED,
                canCancel = transaction.status != TransactionStatus.CANCELLED,
                canDelete = true,
                onAddPayment = onAddPayment,
                onEdit = onEdit,
                onCancel = onCancel,
                onDuplicate = onDuplicate,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun TransactionHeaderCard(
    transaction: Transaction,
    counterpartyName: String?,
    accountName: String?,
    categoryName: String?,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current

    Card(
        modifier = modifier
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
                        color = skinColors.textSecondary.copy(alpha = 0.2f),
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Amount
            AmountDisplay(
                amount = transaction.amount,
                direction = transaction.direction,
                fontSize = 36.sp
            )

            // Direction text
            Text(
                text = when (transaction.direction) {
                    TransactionDirection.GAVE -> "GAVE to ${counterpartyName ?: "Self"}"
                    TransactionDirection.RECEIVED -> "RECEIVED from ${counterpartyName ?: "Self"}"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = skinColors.textPrimary
            )

            // Category and Account
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = categoryName ?: "Unknown",
                    fontSize = 14.sp,
                    color = skinColors.textSecondary,
                    modifier = Modifier
                        .background(
                            color = skinColors.textSecondary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Text(
                    text = "•",
                    fontSize = 14.sp,
                    color = skinColors.textSecondary
                )
                Text(
                    text = accountName ?: "Unknown",
                    fontSize = 14.sp,
                    color = skinColors.textSecondary,
                    modifier = Modifier
                        .background(
                            color = skinColors.textSecondary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Date
            Text(
                text = transaction.transactionDateTime.format(
                    DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' HH:mm")
                ),
                fontSize = 14.sp,
                color = skinColors.textSecondary
            )

            // Notes if present
            transaction.notes?.let { notes ->
                Text(
                    text = notes,
                    fontSize = 14.sp,
                    color = skinColors.textSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = skinColors.textSecondary.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    status: TransactionStatus,
    remainingDue: BigDecimal,
    baseAmount: BigDecimal,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current

    val statusText = when (status) {
        TransactionStatus.PENDING -> "Pending"
        TransactionStatus.PARTIALLY_SETTLED -> "Partially Settled"
        TransactionStatus.SETTLED -> "Settled"
        TransactionStatus.CANCELLED -> "Cancelled"
    }

    val statusColor = when (status) {
        TransactionStatus.SETTLED -> skinColors.receivedColor
        TransactionStatus.CANCELLED -> skinColors.error
        else -> skinColors.textSecondary
    }

    Card(
        modifier = modifier
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
            ),
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
            Column {
                Text(
                    text = "Status",
                    fontSize = 12.sp,
                    color = skinColors.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )
            }

            if (status != TransactionStatus.CANCELLED) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Remaining Due",
                        fontSize = 12.sp,
                        color = skinColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AmountDisplay(
                        amount = remainingDue,
                        color = if (remainingDue > BigDecimal.ZERO) skinColors.gaveColor else skinColors.receivedColor,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * Card showing the linked task information.
 * Displays "Created from Task" indicator with task title.
 * 
 * Requirements: 15.5
 */
@Composable
private fun LinkedTaskCard(
    task: com.ledger.app.domain.model.Task,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current

    Card(
        modifier = modifier
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
            ),
        shape = RoundedCornerShape(skinShapes.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = skinColors.primary.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Task icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = skinColors.primary.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    fontSize = 20.sp,
                    color = skinColors.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Created from Task",
                    fontSize = 12.sp,
                    color = skinColors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = skinColors.textPrimary
                )
                if (task.description != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        fontSize = 13.sp,
                        color = skinColors.textSecondary,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentTimelineItem(
    label: String,
    amount: BigDecimal,
    date: String?,
    isOriginal: Boolean,
    isPending: Boolean = false,
    method: String? = null,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val formatter = DecimalFormat("#,##0.00")

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Timeline indicator
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = when {
                        isPending -> skinColors.textSecondary.copy(alpha = 0.3f)
                        isOriginal -> skinColors.primary
                        else -> skinColors.receivedColor
                    },
                    shape = CircleShape
                )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = if (isOriginal) FontWeight.SemiBold else FontWeight.Normal,
                    color = skinColors.textPrimary
                )
                Text(
                    text = "₹${formatter.format(amount)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        isPending -> skinColors.textSecondary
                        isOriginal -> skinColors.textPrimary
                        else -> skinColors.receivedColor
                    }
                )
            }
            if (date != null) {
                Text(
                    text = date + (method?.let { " • $it" } ?: ""),
                    fontSize = 12.sp,
                    color = skinColors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    canAddPayment: Boolean,
    canCancel: Boolean,
    canDelete: Boolean,
    onAddPayment: () -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Add Payment button (primary)
        if (canAddPayment) {
            Button(
                onClick = onAddPayment,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(skinShapes.buttonCornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = skinColors.primary
                )
            ) {
                Text("Add Payment", fontSize = 16.sp)
            }
        }

        // Secondary actions - Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(skinShapes.buttonCornerRadius)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit", fontSize = 13.sp)
            }

            if (canCancel) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(skinShapes.buttonCornerRadius)
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel", fontSize = 13.sp)
                }
            }

            OutlinedButton(
                onClick = onDuplicate,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(skinShapes.buttonCornerRadius)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Duplicate",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Duplicate", fontSize = 13.sp)
            }
        }

        // Delete button - separate row for destructive action
        if (canDelete) {
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(skinShapes.buttonCornerRadius),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = skinColors.error
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, skinColors.error.copy(alpha = 0.5f))
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Transaction", fontSize = 14.sp)
            }
        }
    }
}


/**
 * Dialog for adding a partial payment to a transaction.
 * 
 * Features:
 * - Amount input with numeric keypad
 * - Payment method selector
 * - Optional note field
 * - Surplus warning if overpayment detected
 * 
 * Requirements: 3.1, 3.3
 */
@Composable
fun AddPartialPaymentDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal, PaymentDirection, PaymentMethod, String?) -> Unit
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    var amountText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var notes by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    // Determine payment direction based on transaction direction
    val paymentDirection = when (transaction.direction) {
        TransactionDirection.GAVE -> PaymentDirection.FROM_COUNTERPARTY
        TransactionDirection.RECEIVED -> PaymentDirection.TO_COUNTERPARTY
    }
    
    // Check for potential surplus
    val enteredAmount = amountText.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val wouldBeSurplus = enteredAmount > transaction.remainingDue
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Payment",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = skinColors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Remaining due info
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = skinColors.textSecondary.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Remaining Due:",
                            fontSize = 14.sp,
                            color = skinColors.textSecondary
                        )
                        AmountDisplay(
                            amount = transaction.remainingDue,
                            color = skinColors.textPrimary,
                            fontSize = 16.sp
                        )
                    }
                }
                
                // Amount input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { 
                        // Only allow numbers and decimal point
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amountText = it
                            showError = false
                        }
                    },
                    label = { Text("Payment Amount") },
                    placeholder = { Text("0.00") },
                    leadingIcon = { Text("₹", fontSize = 18.sp) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    isError = showError,
                    supportingText = if (showError) {
                        { Text(errorMessage, color = skinColors.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = skinColors.primary,
                        unfocusedBorderColor = skinColors.textSecondary.copy(alpha = 0.3f)
                    )
                )
                
                // Surplus warning
                if (wouldBeSurplus && enteredAmount > BigDecimal.ZERO) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = skinColors.error.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = skinColors.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Overpayment Warning",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = skinColors.error
                                )
                                Text(
                                    text = "Amount exceeds remaining due by ₹${DecimalFormat("#,##0.00").format(enteredAmount - transaction.remainingDue)}",
                                    fontSize = 12.sp,
                                    color = skinColors.textSecondary
                                )
                            }
                        }
                    }
                }
                
                // Payment method selector
                Text(
                    text = "Payment Method",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = skinColors.textPrimary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethod.values().take(4).forEach { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(method.name) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Notes input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Add a note...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = skinColors.primary,
                        unfocusedBorderColor = skinColors.textSecondary.copy(alpha = 0.3f)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toBigDecimalOrNull()
                    when {
                        amount == null || amount <= BigDecimal.ZERO -> {
                            showError = true
                            errorMessage = "Please enter a valid amount"
                        }
                        else -> {
                            onConfirm(
                                amount,
                                paymentDirection,
                                selectedMethod,
                                notes.ifBlank { null }
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(skinShapes.buttonCornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = skinColors.primary
                )
            ) {
                Text("Add Payment")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(skinShapes.buttonCornerRadius)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(skinShapes.cardCornerRadius),
        containerColor = skinColors.cardBackground
    )
}

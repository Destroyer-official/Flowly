package com.ledger.app.presentation.ui.counterparty

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ledger.app.domain.model.Transaction
import com.ledger.app.domain.usecase.ExportLedgerUseCase
import com.ledger.app.presentation.theme.LocalSkinColors
import com.ledger.app.presentation.theme.LocalSkinShapes
import com.ledger.app.presentation.ui.components.AmountDisplay
import com.ledger.app.presentation.ui.components.TransactionListItem
import com.ledger.app.presentation.viewmodel.CounterpartyLedgerViewModel
import java.math.BigDecimal

/**
 * Counterparty Ledger screen composable.
 * 
 * Features:
 * - Header with counterparty name and net balance
 * - Filter tabs (All, Loans, Bills, Outstanding)
 * - Transaction timeline sorted by date descending
 * - Add transaction and reminder buttons
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4
 */
@Composable
fun CounterpartyLedgerScreen(
    onNavigateBack: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
    onAddTransaction: (Long) -> Unit = {},
    onAddReminder: (Long) -> Unit = {},
    viewModel: CounterpartyLedgerViewModel = hiltViewModel()
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    val context = LocalContext.current
    
    val counterparty by viewModel.counterparty.collectAsState()
    val netBalance by viewModel.netBalance.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val exportResult by viewModel.exportResult.collectAsState()

    // Handle export result - launch share intent
    LaunchedEffect(exportResult) {
        when (val result = exportResult) {
            is ExportLedgerUseCase.ExportResult.Success -> {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, result.title)
                    putExtra(Intent.EXTRA_TEXT, result.content)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Ledger"))
                viewModel.clearExportResult()
            }
            is ExportLedgerUseCase.ExportResult.Error -> {
                // Error handling could show a snackbar
                viewModel.clearExportResult()
            }
            null -> { /* No action */ }
        }
    }

    Scaffold(
        containerColor = skinColors.background,
        floatingActionButton = {
            // FAB for adding new transaction (Requirements: 4.4)
            counterparty?.let { cp ->
                FloatingActionButton(
                    onClick = { onAddTransaction(cp.id) },
                    containerColor = skinColors.primary,
                    contentColor = skinColors.surface
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add transaction"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top bar with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = skinColors.textPrimary
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = skinColors.primary)
                }
            } else if (counterparty == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Counterparty not found",
                        fontSize = 14.sp,
                        color = skinColors.textSecondary
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header with name and net balance (Requirements: 4.1)
                    CounterpartyHeader(
                        counterpartyName = counterparty!!.displayName,
                        netBalance = netBalance,
                        onAddReminder = { onAddReminder(counterparty!!.id) },
                        onExport = { viewModel.exportLedger() }
                    )

                    // Filter tabs (Requirements: 4.3)
                    FilterTabs(
                        selectedFilter = selectedFilter,
                        onFilterSelected = { viewModel.setFilter(it) }
                    )

                    // Transaction timeline (Requirements: 4.2)
                    if (filteredTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No transactions found",
                                fontSize = 14.sp,
                                color = skinColors.textSecondary
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = filteredTransactions,
                                key = { it.id }
                            ) { transaction ->
                                TransactionListItem(
                                    amount = transaction.amount,
                                    direction = transaction.direction,
                                    counterpartyName = counterparty!!.displayName,
                                    categoryName = getCategoryName(transaction),
                                    dateTime = transaction.transactionDateTime,
                                    status = transaction.status,
                                    remainingDue = transaction.remainingDue,
                                    onClick = { onTransactionClick(transaction.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Header component displaying counterparty name and net balance.
 * 
 * Requirements: 4.1, 4.3
 */
@Composable
private fun CounterpartyHeader(
    counterpartyName: String,
    netBalance: BigDecimal,
    onAddReminder: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    // Determine if we're using glassmorphism
    val isGlass = skinShapes.blurRadius > 0.dp
    
    // Determine balance color and text
    val balanceColor = when {
        netBalance > BigDecimal.ZERO -> skinColors.receivedColor
        netBalance < BigDecimal.ZERO -> skinColors.gaveColor
        else -> skinColors.textSecondary
    }
    
    val balanceText = when {
        netBalance > BigDecimal.ZERO -> "owes you"
        netBalance < BigDecimal.ZERO -> "you owe"
        else -> "settled"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (skinShapes.elevation > 0.dp && !isGlass) {
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
            containerColor = if (isGlass) {
                skinColors.cardBackground.copy(alpha = 0.7f)
            } else {
                skinColors.cardBackground
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Counterparty name with avatar
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = skinColors.primary.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = counterpartyName.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = skinColors.primary
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = counterpartyName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = skinColors.textPrimary
                    )
                    
                    Text(
                        text = balanceText,
                        fontSize = 14.sp,
                        color = skinColors.textSecondary
                    )
                }

                // Export button (Requirements: 4.3)
                IconButton(
                    onClick = onExport,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = skinColors.primary.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export ledger",
                        tint = skinColors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Add reminder button (Requirements: 4.4)
                IconButton(
                    onClick = onAddReminder,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = skinColors.primary.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Set reminder",
                        tint = skinColors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Net balance display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AmountDisplay(
                    amount = netBalance.abs(),
                    color = balanceColor,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Filter tabs component for filtering transactions.
 * 
 * Requirements: 4.3
 */
@Composable
private fun FilterTabs(
    selectedFilter: CounterpartyLedgerViewModel.FilterType,
    onFilterSelected: (CounterpartyLedgerViewModel.FilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == CounterpartyLedgerViewModel.FilterType.ALL,
            onClick = { onFilterSelected(CounterpartyLedgerViewModel.FilterType.ALL) },
            label = { Text("All") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = skinColors.primary,
                selectedLabelColor = skinColors.surface,
                containerColor = skinColors.surface,
                labelColor = skinColors.textPrimary
            ),
            shape = RoundedCornerShape(skinShapes.buttonCornerRadius)
        )

        FilterChip(
            selected = selectedFilter == CounterpartyLedgerViewModel.FilterType.LOANS,
            onClick = { onFilterSelected(CounterpartyLedgerViewModel.FilterType.LOANS) },
            label = { Text("Loans") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = skinColors.primary,
                selectedLabelColor = skinColors.surface,
                containerColor = skinColors.surface,
                labelColor = skinColors.textPrimary
            ),
            shape = RoundedCornerShape(skinShapes.buttonCornerRadius)
        )

        FilterChip(
            selected = selectedFilter == CounterpartyLedgerViewModel.FilterType.BILLS,
            onClick = { onFilterSelected(CounterpartyLedgerViewModel.FilterType.BILLS) },
            label = { Text("Bills") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = skinColors.primary,
                selectedLabelColor = skinColors.surface,
                containerColor = skinColors.surface,
                labelColor = skinColors.textPrimary
            ),
            shape = RoundedCornerShape(skinShapes.buttonCornerRadius)
        )

        FilterChip(
            selected = selectedFilter == CounterpartyLedgerViewModel.FilterType.OUTSTANDING,
            onClick = { onFilterSelected(CounterpartyLedgerViewModel.FilterType.OUTSTANDING) },
            label = { Text("Outstanding") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = skinColors.primary,
                selectedLabelColor = skinColors.surface,
                containerColor = skinColors.surface,
                labelColor = skinColors.textPrimary
            ),
            shape = RoundedCornerShape(skinShapes.buttonCornerRadius)
        )
    }
}

/**
 * Helper function to get category name from transaction.
 * In a real app, this would look up the category from the repository.
 * For now, we use the transaction type as a fallback.
 */
private fun getCategoryName(transaction: Transaction): String {
    // TODO: Look up actual category name from repository
    return transaction.type.name.replace("_", " ").lowercase()
        .replaceFirstChar { it.uppercase() }
}

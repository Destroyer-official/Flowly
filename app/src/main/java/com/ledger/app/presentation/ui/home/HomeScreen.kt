package com.ledger.app.presentation.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ledger.app.domain.model.BillCategory
import com.ledger.app.domain.model.CounterpartyWithBalance
import com.ledger.app.presentation.theme.LocalSkinColors
import com.ledger.app.presentation.theme.LocalSkinShapes
import com.ledger.app.presentation.ui.components.SummaryCard
import com.ledger.app.presentation.ui.components.TransactionListItem
import com.ledger.app.presentation.ui.graphics3d.AdaptiveBalanceVisualization
import com.ledger.app.presentation.ui.quickadd.QuickAddBottomSheet
import com.ledger.app.presentation.viewmodel.HomeViewModel
import com.ledger.app.presentation.viewmodel.TimeFilter
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/**
 * Home/Dashboard screen composable.
 * 
 * Features:
 * - Summary cards showing "They owe you", "You owe", and "Reminders"
 * - Quick filter chips (Today, This Week, This Month)
 * - Recent activity list with TransactionListItem
 * - FAB for quick add transaction
 * - Quick Add bottom sheet with undo snackbar
 * 
 * Requirements: 8.1, 8.2, 8.3, 1.1, 1.4
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onTransactionClick: (Long) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    val dashboardSummary by viewModel.dashboardSummary.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val topDebtors by viewModel.topDebtors.collectAsState()
    val monthlyBillSummary by viewModel.monthlyBillSummary.collectAsState()

    // Quick Add bottom sheet state
    var showQuickAdd by remember { mutableStateOf(false) }
    val quickAddSheetState = rememberModalBottomSheetState()
    
    // Snackbar state for undo
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var lastTransactionId by remember { mutableStateOf<Long?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(skinColors.background)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = skinColors.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Text(
                        text = "Dashboard",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = skinColors.textPrimary
                    )
                }

                // 3D Balance Visualization (Requirements: 6.1)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius),
                        colors = CardDefaults.cardColors(
                            containerColor = skinColors.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        AdaptiveBalanceVisualization(
                            totalOwedToUser = dashboardSummary.totalOwedToUser,
                            totalUserOwes = dashboardSummary.totalUserOwes,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }

                // Summary Cards Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            title = "They owe you",
                            amount = dashboardSummary.totalOwedToUser,
                            amountColor = skinColors.receivedColor,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "You owe",
                            amount = dashboardSummary.totalUserOwes,
                            amountColor = skinColors.gaveColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Reminders card
                item {
                    SummaryCard(
                        title = "🔔 Upcoming Reminders",
                        amount = dashboardSummary.upcomingRemindersCount.toBigDecimal(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Top Debtors Section (Requirements: 9.2)
                if (topDebtors.isNotEmpty()) {
                    item {
                        TopDebtorsSection(
                            topDebtors = topDebtors,
                            skinColors = skinColors,
                            skinShapes = skinShapes
                        )
                    }
                }

                // Monthly Bill Summary Section (Requirements: 9.3)
                if (monthlyBillSummary.isNotEmpty()) {
                    item {
                        MonthlyBillSummarySection(
                            billSummary = monthlyBillSummary,
                            skinColors = skinColors,
                            skinShapes = skinShapes
                        )
                    }
                }

                // Quick Filters
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Recent Activity",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = skinColors.textPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedFilter == TimeFilter.TODAY,
                                onClick = { viewModel.selectFilter(TimeFilter.TODAY) },
                                label = { Text("Today") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = skinColors.primary,
                                    selectedLabelColor = skinColors.onPrimary,
                                    containerColor = skinColors.surface,
                                    labelColor = skinColors.textPrimary
                                ),
                                shape = RoundedCornerShape(skinShapes.buttonCornerRadius)
                            )
                            FilterChip(
                                selected = selectedFilter == TimeFilter.THIS_WEEK,
                                onClick = { viewModel.selectFilter(TimeFilter.THIS_WEEK) },
                                label = { Text("This Week") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = skinColors.primary,
                                    selectedLabelColor = skinColors.onPrimary,
                                    containerColor = skinColors.surface,
                                    labelColor = skinColors.textPrimary
                                ),
                                shape = RoundedCornerShape(skinShapes.buttonCornerRadius)
                            )
                            FilterChip(
                                selected = selectedFilter == TimeFilter.THIS_MONTH,
                                onClick = { viewModel.selectFilter(TimeFilter.THIS_MONTH) },
                                label = { Text("This Month") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = skinColors.primary,
                                    selectedLabelColor = skinColors.onPrimary,
                                    containerColor = skinColors.surface,
                                    labelColor = skinColors.textPrimary
                                ),
                                shape = RoundedCornerShape(skinShapes.buttonCornerRadius)
                            )
                        }
                    }
                }

                // Recent Transactions List
                if (recentTransactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No transactions for this period",
                                fontSize = 14.sp,
                                color = skinColors.textSecondary
                            )
                        }
                    }
                } else {
                    items(
                        items = recentTransactions,
                        key = { it.id }
                    ) { transactionWithCounterparty ->
                        TransactionListItem(
                            amount = transactionWithCounterparty.amount,
                            direction = transactionWithCounterparty.direction,
                            counterpartyName = transactionWithCounterparty.counterpartyName,
                            categoryName = transactionWithCounterparty.type.name.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() },
                            dateTime = transactionWithCounterparty.transactionDateTime,
                            status = transactionWithCounterparty.status,
                            remainingDue = transactionWithCounterparty.remainingDue,
                            onClick = { onTransactionClick(transactionWithCounterparty.id) }
                        )
                    }
                }

                // Bottom spacing for FAB
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // FAB positioned at bottom-end
        FloatingActionButton(
            onClick = { showQuickAdd = true },
            containerColor = skinColors.primary,
            contentColor = skinColors.onPrimary,
            shape = RoundedCornerShape(skinShapes.buttonCornerRadius),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add transaction"
            )
        }

        // Snackbar host at bottom
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }

    // Quick Add Bottom Sheet
    if (showQuickAdd) {
        QuickAddBottomSheet(
            sheetState = quickAddSheetState,
            onDismiss = { showQuickAdd = false },
            onTransactionSaved = { transactionId ->
                showQuickAdd = false
                lastTransactionId = transactionId
                
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Transaction saved",
                        actionLabel = "Undo",
                        withDismissAction = true
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        lastTransactionId?.let { id ->
                            viewModel.undoTransaction(id)
                        }
                    }
                }
            }
        )
    }
}

/**
 * Top Debtors Section - Shows people who owe the most.
 * 
 * Requirements: 9.2
 */
@Composable
private fun TopDebtorsSection(
    topDebtors: List<CounterpartyWithBalance>,
    skinColors: com.ledger.app.presentation.theme.SkinColors,
    skinShapes: com.ledger.app.presentation.theme.SkinShapes
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(skinShapes.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = skinColors.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Top Debtors",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = skinColors.textPrimary
            )
            
            topDebtors.forEach { debtorWithBalance ->
                DebtorItem(
                    name = debtorWithBalance.counterparty.displayName,
                    amount = debtorWithBalance.netBalance,
                    skinColors = skinColors
                )
            }
        }
    }
}

/**
 * Individual debtor item in the Top Debtors list.
 */
@Composable
private fun DebtorItem(
    name: String,
    amount: BigDecimal,
    skinColors: com.ledger.app.presentation.theme.SkinColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar placeholder
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(skinColors.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = skinColors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = skinColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(120.dp)
            )
        }
        
        Text(
            text = formatCurrency(amount),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = skinColors.receivedColor
        )
    }
}

/**
 * Monthly Bill Summary Section - Shows this month's bill payments by category.
 * 
 * Requirements: 9.3
 */
@Composable
private fun MonthlyBillSummarySection(
    billSummary: Map<BillCategory, BigDecimal>,
    skinColors: com.ledger.app.presentation.theme.SkinColors,
    skinShapes: com.ledger.app.presentation.theme.SkinShapes
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(skinShapes.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = skinColors.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "This Month's Bills",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = skinColors.textPrimary
            )
            
            billSummary.forEach { (category, amount) ->
                BillCategoryItem(
                    category = category,
                    amount = amount,
                    skinColors = skinColors
                )
            }
            
            // Total
            val total = billSummary.values.fold(BigDecimal.ZERO) { acc, amount -> acc + amount }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = skinColors.textPrimary
                )
                Text(
                    text = formatCurrency(total),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = skinColors.gaveColor
                )
            }
        }
    }
}

/**
 * Individual bill category item in the Monthly Bill Summary.
 */
@Composable
private fun BillCategoryItem(
    category: BillCategory,
    amount: BigDecimal,
    skinColors: com.ledger.app.presentation.theme.SkinColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category icon
            Text(
                text = getBillCategoryIcon(category),
                fontSize = 20.sp
            )
            
            Text(
                text = getBillCategoryDisplayName(category),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = skinColors.textPrimary
            )
        }
        
        Text(
            text = formatCurrency(amount),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = skinColors.gaveColor
        )
    }
}

/**
 * Returns an emoji icon for the bill category.
 */
private fun getBillCategoryIcon(category: BillCategory): String {
    return when (category) {
        BillCategory.ELECTRICITY -> "⚡"
        BillCategory.TV -> "📺"
        BillCategory.MOBILE -> "📱"
        BillCategory.INTERNET -> "🌐"
        BillCategory.OTHER -> "📄"
    }
}

/**
 * Returns a display name for the bill category.
 */
private fun getBillCategoryDisplayName(category: BillCategory): String {
    return when (category) {
        BillCategory.ELECTRICITY -> "Electricity"
        BillCategory.TV -> "TV"
        BillCategory.MOBILE -> "Mobile"
        BillCategory.INTERNET -> "Internet"
        BillCategory.OTHER -> "Other"
    }
}

/**
 * Formats a BigDecimal as Indian currency.
 */
private fun formatCurrency(amount: BigDecimal): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return formatter.format(amount)
}

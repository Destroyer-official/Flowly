package com.ledger.app.presentation.ui.analytics

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ledger.app.domain.usecase.AnalyticsData
import com.ledger.app.domain.usecase.CategoryBreakdown
import com.ledger.app.domain.usecase.CounterpartyAmount
import com.ledger.app.presentation.theme.LocalSkinColors
import com.ledger.app.presentation.theme.LocalSkinShapes
import com.ledger.app.presentation.ui.components.AmountDisplay
import com.ledger.app.presentation.viewmodel.AnalyticsViewModel
import java.math.BigDecimal
import java.text.DecimalFormat
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/**
 * Analytics screen showing monthly summaries, category breakdowns, and top debtors/creditors.
 * 
 * Features:
 * - Month selector with navigation
 * - Summary card (outflow, inflow, net)
 * - Category breakdown list
 * - Top 5 who owe you / you owe lists
 * - Unrecovered amount display
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4
 */
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val analyticsData by viewModel.analyticsData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error in snackbar
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "Analytics",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = LocalSkinColors.current.textPrimary
            )
            
            // Month selector
            MonthSelector(
                year = selectedYear,
                month = selectedMonth,
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() }
            )
            
            // Loading or content
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                analyticsData?.let { data ->
                    AnalyticsContent(data = data)
                }
            }
        }
    }
}

/**
 * Month selector with navigation arrows.
 */
@Composable
private fun MonthSelector(
    year: Int,
    month: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    val monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault())
    
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Previous month",
                    tint = skinColors.primary
                )
            }
            
            Text(
                text = "$monthName $year",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = skinColors.textPrimary
            )
            
            IconButton(onClick = onNextMonth) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Next month",
                    tint = skinColors.primary
                )
            }
        }
    }
}

/**
 * Main analytics content.
 */
@Composable
private fun AnalyticsContent(data: AnalyticsData) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Monthly summary card
        MonthlySummaryCard(data = data)
        
        // Category breakdown
        if (data.categoryBreakdown.isNotEmpty()) {
            CategoryBreakdownSection(categories = data.categoryBreakdown)
        }
        
        // Top debtors
        if (data.topDebtors.isNotEmpty()) {
            TopListSection(
                title = "Top 5 Who Owe You",
                items = data.topDebtors,
                isDebt = true
            )
        }
        
        // Top creditors
        if (data.topCreditors.isNotEmpty()) {
            TopListSection(
                title = "Top 5 You Owe",
                items = data.topCreditors,
                isDebt = false
            )
        }
        
        // Unrecovered amount
        UnrecoveredAmountCard(amount = data.unrecoveredAmount)
    }
}

/**
 * Monthly summary card showing outflow, inflow, and net balance.
 */
@Composable
private fun MonthlySummaryCard(data: AnalyticsData) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    val summary = data.monthlySummary
    
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Outflow
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Outflow",
                    fontSize = 12.sp,
                    color = skinColors.textSecondary
                )
                AmountDisplay(
                    amount = summary.totalOutflow,
                    color = skinColors.gaveColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Inflow
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Inflow",
                    fontSize = 12.sp,
                    color = skinColors.textSecondary
                )
                AmountDisplay(
                    amount = summary.totalInflow,
                    color = skinColors.receivedColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Net
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Net",
                    fontSize = 12.sp,
                    color = skinColors.textSecondary
                )
                val netColor = when {
                    summary.netBalance > BigDecimal.ZERO -> skinColors.receivedColor
                    summary.netBalance < BigDecimal.ZERO -> skinColors.gaveColor
                    else -> skinColors.textPrimary
                }
                AmountDisplay(
                    amount = summary.netBalance.abs(),
                    color = netColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Category breakdown section.
 */
@Composable
private fun CategoryBreakdownSection(categories: List<CategoryBreakdown>) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "By Category",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = skinColors.textPrimary
        )
        
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                categories.forEach { breakdown ->
                    CategoryBreakdownItem(breakdown = breakdown)
                }
            }
        }
    }
}

/**
 * Individual category breakdown item.
 */
@Composable
private fun CategoryBreakdownItem(breakdown: CategoryBreakdown) {
    val skinColors = LocalSkinColors.current
    val formatter = DecimalFormat("#,##0.00")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = breakdown.category.name,
            fontSize = 14.sp,
            color = skinColors.textPrimary
        )
        
        Text(
            text = "₹${formatter.format(breakdown.total)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = skinColors.textPrimary
        )
    }
}

/**
 * Top debtors or creditors list section.
 */
@Composable
private fun TopListSection(
    title: String,
    items: List<CounterpartyAmount>,
    isDebt: Boolean
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = skinColors.textPrimary
        )
        
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items.forEachIndexed { index, item ->
                    CounterpartyAmountItem(
                        rank = index + 1,
                        item = item,
                        isDebt = isDebt
                    )
                }
            }
        }
    }
}

/**
 * Individual counterparty amount item.
 */
@Composable
private fun CounterpartyAmountItem(
    rank: Int,
    item: CounterpartyAmount,
    isDebt: Boolean
) {
    val skinColors = LocalSkinColors.current
    val formatter = DecimalFormat("#,##0.00")
    val amountColor = if (isDebt) skinColors.receivedColor else skinColors.gaveColor
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$rank.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = skinColors.textSecondary
            )
            Text(
                text = item.counterparty.displayName,
                fontSize = 14.sp,
                color = skinColors.textPrimary
            )
        }
        
        Text(
            text = "₹${formatter.format(item.amount)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = amountColor
        )
    }
}

/**
 * Unrecovered amount card for last 12 months.
 */
@Composable
private fun UnrecoveredAmountCard(amount: BigDecimal) {
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Unrecovered (12 months)",
                fontSize = 14.sp,
                color = skinColors.textSecondary
            )
            
            AmountDisplay(
                amount = amount,
                color = skinColors.gaveColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
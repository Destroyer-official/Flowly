package com.ledger.app.presentation.ui.bills

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ledger.app.domain.model.BillCategory
import com.ledger.app.domain.model.Transaction
import com.ledger.app.presentation.theme.LocalSkinColors
import com.ledger.app.presentation.theme.LocalSkinShapes
import com.ledger.app.presentation.viewmodel.BillHistoryViewModel
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter


/**
 * Bill History screen showing all bill payments and recharges.
 * 
 * Features:
 * - List all bill payments and recharges
 * - Filter by category (Electricity, TV, Mobile, Internet)
 * - Show consumer ID and for whom
 * 
 * Requirements: 3.5
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillHistoryScreen(
    onNavigateBack: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    viewModel: BillHistoryViewModel = hiltViewModel()
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val forSelfFilter by viewModel.forSelfFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bill History",
                        fontWeight = FontWeight.Bold,
                        color = skinColors.textPrimary
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
        containerColor = skinColors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Category filter chips
            CategoryFilterChips(
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.setCategory(it) },
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // For Self / For Others filter
            ForSelfFilterChips(
                forSelfFilter = forSelfFilter,
                onFilterSelected = { viewModel.setForSelfFilter(it) },
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Loading or content
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = skinColors.primary)
                }
            } else if (filteredTransactions.isEmpty()) {
                EmptyBillHistory()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTransactions) { transaction ->
                        BillTransactionItem(
                            transaction = transaction,
                            onClick = { onTransactionClick(transaction.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

/**
 * Category filter chips for bill types.
 */
@Composable
private fun CategoryFilterChips(
    selectedCategory: BillCategory?,
    onCategorySelected: (BillCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    Column(modifier = modifier) {
        Text(
            text = "Filter by Type",
            fontSize = 12.sp,
            color = skinColors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // All chip
            FilterChip(
                text = "All",
                isSelected = selectedCategory == null,
                onClick = { onCategorySelected(null) }
            )
            
            // Category chips
            BillCategory.values().take(4).forEach { category ->
                FilterChip(
                    text = getCategoryEmoji(category),
                    isSelected = selectedCategory == category,
                    onClick = { onCategorySelected(category) }
                )
            }
        }
    }
}

/**
 * For Self / For Others filter chips.
 */
@Composable
private fun ForSelfFilterChips(
    forSelfFilter: Boolean?,
    onFilterSelected: (Boolean?) -> Unit,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    
    Column(modifier = modifier) {
        Text(
            text = "Paid For",
            fontSize = 12.sp,
            color = skinColors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                text = "All",
                isSelected = forSelfFilter == null,
                onClick = { onFilterSelected(null) }
            )
            FilterChip(
                text = "Self",
                isSelected = forSelfFilter == true,
                onClick = { onFilterSelected(true) }
            )
            FilterChip(
                text = "Others",
                isSelected = forSelfFilter == false,
                onClick = { onFilterSelected(false) }
            )
        }
    }
}

/**
 * Reusable filter chip component.
 */
@Composable
private fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    val backgroundColor = if (isSelected) skinColors.primary else skinColors.surface
    val textColor = if (isSelected) skinColors.background else skinColors.textPrimary
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(skinShapes.buttonCornerRadius))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = if (isSelected) skinColors.primary else skinColors.textSecondary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(skinShapes.buttonCornerRadius)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}

/**
 * Individual bill transaction item.
 */
@Composable
private fun BillTransactionItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    val formatter = DecimalFormat("#,##0.00")
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy • HH:mm")
    
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(skinColors.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getCategoryEmoji(transaction.billCategory),
                    fontSize = 24.sp
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Category name and for whom
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.billCategory?.name?.lowercase()?.replaceFirstChar { it.uppercase() } 
                            ?: "Bill",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = skinColors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // For self/others badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (transaction.isForSelf) 
                                    skinColors.receivedColor.copy(alpha = 0.15f)
                                else 
                                    skinColors.gaveColor.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (transaction.isForSelf) "Self" else "Other",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (transaction.isForSelf) 
                                skinColors.receivedColor 
                            else 
                                skinColors.gaveColor
                        )
                    }
                }
                
                // Consumer ID if available
                transaction.consumerId?.let { consumerId ->
                    Text(
                        text = "ID: $consumerId",
                        fontSize = 13.sp,
                        color = skinColors.textSecondary
                    )
                }
                
                // Date
                Text(
                    text = transaction.transactionDateTime.format(dateFormatter),
                    fontSize = 12.sp,
                    color = skinColors.textSecondary.copy(alpha = 0.7f)
                )
            }
            
            // Amount
            Text(
                text = "₹${formatter.format(transaction.amount)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = skinColors.gaveColor
            )
        }
    }
}

/**
 * Empty state when no bills found.
 */
@Composable
private fun EmptyBillHistory() {
    val skinColors = LocalSkinColors.current
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📋",
                fontSize = 48.sp
            )
            Text(
                text = "No bill payments found",
                fontSize = 16.sp,
                color = skinColors.textSecondary
            )
            Text(
                text = "Add a bill payment to see it here",
                fontSize = 14.sp,
                color = skinColors.textSecondary.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Gets the emoji for a bill category.
 */
private fun getCategoryEmoji(category: BillCategory?): String {
    return when (category) {
        BillCategory.ELECTRICITY -> "⚡"
        BillCategory.TV -> "📺"
        BillCategory.MOBILE -> "📱"
        BillCategory.INTERNET -> "🌐"
        BillCategory.OTHER -> "📋"
        null -> "📋"
    }
}

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ledger.app.domain.model.CounterpartyWithBalance
import com.ledger.app.presentation.theme.LocalSkinColors
import com.ledger.app.presentation.theme.LocalSkinShapes
import com.ledger.app.presentation.ui.components.AmountDisplay
import com.ledger.app.presentation.viewmodel.CounterpartyListViewModel
import java.math.BigDecimal

/**
 * Counterparty List screen composable.
 * 
 * Features:
 * - Search bar for filtering counterparties by name or phone
 * - List of counterparties with net balance
 * - Favorites shown first
 * - Click to navigate to counterparty ledger
 * 
 * Requirements: 11.2, 11.3, 11.4
 */
@Composable
fun CounterpartyListScreen(
    onCounterpartyClick: (Long) -> Unit = {},
    viewModel: CounterpartyListViewModel = hiltViewModel()
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    val counterparties by viewModel.counterparties.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(skinColors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "People",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = skinColors.textPrimary
        )

        // Search Bar (Requirements: 11.2)
        SearchBar(
            query = searchQuery,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            onClearClick = { viewModel.clearSearch() }
        )

        // Counterparty List
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = skinColors.primary)
            }
        } else if (counterparties.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank()) {
                        "No counterparties yet"
                    } else {
                        "No results found"
                    },
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
                    items = counterparties,
                    key = { it.counterparty.id }
                ) { counterpartyWithBalance ->
                    CounterpartyListItem(
                        counterpartyWithBalance = counterpartyWithBalance,
                        onClick = { onCounterpartyClick(counterpartyWithBalance.counterparty.id) }
                    )
                }
            }
        }
    }
}

/**
 * Search bar component for filtering counterparties.
 * 
 * Requirements: 11.2
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current

    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth(),
        placeholder = {
            Text(
                text = "Search by name or phone...",
                color = skinColors.textSecondary
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = skinColors.textSecondary
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClearClick) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = skinColors.textSecondary
                    )
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = skinColors.surface,
            unfocusedContainerColor = skinColors.surface,
            focusedTextColor = skinColors.textPrimary,
            unfocusedTextColor = skinColors.textPrimary,
            cursorColor = skinColors.primary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(skinShapes.buttonCornerRadius),
        singleLine = true
    )
}

/**
 * List item component for displaying a counterparty with their net balance.
 * 
 * Features:
 * - Displays counterparty name and phone (if available)
 * - Shows favorite star icon for favorites
 * - Displays net balance with color coding:
 *   - Green (receivedColor) if they owe user (positive balance)
 *   - Red (gaveColor) if user owes them (negative balance)
 * 
 * Requirements: 11.3, 11.4
 */
@Composable
private fun CounterpartyListItem(
    counterpartyWithBalance: CounterpartyWithBalance,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    val counterparty = counterpartyWithBalance.counterparty
    val netBalance = counterpartyWithBalance.netBalance
    
    // Determine if we're using glassmorphism based on blur radius
    val isGlass = skinShapes.blurRadius > 0.dp
    
    // Determine balance color: positive = they owe user (green), negative = user owes them (red)
    val balanceColor = when {
        netBalance > BigDecimal.ZERO -> skinColors.receivedColor
        netBalance < BigDecimal.ZERO -> skinColors.gaveColor
        else -> skinColors.textSecondary
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
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(skinShapes.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = if (isGlass) {
                skinColors.cardBackground.copy(alpha = 0.7f)
            } else {
                skinColors.cardBackground
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Avatar, Name, Phone
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with first letter
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = skinColors.primary.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = counterparty.displayName.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = skinColors.primary
                    )
                }

                // Name and phone
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = counterparty.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = skinColors.textPrimary
                        )
                        
                        // Favorite star icon (Requirements: 11.4)
                        if (counterparty.isFavorite) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Favorite",
                                tint = skinColors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    counterparty.phoneNumber?.let { phone ->
                        Text(
                            text = phone,
                            fontSize = 12.sp,
                            color = skinColors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right side: Net Balance
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                AmountDisplay(
                    amount = netBalance.abs(),
                    color = balanceColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = when {
                        netBalance > BigDecimal.ZERO -> "owes you"
                        netBalance < BigDecimal.ZERO -> "you owe"
                        else -> "settled"
                    },
                    fontSize = 11.sp,
                    color = skinColors.textSecondary
                )
            }
        }
    }
}

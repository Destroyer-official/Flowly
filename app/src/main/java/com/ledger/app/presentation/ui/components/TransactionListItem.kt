package com.ledger.app.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.domain.model.TransactionDirection
import com.ledger.app.domain.model.TransactionStatus
import com.ledger.app.presentation.theme.LocalSkinColors
import com.ledger.app.presentation.theme.LocalSkinShapes
import java.math.BigDecimal
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * List item component for displaying transaction information.
 * 
 * Features:
 * - Direction indicator (arrow up for GAVE, arrow down for RECEIVED)
 * - Amount with color coding
 * - Counterparty name
 * - Category name
 * - Date/time
 * - Status indicator (pending, settled, etc.)
 * 
 * Requirements: 8.3
 */
@Composable
fun TransactionListItem(
    amount: BigDecimal,
    direction: TransactionDirection,
    counterpartyName: String?,
    categoryName: String,
    dateTime: LocalDateTime,
    status: TransactionStatus,
    remainingDue: BigDecimal? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    // Determine colors based on direction
    val directionColor = when (direction) {
        TransactionDirection.GAVE -> skinColors.gaveColor
        TransactionDirection.RECEIVED -> skinColors.receivedColor
    }
    
    // Direction icon
    val directionIcon = when (direction) {
        TransactionDirection.GAVE -> Icons.Default.ArrowUpward
        TransactionDirection.RECEIVED -> Icons.Default.ArrowDownward
    }
    
    // Format date
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
    val formattedDate = dateTime.format(dateFormatter)
    
    // Format amount
    val amountFormatter = DecimalFormat("#,##0.00")
    val formattedAmount = amountFormatter.format(amount)
    
    // Status text
    val statusText = when (status) {
        TransactionStatus.PENDING -> "Pending"
        TransactionStatus.PARTIALLY_SETTLED -> "Partial"
        TransactionStatus.SETTLED -> "Settled"
        TransactionStatus.CANCELLED -> "Cancelled"
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (skinShapes.elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = skinShapes.elevation / 2,
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius),
                        ambientColor = directionColor.copy(alpha = 0.08f),
                        spotColor = directionColor.copy(alpha = 0.05f)
                    )
                } else {
                    Modifier
                }
            )
            .border(
                width = 1.dp,
                color = directionColor.copy(alpha = 0.1f),
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Direction indicator
            Icon(
                imageVector = directionIcon,
                contentDescription = when (direction) {
                    TransactionDirection.GAVE -> "Gave money"
                    TransactionDirection.RECEIVED -> "Received money"
                },
                tint = directionColor,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = directionColor.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
                    .padding(8.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Transaction details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Counterparty and amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = counterpartyName ?: "Self",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = skinColors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = "₹$formattedAmount",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = directionColor
                    )
                }
                
                // Category and date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = categoryName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = skinColors.textSecondary
                    )
                    
                    Text(
                        text = formattedDate,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = skinColors.textSecondary
                    )
                }
                
                // Status and remaining due
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status indicator
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = when (status) {
                            TransactionStatus.SETTLED -> skinColors.receivedColor
                            TransactionStatus.CANCELLED -> skinColors.error
                            else -> skinColors.textSecondary
                        },
                        modifier = Modifier
                            .background(
                                color = when (status) {
                                    TransactionStatus.SETTLED -> skinColors.receivedColor.copy(alpha = 0.1f)
                                    TransactionStatus.CANCELLED -> skinColors.error.copy(alpha = 0.1f)
                                    else -> skinColors.textSecondary.copy(alpha = 0.1f)
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    
                    // Remaining due if applicable
                    if (remainingDue != null && remainingDue > BigDecimal.ZERO && status != TransactionStatus.SETTLED) {
                        Text(
                            text = "Due: ₹${amountFormatter.format(remainingDue)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = skinColors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

package com.ledger.app.presentation.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.domain.model.TransactionDirection
import com.ledger.app.presentation.theme.LocalSkinColors
import java.math.BigDecimal
import java.text.DecimalFormat

/**
 * Displays a monetary amount with currency symbol and color coding.
 * 
 * Color coding:
 * - GAVE (outflow): Uses gaveColor from theme
 * - RECEIVED (inflow): Uses receivedColor from theme
 * 
 * Requirements: 9.4
 */
@Composable
fun AmountDisplay(
    amount: BigDecimal,
    direction: TransactionDirection? = null,
    currency: String = "₹",
    fontSize: TextUnit = 32.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    
    // Determine color based on direction
    val amountColor = when (direction) {
        TransactionDirection.GAVE -> skinColors.gaveColor
        TransactionDirection.RECEIVED -> skinColors.receivedColor
        null -> skinColors.textPrimary
    }
    
    // Format amount with proper decimal places
    val formatter = DecimalFormat("#,##0.00")
    val formattedAmount = formatter.format(amount)
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currency,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = amountColor
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = formattedAmount,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = amountColor
        )
    }
}

/**
 * Displays an amount with explicit color override.
 */
@Composable
fun AmountDisplay(
    amount: BigDecimal,
    color: Color,
    currency: String = "₹",
    fontSize: TextUnit = 32.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    maxLines: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier
) {
    val formatter = DecimalFormat("#,##0.00")
    val formattedAmount = formatter.format(amount)
    
    Text(
        text = "$currency$formattedAmount",
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

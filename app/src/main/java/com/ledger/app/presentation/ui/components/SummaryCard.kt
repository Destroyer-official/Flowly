package com.ledger.app.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.presentation.theme.DesignSkin
import com.ledger.app.presentation.theme.LocalSkinColors
import com.ledger.app.presentation.theme.LocalSkinShapes
import java.math.BigDecimal

/**
 * A card component for displaying summary information with title and amount.
 * Applies premium 2026-style skin-aware styling including glass effects, shadows, and borders.
 * 
 * Requirements: 8.1
 */
@Composable
fun SummaryCard(
    title: String,
    amount: BigDecimal,
    amountColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    // Determine if we're using glassmorphism based on blur radius
    val isGlass = skinShapes.blurRadius > 0.dp
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (skinShapes.elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = skinShapes.elevation,
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius),
                        ambientColor = (amountColor ?: skinColors.primary).copy(alpha = 0.15f),
                        spotColor = (amountColor ?: skinColors.primary).copy(alpha = 0.1f)
                    )
                } else {
                    Modifier
                }
            )
            .border(
                width = 1.dp,
                color = (amountColor ?: skinColors.primary).copy(alpha = 0.15f),
                shape = RoundedCornerShape(skinShapes.cardCornerRadius)
            ),
        shape = RoundedCornerShape(skinShapes.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = skinColors.cardBackground
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            skinColors.cardBackground,
                            skinColors.cardBackground.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = skinColors.textSecondary,
                letterSpacing = 0.3.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            AmountDisplay(
                amount = amount,
                color = amountColor ?: skinColors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

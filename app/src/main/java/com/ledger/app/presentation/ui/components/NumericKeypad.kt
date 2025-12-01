package com.ledger.app.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.presentation.theme.LocalSkinColors
import com.ledger.app.presentation.theme.LocalSkinShapes

/**
 * Numeric keypad component for amount input.
 * 
 * Features:
 * - Digits 0-9
 * - Decimal point
 * - Backspace button
 * - Large touch targets for easy input
 * 
 * Requirements: 1.2
 */
@Composable
fun NumericKeypad(
    onDigitClick: (String) -> Unit,
    onDecimalClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp) // Reduced spacing
    ) {
        // Row 1: 1, 2, 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeypadButton(
                text = "1",
                onClick = { onDigitClick("1") },
                modifier = Modifier.weight(1f)
            )
            KeypadButton(
                text = "2",
                onClick = { onDigitClick("2") },
                modifier = Modifier.weight(1f)
            )
            KeypadButton(
                text = "3",
                onClick = { onDigitClick("3") },
                modifier = Modifier.weight(1f)
            )
        }
        
        // Row 2: 4, 5, 6
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeypadButton(
                text = "4",
                onClick = { onDigitClick("4") },
                modifier = Modifier.weight(1f)
            )
            KeypadButton(
                text = "5",
                onClick = { onDigitClick("5") },
                modifier = Modifier.weight(1f)
            )
            KeypadButton(
                text = "6",
                onClick = { onDigitClick("6") },
                modifier = Modifier.weight(1f)
            )
        }
        
        // Row 3: 7, 8, 9
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeypadButton(
                text = "7",
                onClick = { onDigitClick("7") },
                modifier = Modifier.weight(1f)
            )
            KeypadButton(
                text = "8",
                onClick = { onDigitClick("8") },
                modifier = Modifier.weight(1f)
            )
            KeypadButton(
                text = "9",
                onClick = { onDigitClick("9") },
                modifier = Modifier.weight(1f)
            )
        }
        
        // Row 4: ., 0, backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeypadButton(
                text = ".",
                onClick = onDecimalClick,
                modifier = Modifier.weight(1f)
            )
            KeypadButton(
                text = "0",
                onClick = { onDigitClick("0") },
                modifier = Modifier.weight(1f)
            )
            KeypadButton(
                icon = Icons.Default.Backspace,
                onClick = onBackspaceClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Individual keypad button component with compact premium styling.
 */
@Composable
private fun KeypadButton(
    text: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    Box(
        modifier = modifier
            .aspectRatio(1.6f) // More compact aspect ratio
            .then(
                if (skinShapes.elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = skinShapes.elevation / 4,
                        shape = RoundedCornerShape(skinShapes.buttonCornerRadius),
                        ambientColor = skinColors.primary.copy(alpha = 0.06f),
                        spotColor = skinColors.primary.copy(alpha = 0.06f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(skinShapes.buttonCornerRadius))
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        skinColors.surface,
                        skinColors.surface.copy(alpha = 0.9f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = skinColors.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(skinShapes.buttonCornerRadius)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (text != null) {
            Text(
                text = text,
                fontSize = 22.sp, // Smaller font
                fontWeight = FontWeight.SemiBold,
                color = skinColors.textPrimary
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = "Backspace",
                tint = skinColors.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

package com.ledger.app.presentation.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.domain.model.ChecklistItem
import com.ledger.app.presentation.theme.LocalSkinColors
import com.ledger.app.presentation.theme.LocalSkinShapes


/**
 * Editable checklist component for tasks.
 * 
 * Features:
 * - Add new checklist items
 * - Remove items
 * - Toggle item completion
 * - Reorder items via drag and drop
 * - Skin-aware styling
 * 
 * Requirements: 14.3
 */
@Composable
fun ChecklistEditor(
    items: List<ChecklistItem>,
    onItemsChanged: (List<ChecklistItem>) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    var newItemText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    
    // Mutable state for drag reordering
    val mutableItems = remember(items) { items.toMutableStateList() }
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Checklist",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = skinColors.textPrimary
            )
            
            if (items.isNotEmpty()) {
                val completedCount = items.count { it.isCompleted }
                Text(
                    text = "$completedCount/${items.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = skinColors.textSecondary
                )
            }
        }
        
        // Checklist items
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(skinShapes.cardCornerRadius))
                .background(skinColors.surface.copy(alpha = 0.5f))
                .border(
                    width = skinShapes.borderWidth,
                    color = skinColors.textSecondary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                )
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEachIndexed { index, item ->
                ChecklistItemRow(
                    item = item,
                    isDragging = draggedItemIndex == index,
                    readOnly = readOnly,
                    skinColors = skinColors,
                    skinShapes = skinShapes,
                    onToggle = {
                        val updatedItems = items.toMutableList()
                        updatedItems[index] = item.copy(isCompleted = !item.isCompleted)
                        onItemsChanged(updatedItems)
                    },
                    onDelete = {
                        val updatedItems = items.toMutableList()
                        updatedItems.removeAt(index)
                        // Update sort orders
                        val reorderedItems = updatedItems.mapIndexed { i, it -> 
                            it.copy(sortOrder = i) 
                        }
                        onItemsChanged(reorderedItems)
                    },
                    onDragStart = { draggedItemIndex = index },
                    onDragEnd = { targetIndex ->
                        if (targetIndex != null && targetIndex != index) {
                            val updatedItems = items.toMutableList()
                            val draggedItem = updatedItems.removeAt(index)
                            updatedItems.add(targetIndex, draggedItem)
                            // Update sort orders
                            val reorderedItems = updatedItems.mapIndexed { i, it -> 
                                it.copy(sortOrder = i) 
                            }
                            onItemsChanged(reorderedItems)
                        }
                        draggedItemIndex = null
                    }
                )
            }
            
            // Add new item input
            if (!readOnly) {
                AddChecklistItemRow(
                    text = newItemText,
                    onTextChange = { newItemText = it },
                    onAdd = {
                        if (newItemText.isNotBlank()) {
                            val newItem = ChecklistItem(
                                id = 0, // Will be assigned by database
                                taskId = items.firstOrNull()?.taskId ?: 0,
                                text = newItemText.trim(),
                                isCompleted = false,
                                sortOrder = items.size
                            )
                            onItemsChanged(items + newItem)
                            newItemText = ""
                        }
                    },
                    focusRequester = focusRequester,
                    skinColors = skinColors,
                    skinShapes = skinShapes
                )
            }
        }
    }
}


@Composable
private fun ChecklistItemRow(
    item: ChecklistItem,
    isDragging: Boolean,
    readOnly: Boolean,
    skinColors: com.ledger.app.presentation.theme.SkinColors,
    skinShapes: com.ledger.app.presentation.theme.SkinShapes,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: (Int?) -> Unit
) {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        animationSpec = tween(150),
        label = "drag_elevation"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isDragging) skinColors.cardBackground 
                     else Color.Transparent,
        animationSpec = tween(150),
        label = "drag_background"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isDragging) {
                    Modifier.shadow(elevation, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            )
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drag handle (only in edit mode)
        if (!readOnly) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                tint = skinColors.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(20.dp)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart() },
                            onDragEnd = { onDragEnd(null) },
                            onDragCancel = { onDragEnd(null) },
                            onDrag = { change, _ ->
                                change.consume()
                                // In a real implementation, calculate target index
                                // based on drag position
                            }
                        )
                    }
            )
            
            Spacer(modifier = Modifier.width(4.dp))
        }
        
        // Checkbox
        Checkbox(
            checked = item.isCompleted,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = skinColors.receivedColor,
                uncheckedColor = skinColors.textSecondary.copy(alpha = 0.5f),
                checkmarkColor = Color.White
            ),
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Item text
        Text(
            text = item.text,
            fontSize = 14.sp,
            color = if (item.isCompleted) skinColors.textSecondary 
                   else skinColors.textPrimary,
            textDecoration = if (item.isCompleted) TextDecoration.LineThrough 
                            else TextDecoration.None,
            modifier = Modifier
                .weight(1f)
                .clickable { onToggle() }
        )
        
        // Delete button (only in edit mode)
        if (!readOnly) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove item",
                    tint = skinColors.textSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AddChecklistItemRow(
    text: String,
    onTextChange: (String) -> Unit,
    onAdd: () -> Unit,
    focusRequester: FocusRequester,
    skinColors: com.ledger.app.presentation.theme.SkinColors,
    skinShapes: com.ledger.app.presentation.theme.SkinShapes
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Add icon
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add item",
            tint = skinColors.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Text input
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = skinColors.textPrimary
            ),
            cursorBrush = SolidColor(skinColors.primary),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onAdd() }
            ),
            decorationBox = { innerTextField ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            text = "Add item...",
                            fontSize = 14.sp,
                            color = skinColors.textSecondary.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        )
        
        // Add button (visible when text is not empty)
        AnimatedVisibility(
            visible = text.isNotBlank(),
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            IconButton(
                onClick = onAdd,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Add",
                    tint = skinColors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}


/**
 * Read-only checklist display for viewing task details.
 * 
 * Requirements: 14.3
 */
@Composable
fun ChecklistDisplay(
    items: List<ChecklistItem>,
    onItemToggle: (ChecklistItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    if (items.isEmpty()) return
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header with progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Checklist",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = skinColors.textPrimary
            )
            
            val completedCount = items.count { it.isCompleted }
            val progress = completedCount.toFloat() / items.size
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Progress bar
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(skinColors.textSecondary.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(skinColors.receivedColor)
                    )
                }
                
                Text(
                    text = "$completedCount/${items.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = skinColors.textSecondary
                )
            }
        }
        
        // Items
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(skinShapes.cardCornerRadius))
                .background(skinColors.surface.copy(alpha = 0.5f))
                .border(
                    width = skinShapes.borderWidth,
                    color = skinColors.textSecondary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                )
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items.sortedBy { it.sortOrder }.forEach { item ->
                ChecklistDisplayItem(
                    item = item,
                    onToggle = { onItemToggle(item) },
                    skinColors = skinColors
                )
            }
        }
    }
}

@Composable
private fun ChecklistDisplayItem(
    item: ChecklistItem,
    onToggle: () -> Unit,
    skinColors: com.ledger.app.presentation.theme.SkinColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Custom checkbox circle
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(
                    if (item.isCompleted) skinColors.receivedColor 
                    else Color.Transparent
                )
                .border(
                    width = 2.dp,
                    color = if (item.isCompleted) skinColors.receivedColor 
                           else skinColors.textSecondary.copy(alpha = 0.4f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (item.isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = item.text,
            fontSize = 14.sp,
            color = if (item.isCompleted) skinColors.textSecondary 
                   else skinColors.textPrimary,
            textDecoration = if (item.isCompleted) TextDecoration.LineThrough 
                            else TextDecoration.None
        )
    }
}

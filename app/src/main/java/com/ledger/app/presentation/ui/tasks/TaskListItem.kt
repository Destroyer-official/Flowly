package com.ledger.app.presentation.ui.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.domain.model.Task
import com.ledger.app.domain.model.TaskPriority
import com.ledger.app.domain.model.TaskStatus
import com.ledger.app.presentation.theme.LocalSkinColors
import com.ledger.app.presentation.theme.LocalSkinShapes
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


/**
 * List item component for displaying task information with swipe actions.
 * 
 * Features:
 * - Title with priority indicator
 * - Due date display with overdue highlighting
 * - Checkbox for quick completion
 * - Swipe actions (delete, mark done)
 * - Skin-aware styling
 * 
 * Requirements: 14.1, 14.5
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListItem(
    task: Task,
    onTaskClick: (Task) -> Unit,
    onTaskComplete: (Task) -> Unit,
    onTaskDelete: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    var isCompleting by remember { mutableStateOf(false) }
    
    // Priority colors
    val priorityColor = when (task.priority) {
        TaskPriority.HIGH -> Color(0xFFEF4444)    // Red
        TaskPriority.MEDIUM -> Color(0xFFF59E0B)  // Amber
        TaskPriority.LOW -> Color(0xFF10B981)     // Green
    }
    
    // Check if task is overdue
    val isOverdue = task.dueDate?.let { 
        it.isBefore(LocalDateTime.now()) && task.status == TaskStatus.PENDING 
    } ?: false
    
    // Swipe to dismiss state
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onTaskDelete(task)
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (task.status == TaskStatus.PENDING) {
                        isCompleting = true
                        onTaskComplete(task)
                    }
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )
    
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            SwipeBackground(
                dismissValue = dismissState.dismissDirection,
                skinColors = skinColors
            )
        },
        content = {
            TaskItemContent(
                task = task,
                priorityColor = priorityColor,
                isOverdue = isOverdue,
                isCompleting = isCompleting,
                skinColors = skinColors,
                skinShapes = skinShapes,
                onTaskClick = onTaskClick,
                onCheckboxClick = {
                    if (task.status == TaskStatus.PENDING) {
                        isCompleting = true
                        onTaskComplete(task)
                    }
                }
            )
        }
    )
}

@Composable
private fun SwipeBackground(
    dismissValue: SwipeToDismissBoxValue,
    skinColors: com.ledger.app.presentation.theme.SkinColors
) {
    val color by animateColorAsState(
        targetValue = when (dismissValue) {
            SwipeToDismissBoxValue.StartToEnd -> skinColors.receivedColor.copy(alpha = 0.9f)
            SwipeToDismissBoxValue.EndToStart -> skinColors.error.copy(alpha = 0.9f)
            SwipeToDismissBoxValue.Settled -> Color.Transparent
        },
        animationSpec = tween(200),
        label = "swipe_background_color"
    )
    
    val icon = when (dismissValue) {
        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Check
        SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
        SwipeToDismissBoxValue.Settled -> null
    }
    
    val alignment = when (dismissValue) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        SwipeToDismissBoxValue.Settled -> Alignment.Center
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = when (dismissValue) {
                    SwipeToDismissBoxValue.StartToEnd -> "Mark as done"
                    SwipeToDismissBoxValue.EndToStart -> "Delete task"
                    else -> null
                },
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}


@Composable
private fun TaskItemContent(
    task: Task,
    priorityColor: Color,
    isOverdue: Boolean,
    isCompleting: Boolean,
    skinColors: com.ledger.app.presentation.theme.SkinColors,
    skinShapes: com.ledger.app.presentation.theme.SkinShapes,
    onTaskClick: (Task) -> Unit,
    onCheckboxClick: () -> Unit
) {
    val isCompleted = task.status == TaskStatus.COMPLETED
    
    // Animation for completion
    val scale by animateFloatAsState(
        targetValue = if (isCompleting) 0.95f else 1f,
        animationSpec = tween(150),
        label = "task_scale"
    )
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (isCompleted) 0.6f else 1f,
        animationSpec = tween(200),
        label = "content_alpha"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { onTaskClick(task) }
            .then(
                if (skinShapes.elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = skinShapes.elevation / 2,
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius),
                        ambientColor = priorityColor.copy(alpha = 0.08f),
                        spotColor = priorityColor.copy(alpha = 0.05f)
                    )
                } else {
                    Modifier
                }
            )
            .border(
                width = skinShapes.borderWidth,
                color = if (isOverdue) skinColors.error.copy(alpha = 0.3f) 
                       else priorityColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(skinShapes.cardCornerRadius)
            ),
        shape = RoundedCornerShape(skinShapes.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = skinColors.cardBackground.copy(alpha = contentAlpha)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority indicator bar
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(priorityColor.copy(alpha = if (isCompleted) 0.4f else 1f))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Checkbox for quick completion
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { if (!isCompleted) onCheckboxClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = skinColors.receivedColor,
                    uncheckedColor = skinColors.textSecondary,
                    checkmarkColor = Color.White
                ),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Task details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title row with priority icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Priority icon
                    PriorityIcon(
                        priority = task.priority,
                        color = priorityColor.copy(alpha = if (isCompleted) 0.5f else 1f)
                    )
                    
                    // Title
                    Text(
                        text = task.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = skinColors.textPrimary.copy(alpha = contentAlpha),
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Description preview (if exists)
                task.description?.let { desc ->
                    if (desc.isNotBlank()) {
                        Text(
                            text = desc,
                            fontSize = 13.sp,
                            color = skinColors.textSecondary.copy(alpha = contentAlpha),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Bottom row: due date and checklist count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Due date
                    task.dueDate?.let { dueDate ->
                        DueDateChip(
                            dueDate = dueDate,
                            isOverdue = isOverdue,
                            isCompleted = isCompleted,
                            skinColors = skinColors
                        )
                    }
                    
                    // Checklist progress
                    if (task.checklistItems.isNotEmpty()) {
                        val completedCount = task.checklistItems.count { it.isCompleted }
                        val totalCount = task.checklistItems.size
                        
                        Text(
                            text = "✓ $completedCount/$totalCount",
                            fontSize = 12.sp,
                            color = skinColors.textSecondary.copy(alpha = contentAlpha),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun PriorityIcon(
    priority: TaskPriority,
    color: Color
) {
    val icon = when (priority) {
        TaskPriority.HIGH -> Icons.Default.KeyboardArrowUp
        TaskPriority.MEDIUM -> Icons.Default.Remove
        TaskPriority.LOW -> Icons.Default.KeyboardArrowDown
    }
    
    Icon(
        imageVector = icon,
        contentDescription = "${priority.name} priority",
        tint = color,
        modifier = Modifier.size(16.dp)
    )
}

@Composable
private fun DueDateChip(
    dueDate: LocalDateTime,
    isOverdue: Boolean,
    isCompleted: Boolean,
    skinColors: com.ledger.app.presentation.theme.SkinColors
) {
    val now = LocalDateTime.now()
    val daysUntilDue = ChronoUnit.DAYS.between(now.toLocalDate(), dueDate.toLocalDate())
    
    val (text, chipColor) = when {
        isCompleted -> {
            val formatter = DateTimeFormatter.ofPattern("MMM d")
            dueDate.format(formatter) to skinColors.textSecondary
        }
        isOverdue -> {
            val daysOverdue = -daysUntilDue
            when {
                daysOverdue == 0L -> "Today" to skinColors.error
                daysOverdue == 1L -> "Yesterday" to skinColors.error
                else -> "$daysOverdue days overdue" to skinColors.error
            }
        }
        daysUntilDue == 0L -> "Today" to Color(0xFFF59E0B)
        daysUntilDue == 1L -> "Tomorrow" to Color(0xFFF59E0B)
        daysUntilDue <= 7L -> "In $daysUntilDue days" to skinColors.textSecondary
        else -> {
            val formatter = DateTimeFormatter.ofPattern("MMM d")
            dueDate.format(formatter) to skinColors.textSecondary
        }
    }
    
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = chipColor,
        modifier = Modifier
            .background(
                color = chipColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

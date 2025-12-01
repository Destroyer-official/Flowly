package com.ledger.app.presentation.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ledger.app.domain.model.Task
import com.ledger.app.domain.model.TaskPriority
import com.ledger.app.domain.model.TaskStatus
import com.ledger.app.presentation.theme.LocalSkinColors
import com.ledger.app.presentation.theme.LocalSkinShapes
import com.ledger.app.presentation.viewmodel.TaskDetailsViewModel
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Screen for displaying task details.
 * 
 * Features:
 * - Display all task details
 * - Checklist with checkable items
 * - "Convert to Transaction" / "Paid It" button
 * - Edit and Delete actions
 * - Link to related transaction if exists
 * 
 * Requirements: 14.3, 15.1, 15.5
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToTransaction: (Long) -> Unit,
    onConvertToTransaction: (Long, Long?) -> Unit, // taskId, counterpartyId
    viewModel: TaskDetailsViewModel = hiltViewModel()
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    val task by viewModel.task.collectAsState()
    val linkedCounterparty by viewModel.linkedCounterparty.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val showCompletionAnimation by viewModel.showCompletionAnimation.collectAsState()
    val taskDeleted by viewModel.taskDeleted.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Navigate back when task is deleted
    LaunchedEffect(taskDeleted) {
        if (taskDeleted) {
            onNavigateBack()
        }
    }
    
    // Show error in snackbar
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Task Details",
                        color = skinColors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = skinColors.textPrimary
                        )
                    }
                },
                actions = {
                    task?.let { currentTask ->
                        if (currentTask.status == TaskStatus.PENDING) {
                            IconButton(onClick = { onNavigateToEdit(currentTask.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = skinColors.textSecondary
                                )
                            }
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = skinColors.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = skinColors.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = skinColors.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = skinColors.primary
                    )
                }
                task == null -> {
                    Text(
                        text = "Task not found",
                        modifier = Modifier.align(Alignment.Center),
                        color = skinColors.textSecondary
                    )
                }
                else -> {
                    TaskDetailsContent(
                        task = task!!,
                        linkedCounterpartyName = linkedCounterparty?.displayName,
                        onChecklistItemToggle = { viewModel.toggleChecklistItem(it) },
                        onCompleteTask = { viewModel.completeTask() },
                        onConvertToTransaction = {
                            onConvertToTransaction(task!!.id, task!!.linkedCounterpartyId)
                        },
                        onViewTransaction = { transactionId ->
                            onNavigateToTransaction(transactionId)
                        },
                        canConvertToTransaction = viewModel.canConvertToTransaction(),
                        skinColors = skinColors,
                        skinShapes = skinShapes
                    )
                }
            }
            
            // Completion animation overlay
            AnimatedVisibility(
                visible = showCompletionAnimation,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TaskCompletionOverlay(
                    isVisible = showCompletionAnimation,
                    onDismiss = { viewModel.clearCompletionAnimation() }
                )
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Delete Task",
                    color = skinColors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this task? This action cannot be undone.",
                    color = skinColors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTask()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = skinColors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = skinColors.textSecondary)
                }
            },
            containerColor = skinColors.cardBackground
        )
    }
}

@Composable
private fun TaskDetailsContent(
    task: Task,
    linkedCounterpartyName: String?,
    onChecklistItemToggle: (com.ledger.app.domain.model.ChecklistItem) -> Unit,
    onCompleteTask: () -> Unit,
    onConvertToTransaction: () -> Unit,
    onViewTransaction: (Long) -> Unit,
    canConvertToTransaction: Boolean,
    skinColors: com.ledger.app.presentation.theme.SkinColors,
    skinShapes: com.ledger.app.presentation.theme.SkinShapes
) {
    val isCompleted = task.status == TaskStatus.COMPLETED
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Status badge and priority
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadge(
                status = task.status,
                skinColors = skinColors,
                skinShapes = skinShapes
            )
            
            PriorityBadge(
                priority = task.priority,
                skinShapes = skinShapes
            )
        }
        
        // Title
        Text(
            text = task.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = skinColors.textPrimary,
            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
        )
        
        // Description
        task.description?.let { description ->
            Text(
                text = description,
                fontSize = 15.sp,
                color = skinColors.textSecondary,
                lineHeight = 22.sp
            )
        }
        
        // Due date
        task.dueDate?.let { dueDate ->
            InfoRow(
                icon = Icons.Default.CalendarToday,
                label = "Due Date",
                value = dueDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")),
                isOverdue = !isCompleted && dueDate.isBefore(java.time.LocalDateTime.now()),
                skinColors = skinColors,
                skinShapes = skinShapes
            )
        }
        
        // Linked counterparty
        linkedCounterpartyName?.let { name ->
            InfoRow(
                icon = Icons.Default.Person,
                label = "Linked Person",
                value = name,
                skinColors = skinColors,
                skinShapes = skinShapes
            )
        }
        
        // Linked transaction
        task.linkedTransactionId?.let { transactionId ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(skinShapes.cardCornerRadius))
                    .background(skinColors.primary.copy(alpha = 0.1f))
                    .border(
                        width = skinShapes.borderWidth,
                        color = skinColors.primary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = skinColors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Linked Transaction",
                            fontSize = 12.sp,
                            color = skinColors.textSecondary
                        )
                        Text(
                            text = "Transaction #$transactionId",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = skinColors.primary
                        )
                    }
                    
                    TextButton(onClick = { onViewTransaction(transactionId) }) {
                        Text("View", color = skinColors.primary)
                    }
                }
            }
        }
        
        // Checklist
        if (task.checklistItems.isNotEmpty()) {
            ChecklistDisplay(
                items = task.checklistItems,
                onItemToggle = onChecklistItemToggle
            )
        }
        
        // Created/Completed dates
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Created: ${task.createdAt.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"))}",
                fontSize = 12.sp,
                color = skinColors.textSecondary
            )
            
            task.completedAt?.let { completedAt ->
                Text(
                    text = "Completed: ${completedAt.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"))}",
                    fontSize = 12.sp,
                    color = skinColors.receivedColor
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action buttons
        if (!isCompleted) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Complete task button
                Button(
                    onClick = onCompleteTask,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = skinColors.receivedColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mark as Complete",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Convert to transaction button (Requirements 15.1)
                if (canConvertToTransaction) {
                    OutlinedButton(
                        onClick = onConvertToTransaction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = skinColors.primary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 2.dp,
                            color = skinColors.primary
                        ),
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Convert to Transaction",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun StatusBadge(
    status: TaskStatus,
    skinColors: com.ledger.app.presentation.theme.SkinColors,
    skinShapes: com.ledger.app.presentation.theme.SkinShapes
) {
    val (text, color) = when (status) {
        TaskStatus.PENDING -> "Pending" to skinColors.primary
        TaskStatus.COMPLETED -> "Completed" to skinColors.receivedColor
        TaskStatus.CANCELLED -> "Cancelled" to skinColors.error
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(skinShapes.cardCornerRadius / 2))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun PriorityBadge(
    priority: TaskPriority,
    skinShapes: com.ledger.app.presentation.theme.SkinShapes
) {
    val (text, color, icon) = when (priority) {
        TaskPriority.HIGH -> Triple("High", Color(0xFFEF4444), Icons.Default.KeyboardArrowUp)
        TaskPriority.MEDIUM -> Triple("Medium", Color(0xFFF59E0B), Icons.Default.Remove)
        TaskPriority.LOW -> Triple("Low", Color(0xFF10B981), Icons.Default.KeyboardArrowDown)
    }
    
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(skinShapes.cardCornerRadius / 2))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isOverdue: Boolean = false,
    skinColors: com.ledger.app.presentation.theme.SkinColors,
    skinShapes: com.ledger.app.presentation.theme.SkinShapes
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(skinShapes.cardCornerRadius))
            .background(
                if (isOverdue) skinColors.error.copy(alpha = 0.1f)
                else skinColors.surface
            )
            .border(
                width = skinShapes.borderWidth,
                color = if (isOverdue) skinColors.error.copy(alpha = 0.3f)
                       else skinColors.textSecondary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(skinShapes.cardCornerRadius)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isOverdue) skinColors.error else skinColors.primary,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = skinColors.textSecondary
            )
            Text(
                text = if (isOverdue) "$value (Overdue)" else value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isOverdue) skinColors.error else skinColors.textPrimary
            )
        }
    }
}

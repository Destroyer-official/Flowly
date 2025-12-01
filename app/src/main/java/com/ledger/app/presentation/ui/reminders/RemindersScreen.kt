package com.ledger.app.presentation.ui.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ledger.app.domain.model.Reminder
import com.ledger.app.domain.model.RepeatPattern
import com.ledger.app.presentation.viewmodel.RemindersViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Screen for managing reminders.
 * 
 * Requirements: 6.1, 6.5
 * - List upcoming reminders
 * - Create new reminder dialog
 * - Mark done, snooze actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: RemindersViewModel = hiltViewModel()
) {
    val upcomingReminders by viewModel.upcomingReminders.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminders") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateDialog() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Reminder")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (upcomingReminders.isEmpty()) {
                EmptyRemindersState(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                RemindersList(
                    reminders = upcomingReminders,
                    onMarkDone = { viewModel.markDone(it) },
                    onSnooze = { viewModel.snooze(it) },
                    onDelete = { viewModel.deleteReminder(it) }
                )
            }
        }

        if (showCreateDialog) {
            CreateReminderDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.hideCreateDialog() },
                onCreate = { viewModel.createReminder() }
            )
        }
    }
}

@Composable
private fun EmptyRemindersState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No upcoming reminders",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap + to create a reminder",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RemindersList(
    reminders: List<Reminder>,
    onMarkDone: (Reminder) -> Unit,
    onSnooze: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(reminders, key = { it.id }) { reminder ->
            ReminderCard(
                reminder = reminder,
                onMarkDone = { onMarkDone(reminder) },
                onSnooze = { onSnooze(reminder) },
                onDelete = { onDelete(reminder) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderCard(
    reminder: Reminder,
    onMarkDone: () -> Unit,
    onSnooze: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val skinColors = com.ledger.app.presentation.theme.LocalSkinColors.current
    val skinShapes = com.ledger.app.presentation.theme.LocalSkinShapes.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = skinColors.cardBackground
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(skinShapes.cardCornerRadius)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = skinColors.textPrimary
                    )
                    
                    if (reminder.description != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = reminder.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = skinColors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = skinColors.primary
                        )
                        Text(
                            text = formatDateTime(reminder.dueDateTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = skinColors.primary
                        )
                    }

                    if (reminder.repeatPattern != RepeatPattern.NONE) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = skinColors.secondary
                            )
                            Text(
                                text = reminder.repeatPattern.name.lowercase().capitalize(),
                                style = MaterialTheme.typography.bodySmall,
                                color = skinColors.secondary
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert, 
                            contentDescription = "More options",
                            tint = skinColors.textSecondary
                        )
                    }
                    
                    // Premium styled dropdown menu with proper background
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(
                                color = skinColors.cardBackground,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(skinShapes.cardCornerRadius)
                            )
                    ) {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Mark Done",
                                    color = skinColors.textPrimary
                                )
                            },
                            onClick = {
                                onMarkDone()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Check, 
                                    contentDescription = null,
                                    tint = skinColors.receivedColor
                                )
                            },
                            modifier = Modifier.background(skinColors.cardBackground)
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Snooze 1 day",
                                    color = skinColors.textPrimary
                                )
                            },
                            onClick = {
                                onSnooze()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Snooze, 
                                    contentDescription = null,
                                    tint = skinColors.primary
                                )
                            },
                            modifier = Modifier.background(skinColors.cardBackground)
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Delete",
                                    color = skinColors.gaveColor
                                )
                            },
                            onClick = {
                                onDelete()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete, 
                                    contentDescription = null,
                                    tint = skinColors.gaveColor
                                )
                            },
                            modifier = Modifier.background(skinColors.cardBackground)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDateTime(dateTime: LocalDateTime): String {
    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a")
    
    return when {
        dateTime.toLocalDate() == now.toLocalDate() -> {
            "Today at ${dateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}"
        }
        dateTime.toLocalDate() == now.plusDays(1).toLocalDate() -> {
            "Tomorrow at ${dateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}"
        }
        else -> dateTime.format(formatter)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateReminderDialog(
    viewModel: RemindersViewModel,
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    val title by viewModel.newReminderTitle.collectAsState()
    val description by viewModel.newReminderDescription.collectAsState()
    val dateTime by viewModel.newReminderDateTime.collectAsState()
    val repeatPattern by viewModel.newReminderRepeatPattern.collectAsState()
    val skinColors = com.ledger.app.presentation.theme.LocalSkinColors.current
    val skinShapes = com.ledger.app.presentation.theme.LocalSkinShapes.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Create Reminder",
                color = skinColors.textPrimary
            )
        },
        containerColor = skinColors.cardBackground,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { viewModel.updateTitle(it) },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = skinColors.primary,
                        unfocusedBorderColor = skinColors.textSecondary.copy(alpha = 0.3f),
                        focusedLabelColor = skinColors.primary,
                        cursorColor = skinColors.primary,
                        focusedTextColor = skinColors.textPrimary,
                        unfocusedTextColor = skinColors.textPrimary
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = skinColors.primary,
                        unfocusedBorderColor = skinColors.textSecondary.copy(alpha = 0.3f),
                        focusedLabelColor = skinColors.primary,
                        cursorColor = skinColors.primary,
                        focusedTextColor = skinColors.textPrimary,
                        unfocusedTextColor = skinColors.textPrimary
                    )
                )

                // Date/Time display (simplified - in production would use date/time picker)
                OutlinedTextField(
                    value = formatDateTime(dateTime),
                    onValueChange = { },
                    label = { Text("Due Date & Time") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { 
                            // In production, open date/time picker
                            // For now, just add 1 hour
                            viewModel.updateDateTime(dateTime.plusHours(1))
                        }) {
                            Icon(
                                Icons.Default.CalendarToday, 
                                contentDescription = "Pick date/time",
                                tint = skinColors.primary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = skinColors.primary,
                        unfocusedBorderColor = skinColors.textSecondary.copy(alpha = 0.3f),
                        focusedLabelColor = skinColors.primary,
                        cursorColor = skinColors.primary,
                        focusedTextColor = skinColors.textPrimary,
                        unfocusedTextColor = skinColors.textPrimary
                    )
                )

                // Repeat pattern selector with premium styling
                var expandedRepeat by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedRepeat,
                    onExpandedChange = { expandedRepeat = it }
                ) {
                    OutlinedTextField(
                        value = repeatPattern.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Repeat") },
                        trailingIcon = { 
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRepeat)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = skinColors.primary,
                            unfocusedBorderColor = skinColors.textSecondary.copy(alpha = 0.3f),
                            focusedLabelColor = skinColors.primary,
                            cursorColor = skinColors.primary,
                            focusedTextColor = skinColors.textPrimary,
                            unfocusedTextColor = skinColors.textPrimary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedRepeat,
                        onDismissRequest = { expandedRepeat = false },
                        modifier = Modifier.background(skinColors.cardBackground)
                    ) {
                        RepeatPattern.values().forEach { pattern ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        pattern.name.lowercase().replaceFirstChar { it.uppercase() },
                                        color = skinColors.textPrimary
                                    )
                                },
                                onClick = {
                                    viewModel.updateRepeatPattern(pattern)
                                    expandedRepeat = false
                                },
                                modifier = Modifier.background(skinColors.cardBackground)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onCreate,
                enabled = title.isNotBlank()
            ) {
                Text("Create", color = skinColors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = skinColors.textSecondary)
            }
        }
    )
}

package com.ledger.app.presentation.ui.tasks

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ledger.app.domain.model.TaskPriority
import com.ledger.app.presentation.theme.LocalSkinColors
import com.ledger.app.presentation.theme.LocalSkinShapes
import com.ledger.app.presentation.viewmodel.AddEditTaskViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Screen for adding or editing a task.
 * 
 * Features:
 * - Title input (required)
 * - Description input (optional)
 * - Priority selector (High/Medium/Low)
 * - Due date picker (optional)
 * - Checklist editor
 * - Counterparty selector (optional, for financial tasks)
 * 
 * Requirements: 14.2, 14.3
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditTaskViewModel = hiltViewModel()
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    val title by viewModel.title.collectAsState()
    val description by viewModel.description.collectAsState()
    val priority by viewModel.priority.collectAsState()
    val dueDate by viewModel.dueDate.collectAsState()
    val linkedCounterpartyId by viewModel.linkedCounterpartyId.collectAsState()
    val checklistItems by viewModel.checklistItems.collectAsState()
    val counterparties by viewModel.counterparties.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val error by viewModel.error.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCounterpartyDropdown by remember { mutableStateOf(false) }
    
    // Navigate back on successful save
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
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
                        text = if (viewModel.isEditMode) "Edit Task" else "New Task",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = skinColors.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = skinColors.background
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = skinColors.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Title input (required)
                OutlinedTextField(
                    value = title,
                    onValueChange = { viewModel.updateTitle(it) },
                    label = { Text("Title *") },
                    placeholder = { Text("What needs to be done?") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors(skinColors),
                    shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                )
                
                // Description input (optional)
                OutlinedTextField(
                    value = description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text("Description") },
                    placeholder = { Text("Add more details...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    colors = textFieldColors(skinColors),
                    shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                )
                
                // Priority selector
                PrioritySelector(
                    selectedPriority = priority,
                    onPrioritySelected = { viewModel.updatePriority(it) },
                    skinColors = skinColors,
                    skinShapes = skinShapes
                )
                
                // Due date picker
                DueDateSelector(
                    dueDate = dueDate,
                    onDateClick = { showDatePicker = true },
                    onClearDate = { viewModel.updateDueDate(null) },
                    skinColors = skinColors,
                    skinShapes = skinShapes
                )
                
                // Counterparty selector (for financial tasks)
                CounterpartySelector(
                    selectedCounterpartyId = linkedCounterpartyId,
                    counterparties = counterparties,
                    expanded = showCounterpartyDropdown,
                    onExpandedChange = { showCounterpartyDropdown = it },
                    onCounterpartySelected = { 
                        viewModel.updateLinkedCounterparty(it)
                        showCounterpartyDropdown = false
                    },
                    skinColors = skinColors,
                    skinShapes = skinShapes
                )
                
                // Checklist editor
                ChecklistEditor(
                    items = checklistItems,
                    onItemsChanged = { viewModel.updateChecklistItems(it) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Save button
                Button(
                    onClick = { viewModel.saveTask() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = viewModel.isFormValid() && !isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = skinColors.primary,
                        contentColor = Color.White,
                        disabledContainerColor = skinColors.primary.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (viewModel.isEditMode) "Update Task" else "Create Task",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate?.atZone(ZoneId.systemDefault())
                ?.toInstant()?.toEpochMilli()
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime()
                                .withHour(9)
                                .withMinute(0)
                            viewModel.updateDueDate(selectedDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = skinColors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = skinColors.textSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun PrioritySelector(
    selectedPriority: TaskPriority,
    onPrioritySelected: (TaskPriority) -> Unit,
    skinColors: com.ledger.app.presentation.theme.SkinColors,
    skinShapes: com.ledger.app.presentation.theme.SkinShapes
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Priority",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = skinColors.textSecondary
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TaskPriority.entries.forEach { priority ->
                val isSelected = priority == selectedPriority
                val (color, icon) = when (priority) {
                    TaskPriority.HIGH -> Color(0xFFEF4444) to Icons.Default.KeyboardArrowUp
                    TaskPriority.MEDIUM -> Color(0xFFF59E0B) to Icons.Default.Remove
                    TaskPriority.LOW -> Color(0xFF10B981) to Icons.Default.KeyboardArrowDown
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(skinShapes.cardCornerRadius))
                        .background(
                            if (isSelected) color.copy(alpha = 0.15f)
                            else skinColors.surface
                        )
                        .border(
                            width = if (isSelected) 2.dp else skinShapes.borderWidth,
                            color = if (isSelected) color else skinColors.textSecondary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                        )
                        .clickable { onPrioritySelected(priority) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) color else skinColors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = priority.name.lowercase()
                                .replaceFirstChar { it.uppercase() },
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) color else skinColors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DueDateSelector(
    dueDate: LocalDateTime?,
    onDateClick: () -> Unit,
    onClearDate: () -> Unit,
    skinColors: com.ledger.app.presentation.theme.SkinColors,
    skinShapes: com.ledger.app.presentation.theme.SkinShapes
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Due Date",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = skinColors.textSecondary
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(skinShapes.cardCornerRadius))
                .border(
                    width = skinShapes.borderWidth,
                    color = skinColors.textSecondary.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                )
                .clickable { onDateClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = skinColors.primary,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = dueDate?.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))
                    ?: "No due date",
                fontSize = 15.sp,
                color = if (dueDate != null) skinColors.textPrimary else skinColors.textSecondary,
                modifier = Modifier.weight(1f)
            )
            
            if (dueDate != null) {
                IconButton(
                    onClick = onClearDate,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear date",
                        tint = skinColors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CounterpartySelector(
    selectedCounterpartyId: Long?,
    counterparties: List<com.ledger.app.domain.model.Counterparty>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCounterpartySelected: (Long?) -> Unit,
    skinColors: com.ledger.app.presentation.theme.SkinColors,
    skinShapes: com.ledger.app.presentation.theme.SkinShapes
) {
    val selectedCounterparty = counterparties.find { it.id == selectedCounterpartyId }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Link to Person (optional)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = skinColors.textSecondary
        )
        
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(skinShapes.cardCornerRadius))
                    .border(
                        width = skinShapes.borderWidth,
                        color = skinColors.textSecondary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                    )
                    .clickable { onExpandedChange(true) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = if (selectedCounterparty != null) skinColors.primary else skinColors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = selectedCounterparty?.displayName ?: "Select a person",
                    fontSize = 15.sp,
                    color = if (selectedCounterparty != null) skinColors.textPrimary else skinColors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                
                if (selectedCounterparty != null) {
                    IconButton(
                        onClick = { onCounterpartySelected(null) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear selection",
                            tint = skinColors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.background(skinColors.cardBackground)
            ) {
                if (counterparties.isEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "No people added yet",
                                color = skinColors.textSecondary
                            )
                        },
                        onClick = { onExpandedChange(false) },
                        enabled = false
                    )
                } else {
                    counterparties.forEach { counterparty ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = counterparty.displayName,
                                    color = skinColors.textPrimary
                                )
                            },
                            onClick = { onCounterpartySelected(counterparty.id) },
                            modifier = Modifier.background(skinColors.cardBackground)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun textFieldColors(skinColors: com.ledger.app.presentation.theme.SkinColors) =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = skinColors.primary,
        unfocusedBorderColor = skinColors.textSecondary.copy(alpha = 0.3f),
        focusedLabelColor = skinColors.primary,
        unfocusedLabelColor = skinColors.textSecondary,
        cursorColor = skinColors.primary,
        focusedTextColor = skinColors.textPrimary,
        unfocusedTextColor = skinColors.textPrimary,
        focusedPlaceholderColor = skinColors.textSecondary.copy(alpha = 0.5f),
        unfocusedPlaceholderColor = skinColors.textSecondary.copy(alpha = 0.5f)
    )

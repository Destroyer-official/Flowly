package com.ledger.app.presentation.ui.quickadd

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ledger.app.domain.model.Account
import com.ledger.app.domain.model.Category
import com.ledger.app.domain.model.Counterparty
import com.ledger.app.domain.model.TransactionDirection
import com.ledger.app.presentation.theme.LocalSkinColors
import com.ledger.app.presentation.theme.LocalSkinShapes
import com.ledger.app.presentation.ui.components.NumericKeypad
import com.ledger.app.presentation.viewmodel.EditResult
import com.ledger.app.presentation.viewmodel.EditTransactionViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Screen for editing an existing transaction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    onNavigateBack: () -> Unit,
    onTransactionUpdated: () -> Unit,
    viewModel: EditTransactionViewModel = hiltViewModel()
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    val context = LocalContext.current

    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()
    val amountString by viewModel.amountString.collectAsState()
    val direction by viewModel.direction.collectAsState()
    val selectedCounterparty by viewModel.selectedCounterparty.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedAccount by viewModel.selectedAccount.collectAsState()
    val note by viewModel.note.collectAsState()
    val transactionDateTime by viewModel.transactionDateTime.collectAsState()
    val counterparties by viewModel.counterparties.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    // Handle save result
    LaunchedEffect(saveResult) {
        when (saveResult) {
            is EditResult.Success -> {
                onTransactionUpdated()
                viewModel.clearSaveResult()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Transaction") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = skinColors.background,
                    titleContentColor = skinColors.textPrimary
                )
            )
        },
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Amount Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(skinShapes.cardCornerRadius))
                        .background(skinColors.cardBackground)
                        .border(
                            width = 1.dp,
                            color = skinColors.textSecondary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                        )
                        .padding(vertical = 16.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val amountColor = when (direction) {
                        TransactionDirection.GAVE -> skinColors.gaveColor
                        TransactionDirection.RECEIVED -> skinColors.receivedColor
                    }
                    Text(
                        text = "₹ $amountString",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )
                }

                // Direction Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DirectionButton(
                        text = "I GAVE",
                        isSelected = direction == TransactionDirection.GAVE,
                        onClick = { viewModel.setDirection(TransactionDirection.GAVE) },
                        modifier = Modifier.weight(1f),
                        skinColors = skinColors,
                        skinShapes = skinShapes
                    )
                    DirectionButton(
                        text = "I RECEIVED",
                        isSelected = direction == TransactionDirection.RECEIVED,
                        onClick = { viewModel.setDirection(TransactionDirection.RECEIVED) },
                        modifier = Modifier.weight(1f),
                        skinColors = skinColors,
                        skinShapes = skinShapes
                    )
                }

                // Date/Time Picker
                DateTimeSelector(
                    dateTime = transactionDateTime,
                    onDateTimeChanged = { viewModel.setDateTime(it) },
                    skinColors = skinColors,
                    skinShapes = skinShapes
                )

                // Counterparty Selector
                DropdownSelector(
                    label = "Person",
                    selectedText = selectedCounterparty?.displayName ?: "None (Self)",
                    items = listOf(null) + counterparties,
                    itemText = { it?.displayName ?: "None (Self)" },
                    onItemSelected = { viewModel.selectCounterparty(it) },
                    skinColors = skinColors,
                    skinShapes = skinShapes
                )

                // Category and Account Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        DropdownSelector(
                            label = "Category",
                            selectedText = selectedCategory?.name ?: "Select",
                            items = categories,
                            itemText = { it.name },
                            onItemSelected = { viewModel.selectCategory(it) },
                            skinColors = skinColors,
                            skinShapes = skinShapes
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        DropdownSelector(
                            label = "Account",
                            selectedText = selectedAccount?.name ?: "Select",
                            items = accounts,
                            itemText = { it.name },
                            onItemSelected = { viewModel.selectAccount(it) },
                            skinColors = skinColors,
                            skinShapes = skinShapes
                        )
                    }
                }

                // Note Field
                OutlinedTextField(
                    value = note,
                    onValueChange = { viewModel.setNote(it) },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Numeric Keypad
                NumericKeypad(
                    onDigitClick = { viewModel.onDigitInput(it) },
                    onDecimalClick = { viewModel.onDecimalInput() },
                    onBackspaceClick = { viewModel.onBackspace() }
                )

                // Error Message
                if (saveResult is EditResult.Error) {
                    Text(
                        text = (saveResult as EditResult.Error).message,
                        color = skinColors.error,
                        fontSize = 14.sp
                    )
                }

                // Save Button
                Button(
                    onClick = { viewModel.updateTransaction() },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = skinColors.primary
                    ),
                    shape = RoundedCornerShape(skinShapes.buttonCornerRadius)
                ) {
                    Text(
                        text = if (isSaving) "SAVING..." else "UPDATE TRANSACTION",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectionButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    skinColors: com.ledger.app.presentation.theme.SkinColors,
    skinShapes: com.ledger.app.presentation.theme.SkinShapes
) {
    val backgroundColor = if (isSelected) skinColors.primary else skinColors.surface
    val textColor = if (isSelected) skinColors.background else skinColors.textPrimary

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(skinShapes.buttonCornerRadius))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
private fun DateTimeSelector(
    dateTime: LocalDateTime,
    onDateTimeChanged: (LocalDateTime) -> Unit,
    skinColors: com.ledger.app.presentation.theme.SkinColors,
    skinShapes: com.ledger.app.presentation.theme.SkinShapes
) {
    val context = LocalContext.current
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")

    Column {
        Text(
            text = "Date & Time",
            fontSize = 12.sp,
            color = skinColors.textSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(skinShapes.cardCornerRadius))
                .background(skinColors.surface)
                .border(
                    width = 1.dp,
                    color = skinColors.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                )
                .clickable {
                    // Show date picker
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            // After date selected, show time picker
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    onDateTimeChanged(
                                        LocalDateTime.of(year, month + 1, day, hour, minute)
                                    )
                                },
                                dateTime.hour,
                                dateTime.minute,
                                true
                            ).show()
                        },
                        dateTime.year,
                        dateTime.monthValue - 1,
                        dateTime.dayOfMonth
                    ).show()
                }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateTime.format(formatter),
                    fontSize = 14.sp,
                    color = skinColors.textPrimary
                )
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Select date",
                    tint = skinColors.primary
                )
            }
        }
    }
}

@Composable
private fun <T> DropdownSelector(
    label: String,
    selectedText: String,
    items: List<T>,
    itemText: (T) -> String,
    onItemSelected: (T) -> Unit,
    skinColors: com.ledger.app.presentation.theme.SkinColors,
    skinShapes: com.ledger.app.presentation.theme.SkinShapes
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = skinColors.textSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(skinShapes.cardCornerRadius))
                .background(skinColors.surface)
                .border(
                    width = 1.dp,
                    color = skinColors.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                )
                .clickable { expanded = true }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedText,
                    fontSize = 14.sp,
                    color = skinColors.textPrimary
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown",
                    tint = skinColors.primary
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(skinColors.cardBackground)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemText(item), color = skinColors.textPrimary) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

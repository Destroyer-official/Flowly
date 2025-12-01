package com.ledger.app.presentation.ui.quickadd

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
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
import com.ledger.app.presentation.viewmodel.QuickAddViewModel
import com.ledger.app.presentation.viewmodel.SaveResult
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.CalendarMonth

/**
 * Quick Add bottom sheet for instant transaction capture.
 * 
 * Features:
 * - Amount display with NumericKeypad
 * - Direction toggle (I GAVE / I RECEIVED)
 * - Counterparty dropdown with search and quick-add
 * - Category and Account dropdowns
 * - Optional note field
 * - Save button
 * 
 * Requirements: 1.1, 1.2
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onTransactionSaved: (Long) -> Unit,
    prefillCounterpartyId: Long? = null,
    prefillNote: String? = null,
    linkedTaskId: Long? = null,
    viewModel: QuickAddViewModel = hiltViewModel()
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current

    // Collect state
    val amountString by viewModel.amountString.collectAsState()
    val direction by viewModel.direction.collectAsState()
    val selectedCounterparty by viewModel.selectedCounterparty.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedAccount by viewModel.selectedAccount.collectAsState()
    val note by viewModel.note.collectAsState()
    val counterparties by viewModel.counterparties.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()
    
    // Bill payment specific state
    val isBillPayment by viewModel.isBillPayment.collectAsState()
    val selectedBillCategory by viewModel.selectedBillCategory.collectAsState()
    val consumerId by viewModel.consumerId.collectAsState()
    val isForSelf by viewModel.isForSelf.collectAsState()
    val transactionDateTime by viewModel.transactionDateTime.collectAsState()

    // Prefill counterparty if provided
    LaunchedEffect(prefillCounterpartyId) {
        if (prefillCounterpartyId != null) {
            viewModel.prefillCounterparty(prefillCounterpartyId)
        }
    }

    // Prefill note and linkedTaskId if provided (Requirements: 15.2, 15.4)
    LaunchedEffect(prefillNote, linkedTaskId) {
        if (prefillNote != null) {
            viewModel.setNote(prefillNote)
        }
        if (linkedTaskId != null) {
            viewModel.setLinkedTaskId(linkedTaskId)
        }
    }

    // Handle save result
    LaunchedEffect(saveResult) {
        when (val result = saveResult) {
            is SaveResult.Success -> {
                onTransactionSaved(result.transactionId)
                viewModel.clearSaveResult()
            }
            is SaveResult.Error -> {
                // Error is displayed in UI
            }
            null -> {}
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = skinColors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Fixed header section (Amount + Direction) - NOT scrollable
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Amount Display - always visible at top
                AmountDisplaySection(
                    amountString = amountString,
                    direction = direction,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Direction Toggle (3 options: I GAVE, I RECEIVED, BILL)
                DirectionToggle(
                    selectedDirection = direction,
                    isBillPayment = isBillPayment,
                    onDirectionSelected = { 
                        viewModel.setDirection(it)
                        viewModel.setBillPaymentMode(false)
                    },
                    onBillPaymentSelected = {
                        viewModel.setBillPaymentMode(true)
                    },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Scrollable content section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                // Bill Payment specific fields
                if (isBillPayment) {
                    // Bill Category Selector
                    BillCategorySelector(
                        selectedBillCategory = selectedBillCategory,
                        onBillCategorySelected = { viewModel.setBillCategory(it) },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // For Self / For Other Toggle
                    ForSelfToggle(
                        isForSelf = isForSelf,
                        onForSelfChanged = { viewModel.setIsForSelf(it) },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Consumer ID Input
                    OutlinedTextField(
                        value = consumerId,
                        onValueChange = { viewModel.setConsumerId(it) },
                        label = { Text("Consumer ID / Phone Number") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        singleLine = true,
                        placeholder = { Text("e.g., 9876543210 or Account No.") }
                    )
                }

                // Counterparty Selector (only show if not for self)
                if (!isBillPayment || !isForSelf) {
                    CounterpartySelector(
                        selectedCounterparty = selectedCounterparty,
                        counterparties = counterparties,
                        onCounterpartySelected = { viewModel.selectCounterparty(it) },
                        onCreateNew = { viewModel.createAndSelectCounterparty(it) },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Category and Account in a row for compactness
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category Selector
                    Box(modifier = Modifier.weight(1f)) {
                        CategorySelector(
                            selectedCategory = selectedCategory,
                            categories = categories,
                            onCategorySelected = { viewModel.selectCategory(it) }
                        )
                    }

                    // Account Selector
                    Box(modifier = Modifier.weight(1f)) {
                        AccountSelector(
                            selectedAccount = selectedAccount,
                            accounts = accounts,
                            onAccountSelected = { viewModel.selectAccount(it) }
                        )
                    }
                }

                // Date/Time Picker
                DateTimeSelectorCompact(
                    dateTime = transactionDateTime,
                    onDateTimeChanged = { viewModel.setTransactionDateTime(it) },
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Note Field - more compact
                OutlinedTextField(
                    value = note,
                    onValueChange = { viewModel.setNote(it) },
                    label = { Text("Note (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    singleLine = true
                )

                // Numeric Keypad
                NumericKeypad(
                    onDigitClick = { viewModel.onDigitInput(it) },
                    onDecimalClick = { viewModel.onDecimalInput() },
                    onBackspaceClick = { viewModel.onBackspace() },
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Error Message
                if (saveResult is SaveResult.Error) {
                    Text(
                        text = (saveResult as SaveResult.Error).message,
                        color = skinColors.gaveColor,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Save Button
                Button(
                    onClick = { viewModel.saveTransaction() },
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
                        text = if (isSaving) "SAVING..." else "SAVE TRANSACTION",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } // End of scrollable column
        } // End of main column
    }
}

/**
 * Amount display section showing the current amount being entered.
 */
@Composable
private fun AmountDisplaySection(
    amountString: String,
    direction: TransactionDirection,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (skinShapes.elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = skinShapes.elevation / 2,
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                    )
                } else {
                    Modifier
                }
            )
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
}

/**
 * Direction toggle for selecting I GAVE, I RECEIVED, or BILL.
 */
@Composable
private fun DirectionToggle(
    selectedDirection: TransactionDirection,
    isBillPayment: Boolean,
    onDirectionSelected: (TransactionDirection) -> Unit,
    onBillPaymentSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DirectionButton(
            text = "I GAVE",
            isSelected = selectedDirection == TransactionDirection.GAVE && !isBillPayment,
            onClick = { onDirectionSelected(TransactionDirection.GAVE) },
            modifier = Modifier.weight(1f)
        )
        DirectionButton(
            text = "I RECEIVED",
            isSelected = selectedDirection == TransactionDirection.RECEIVED && !isBillPayment,
            onClick = { onDirectionSelected(TransactionDirection.RECEIVED) },
            modifier = Modifier.weight(1f)
        )
        DirectionButton(
            text = "BILL",
            isSelected = isBillPayment,
            onClick = { onBillPaymentSelected() },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Individual direction button.
 */
@Composable
private fun DirectionButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current

    val backgroundColor = if (isSelected) skinColors.primary else skinColors.surface
    val textColor = if (isSelected) skinColors.background else skinColors.textPrimary

    Box(
        modifier = modifier
            .height(48.dp)
            .then(
                if (skinShapes.elevation > 0.dp && !isSelected) {
                    Modifier.shadow(
                        elevation = skinShapes.elevation / 2,
                        shape = RoundedCornerShape(skinShapes.buttonCornerRadius)
                    )
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(skinShapes.buttonCornerRadius))
            .background(backgroundColor)
            .then(
                if (skinShapes.borderWidth > 0.dp) {
                    Modifier.border(
                        width = skinShapes.borderWidth,
                        color = if (isSelected) skinColors.primary else skinColors.textSecondary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(skinShapes.buttonCornerRadius)
                    )
                } else {
                    Modifier
                }
            )
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

/**
 * Counterparty selector dropdown with add new functionality.
 * Premium styled with proper dark mode support.
 */
@Composable
private fun CounterpartySelector(
    selectedCounterparty: Counterparty?,
    counterparties: List<Counterparty>,
    onCounterpartySelected: (Counterparty?) -> Unit,
    onCreateNew: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    var expanded by remember { mutableStateOf(false) }
    var showAddNew by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        DropdownSelector(
            label = "Person",
            selectedText = selectedCounterparty?.displayName ?: "Select or add new",
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier
        ) {
            // "None" option with proper styling
            DropdownMenuItem(
                text = { 
                    Text(
                        text = "None (Self)",
                        color = skinColors.textSecondary,
                        fontSize = 14.sp
                    )
                },
                onClick = {
                    onCounterpartySelected(null)
                    expanded = false
                },
                modifier = Modifier.background(skinColors.cardBackground)
            )
            // Add new person option - highlighted
            DropdownMenuItem(
                text = { 
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = skinColors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Add New Person",
                            color = skinColors.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                },
                onClick = {
                    expanded = false
                    showAddNew = true
                },
                modifier = Modifier.background(skinColors.primary.copy(alpha = 0.08f))
            )
            // Divider
            if (counterparties.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(skinColors.textSecondary.copy(alpha = 0.1f))
                )
            }
            // Existing counterparties
            counterparties.forEach { counterparty ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = counterparty.displayName,
                            color = skinColors.textPrimary,
                            fontSize = 14.sp
                        )
                    },
                    onClick = {
                        onCounterpartySelected(counterparty)
                        expanded = false
                    },
                    modifier = Modifier.background(skinColors.cardBackground)
                )
            }
        }

        // Add new person input field with premium styling
        if (showAddNew) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(skinShapes.cardCornerRadius))
                    .background(skinColors.primary.copy(alpha = 0.05f))
                    .border(
                        width = 1.dp,
                        color = skinColors.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                    )
                    .padding(12.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("New person name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = skinColors.primary,
                            unfocusedBorderColor = skinColors.textSecondary.copy(alpha = 0.3f),
                            focusedLabelColor = skinColors.primary,
                            cursorColor = skinColors.primary
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                newName = ""
                                showAddNew = false
                            }
                        ) {
                            Text("Cancel", color = skinColors.textSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    onCreateNew(newName)
                                    newName = ""
                                    showAddNew = false
                                }
                            },
                            enabled = newName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = skinColors.primary
                            )
                        ) {
                            Text("Add Person")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Category selector dropdown with premium styling.
 */
@Composable
private fun CategorySelector(
    selectedCategory: Category?,
    categories: List<Category>,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    var expanded by remember { mutableStateOf(false) }

    DropdownSelector(
        label = "Category",
        selectedText = selectedCategory?.name ?: "Select category",
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        categories.forEach { category ->
            DropdownMenuItem(
                text = { 
                    Text(
                        text = category.name,
                        color = skinColors.textPrimary,
                        fontSize = 14.sp
                    )
                },
                onClick = {
                    onCategorySelected(category)
                    expanded = false
                },
                modifier = Modifier.background(skinColors.cardBackground)
            )
        }
    }
}

/**
 * Account selector dropdown with premium styling.
 */
@Composable
private fun AccountSelector(
    selectedAccount: Account?,
    accounts: List<Account>,
    onAccountSelected: (Account) -> Unit,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    var expanded by remember { mutableStateOf(false) }

    DropdownSelector(
        label = "Account",
        selectedText = selectedAccount?.name ?: "Select account",
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        accounts.forEach { account ->
            DropdownMenuItem(
                text = { 
                    Text(
                        text = account.name,
                        color = skinColors.textPrimary,
                        fontSize = 14.sp
                    )
                },
                onClick = {
                    onAccountSelected(account)
                    expanded = false
                },
                modifier = Modifier.background(skinColors.cardBackground)
            )
        }
    }
}

/**
 * Reusable dropdown selector component with premium dark mode styling.
 */
@Composable
private fun DropdownSelector(
    label: String,
    selectedText: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current

    Box(modifier = modifier.fillMaxWidth()) {
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
                    .then(
                        if (skinShapes.elevation > 0.dp) {
                            Modifier.shadow(
                                elevation = skinShapes.elevation / 2,
                                shape = RoundedCornerShape(skinShapes.cardCornerRadius),
                                ambientColor = skinColors.primary.copy(alpha = 0.1f),
                                spotColor = skinColors.primary.copy(alpha = 0.1f)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clip(RoundedCornerShape(skinShapes.cardCornerRadius))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                skinColors.surface,
                                skinColors.surface.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = skinColors.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                    )
                    .clickable { onExpandedChange(true) }
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
        }

        // Premium styled dropdown menu with proper background
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier
                .background(
                    color = skinColors.cardBackground,
                    shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                )
                .border(
                    width = 1.dp,
                    color = skinColors.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                )
        ) {
            content()
        }
    }
}

/**
 * Bill category selector for bill payments.
 * Shows Electricity, TV, Mobile, Internet options.
 */
@Composable
private fun BillCategorySelector(
    selectedBillCategory: com.ledger.app.domain.model.BillCategory?,
    onBillCategorySelected: (com.ledger.app.domain.model.BillCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    val billCategories = com.ledger.app.domain.model.BillCategory.values()

    Column(modifier = modifier) {
        Text(
            text = "Bill Type",
            fontSize = 12.sp,
            color = skinColors.textSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            billCategories.forEach { category ->
                val isSelected = selectedBillCategory == category
                val backgroundColor = if (isSelected) skinColors.primary else skinColors.surface
                val textColor = if (isSelected) skinColors.background else skinColors.textPrimary
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(skinShapes.buttonCornerRadius))
                        .background(backgroundColor)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) skinColors.primary else skinColors.textSecondary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(skinShapes.buttonCornerRadius)
                        )
                        .clickable { onBillCategorySelected(category) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (category) {
                            com.ledger.app.domain.model.BillCategory.ELECTRICITY -> "⚡"
                            com.ledger.app.domain.model.BillCategory.TV -> "📺"
                            com.ledger.app.domain.model.BillCategory.MOBILE -> "📱"
                            com.ledger.app.domain.model.BillCategory.INTERNET -> "🌐"
                            com.ledger.app.domain.model.BillCategory.OTHER -> "📋"
                        },
                        fontSize = 18.sp,
                        color = textColor
                    )
                }
            }
        }
        // Show selected category name
        Text(
            text = selectedBillCategory?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Select type",
            fontSize = 12.sp,
            color = skinColors.textSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * Toggle for selecting whether bill is for self or for others.
 */
@Composable
private fun ForSelfToggle(
    isForSelf: Boolean,
    onForSelfChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current

    Column(modifier = modifier) {
        Text(
            text = "Paid For",
            fontSize = 12.sp,
            color = skinColors.textSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // For Self button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(skinShapes.buttonCornerRadius))
                    .background(if (isForSelf) skinColors.primary else skinColors.surface)
                    .border(
                        width = 1.dp,
                        color = if (isForSelf) skinColors.primary else skinColors.textSecondary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(skinShapes.buttonCornerRadius)
                    )
                    .clickable { onForSelfChanged(true) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "For Self",
                    fontSize = 14.sp,
                    fontWeight = if (isForSelf) FontWeight.Bold else FontWeight.Medium,
                    color = if (isForSelf) skinColors.background else skinColors.textPrimary
                )
            }
            // For Other button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(skinShapes.buttonCornerRadius))
                    .background(if (!isForSelf) skinColors.primary else skinColors.surface)
                    .border(
                        width = 1.dp,
                        color = if (!isForSelf) skinColors.primary else skinColors.textSecondary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(skinShapes.buttonCornerRadius)
                    )
                    .clickable { onForSelfChanged(false) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "For Other",
                    fontSize = 14.sp,
                    fontWeight = if (!isForSelf) FontWeight.Bold else FontWeight.Medium,
                    color = if (!isForSelf) skinColors.background else skinColors.textPrimary
                )
            }
        }
        // Help text
        Text(
            text = if (isForSelf) "No debt will be tracked" else "Amount will be added to their debt",
            fontSize = 11.sp,
            color = skinColors.textSecondary.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * Compact date/time selector for the quick add form.
 */
@Composable
private fun DateTimeSelectorCompact(
    dateTime: LocalDateTime,
    onDateTimeChanged: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    val context = LocalContext.current
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")

    Column(modifier = modifier) {
        Text(
            text = "Date & Time",
            fontSize = 12.sp,
            color = skinColors.textSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(skinShapes.cardCornerRadius))
                .background(skinColors.surface)
                .border(
                    width = 1.dp,
                    color = skinColors.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                )
                .clickable {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
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
                    tint = skinColors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

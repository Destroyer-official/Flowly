package com.ledger.app.presentation.ui.audit

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ledger.app.domain.model.AuditAction
import com.ledger.app.domain.model.AuditLog
import com.ledger.app.presentation.theme.LocalSkinColors
import com.ledger.app.presentation.theme.LocalSkinShapes
import com.ledger.app.presentation.viewmodel.AuditLogUiState
import com.ledger.app.presentation.viewmodel.AuditLogViewModel
import com.ledger.app.presentation.viewmodel.ExportState
import java.time.format.DateTimeFormatter


/**
 * Audit Log screen for viewing and searching all data modifications.
 * 
 * Features:
 * - List all audit logs
 * - Search by date, amount, or note text
 * - Export audit log to file
 * 
 * Requirements: 7.3, 7.4
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogScreen(
    viewModel: AuditLogViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val context = LocalContext.current
    val skinColors = LocalSkinColors.current

    // Handle export state
    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is ExportState.Success -> {
                Toast.makeText(context, "Audit log exported successfully", Toast.LENGTH_SHORT).show()
                val intent = viewModel.shareExportedFile(state.file)
                context.startActivity(Intent.createChooser(intent, "Share Audit Log"))
                viewModel.clearExportState()
            }
            is ExportState.Error -> {
                Toast.makeText(context, "Export failed: ${state.message}", Toast.LENGTH_SHORT).show()
                viewModel.clearExportState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audit Logs", color = skinColors.textPrimary) },
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
                    IconButton(
                        onClick = { viewModel.exportAuditLog() },
                        enabled = exportState !is ExportState.Exporting
                    ) {
                        if (exportState is ExportState.Exporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = skinColors.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export",
                                tint = skinColors.textPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = skinColors.background
                )
            )
        },
        containerColor = skinColors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onClear = { viewModel.clearSearch() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Content
            when (val state = uiState) {
                is AuditLogUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = skinColors.primary)
                    }
                }
                is AuditLogUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            color = skinColors.gaveColor,
                            fontSize = 16.sp
                        )
                    }
                }
                is AuditLogUiState.Success -> {
                    if (state.logs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "No audit logs yet" else "No results found",
                                color = skinColors.textSecondary,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.logs) { log ->
                                AuditLogItem(log = log)
                            }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}


/**
 * Search bar for filtering audit logs.
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = "Search by date, amount, or note...",
                color = skinColors.textSecondary
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = skinColors.textSecondary
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = skinColors.textSecondary
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(skinShapes.cardCornerRadius),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = skinColors.primary,
            unfocusedBorderColor = skinColors.textSecondary.copy(alpha = 0.3f),
            focusedTextColor = skinColors.textPrimary,
            unfocusedTextColor = skinColors.textPrimary,
            cursorColor = skinColors.primary
        )
    )
}

/**
 * Individual audit log item.
 */
@Composable
private fun AuditLogItem(log: AuditLog) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (skinShapes.elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = skinShapes.elevation,
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (skinShapes.borderWidth > 0.dp) {
                    Modifier.border(
                        width = skinShapes.borderWidth,
                        color = skinColors.textSecondary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(skinShapes.cardCornerRadius)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(skinShapes.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = skinColors.cardBackground
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row with action and timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionBadge(action = log.action)
                Text(
                    text = log.timestamp.format(dateFormatter),
                    fontSize = 12.sp,
                    color = skinColors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Entity info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.entityType,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = skinColors.textPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ID: ${log.entityId}",
                    fontSize = 12.sp,
                    color = skinColors.textSecondary
                )
            }

            // Details if present
            log.details?.let { details ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = details,
                    fontSize = 13.sp,
                    color = skinColors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Old/New values if present
            if (log.oldValue != null || log.newValue != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                log.oldValue?.let { oldValue ->
                    ValueSection(
                        label = "Old Value",
                        value = oldValue,
                        color = skinColors.gaveColor
                    )
                }
                
                log.newValue?.let { newValue ->
                    if (log.oldValue != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    ValueSection(
                        label = "New Value",
                        value = newValue,
                        color = skinColors.receivedColor
                    )
                }
            }
        }
    }
}


/**
 * Badge showing the action type with appropriate color.
 */
@Composable
private fun ActionBadge(action: AuditAction) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    val (backgroundColor, textColor) = when (action) {
        AuditAction.CREATE -> skinColors.receivedColor.copy(alpha = 0.2f) to skinColors.receivedColor
        AuditAction.UPDATE -> skinColors.primary.copy(alpha = 0.2f) to skinColors.primary
        AuditAction.DELETE -> skinColors.gaveColor.copy(alpha = 0.2f) to skinColors.gaveColor
        AuditAction.PARTIAL_PAYMENT -> skinColors.secondary.copy(alpha = 0.2f) to skinColors.secondary
        AuditAction.BACKUP -> skinColors.textSecondary.copy(alpha = 0.2f) to skinColors.textSecondary
        AuditAction.RESTORE -> skinColors.textSecondary.copy(alpha = 0.2f) to skinColors.textSecondary
    }

    Card(
        shape = RoundedCornerShape(skinShapes.buttonCornerRadius),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Text(
            text = action.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

/**
 * Section showing old or new value.
 */
@Composable
private fun ValueSection(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(skinShapes.buttonCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = skinColors.background.copy(alpha = 0.5f)
            )
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(8.dp),
                fontSize = 11.sp,
                color = skinColors.textSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Import for Intent
private typealias Intent = android.content.Intent

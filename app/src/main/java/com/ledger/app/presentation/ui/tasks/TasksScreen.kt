package com.ledger.app.presentation.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ledger.app.domain.model.Task
import com.ledger.app.presentation.theme.LocalSkinColors
import com.ledger.app.presentation.theme.LocalSkinShapes
import com.ledger.app.presentation.viewmodel.TaskSortOption
import com.ledger.app.presentation.viewmodel.TasksViewModel

/**
 * Main screen for displaying and managing tasks.
 * 
 * Features:
 * - Tab layout: Pending / Completed
 * - Task list with TaskListItem
 * - FAB for adding new task
 * - Empty state illustrations
 * 
 * Requirements: 14.1, 14.4
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onNavigateToAddTask: () -> Unit,
    onNavigateToTaskDetails: (Long) -> Unit,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    val pendingTasks by viewModel.pendingTasks.collectAsState()
    val completedTasks by viewModel.completedTasks.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val showCompletionAnimation by viewModel.showCompletionAnimation.collectAsState()
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showSortMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tasks",
                        color = skinColors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Sort button (only for pending tab)
                    if (selectedTabIndex == 0) {
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Sort tasks",
                                    tint = skinColors.textSecondary
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.background(skinColors.cardBackground)
                            ) {
                                TaskSortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = when (option) {
                                                    TaskSortOption.PRIORITY -> "By Priority"
                                                    TaskSortOption.DUE_DATE -> "By Due Date"
                                                    TaskSortOption.CREATED_DATE -> "By Created Date"
                                                },
                                                color = if (sortOption == option) 
                                                    skinColors.primary 
                                                else 
                                                    skinColors.textPrimary
                                            )
                                        },
                                        onClick = {
                                            viewModel.setSortOption(option)
                                            showSortMenu = false
                                        },
                                        modifier = Modifier.background(skinColors.cardBackground)
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = skinColors.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTask,
                containerColor = skinColors.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(skinShapes.cardCornerRadius)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Task"
                )
            }
        },
        containerColor = skinColors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = skinColors.background,
                contentColor = skinColors.primary,
                indicator = { tabPositions ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Pending",
                                fontWeight = if (selectedTabIndex == 0) 
                                    FontWeight.Bold 
                                else 
                                    FontWeight.Normal,
                                color = if (selectedTabIndex == 0) 
                                    skinColors.primary 
                                else 
                                    skinColors.textSecondary
                            )
                            if (pendingTasks.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                TaskCountBadge(
                                    count = pendingTasks.size,
                                    color = skinColors.primary
                                )
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Completed",
                                fontWeight = if (selectedTabIndex == 1) 
                                    FontWeight.Bold 
                                else 
                                    FontWeight.Normal,
                                color = if (selectedTabIndex == 1) 
                                    skinColors.primary 
                                else 
                                    skinColors.textSecondary
                            )
                            if (completedTasks.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                TaskCountBadge(
                                    count = completedTasks.size,
                                    color = skinColors.receivedColor
                                )
                            }
                        }
                    }
                )
            }
            
            // Content based on selected tab
            when (selectedTabIndex) {
                0 -> {
                    if (pendingTasks.isEmpty()) {
                        EmptyPendingTasksState(
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        TasksList(
                            tasks = pendingTasks,
                            onTaskClick = { onNavigateToTaskDetails(it.id) },
                            onTaskComplete = { viewModel.completeTask(it) },
                            onTaskDelete = { viewModel.deleteTask(it) }
                        )
                    }
                }
                1 -> {
                    if (completedTasks.isEmpty()) {
                        EmptyCompletedTasksState(
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        TasksList(
                            tasks = completedTasks,
                            onTaskClick = { onNavigateToTaskDetails(it.id) },
                            onTaskComplete = { /* Already completed */ },
                            onTaskDelete = { viewModel.deleteTask(it) }
                        )
                    }
                }
            }
        }
        
        // Completion animation overlay
        AnimatedVisibility(
            visible = showCompletionAnimation != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TaskCompletionOverlay(
                isVisible = showCompletionAnimation != null,
                onDismiss = { viewModel.clearCompletionAnimation() }
            )
        }
    }
}

@Composable
private fun TaskCountBadge(
    count: Int,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun TasksList(
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    onTaskComplete: (Task) -> Unit,
    onTaskDelete: (Task) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = tasks,
            key = { it.id }
        ) { task ->
            TaskListItem(
                task = task,
                onTaskClick = onTaskClick,
                onTaskComplete = onTaskComplete,
                onTaskDelete = onTaskDelete
            )
        }
    }
}

@Composable
private fun EmptyPendingTasksState(
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Task,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = skinColors.textSecondary.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "No pending tasks",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = skinColors.textPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Tap + to add a new task\nand stay organized",
            fontSize = 14.sp,
            color = skinColors.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun EmptyCompletedTasksState(
    modifier: Modifier = Modifier
) {
    val skinColors = LocalSkinColors.current
    
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = skinColors.textSecondary.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "No completed tasks yet",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = skinColors.textPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Complete your pending tasks\nto see them here",
            fontSize = 14.sp,
            color = skinColors.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

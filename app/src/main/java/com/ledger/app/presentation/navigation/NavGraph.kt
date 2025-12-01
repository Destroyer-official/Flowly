package com.ledger.app.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ledger.app.presentation.theme.LocalSkinColors
import com.ledger.app.presentation.ui.analytics.AnalyticsScreen
import com.ledger.app.presentation.ui.counterparty.CounterpartyLedgerScreen
import com.ledger.app.presentation.ui.counterparty.CounterpartyListScreen
import com.ledger.app.presentation.ui.home.HomeScreen
import com.ledger.app.presentation.ui.reminders.RemindersScreen
import com.ledger.app.presentation.ui.settings.AccountManagementScreen
import com.ledger.app.presentation.ui.settings.SettingsScreen
import com.ledger.app.presentation.ui.tasks.AddEditTaskScreen
import com.ledger.app.presentation.ui.tasks.TaskDetailsScreen
import com.ledger.app.presentation.ui.tasks.TasksScreen
import com.ledger.app.presentation.ui.transaction.TransactionDetailsScreen

/**
 * Main navigation graph for the Offline Ledger app.
 * Defines all navigation routes and their corresponding composables.
 * 
 * @param navController The navigation controller for managing navigation
 * @param startDestination The initial screen to display (default: Home)
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Home/Dashboard screen
        composable(route = Screen.Home.route) {
            HomeScreen(
                onTransactionClick = { transactionId ->
                    navController.navigate(Screen.TransactionDetails.createRoute(transactionId))
                }
            )
        }

        // People/Counterparty list screen
        composable(route = Screen.People.route) {
            CounterpartyListScreen(
                onCounterpartyClick = { counterpartyId ->
                    navController.navigate(Screen.CounterpartyLedger.createRoute(counterpartyId))
                }
            )
        }

        // Counterparty ledger screen
        composable(
            route = Screen.CounterpartyLedger.route,
            arguments = listOf(
                navArgument("counterpartyId") {
                    type = NavType.LongType
                }
            )
        ) {
            CounterpartyLedgerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onTransactionClick = { transactionId ->
                    navController.navigate(Screen.TransactionDetails.createRoute(transactionId))
                },
                onAddTransaction = { counterpartyId ->
                    // TODO: Navigate to quick add with pre-filled counterparty
                    // For now, just go back to home where the FAB is available
                    navController.navigate(Screen.Home.route)
                },
                onAddReminder = { counterpartyId ->
                    // TODO: Navigate to add reminder with pre-filled counterparty
                    navController.navigate(Screen.Reminders.route)
                }
            )
        }

        // Analytics screen
        composable(route = Screen.Analytics.route) {
            AnalyticsScreen()
        }

        // Settings screen
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigateToAccountManagement = {
                    navController.navigate(Screen.AccountManagement.route)
                },
                onNavigateToAuditLog = {
                    navController.navigate(Screen.AuditLog.route)
                }
            )
        }

        // Transaction details screen
        composable(
            route = Screen.TransactionDetails.route,
            arguments = listOf(
                navArgument("transactionId") {
                    type = NavType.LongType
                }
            )
        ) {
            // transactionId is automatically passed to ViewModel via SavedStateHandle
            TransactionDetailsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onEditTransaction = { transactionId ->
                    navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                }
            )
        }

        // Edit Transaction screen
        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(
                navArgument("transactionId") {
                    type = NavType.LongType
                }
            )
        ) {
            com.ledger.app.presentation.ui.quickadd.EditTransactionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onTransactionUpdated = {
                    navController.popBackStack()
                }
            )
        }

        // Account management screen
        composable(route = Screen.AccountManagement.route) {
            AccountManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Reminders screen
        composable(route = Screen.Reminders.route) {
            RemindersScreen()
        }

        // Bill History screen
        composable(route = Screen.BillHistory.route) {
            com.ledger.app.presentation.ui.bills.BillHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onTransactionClick = { transactionId ->
                    navController.navigate(Screen.TransactionDetails.createRoute(transactionId))
                }
            )
        }

        // Audit Log screen
        composable(route = Screen.AuditLog.route) {
            com.ledger.app.presentation.ui.audit.AuditLogScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Tasks screen
        composable(route = Screen.Tasks.route) {
            TasksScreen(
                onNavigateToAddTask = {
                    navController.navigate(Screen.AddTask.route)
                },
                onNavigateToTaskDetails = { taskId ->
                    navController.navigate(Screen.TaskDetails.createRoute(taskId))
                }
            )
        }

        // Add Task screen
        composable(route = Screen.AddTask.route) {
            AddEditTaskScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Edit Task screen
        composable(
            route = Screen.EditTask.route,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.LongType
                }
            )
        ) {
            AddEditTaskScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Task Details screen
        composable(
            route = Screen.TaskDetails.route,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.LongType
                }
            )
        ) {
            TaskDetailsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = { taskId ->
                    navController.navigate(Screen.EditTask.createRoute(taskId))
                },
                onNavigateToTransaction = { transactionId ->
                    navController.navigate(Screen.TransactionDetails.createRoute(transactionId))
                },
                onConvertToTransaction = { taskId, counterpartyId ->
                    // Navigate to home where the quick add sheet can be opened
                    // In a full implementation, this would pass the task data to pre-fill the form
                    navController.navigate(Screen.Home.route)
                }
            )
        }
    }
}

/**
 * Placeholder screen for screens not yet implemented.
 */
@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$title Screen\n(Coming in Task 12)",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = LocalSkinColors.current.textPrimary
        )
    }
}

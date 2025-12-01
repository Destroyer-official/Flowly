package com.ledger.app.presentation.navigation

/**
 * Sealed class representing all navigation destinations in the app.
 * Each screen has a route string used by Navigation Compose.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object People : Screen("people")
    data object Analytics : Screen("analytics")
    data object Settings : Screen("settings")
    data object Tasks : Screen("tasks")
    data object TransactionDetails : Screen("transaction/{transactionId}") {
        fun createRoute(transactionId: Long) = "transaction/$transactionId"
    }
    data object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: Long) = "edit_transaction/$transactionId"
    }
    data object CounterpartyLedger : Screen("counterparty/{counterpartyId}") {
        fun createRoute(counterpartyId: Long) = "counterparty/$counterpartyId"
    }
    data object AccountManagement : Screen("account_management")
    data object Reminders : Screen("reminders")
    data object BillHistory : Screen("bill_history")
    data object AuditLog : Screen("audit_log")
    data object AddTask : Screen("add_task")
    data object EditTask : Screen("edit_task/{taskId}") {
        fun createRoute(taskId: Long) = "edit_task/$taskId"
    }
    data object TaskDetails : Screen("task/{taskId}") {
        fun createRoute(taskId: Long) = "task/$taskId"
    }
}

/**
 * Bottom navigation items for the main navigation bar.
 */
enum class BottomNavItem(
    val screen: Screen,
    val title: String,
    val icon: String
) {
    HOME(Screen.Home, "Home", "home"),
    TASKS(Screen.Tasks, "Tasks", "task"),
    PEOPLE(Screen.People, "People", "people"),
    ANALYTICS(Screen.Analytics, "Analytics", "analytics"),
    SETTINGS(Screen.Settings, "Settings", "settings")
}

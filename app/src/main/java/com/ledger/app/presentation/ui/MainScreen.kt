package com.ledger.app.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ledger.app.data.local.PreferencesManager
import com.ledger.app.presentation.navigation.BottomNavItem
import com.ledger.app.presentation.navigation.NavGraph
import com.ledger.app.presentation.navigation.Screen
import com.ledger.app.presentation.theme.LedgerTheme
import com.ledger.app.presentation.theme.LocalSkinColors
import com.ledger.app.presentation.theme.LocalSkinShapes

/**
 * Main screen composable that provides the app shell with bottom navigation.
 * Applies the LedgerTheme with current theme mode and design skin from preferences.
 * 
 * Features:
 * - Bottom navigation bar with Home, People, Analytics, Settings
 * - Theme switching without app restart
 * - Skin-aware styling
 * 
 * Requirements: 9.1, 9.2, 9.3, UI design
 */
@Composable
fun MainScreen(
    preferencesManager: PreferencesManager
) {
    val themeMode by preferencesManager.themeMode.collectAsState()
    val designSkin by preferencesManager.designSkin.collectAsState()
    
    // Apply theme with current preferences
    LedgerTheme(
        themeMode = themeMode,
        designSkin = designSkin
    ) {
        MainScreenContent()
    }
}

/**
 * Main screen content with navigation and bottom bar.
 */
@Composable
private fun MainScreenContent() {
    val navController = rememberNavController()
    val skinColors = LocalSkinColors.current
    val skinShapes = LocalSkinShapes.current
    
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            // Only show bottom bar on main screens
            val showBottomBar = currentDestination?.route in listOf(
                Screen.Home.route,
                Screen.Tasks.route,
                Screen.People.route,
                Screen.Analytics.route,
                Screen.Settings.route
            )
            
            if (showBottomBar) {
                NavigationBar(
                    containerColor = skinColors.surface,
                    contentColor = skinColors.textPrimary
                ) {
                    BottomNavItem.entries.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { 
                            it.route == item.screen.route 
                        } == true
                        
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = getIconForNavItem(item, selected),
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = skinColors.primary,
                                selectedTextColor = skinColors.primary,
                                unselectedIconColor = skinColors.textSecondary,
                                unselectedTextColor = skinColors.textSecondary,
                                indicatorColor = skinColors.primary.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        },
        containerColor = skinColors.background
    ) { paddingValues ->
        // Apply padding to NavGraph so screens account for bottom nav
        Box(modifier = Modifier.padding(paddingValues)) {
            NavGraph(
                navController = navController,
                startDestination = Screen.Home.route
            )
        }
    }
}

/**
 * Get the appropriate icon for a navigation item based on selection state.
 */
private fun getIconForNavItem(item: BottomNavItem, selected: Boolean): ImageVector {
    return when (item) {
        BottomNavItem.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
        BottomNavItem.TASKS -> if (selected) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle
        BottomNavItem.PEOPLE -> if (selected) Icons.Filled.People else Icons.Outlined.People
        BottomNavItem.ANALYTICS -> if (selected) Icons.Filled.Analytics else Icons.Outlined.Analytics
        BottomNavItem.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
    }
}

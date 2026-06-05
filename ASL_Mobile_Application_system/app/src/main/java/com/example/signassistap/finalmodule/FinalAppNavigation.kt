package com.example.signassistap.finalmodule

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SignLanguage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.*

data class FinalNavItem(val route: String, val icon: ImageVector, val label: String)

val finalBottomNavItems = listOf(
    FinalNavItem("final_text_to_sign", Icons.Default.SignLanguage, "Text to Sign"),
    FinalNavItem("final_feedback", Icons.Default.Feedback, "Feedback"),
    FinalNavItem("final_history", Icons.Default.History, "History")
)

private val finalBottomBarRoutes = listOf(
    "final_text_to_sign",
    "final_feedback",
    "final_history"
)

@Composable
fun FinalAppNavigation(feedbackList: MutableList<String>) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in finalBottomBarRoutes) {
                NavigationBar {
                    finalBottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "final_text_to_sign",
            modifier = Modifier.padding(padding)
        ) {
            composable("final_text_to_sign") { FinalTextToSignScreen() }
            composable("final_feedback") { FinalFeedbackScreen(feedbackList) }
            composable("final_history") { FinalTranslationHistoryScreen() }
        }
    }
}

package com.example.signassistap.finalmodule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.signassistap.network.ApiClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalAdminDashboardScreen(navController: NavController) {
    var totalUsers by remember { mutableStateOf("...") }
    var totalFeedbacks by remember { mutableStateOf("...") }
    var totalTranslations by remember { mutableStateOf("...") }
    var statsError by remember { mutableStateOf<String?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    suspend fun loadStats() {
        statsError = null
        try {
            val usersResponse = ApiClient.signAssistApi.getUsers()
            val feedbackResponse = ApiClient.signAssistApi.getAdminFeedback()
            val historyResponse = ApiClient.signAssistApi.getAdminHistory()

            if (usersResponse.isSuccessful) {
                totalUsers = usersResponse.body().orEmpty().size.toString()
            } else {
                totalUsers = "-"
                statsError = "Some dashboard counts could not load."
            }

            if (feedbackResponse.isSuccessful) {
                totalFeedbacks = feedbackResponse.body().orEmpty()
                    .sumOf { it.feedbacks.size }
                    .toString()
            } else {
                totalFeedbacks = "-"
                statsError = "Some dashboard counts could not load."
            }

            if (historyResponse.isSuccessful) {
                totalTranslations = historyResponse.body().orEmpty().size.toString()
            } else {
                totalTranslations = "-"
                statsError = "Some dashboard counts could not load."
            }
        } catch (e: Exception) {
            totalUsers = "-"
            totalFeedbacks = "-"
            totalTranslations = "-"
            statsError = e.message ?: "Dashboard counts could not load."
        }
    }

    LaunchedEffect(Unit) {
        loadStats()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { loadStats() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val stats = listOf(
        Triple("Total Users", totalUsers, Color(0xFF1565C0)),
        Triple("Feedbacks", totalFeedbacks, Color(0xFF2E7D32)),
        Triple("Translations", totalTranslations, Color(0xFF6A1B9A))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Admin Dashboard",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1565C0)),
                actions = {
                    IconButton(onClick = {
                        navController.navigate("login") {
                            popUpTo("final_dashboard") { inclusive = true }
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF0F4FF))
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Welcome, Admin",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1565C0)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                stats.forEach { (title, value, color) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = color)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(title, fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                        }
                    }
                }
            }

            if (!statsError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(statsError!!, fontSize = 12.sp, color = Color(0xFFC62828))
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Manage", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))

            FinalAdminMenuCard(
                icon = Icons.Default.People,
                title = "Users",
                subtitle = "View all registered users",
                color = Color(0xFF1565C0),
                onClick = { navController.navigate("final_admin_users") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            FinalAdminMenuCard(
                icon = Icons.Default.Feedback,
                title = "Feedbacks",
                subtitle = "View all user feedbacks",
                color = Color(0xFF2E7D32),
                onClick = { navController.navigate("final_admin_feedback") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            FinalAdminMenuCard(
                icon = Icons.Default.History,
                title = "Text-to-Sign Data",
                subtitle = "View all Text-to-Sign records",
                color = Color(0xFF6A1B9A),
                onClick = { navController.navigate("final_admin_text_to_sign") }
            )
        }
    }
}

@Composable
fun FinalAdminMenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

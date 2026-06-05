package com.example.signassistap.finalmodule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.signassistap.network.AdminFeedbackResponse
import com.example.signassistap.network.ApiClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalAdminFeedbackScreen(navController: NavController) {
    var feedbackRows by remember { mutableStateOf<List<AdminFeedbackResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        error = null
        try {
            val response = ApiClient.signAssistApi.getAdminFeedback()
            if (response.isSuccessful) {
                feedbackRows = response.body().orEmpty()
            } else {
                error = response.errorBody()?.string()?.take(180) ?: "Failed to load feedback."
            }
        } catch (e: Exception) {
            error = e.message ?: "Failed to load feedback."
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Feedback", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2E7D32))
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
            Text("${feedbackRows.size} users with feedback", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                !error.isNullOrBlank() -> Text(error!!, color = Color.Red)
                feedbackRows.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No feedback submitted yet.", color = Color.Gray)
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(feedbackRows) { row ->
                        AdminFeedbackCard(row)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminFeedbackCard(row: AdminFeedbackResponse) {
    var showAll by remember { mutableStateOf(false) }
    val latestFirst = row.feedbacks.asReversed()
    val visibleFeedbacks = if (showAll) latestFirst else latestFirst.take(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Feedback, contentDescription = null, tint = Color(0xFF2E7D32))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(row.fullName.orEmpty(), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(row.email.orEmpty(), fontSize = 12.sp, color = Color.Gray)
                }
                if (row.feedbacks.size > 1) {
                    TextButton(onClick = { showAll = !showAll }) {
                        Text(if (showAll) "Show less" else "Show more")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (showAll) 220.dp else 80.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                visibleFeedbacks.forEachIndexed { index, feedback ->
                    Text(
                        text = "${index + 1}. $feedback",
                        fontSize = 14.sp,
                        color = Color(0xFF1A1A2E),
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }
        }
    }
}

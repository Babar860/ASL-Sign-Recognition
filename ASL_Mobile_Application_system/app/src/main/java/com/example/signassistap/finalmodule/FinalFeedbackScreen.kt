package com.example.signassistap.finalmodule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.signassistap.network.AddFeedbackRequest
import com.example.signassistap.network.ApiClient
import com.example.signassistap.utils.UserSession
import kotlinx.coroutines.launch

@Composable
fun FinalFeedbackScreen(feedbackList: MutableList<String>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var feedbackText by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var feedbacks by remember { mutableStateOf<List<String>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAllFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val userId = UserSession.getUserId(context)
        if (!userId.isNullOrBlank()) {
            try {
                val response = ApiClient.signAssistApi.getFeedback(userId)
                if (response.isSuccessful) {
                    feedbacks = response.body()?.feedbacks.orEmpty()
                }
            } catch (_: Exception) {
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4FF))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Feedback,
                contentDescription = null,
                tint = Color(0xFF1565C0),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Feedback",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1565C0)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Share your feedback", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it; submitted = false },
                    placeholder = { Text("Write your feedback here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    maxLines = 5,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (feedbackText.isNotBlank()) {
                            val userId = UserSession.getUserId(context)
                            if (userId.isNullOrBlank()) {
                                error = "User not logged in."
                                return@Button
                            }
                            val text = feedbackText.trim()
                            scope.launch {
                                try {
                                    val response = ApiClient.signAssistApi.addFeedback(
                                        AddFeedbackRequest(userId = userId, feedback = text)
                                    )
                                    if (response.isSuccessful) {
                                        feedbacks = response.body()?.feedbacks.orEmpty()
                                        feedbackText = ""
                                        submitted = true
                                        error = null
                                    } else {
                                        error = response.errorBody()?.string()?.take(180)
                                            ?: "Failed to submit feedback."
                                    }
                                } catch (e: Exception) {
                                    error = e.message ?: "Failed to submit feedback."
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Feedback", color = Color.White, fontWeight = FontWeight.Bold)
                }

                if (submitted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Feedback submitted!", color = Color(0xFF2E7D32), fontSize = 13.sp)
                }
                if (!error.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error!!, color = Color.Red, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (feedbacks.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showAllFeedback) "All Feedbacks (${feedbacks.size})" else "Latest Feedback",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1A1A2E)
                )
                if (feedbacks.size > 1) {
                    TextButton(onClick = { showAllFeedback = !showAllFeedback }) {
                        Text(if (showAllFeedback) "Show less" else "Show more")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val visibleFeedbacks = if (showAllFeedback) feedbacks else feedbacks.takeLast(1)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(visibleFeedbacks) { index, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        Color(0xFF1565C0).copy(alpha = 0.12f),
                                        RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${if (showAllFeedback) index + 1 else feedbacks.size}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1565C0)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(item, fontSize = 14.sp, color = Color(0xFF333333))
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No feedback yet", color = Color.Gray, fontSize = 15.sp)
            }
        }
    }
}

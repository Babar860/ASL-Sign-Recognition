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
import androidx.compose.material.icons.filled.SignLanguage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.signassistap.network.AdminTranslationHistoryResponse
import com.example.signassistap.network.ApiClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalAdminTextToSignScreen(navController: NavController) {
    var records by remember { mutableStateOf<List<AdminTranslationHistoryResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        error = null
        try {
            val response = ApiClient.signAssistApi.getAdminTextToSignHistory()
            if (response.isSuccessful) {
                records = response.body().orEmpty()
            } else {
                error = response.errorBody()?.string()?.take(180) ?: "Failed to load Text-to-Sign data."
            }
        } catch (e: Exception) {
            error = e.message ?: "Failed to load Text-to-Sign data."
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Text-to-Sign Data", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF6A1B9A))
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
            Text("${records.size} Text-to-Sign records from the last 24 hours", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                !error.isNullOrBlank() -> Text(error!!, color = Color.Red)
                records.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No Text-to-Sign records yet.", color = Color.Gray)
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(records.groupBy { it.userId ?: it.email.orEmpty() }.values.toList()) { userRecords ->
                        AdminTextToSignCard(userRecords)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminTextToSignCard(records: List<AdminTranslationHistoryResponse>) {
    var showAll by remember { mutableStateOf(false) }
    val latestFirst = records.sortedByDescending { it.createdAt.orEmpty() }
    val firstRecord = latestFirst.first()
    val visibleRecords = if (showAll) latestFirst else latestFirst.take(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SignLanguage, contentDescription = null, tint = Color(0xFF6A1B9A))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(firstRecord.fullName.orEmpty(), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(firstRecord.email.orEmpty(), fontSize = 12.sp, color = Color.Gray)
                }
                if (latestFirst.size > 1) {
                    TextButton(onClick = { showAll = !showAll }) {
                        Text(if (showAll) "Show less" else "Show more")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (showAll) 260.dp else 110.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                visibleRecords.forEach { record ->
                    Text(record.sentence.orEmpty(), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    if (record.words.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Words: ${record.words.joinToString(", ")}", fontSize = 12.sp, color = Color.Gray)
                    }
                    if (!record.createdAt.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(record.createdAt, fontSize = 11.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

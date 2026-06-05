package com.example.signassistap

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.signassistap.EditProfileFormScreen
import com.example.signassistap.models.ConfirmPasswordResetRequest
import com.example.signassistap.models.RequestPasswordResetRequest
import com.example.signassistap.network.ApiClient
import com.example.signassistap.network.CreateHistoryRequest
import com.example.signassistap.network.LearningSignResponse
import com.example.signassistap.network.TranslationHistoryResponse
import com.example.signassistap.ui.theme.SignAssistAppTheme
import com.example.signassistap.finalmodule.FinalAdminDashboardScreen
import com.example.signassistap.finalmodule.FinalAdminFeedbackScreen
import com.example.signassistap.finalmodule.FinalAdminTextToSignScreen
import com.example.signassistap.finalmodule.FinalAdminUsersScreen
import com.example.signassistap.finalmodule.FinalFeedbackScreen
import com.example.signassistap.finalmodule.FinalTextToSignScreen
import com.example.signassistap.finalmodule.FinalTranslationHistoryScreen
import com.example.signassistap.utils.FileUploadUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.net.URL

class MainActivity : ComponentActivity() {

    private val REQUEST_CODE_SPEECH_INPUT = 100
    private val REQUEST_CODE_VIDEO_CAPTURE = 200

    private var speechCallback: ((String) -> Unit)? = null
    private var videoCallback: ((Uri) -> Unit)? = null

    private val translationHistory = mutableStateListOf<HistoryItem>()
    val feedbackList = mutableStateListOf<String>()

    // ✅ Deep link token state
    private val deepLinkTokenState = mutableStateOf<String?>(null)

    // ✅ Extract token from: signassist://reset?token=XXXX
    private fun extractTokenFromIntent(intent: Intent): String? {
        val data: Uri? = intent.data
        return data?.getQueryParameter("token")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Read token if app opened from deep link (cold start)
        deepLinkTokenState.value = extractTokenFromIntent(intent)

        setContent {
            SignAssistAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val token = deepLinkTokenState.value

                    if (!token.isNullOrEmpty()) {
                        ResetPasswordScreen(
                            token = token,
                            onDone = { deepLinkTokenState.value = null }
                        )
                    } else {
                        AppNavigation(
                            activity = this@MainActivity,
                            historyList = translationHistory,
                            feedbackList = feedbackList
                        )
                    }
                }
            }
        }
    }

    // ✅ IMPORTANT: When app is already running & user clicks email link again
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLinkTokenState.value = extractTokenFromIntent(intent)
    }

    // 🎤 Speech-to-Text
    fun startSpeechToText(onResult: (String) -> Unit) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")

        try {
            speechCallback = onResult
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        } catch (e: Exception) {
            Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_SHORT).show()
        }
    }

    // 🎥 Video Recording
    fun startVideoRecording(onVideoRecorded: (Uri) -> Unit) {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        try {
            videoCallback = onVideoRecorded
            startActivityForResult(intent, REQUEST_CODE_VIDEO_CAPTURE)
        } catch (e: Exception) {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show()
        }
    }

    // 🎙️ Handle Results
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_CODE_SPEECH_INPUT -> {
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    val spokenText = result?.get(0) ?: ""
                    speechCallback?.invoke(spokenText)
                    if (spokenText.isNotEmpty()) {
                        val signResult = "Sign for: \"$spokenText\""
                        translationHistory.add(0, HistoryItem(Date().time, signResult, "Text-to-Sign"))
                    }
                }

                REQUEST_CODE_VIDEO_CAPTURE -> {
                    val videoUri = data.data
                    if (videoUri != null) {
                        videoCallback?.invoke(videoUri)
                        Toast.makeText(this, "🎬 Video saved successfully!", Toast.LENGTH_SHORT).show()

                        val signResult = "Sign Recognized from Video"
                        translationHistory.add(0, HistoryItem(Date().time, signResult, "Video-to-Text"))
                    }
                }
            }
        }
    }
}

// -------------------- DATA --------------------

data class HistoryItem(
    val timestamp: Long,
    val translation: String,
    val type: String
)

data class NavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    NavItem("text_to_sign", Icons.Default.SignLanguage, "Text to Sign"),
    NavItem("live_detection", Icons.Default.Camera, "Sign to Text"),
    NavItem("learn_signs", Icons.Default.School, "Learn"),
    NavItem("feedback", Icons.Default.Feedback, "Feedback"),
    NavItem("history", Icons.Default.History, "History")
)

private val mainBottomBarRoutes = bottomNavItems.map { it.route }.toSet()

// -------------------- NAVIGATION --------------------

@Composable
fun AppNavigation(
    activity: MainActivity,
    historyList: MutableList<HistoryItem>,
    feedbackList: MutableList<String>
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash_screen") {

        composable("splash_screen") { SplashScreen(navController) }

        composable("start_screen") { StartScreen(navController) }
        composable("login") { LoginScreen(navController) }

        // ✅ SignupScreen tumhari alag file (SignupScreen.kt) me already hai
        composable("signup") { SignupScreen(navController) }

        // ✅ Main app
        composable("home") {
            MainContentScreen(
                activity = activity,
                rootNavController = navController,
                historyList = historyList,
                feedbackList = feedbackList
            )
        }

        // extra pages
        composable("sign_detail/{letter}") { backStackEntry ->
            val letter = backStackEntry.arguments?.getString("letter") ?: "A"
            SignDetailScreen(navController, letter)
        }

        composable("settings") { SettingsScreen(navController) }
        composable("about_app") { AboutScreen(navController) }
        composable("profile") { ProfileScreen(navController) }
        composable("edit_profile") { EditProfileScreen(navController) }
        composable("help_support") { HelpSupportScreen(navController) }

        // ✅ Forgot Password Screen
        composable("forgot_password") { ForgotPasswordScreen(navController) }
        composable("final_dashboard") { FinalAdminDashboardScreen(navController) }
        composable("final_admin_users") { FinalAdminUsersScreen(navController) }
        composable("final_admin_feedback") { FinalAdminFeedbackScreen(navController) }
        composable("final_admin_text_to_sign") { FinalAdminTextToSignScreen(navController) }
    }
}

// -------------------- ✅ FORGOT PASSWORD (NOW API CONNECTED) --------------------

@Composable
fun ForgotPasswordScreen(navController: NavHostController) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3F2FD))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Forgot Password",
            fontSize = 26.sp,
            color = Color(0xFF2196F3),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; error = null },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (!error.isNullOrBlank()) {
            Text(text = error!!, color = Color.Red, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(10.dp))
        }

        Button(
            onClick = {
                val e = email.trim()
                if (e.isBlank()) {
                    Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true
                error = null

                scope.launch {
                    try {
                        val resp = ApiClient.authApi.requestPasswordReset(
                            RequestPasswordResetRequest(email = e)
                        )

                        if (resp.isSuccessful) {
                            Toast.makeText(
                                context,
                                "If email exists, reset link has been sent ✅",
                                Toast.LENGTH_LONG
                            ).show()

                            // ✅ back to login screen
                            navController.popBackStack()
                        } else {
                            val msg = resp.errorBody()?.string()?.take(200)
                            error = msg ?: "Failed to send reset link."
                        }
                    } catch (ex: Exception) {
                        error = ex.message ?: "Network error"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Sending...", color = Color.White, fontWeight = FontWeight.Bold)
            } else {
                Text("Send Reset Link", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = { navController.popBackStack() }, enabled = !isLoading) {
            Text("Back to Login", color = Color(0xFF1565C0))
        }
    }
}

// -------------------- SPLASH --------------------

@Composable
fun SplashScreen(navController: NavHostController) {
    LaunchedEffect(key1 = true) {
        Handler(Looper.getMainLooper()).postDelayed({
            navController.navigate("start_screen") {
                popUpTo("splash_screen") { inclusive = true }
            }
        }, 2000)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2196F3))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SignAssist", fontSize = 48.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(20.dp))
        CircularProgressIndicator(color = Color.White)
    }
}

// -------------------- START SCREEN --------------------

@Composable
fun StartScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SignAssist", fontSize = 32.sp, color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(40.dp))

        Image(
            painter = painterResource(id = R.drawable.hand_icon),
            contentDescription = "App Logo",
            modifier = Modifier.size(200.dp)
        )

        Spacer(modifier = Modifier.height(60.dp))

        Button(
            onClick = { navController.navigate("login") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
            modifier = Modifier.fillMaxWidth(0.7f).height(50.dp)
        ) {
            Text("Get Started", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// -------------------- ✅ LOGIN (API based) --------------------

@Composable
fun LoginScreen(navController: NavHostController) {

    val authViewModel: com.example.signassistap.viewmodel.AuthViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()

    val uiState by authViewModel.loginUiState.collectAsState()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    // ✅ ONLY success pe hi home
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
            authViewModel.clearLoginState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3F2FD))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text("Welcome Back!", fontSize = 28.sp, color = Color(0xFF2196F3))
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(16.dp))

        val errorToShow = localError ?: uiState.error
        if (!errorToShow.isNullOrBlank()) {
            Text(text = errorToShow, color = Color.Red, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
        }

        Button(
            onClick = {
                localError = null
                authViewModel.clearLoginState()

                if (email.isBlank() || password.isBlank()) {
                    localError = "Please enter email and password."
                    return@Button
                }

                if (email.trim().equals("admin@gmail.com", ignoreCase = true) && password == "admin123") {
                    navController.navigate("final_dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                    return@Button
                }

                authViewModel.login(
                    context = context,
                    email = email.trim(),
                    password = password
                )
            },
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Logging in...", color = Color.White, fontSize = 16.sp)
            } else {
                Text("Login", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = { navController.navigate("signup") }) {
                Text("Sign Up", color = Color(0xFF1565C0))
            }

            TextButton(onClick = { navController.navigate("forgot_password") }) {
                Text("Forgot Password?", color = Color(0xFF1565C0))
            }
        }
    }
}

// -------------------- ✅ MAIN CONTENT (Bottom Nav + TopBar) --------------------

@Composable
fun MainContentScreen(
    activity: MainActivity,
    rootNavController: NavHostController,
    historyList: MutableList<HistoryItem>,
    feedbackList: MutableList<String>
) {
    val bottomNavController = rememberNavController()
    val currentEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val showBottomBar = currentRoute in mainBottomBarRoutes

    Scaffold(
        topBar = { SignAssistTopBar(rootNavController) },
        bottomBar = {
            if (showBottomBar) {
                SignAssistBottomBar(bottomNavController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = bottomNavController,
            startDestination = "text_to_sign",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("text_to_sign") { FinalTextToSignScreen() }
            composable("live_detection") { LiveDetectionScreen(activity) }
            composable("learn_signs") { LearnSignsScreen(rootNavController) }
            composable("feedback") { FinalFeedbackScreen(feedbackList) }
            composable("history") { HistoryScreen(historyList) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignAssistTopBar(navController: NavHostController) {
    TopAppBar(
        title = { Text("SignAssist", fontWeight = FontWeight.Bold) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF2196F3),
            titleContentColor = Color.White
        ),
        actions = {
            IconButton(onClick = { navController.navigate("settings") }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
            }
            IconButton(onClick = { navController.navigate("profile") }) {
                Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White)
            }
        }
    )
}

@Composable
fun SignAssistBottomBar(navController: NavHostController) {
    NavigationBar(containerColor = Color.White) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        bottomNavItems.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

// -------------------- LIVE --------------------

@Composable
fun LiveDetectionScreen(activity: MainActivity) {
    val viewModel: com.example.signassistap.viewmodel.AslTranslationViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val result by viewModel.signToTextResult.collectAsState()
    val confidence by viewModel.signConfidence.collectAsState()
    val frameProgress by viewModel.liveFrameProgress.collectAsState()
    val error by viewModel.error.collectAsState()
    var liveEnabled by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var detectedParagraph by remember { mutableStateOf("") }
    var lastAppendedPrediction by remember { mutableStateOf("") }
    val sessionId = remember { UUID.randomUUID().toString() }
    val detectedSign = when {
        !cameraError.isNullOrBlank() -> cameraError!!
        !error.isNullOrBlank() -> error!!
        !result.isNullOrBlank() -> result!!
        liveEnabled -> "Waiting for Hand Gesture"
        else -> "Start detection and show a sign to begin translation."
    }
    val hasError = !cameraError.isNullOrBlank() || !error.isNullOrBlank()
    val confidenceText = confidence?.let { "Confidence: ${(it * 100).toInt()}%" }
    val frameProgressValue = frameProgress?.let { (buffered, required) ->
        if (required > 0) (buffered.toFloat() / required.toFloat()).coerceIn(0f, 1f) else 0f
    } ?: 0f
    val showFrameProgress = liveEnabled && frameProgress != null && !hasError
    val isFrameProgressComplete = showFrameProgress && frameProgressValue >= 1f
    val statusText = when {
        confidenceText != null -> confidenceText
        liveEnabled && result == "Collecting..." -> "Collecting live frames"
        liveEnabled -> "Detecting Sign Language"
        else -> "Camera preview is ready"
    }

    LaunchedEffect(result) {
        val clean = result
            ?.replace(Regex("\\s*\\(\\d+/\\d+\\)\\s*$"), "")
            ?.trim()
            .orEmpty()
        val ignored = setOf(
            "",
            "Collecting...",
            "Waiting for Hand Gesture",
            "Low Confidence",
            "Prediction completed but no text returned"
        )
        if (clean !in ignored && clean != lastAppendedPrediction) {
            detectedParagraph = listOf(detectedParagraph, clean)
                .filter { it.isNotBlank() }
                .joinToString(" ")
            lastAppendedPrediction = clean
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Live Sign to Text",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D47A1)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.35f),
            shape = RoundedCornerShape(12.dp)
        ) {
            LiveCameraFrameAnalyzer(
                modifier = Modifier.fillMaxSize(),
                enabled = liveEnabled,
                throttleMs = 220L,
                onFrame = { jpegBytes ->
                    cameraError = null
                    viewModel.submitLiveFrame(jpegBytes, sessionId)
                },
                onAnalyzerError = { message ->
                    cameraError = message
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                liveEnabled = !liveEnabled
                cameraError = null
                viewModel.clearError()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (liveEnabled) Color(0xFFC62828) else Color(0xFF4CAF50)
            )
        ) {
            Icon(
                if (liveEnabled) Icons.Default.Stop else Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (liveEnabled) "Stop Live Detection" else "Start Live Detection",
                color = Color.White,
                fontSize = 17.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    detectedSign,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (!hasError) Color(0xFF0D47A1) else Color(0xFFC62828)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    statusText,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                if (showFrameProgress) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = frameProgressValue,
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = if (isFrameProgressComplete) Color(0xFF2E7D32) else Color(0xFF1976D2),
                        trackColor = Color(0xFFE3F2FD)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Detected Text", fontSize = 16.sp, color = Color.Gray)
                    TextButton(
                        onClick = {
                            val sentence = detectedParagraph.trim()
                            if (sentence.isNotBlank()) {
                                val userId = com.example.signassistap.utils.UserSession.getUserId(context)
                                if (!userId.isNullOrBlank()) {
                                    scope.launch {
                                        try {
                                            ApiClient.signAssistApi.saveHistory(
                                                CreateHistoryRequest(
                                                    userId = userId,
                                                    sentence = sentence,
                                                    words = sentence.split(" ")
                                                        .map { it.trim() }
                                                        .filter { it.isNotBlank() }
                                                )
                                            )
                                        } catch (_: Exception) {
                                        }
                                    }
                                }
                            }
                            detectedParagraph = ""
                            lastAppendedPrediction = ""
                            cameraError = null
                            viewModel.clearSignToTextResult()
                        }
                    ) {
                        Text("Clear")
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = detectedParagraph.ifBlank { "Start detection and show a sign to begin translation." },
                    fontSize = 18.sp,
                    color = if (detectedParagraph.isBlank()) Color.Gray else Color(0xFF1A237E),
                    lineHeight = 26.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// -------------------- LEARN --------------------

@Composable
fun LearnSignsScreen(navController: NavHostController) {
    var alphabetSigns by remember { mutableStateOf<List<LearningSignResponse>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val response = ApiClient.signAssistApi.getAlphabetSigns()
            if (response.isSuccessful) {
                alphabetSigns = response.body().orEmpty()
            } else {
                error = "Could not load signs."
            }
        } catch (e: Exception) {
            error = e.message ?: "Could not load signs."
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Learn Sign Language (A-Z)",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D47A1)
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(alphabetSigns.ifEmpty {
                ('A'..'Z').map { LearningSignResponse(letter = it.toString()) }
            }) { item ->
                Card(
                    modifier = Modifier.aspectRatio(1f).clickable {
                        navController.navigate("sign_detail/${item.letter}")
                    },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFBBDEFB)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            item.letter,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0D47A1)
                        )
                    }
                }
            }
        }
        if (!error.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(error!!, color = Color.Red, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignDetailScreen(navController: NavHostController, letter: String) {
    var sign by remember { mutableStateOf<LearningSignResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(letter) {
        try {
            val response = ApiClient.signAssistApi.getAlphabetSign(letter)
            if (response.isSuccessful) {
                sign = response.body()
            } else {
                error = "Could not load sign."
            }
        } catch (e: Exception) {
            error = e.message ?: "Could not load sign."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign for '$letter'") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "ASL Sign for Letter: $letter",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(0.8f).aspectRatio(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val imageUrl = sign?.imageUrl
                    if (!imageUrl.isNullOrBlank()) {
                        RemoteImage(
                            url = imageUrl,
                            contentDescription = "Sign for $letter",
                            modifier = Modifier.fillMaxSize().padding(18.dp)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.hand_icon),
                            contentDescription = null,
                            modifier = Modifier.size(220.dp)
                        )
                    }
                }
            }
            if (!error.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(error!!, color = Color.Red)
            }
        }
    }
}

@Composable
fun RemoteImage(url: String, contentDescription: String?, modifier: Modifier = Modifier) {
    var bitmap by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var failed by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        failed = false
        bitmap = try {
            withContext(Dispatchers.IO) {
                URL(url).openStream().use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        } catch (_: Exception) {
            failed = true
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier
        )
    } else if (failed) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.hand_icon),
                contentDescription = contentDescription,
                modifier = Modifier.size(220.dp)
            )
        }
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

// -------------------- HISTORY --------------------

@Composable
fun HistoryScreen(historyList: List<HistoryItem>) {
    val context = LocalContext.current
    var dbHistory by remember { mutableStateOf<List<TranslationHistoryResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAllHistory by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val userId = com.example.signassistap.utils.UserSession.getUserId(context)
        if (userId.isNullOrBlank()) {
            error = "User not logged in."
            return@LaunchedEffect
        }
        isLoading = true
        error = null
        try {
            val response = ApiClient.signAssistApi.getHistory(userId)
            if (response.isSuccessful) {
                dbHistory = response.body().orEmpty()
            } else {
                error = response.errorBody()?.string()?.take(180) ?: "Failed to load history."
            }
        } catch (e: Exception) {
            error = e.message ?: "Failed to load history."
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Recent Translations",
            modifier = Modifier.padding(16.dp),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D47A1)
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (!error.isNullOrBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error!!, color = Color.Red, fontSize = 16.sp)
            }
        } else if (dbHistory.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No translation history yet.", color = Color.Gray, fontSize = 18.sp)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (showAllHistory) "Last 24 Hours (${dbHistory.size})" else "Latest Translation",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1A1A2E)
                )
                if (dbHistory.size > 1) {
                    TextButton(onClick = { showAllHistory = !showAllHistory }) {
                        Text(if (showAllHistory) "Show less" else "Show more")
                    }
                }
            }
            val visibleHistory = if (showAllHistory) dbHistory else dbHistory.take(1)
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                items(visibleHistory) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(item.sentence.orEmpty(), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Type: ${item.translationType ?: "Sign-to-Text"}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------- UPLOAD --------------------

@Composable
fun UploadScreen(activity: MainActivity, historyList: MutableList<HistoryItem>) {

    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val videoViewModel: com.example.signassistap.viewmodel.VideoViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()

    val uploading by videoViewModel.isUploading.collectAsState()
    val resultText by videoViewModel.uploadResult.collectAsState()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedUri = uri
        if (uri != null) {
            Toast.makeText(context, "Selected video!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Video Recognition", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Prediction Result", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = resultText ?: "—",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D47A1)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = { galleryLauncher.launch("video/*") },
            modifier = Modifier.fillMaxWidth(0.75f).height(50.dp),
        ) {
            Text("Upload Video from Mobile")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            enabled = (selectedUri != null) && !uploading,
            onClick = {
                val uri = selectedUri ?: return@Button
                val durationSeconds = 10

                val userId = com.example.signassistap.utils.UserSession.getUserId(context)
                Log.d("SESSION", "UserId from session = $userId")

                if (userId.isNullOrBlank()) {
                    Toast.makeText(
                        context,
                        "User not logged in. Please login again.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@Button
                }

                val filePart = FileUploadUtils.uriToVideoPart(context, uri)

                videoViewModel.uploadVideo(
                    filePart = filePart,
                    userId = userId,
                    durationSeconds = durationSeconds
                )
            },
            modifier = Modifier.fillMaxWidth(0.75f).height(50.dp),
        ) {
            Text(if (uploading) "Uploading & Predicting..." else "Upload Now")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                activity.startVideoRecording { uri ->
                    selectedUri = uri
                    Toast.makeText(context, "Recorded video selected!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(0.75f).height(50.dp),
        ) {
            Text("Record Video")
        }
    }
}

// -------------------- SETTINGS / ABOUT / PROFILE --------------------

@Composable
fun SettingsScreen(navController: NavHostController) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { navController.navigate("about_app") }) {
            Text("About App")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = {
            navController.navigate("login") {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }) {
            Text("Logout")
        }
    }
}

@Composable
fun AboutScreen(navController: NavHostController) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("About SignAssist", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Version 1.0.0")
        Spacer(modifier = Modifier.height(12.dp))
        Text("SignAssist is a tool for sign language learning + detection.")
    }
}

@Composable
fun ProfileScreen(navController: NavHostController) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Profile", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("edit_profile") }) { Text("Edit Profile") }
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = { navController.navigate("help_support") }) { Text("Help & Support") }
    }
}

@Composable
fun EditProfileScreen(navController: NavHostController) {
    EditProfileFormScreen()
}

@Composable
fun HelpSupportScreen(navController: NavHostController) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Help & Support", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Placeholder screen")
    }
}

// -------------------- ✅ RESET PASSWORD (NOW API CONNECTED) --------------------

@Composable
fun ResetPasswordScreen(
    token: String,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3F2FD))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Reset Password",
            fontSize = 26.sp,
            color = Color(0xFF2196F3),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text("Token: $token", fontSize = 12.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it; error = null },
            label = { Text("New Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; error = null },
            label = { Text("Confirm Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (!error.isNullOrBlank()) {
            Text(text = error!!, color = Color.Red, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(10.dp))
        }

        Button(
            onClick = {
                val p1 = newPassword.trim()
                val p2 = confirmPassword.trim()

                if (p1.isBlank() || p2.isBlank()) {
                    error = "Please enter both fields."
                    return@Button
                }
                if (p1.length < 6) {
                    error = "Password must be at least 6 characters."
                    return@Button
                }
                if (p1 != p2) {
                    error = "Passwords do not match."
                    return@Button
                }

                isLoading = true
                error = null

                scope.launch {
                    try {
                        val resp = ApiClient.authApi.confirmPasswordReset(
                            ConfirmPasswordResetRequest(
                                token = token,
                                newPassword = p1,
                                confirmPassword = p2
                            )
                        )

                        if (resp.isSuccessful) {
                            Toast.makeText(context, "Password updated ✅", Toast.LENGTH_LONG).show()
                            onDone()
                        } else {
                            val msg = resp.errorBody()?.string()?.take(200)
                            error = msg ?: "Failed to reset password."
                        }
                    } catch (ex: Exception) {
                        error = ex.message ?: "Network error"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Updating...", color = Color.White, fontWeight = FontWeight.Bold)
            } else {
                Text("Update Password", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(onClick = onDone, enabled = !isLoading) {
            Text("Back", color = Color(0xFF1565C0))
        }
    }
}

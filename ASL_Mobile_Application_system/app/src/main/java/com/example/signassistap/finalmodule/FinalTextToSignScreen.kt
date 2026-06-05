package com.example.signassistap.finalmodule

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.signassistap.network.ApiClient
import com.example.signassistap.network.CreateHistoryRequest
import com.example.signassistap.network.TextToSignItem
import com.example.signassistap.utils.UserSession
import com.example.signassistap.viewmodel.AslTranslationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalTextToSignScreen() {
    var inputText by remember { mutableStateOf("") }
    var currentIndex by remember { mutableStateOf(0) }
    var playbackState by remember { mutableStateOf(SignPlaybackState.STOPPED) }
    var videoSpeed by remember { mutableStateOf(0.75f) }
    var speedMenuExpanded by remember { mutableStateOf(false) }
    var lastSavedText by remember { mutableStateOf("") }
    val context = LocalContext.current

    val viewModel: AslTranslationViewModel = viewModel()
    val isLoading by viewModel.isLoading.collectAsState()
    val response by viewModel.textToSignResult.collectAsState()
    val error by viewModel.error.collectAsState()
    val playableItems = remember(response) {
        response?.items.orEmpty().filter { it.type != "space" && !it.url.isNullOrBlank() }
    }
    val videoUrl = ApiClient.resolveAslUrl(response?.videoUrl)

    LaunchedEffect(response) {
        currentIndex = 0
        playbackState = if (response == null) SignPlaybackState.STOPPED else SignPlaybackState.PLAYING
    }

    LaunchedEffect(response?.inputText, response?.normalizedText) {
        val convertedText = response?.normalizedText?.takeIf { it.isNotBlank() }
            ?: response?.inputText?.takeIf { it.isNotBlank() }
            ?: return@LaunchedEffect

        if (convertedText == lastSavedText) return@LaunchedEffect

        val userId = UserSession.getUserId(context)
        if (!userId.isNullOrBlank()) {
            runCatching {
                ApiClient.signAssistApi.saveHistory(
                    CreateHistoryRequest(
                        userId = userId,
                        sentence = convertedText,
                        words = convertedText.split(Regex("\\s+")).filter { it.isNotBlank() },
                        translationType = "Text-to-Sign"
                    )
                )
            }
            lastSavedText = convertedText
        }
    }

    LaunchedEffect(playableItems, playbackState) {
        currentIndex = 0
        while (playableItems.size > 1 && playbackState == SignPlaybackState.PLAYING) {
            val currentItem = playableItems.getOrNull(currentIndex)
            val baseDelayMs = currentItem?.durationMs ?: 1000
            val delayMs = if (currentItem?.type == "video") {
                (baseDelayMs / videoSpeed).toLong()
            } else {
                baseDelayMs.toLong()
            }
            delay(delayMs.coerceAtLeast(600L))
            currentIndex = (currentIndex + 1) % playableItems.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Text to Sign", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = inputText,
            onValueChange = {
                inputText = it
                viewModel.clearError()
            },
            placeholder = { Text("Enter text e.g. hello") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.textToSign(inputText) },
                enabled = inputText.isNotBlank() && !isLoading,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(if (isLoading) "Converting..." else "Convert to Sign")
            }

            ExposedDropdownMenuBox(
                expanded = speedMenuExpanded,
                onExpandedChange = { speedMenuExpanded = !speedMenuExpanded },
                modifier = Modifier.width(168.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .height(46.dp)
                        .clickable { speedMenuExpanded = true },
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 12.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Video speed", fontSize = 14.sp, modifier = Modifier.weight(1f))
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = speedMenuExpanded)
                    }
                }
                ExposedDropdownMenu(
                    expanded = speedMenuExpanded,
                    onDismissRequest = { speedMenuExpanded = false }
                ) {
                    listOf(0.5f, 0.75f, 1.0f).forEach { speed ->
                        DropdownMenuItem(
                            text = { Text("${speed}x") },
                            trailingIcon = {
                                if (videoSpeed == speed) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected")
                                }
                            },
                            onClick = {
                                videoSpeed = speed
                                speedMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    playbackState =
                        if (playbackState == SignPlaybackState.PLAYING) SignPlaybackState.PAUSED
                        else SignPlaybackState.PLAYING
                },
                enabled = response != null,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = Color(0xFFB0BEC5),
                    disabledContentColor = Color.White
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    if (playbackState == SignPlaybackState.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (playbackState == SignPlaybackState.PLAYING) "Pause" else "Resume")
            }

            Button(
                onClick = {
                    inputText = ""
                    currentIndex = 0
                    lastSavedText = ""
                    playbackState = SignPlaybackState.STOPPED
                    viewModel.clearTextToSignResult()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFC62828),
                    contentColor = Color.White
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear")
            }
        }

        error?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = msg, color = Color(0xFFC62828), fontSize = 13.sp)
        }

        response?.unsupportedCharacters?.takeIf { it.isNotEmpty() }?.let { missing ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Unsupported or missing signs: ${missing.joinToString(" ")}",
                color = Color(0xFF795548),
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        when {
            !videoUrl.isNullOrBlank() && playbackState != SignPlaybackState.STOPPED -> {
                Text("Showing: ${response?.normalizedText.orEmpty()}", fontSize = 16.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                NetworkVideoPlayer(videoUrl, playbackState, speed = videoSpeed)
            }

            playableItems.isNotEmpty() && playbackState != SignPlaybackState.STOPPED -> {
                val item = playableItems.getOrElse(currentIndex) { playableItems.first() }
                Text(
                    "Showing: ${item.label.orEmpty()} (${currentIndex + 1}/${playableItems.size})",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (item.type == "video") {
                    val itemVideoUrl = ApiClient.resolveAslUrl(item.url)
                    if (!itemVideoUrl.isNullOrBlank()) {
                        NetworkVideoPlayer(
                            videoUrl = itemVideoUrl,
                            playbackState = playbackState,
                            loop = playableItems.size == 1,
                            speed = videoSpeed
                        )
                    } else {
                        Text("Could not load sign video.", color = Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    NetworkImagePlayer(item)
                }
            }

            !isLoading -> {
                Text("Enter text and convert to load ASL assets from backend.", color = Color.Gray)
            }
        }
    }
}

private enum class SignPlaybackState {
    PLAYING,
    PAUSED,
    STOPPED
}

@Composable
private fun NetworkVideoPlayer(
    videoUrl: String,
    playbackState: SignPlaybackState,
    loop: Boolean = true,
    speed: Float = 1.0f
) {
    var mediaPlayer by remember(videoUrl) { mutableStateOf<MediaPlayer?>(null) }

    fun applySpeed(player: MediaPlayer?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && player != null) {
            runCatching {
                player.playbackParams = player.playbackParams.setSpeed(speed)
            }
        }
    }

    LaunchedEffect(speed, mediaPlayer) {
        applySpeed(mediaPlayer)
    }

    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                setVideoURI(Uri.parse(videoUrl))
                setOnPreparedListener { player ->
                    mediaPlayer = player
                    player.isLooping = loop
                    applySpeed(player)
                    if (playbackState == SignPlaybackState.PLAYING) start()
                }
            }
        },
        update = { videoView ->
            if (videoView.tag != videoUrl) {
                videoView.tag = videoUrl
                videoView.setVideoURI(Uri.parse(videoUrl))
                videoView.setOnPreparedListener { player ->
                    mediaPlayer = player
                    player.isLooping = loop
                    applySpeed(player)
                    if (playbackState == SignPlaybackState.PLAYING) {
                        videoView.start()
                    }
                }
            }
            when (playbackState) {
                SignPlaybackState.PLAYING -> {
                    if (!videoView.isPlaying) {
                        videoView.start()
                    }
                }

                SignPlaybackState.PAUSED -> {
                    if (videoView.isPlaying) {
                        videoView.pause()
                    }
                }

                SignPlaybackState.STOPPED -> {
                    videoView.stopPlayback()
                    videoView.tag = null
                    videoView.setVideoURI(Uri.parse(videoUrl))
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    )
}

@Composable
private fun NetworkImagePlayer(item: TextToSignItem) {
    val imageUrl = ApiClient.resolveAslUrl(item.url)
    var bitmap by remember(imageUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var failed by remember(imageUrl) { mutableStateOf(false) }

    LaunchedEffect(imageUrl) {
        bitmap = null
        failed = false
        if (imageUrl.isNullOrBlank()) {
            failed = true
            return@LaunchedEffect
        }
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL(imageUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.inputStream.use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        }
        failed = bitmap == null
    }

    when {
        bitmap != null -> {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = item.label,
                modifier = Modifier
                    .width(300.dp)
                    .height(300.dp),
                contentScale = ContentScale.Fit
            )
        }

        failed -> {
            Text("Could not load sign image.", color = Color.Gray, fontSize = 14.sp)
        }

        else -> {
            AndroidView(
                factory = { ImageView(it) },
                modifier = Modifier
                    .width(300.dp)
                    .height(300.dp)
            )
        }
    }
}

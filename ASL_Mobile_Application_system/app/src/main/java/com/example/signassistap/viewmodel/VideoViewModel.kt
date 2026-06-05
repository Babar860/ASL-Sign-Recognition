package com.example.signassistap.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.signassistap.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

class VideoViewModel : ViewModel() {

    private val repo = VideoRepository()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    private val _uploadResult = MutableStateFlow<String?>(null)
    val uploadResult: StateFlow<String?> = _uploadResult

    fun uploadVideo(
        filePart: MultipartBody.Part,
        userId: String,
        durationSeconds: Int
    ) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                val res = repo.uploadVideo(filePart, userId, durationSeconds)

                // ✅ LOGCAT DEBUG (MOST IMPORTANT)
                Log.d("UPLOAD", "HTTP ${res.code()} success=${res.isSuccessful}")
                Log.d("UPLOAD", "body=${res.body()}")
                Log.d("UPLOAD", "error=${res.errorBody()?.string()}")

                if (res.isSuccessful) {
                    val body = res.body()

                    val videoId = body?.videoId
                    if (videoId.isNullOrBlank()) {
                        _uploadResult.value = "Upload success ✅ but videoId missing!"
                        return@launch
                    }

                    // (optional) show processing
                    _uploadResult.value = "Uploaded ✅ Now processing..."

                    // ✅ Predict call (Option A)
                    val predRes = repo.predictVideo(videoId)

                    Log.d("PREDICT", "HTTP ${predRes.code()} success=${predRes.isSuccessful}")
                    Log.d("PREDICT", "body=${predRes.body()}")
                    Log.d("PREDICT", "error=${predRes.errorBody()?.string()}")

                    if (predRes.isSuccessful) {
                        val predictedText = predRes.body()?.predicted_text ?: ""

                        // ✅ CLEAN the output (camelCase split + normalize spaces)
                        val cleaned = predictedText
                            .replace(Regex("([a-z])([A-Z])"), "$1 $2")
                            .replace(Regex("\\s+"), " ")
                            .trim()

                        _uploadResult.value =
                            if (cleaned.isNotBlank()) cleaned
                            else "Prediction done but empty text"
                    } else {
                        _uploadResult.value = "Predict failed ❌ ${predRes.code()} (check Logcat: PREDICT)"
                    }

                } else {
                    _uploadResult.value = "Upload failed ❌ ${res.code()} (check Logcat: UPLOAD)"
                }
            } catch (e: Exception) {
                Log.e("UPLOAD", "Exception: ${e.message}", e)
                _uploadResult.value = "Error ❌ ${e.message}"
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun clearResult() {
        _uploadResult.value = null
    }
}
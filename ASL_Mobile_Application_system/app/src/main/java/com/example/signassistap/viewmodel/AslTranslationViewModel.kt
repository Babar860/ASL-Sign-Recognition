package com.example.signassistap.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.signassistap.network.TextToSignResponse
import com.example.signassistap.repository.AslTranslationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class AslTranslationViewModel : ViewModel() {

    private val repo = AslTranslationRepository()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _signToTextResult = MutableStateFlow<String?>(null)
    val signToTextResult: StateFlow<String?> = _signToTextResult

    private val _signConfidence = MutableStateFlow<Double?>(null)
    val signConfidence: StateFlow<Double?> = _signConfidence

    private val _liveFrameProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val liveFrameProgress: StateFlow<Pair<Int, Int>?> = _liveFrameProgress

    private val _textToSignResult = MutableStateFlow<TextToSignResponse?>(null)
    val textToSignResult: StateFlow<TextToSignResponse?> = _textToSignResult

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val liveFrameInFlight = AtomicBoolean(false)

    fun signToText(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = repo.signToText(context, uri)
                Log.d("ASL_SIGN_TO_TEXT", "HTTP ${response.code()} success=${response.isSuccessful}")
                if (response.isSuccessful) {
                    val body = response.body()
                    val text = body?.text ?: body?.predicted_text
                    _signConfidence.value = body?.confidence
                    _liveFrameProgress.value = null
                    _signToTextResult.value = text?.trim().takeUnless { it.isNullOrBlank() }
                        ?: "Prediction completed but no text returned"
                } else {
                    _error.value = response.errorBody()?.string()?.take(250)
                        ?: "Sign-to-text failed (${response.code()})"
                }
            } catch (e: Exception) {
                Log.e("ASL_SIGN_TO_TEXT", "Exception: ${e.message}", e)
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun textToSign(text: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = repo.textToSign(text)
                Log.d("ASL_TEXT_TO_SIGN", "HTTP ${response.code()} success=${response.isSuccessful}")
                if (response.isSuccessful) {
                    _textToSignResult.value = response.body()
                } else {
                    _error.value = response.errorBody()?.string()?.take(250)
                        ?: "Text-to-sign failed (${response.code()})"
                }
            } catch (e: Exception) {
                Log.e("ASL_TEXT_TO_SIGN", "Exception: ${e.message}", e)
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitLiveFrame(jpegBytes: ByteArray, sessionId: String) {
        if (!liveFrameInFlight.compareAndSet(false, true)) return

        viewModelScope.launch {
            try {
                val response = repo.signToTextLiveFrame(jpegBytes, sessionId)
                if (response.isSuccessful) {
                    val body = response.body()
                    val text = body?.text ?: body?.predicted_text
                    _signConfidence.value = body?.confidence
                    _liveFrameProgress.value = if (body?.requiredFrames != null) {
                        Pair(body.bufferedFrames ?: 0, body.requiredFrames ?: 0)
                    } else {
                        null
                    }
                    if (!text.isNullOrBlank()) {
                        _signToTextResult.value = text.trim()
                    }
                    _error.value = null
                } else {
                    _error.value = response.errorBody()?.string()?.take(250)
                        ?: "Live sign detection failed (${response.code()})"
                }
            } catch (e: Exception) {
                Log.e("ASL_LIVE_SIGN", "Exception: ${e.message}", e)
                _error.value = e.message ?: "Network error"
            } finally {
                liveFrameInFlight.set(false)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSignToTextResult() {
        _signToTextResult.value = null
        _signConfidence.value = null
        _liveFrameProgress.value = null
        _error.value = null
    }

    fun clearTextToSignResult() {
        _textToSignResult.value = null
        _error.value = null
    }
}

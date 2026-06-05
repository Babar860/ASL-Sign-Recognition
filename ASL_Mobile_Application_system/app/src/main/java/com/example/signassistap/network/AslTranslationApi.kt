package com.example.signassistap.network

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class SignToTextResponse(
    val text: String? = null,
    val predicted_text: String? = null,
    val confidence: Double? = null,
    val modelType: String? = null,
    val sessionId: String? = null,
    val bufferedFrames: Int? = null,
    val requiredFrames: Int? = null,
    val isReady: Boolean? = null
)

data class TextToSignRequest(
    val text: String
)

data class TextToSignItem(
    val type: String? = null,
    val label: String? = null,
    val url: String? = null,
    val durationMs: Int? = null
)

data class TextToSignResponse(
    val inputText: String? = null,
    val normalizedText: String? = null,
    val mode: String? = null,
    val videoUrl: String? = null,
    val items: List<TextToSignItem> = emptyList(),
    val unsupportedCharacters: List<String> = emptyList()
)

interface AslTranslationApi {

    @Multipart
    @POST("api/sign-to-text")
    suspend fun signToText(
        @Part file: MultipartBody.Part
    ): Response<SignToTextResponse>

    @Multipart
    @POST("api/sign-to-text/live-frame")
    suspend fun signToTextLiveFrame(
        @Part frame: MultipartBody.Part,
        @Part("SessionId") sessionId: String
    ): Response<SignToTextResponse>

    @POST("api/text-to-sign")
    suspend fun textToSign(
        @Body request: TextToSignRequest
    ): Response<TextToSignResponse>
}

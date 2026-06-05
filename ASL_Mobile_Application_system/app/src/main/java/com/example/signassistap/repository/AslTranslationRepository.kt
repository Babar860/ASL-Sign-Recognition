package com.example.signassistap.repository

import android.content.Context
import android.net.Uri
import com.example.signassistap.network.ApiClient
import com.example.signassistap.network.SignToTextResponse
import com.example.signassistap.network.TextToSignRequest
import com.example.signassistap.network.TextToSignResponse
import com.example.signassistap.utils.FileUploadUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

class AslTranslationRepository {

    suspend fun signToText(context: Context, uri: Uri): Response<SignToTextResponse> {
        val filePart = FileUploadUtils.uriToVideoPart(context, uri)
        return ApiClient.aslTranslationApi.signToText(filePart)
    }

    suspend fun signToTextLiveFrame(
        jpegBytes: ByteArray,
        sessionId: String
    ): Response<SignToTextResponse> {
        val requestBody = jpegBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val framePart = MultipartBody.Part.createFormData(
            "Frame",
            "live-frame.jpg",
            requestBody
        )
        return ApiClient.aslTranslationApi.signToTextLiveFrame(framePart, sessionId)
    }

    suspend fun textToSign(text: String): Response<TextToSignResponse> {
        return ApiClient.aslTranslationApi.textToSign(TextToSignRequest(text.trim()))
    }
}

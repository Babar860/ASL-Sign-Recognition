package com.example.signassistap.repository

import android.content.Context
import android.net.Uri
import com.example.signassistap.network.ApiClient
import com.example.signassistap.network.UploadVideoResponse
import com.example.signassistap.utils.FileUploadUtils
import com.example.signassistap.utils.UserSession
import com.example.signassistap.network.PredictResponse
import okhttp3.MultipartBody
import retrofit2.Response

class VideoRepository {

    // ✅ upload using already-made filePart + userId + duration
    suspend fun uploadVideo(
        filePart: MultipartBody.Part,
        userId: String,
        durationSeconds: Int
    ): Response<UploadVideoResponse> {



        return ApiClient.videoApi.uploadVideo(
            file = filePart,
            userId = userId.trim(),
            durationSeconds = durationSeconds
        )
    }

    // ✅ upload using uri + session userId
    suspend fun uploadVideo(
        context: Context,
        uri: Uri,
        durationSeconds: Int
    ): Response<UploadVideoResponse> {

        val userId = UserSession.getUserId(context)
            ?: throw Exception("User not logged in")

        val filePart = FileUploadUtils.uriToVideoPart(context, uri)

        return uploadVideo(
            filePart = filePart,
            userId = userId,
            durationSeconds = durationSeconds
        )
    }

    suspend fun predictVideo(videoId: String): Response<PredictResponse> {
        return ApiClient.videoApi.predictVideo(videoId.trim())
    }
}
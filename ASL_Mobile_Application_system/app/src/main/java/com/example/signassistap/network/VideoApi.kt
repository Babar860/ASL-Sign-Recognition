package com.example.signassistap.network

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class UploadVideoResponse(
    val message: String? = null,
    val fileUrl: String? = null,
    val videoId: String? = null
)

data class PredictResponse(
    val videoId: String,
    val predicted_text: String
)

interface VideoApi {

    @Multipart
    @POST("api/Videos/upload")
    suspend fun uploadVideo(
        @Part file: MultipartBody.Part,
        @Part("UserId") userId: String,          // ✅ simple string
        @Part("DurationSeconds") durationSeconds: Int
    ): Response<UploadVideoResponse>


@POST("api/Videos/{videoId}/predict")
suspend fun predictVideo(
    @retrofit2.http.Path("videoId") videoId: String
): Response<PredictResponse>

}
package com.example.signassistap.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class CreateHistoryRequest(
    val userId: String,
    val sentence: String,
    val words: List<String>,
    val translationType: String = "Sign-to-Text"
)

data class TranslationHistoryResponse(
    @SerializedName(value = "historyId", alternate = ["history_id"])
    val historyId: String? = null,
    val userId: String? = null,
    val sentence: String? = null,
    val words: List<String> = emptyList(),
    val translationType: String? = null,
    val createdAt: String? = null
)

data class AdminTranslationHistoryResponse(
    @SerializedName(value = "history_id", alternate = ["historyId"])
    val historyId: String? = null,
    @SerializedName(value = "user_id", alternate = ["userId"])
    val userId: String? = null,
    @SerializedName(value = "full_name", alternate = ["fullName"])
    val fullName: String? = null,
    val email: String? = null,
    val sentence: String? = null,
    val words: List<String> = emptyList(),
    @SerializedName(value = "translation_type", alternate = ["translationType"])
    val translationType: String? = null,
    @SerializedName(value = "created_at", alternate = ["createdAt"])
    val createdAt: String? = null
)

data class AddFeedbackRequest(
    val userId: String,
    val feedback: String
)

data class FeedbackResponse(
    val feedbackId: String? = null,
    val userId: String? = null,
    val feedbacks: List<String> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class AdminFeedbackResponse(
    @SerializedName(value = "feedback_id", alternate = ["feedbackId"])
    val feedbackId: String? = null,
    @SerializedName(value = "user_id", alternate = ["userId"])
    val userId: String? = null,
    @SerializedName(value = "full_name", alternate = ["fullName"])
    val fullName: String? = null,
    val email: String? = null,
    val feedbacks: List<String> = emptyList(),
    @SerializedName(value = "created_at", alternate = ["createdAt"])
    val createdAt: String? = null,
    @SerializedName(value = "updated_at", alternate = ["updatedAt"])
    val updatedAt: String? = null
)

data class AdminUserResponse(
    @SerializedName(value = "user_id", alternate = ["userId"])
    val userId: String? = null,
    @SerializedName(value = "full_name", alternate = ["fullName"])
    val fullName: String? = null,
    val email: String? = null
)

data class LearningSignResponse(
    val letter: String,
    val label: String? = null,
    val imageUrl: String? = null
)

interface SignAssistApi {
    @GET("api/Auth/users")
    suspend fun getUsers(): Response<List<AdminUserResponse>>

    @POST("api/TranslationHistory")
    suspend fun saveHistory(@Body request: CreateHistoryRequest): Response<TranslationHistoryResponse>

    @GET("api/TranslationHistory/user/{userId}")
    suspend fun getHistory(@Path("userId") userId: String): Response<List<TranslationHistoryResponse>>

    @GET("api/TranslationHistory/admin")
    suspend fun getAdminHistory(): Response<List<AdminTranslationHistoryResponse>>

    @GET("api/TranslationHistory/admin")
    suspend fun getAdminTextToSignHistory(
        @Query("type") type: String = "Text-to-Sign"
    ): Response<List<AdminTranslationHistoryResponse>>

    @POST("api/Feedback")
    suspend fun addFeedback(@Body request: AddFeedbackRequest): Response<FeedbackResponse>

    @GET("api/Feedback/user/{userId}")
    suspend fun getFeedback(@Path("userId") userId: String): Response<FeedbackResponse>

    @GET("api/Feedback/admin")
    suspend fun getAdminFeedback(): Response<List<AdminFeedbackResponse>>

    @GET("api/Learning/alphabet")
    suspend fun getAlphabetSigns(): Response<List<LearningSignResponse>>

    @GET("api/Learning/alphabet/{letter}")
    suspend fun getAlphabetSign(@Path("letter") letter: String): Response<LearningSignResponse>
}

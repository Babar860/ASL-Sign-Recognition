package com.example.signassistap.network

import com.example.signassistap.models.LoginRequest
import com.example.signassistap.models.LoginResponse
import com.example.signassistap.models.SignupRequest
import com.example.signassistap.models.SignupResponse
import com.example.signassistap.models.EditProfileRequest
import com.example.signassistap.models.SimpleMessageResponse
import com.example.signassistap.models.ResetPasswordRequest
import com.example.signassistap.models.RequestPasswordResetRequest
import com.example.signassistap.models.ConfirmPasswordResetRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.Path

interface AuthApi {

    @POST("api/Auth/signup")
    suspend fun signup(@Body request: SignupRequest): Response<SignupResponse>

    @POST("api/Auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @PUT("api/Auth/edit-profile/{id}")
    suspend fun editProfile(
        @Path("id") id: String,
        @Body body: EditProfileRequest
    ): Response<SimpleMessageResponse>

    @DELETE("api/Auth/delete-profile/{id}")
    suspend fun deleteProfile(
        @Path("id") id: String
    ): Response<SimpleMessageResponse>

    // ✅ Old security-answer reset (keeping as-is)
    @POST("api/Auth/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): Response<SimpleMessageResponse>

    // ✅ NEW: Email token reset flow (API-1)
    @POST("api/Auth/request-password-reset")
    suspend fun requestPasswordReset(
        @Body request: RequestPasswordResetRequest
    ): Response<SimpleMessageResponse>

    // ✅ NEW: Email token reset flow (API-2)
    @POST("api/Auth/confirm-password-reset")
    suspend fun confirmPasswordReset(
        @Body request: ConfirmPasswordResetRequest
    ): Response<SimpleMessageResponse>
}
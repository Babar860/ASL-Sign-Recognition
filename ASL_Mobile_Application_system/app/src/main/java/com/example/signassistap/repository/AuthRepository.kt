package com.example.signassistap.repository

import android.content.Context
import android.util.Log
import com.example.signassistap.models.LoginRequest
import com.example.signassistap.models.LoginResponse
import com.example.signassistap.models.SignupRequest
import com.example.signassistap.models.SignupResponse
import com.example.signassistap.network.ApiClient
import com.example.signassistap.utils.UserSession
import com.example.signassistap.models.EditProfileRequest
import com.example.signassistap.models.SimpleMessageResponse
import retrofit2.Response

class AuthRepository {

    private val api = ApiClient.authApi

    // ---------------- SIGNUP ----------------
    suspend fun signup(request: SignupRequest): Response<SignupResponse> {
        Log.d("API_SIGNUP", "Sending signup request: $request")
        return api.signup(request)
    }

    suspend fun signupAndSaveUserId(
        context: Context,
        request: SignupRequest
    ): Response<SignupResponse> {

        val response = signup(request)

        if (response.isSuccessful) {
            val userId = response.body()?.userId
            if (!userId.isNullOrBlank()) {
                UserSession.saveUserId(context, userId)
                Log.d("USER_SESSION", "✅ Saved userId (signup): $userId")
            }
        }

        return response
    }

    suspend fun editProfile(
        context: Context,
        fullName: String,
        email: String
    ): Response<SimpleMessageResponse> {

        val userId = UserSession.getUserId(context)
            ?: throw Exception("User not logged in")

        Log.d("EDIT_PROFILE", "Editing profile for userId: $userId")

        return api.editProfile(
            userId,
            EditProfileRequest(
                full_name = fullName,
                email = email
            )
        )
    }

    // ---------------- DELETE PROFILE ----------------
    suspend fun deleteProfile(context: Context): Response<SimpleMessageResponse> {

        val userId = UserSession.getUserId(context)
            ?: throw Exception("User not logged in")

        Log.d("DELETE_PROFILE", "Deleting profile for userId: $userId")

        return api.deleteProfile(userId)
    }
    // ---------------- LOGIN ----------------
    suspend fun loginAndSaveUserId(
        context: Context,
        request: LoginRequest
    ): Response<LoginResponse> {

        Log.d("API_LOGIN", "Sending login request: $request")

        val response = api.login(request)

        Log.d("API_LOGIN", "Response code: ${response.code()}")
        Log.d("API_LOGIN", "Response body: ${response.body()}")
        Log.d("API_LOGIN", "Error body: ${response.errorBody()?.string()}")

        if (response.isSuccessful) {
            val userId = response.body()?.userId
            if (!userId.isNullOrBlank()) {
                UserSession.saveUserId(context, userId)
                Log.d("USER_SESSION", "✅ Saved userId (login): $userId")
            }
        }

        return response
    }
}

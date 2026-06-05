package com.example.signassistap.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.signassistap.models.LoginRequest
import com.example.signassistap.models.SignupRequest
import com.example.signassistap.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log
import com.example.signassistap.utils.UserSession

// 🔹 LOGIN STATE
data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

// 🔹 SIGNUP STATE
data class SignupUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class AuthViewModel : ViewModel() {

    private val repo = AuthRepository()

    // 🔹 Login state
    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState

    // 🔹 Signup state
    private val _signupUiState = MutableStateFlow(SignupUiState())
    val signupUiState: StateFlow<SignupUiState> = _signupUiState

    // ================= EDIT PROFILE =================
    fun editProfile(
        context: Context,
        fullName: String,
        email: String
    ) {
        viewModelScope.launch {
            try {
                val response = repo.editProfile(context, fullName, email)

                if (response.isSuccessful) {
                    Log.d("EDIT_PROFILE", "✅ Profile updated: ${response.body()?.message}")
                } else {
                    Log.e("EDIT_PROFILE", "❌ Failed: ${response.code()}")
                }

            } catch (e: Exception) {
                Log.e("EDIT_PROFILE", "❌ Error: ${e.message}")
            }
        }
    }

    // ================= DELETE PROFILE =================
    fun deleteProfile(context: Context) {
        viewModelScope.launch {
            try {
                val response = repo.deleteProfile(context)

                if (response.isSuccessful) {
                    UserSession.clear(context)
                    Log.d("DELETE_PROFILE", "✅ Profile deleted & session cleared")
                } else {
                    Log.e("DELETE_PROFILE", "❌ Failed")
                }

            } catch (e: Exception) {
                Log.e("DELETE_PROFILE", "❌ Error: ${e.message}")
            }
        }
    }


    // ================= LOGIN =================
    fun login(
        context: Context,
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            _loginUiState.value = LoginUiState(isLoading = true)

            try {
                val request = LoginRequest(
                    email = email,
                    password = password
                )

                val response = repo.loginAndSaveUserId(context, request)

                if (response.isSuccessful) {
                    Log.d(
                        "SESSION",
                        "Login success, stored UserId = ${UserSession.getUserId(context)}"
                    )
                    _loginUiState.value = LoginUiState(isSuccess = true)
                } else {
                    _loginUiState.value = LoginUiState(
                        error = "Invalid email or password"
                    )
                }
            } catch (e: Exception) {
                _loginUiState.value = LoginUiState(
                    error = e.message ?: "Login failed"
                )
            }
        }
    }

    fun clearLoginState() {
        _loginUiState.value = LoginUiState()
    }

    // ================= SIGNUP =================
    fun signup(
        context: Context,
        fullName: String,
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            _signupUiState.value = SignupUiState(isLoading = true)

            try {
                val request = SignupRequest(
                    fullName = fullName,
                    email = email,
                    passwordHash = password
                )

                val response = repo.signupAndSaveUserId(context, request)

                if (response.isSuccessful) {
                    _signupUiState.value = SignupUiState(isSuccess = true)
                } else {
                    _signupUiState.value = SignupUiState(
                        error = "Signup failed"
                    )
                }
            } catch (e: Exception) {
                _signupUiState.value = SignupUiState(
                    error = e.message ?: "Signup error"
                )
            }
        }
    }

    fun clearSignupState() {
        _signupUiState.value = SignupUiState()
    }
}

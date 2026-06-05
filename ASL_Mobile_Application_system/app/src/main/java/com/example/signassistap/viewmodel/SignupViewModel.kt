package com.example.signassistap.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.signassistap.models.SignupRequest
import com.example.signassistap.network.ApiClient
import kotlinx.coroutines.launch

class SignupViewModel : ViewModel() {

    fun signup(
        fullName: String,
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            try {
                val request = SignupRequest(
                    fullName = fullName,
                    email = email,
                    passwordHash = password
                )

                val response = ApiClient.authApi.signup(request)

                if (response.isSuccessful) {
                    println("Signup Success")
                } else {
                    println("Signup Failed: ${response.errorBody()?.string()}")
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
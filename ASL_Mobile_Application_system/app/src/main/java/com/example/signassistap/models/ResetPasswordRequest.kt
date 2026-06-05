package com.example.signassistap.models

data class ResetPasswordRequest(
    val token: String,
    val newPassword: String
)

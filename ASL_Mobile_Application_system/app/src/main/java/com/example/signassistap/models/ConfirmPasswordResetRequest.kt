package com.example.signassistap.models

data class ConfirmPasswordResetRequest(
    val token: String,
    val newPassword: String,
    val confirmPassword: String
)
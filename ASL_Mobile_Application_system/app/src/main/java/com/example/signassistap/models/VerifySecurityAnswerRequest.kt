package com.example.signassistap.models

data class VerifySecurityAnswerRequest(
    val email: String,
    val answer: String
)
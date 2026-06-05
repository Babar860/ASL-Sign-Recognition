package com.example.signassistap.models

import com.google.gson.annotations.SerializedName

data class SignupRequest(
    @SerializedName("full_name")
    val fullName: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("password_hash")
    val passwordHash: String
)
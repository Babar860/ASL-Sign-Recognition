package com.example.signassistap.models

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName(value = "user_id", alternate = ["userId"])
    val userId: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName(value = "full_name", alternate = ["fullName"])
    val fullName: String? = null,

    @SerializedName("email")
    val email: String? = null
)
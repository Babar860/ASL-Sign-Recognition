package com.example.signassistap.models

data class VideoUploadResponse(
    val videoId: String?,
    val userId: String?,
    val fileUrl: String?,
    val durationSeconds: Int?
)
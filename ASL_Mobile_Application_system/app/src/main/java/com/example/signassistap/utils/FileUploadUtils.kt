package com.example.signassistap.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

object FileUploadUtils {

    fun uriToVideoPart(context: Context, uri: Uri): MultipartBody.Part {

        val contentResolver = context.contentResolver

        val fileName = getFileName(context, uri) ?: "upload.mp4"

        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Cannot open selected file")

        val requestBody = bytes.toRequestBody("video/mp4".toMediaTypeOrNull())

        // ✅ MUST match backend property name: IFormFile File
        val fieldName = "File" // Capital F

        return MultipartBody.Part.createFormData(
            fieldName,
            fileName,
            requestBody
        )
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
        cursor.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && nameIndex != -1) return it.getString(nameIndex)
        }
        return null
    }
}
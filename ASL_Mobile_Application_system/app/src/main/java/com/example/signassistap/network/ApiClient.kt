package com.example.signassistap.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.signassistap.network.VideoApi

object ApiClient {

    // ✅ BASE URL (SignLanguage API – HTTP only)
    // Agar REAL mobile use kar rahe ho → PC ka LAN IP
    // Agar Emulator use karna ho → http://10.0.2.2:5140/
    private const val BASE_URL = "http://192.168.0.33:5140/"
    private const val ASL_BASE_URL = "http://192.168.0.33:8000/"


    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .build()


    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val aslRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(ASL_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // ✅ Auth API (login, signup, forget, reset — all in ONE API now)
    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    // ✅ Video API
    val videoApi: VideoApi by lazy {
        retrofit.create(VideoApi::class.java)
    }

    val aslTranslationApi: AslTranslationApi by lazy {
        aslRetrofit.create(AslTranslationApi::class.java)
    }

    val signAssistApi: SignAssistApi by lazy {
        retrofit.create(SignAssistApi::class.java)
    }

    fun resolveAslUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        return ASL_BASE_URL.trimEnd('/') + "/" + url.trimStart('/')
    }
}

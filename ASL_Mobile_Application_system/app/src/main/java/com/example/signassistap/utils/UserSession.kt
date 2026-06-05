package com.example.signassistap.utils

import android.content.Context
import android.content.SharedPreferences

object UserSession {

    private const val PREF_NAME = "user_session"
    private const val KEY_USER_ID = "user_id"

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // ✅ Save UserId after Signup
    fun saveUserId(context: Context, userId: String) {
        prefs(context).edit()
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    // ✅ Get UserId anywhere in app
    fun getUserId(context: Context): String? {
        return prefs(context).getString(KEY_USER_ID, null)
    }

    // ✅ Clear session (future logout use)
    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
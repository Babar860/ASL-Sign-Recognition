package com.example.signassistap.finalmodule

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import com.example.signassistap.ui.theme.SignAssistAppTheme

/**
 * Hosts all features imported from FinalSignAssist without modifying the legacy MainActivity.
 */
class FinalSignAssistActivity : ComponentActivity() {

    private val feedbackList = mutableStateListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SignAssistAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FinalAppNavigation(feedbackList)
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, FinalSignAssistActivity::class.java)
    }
}

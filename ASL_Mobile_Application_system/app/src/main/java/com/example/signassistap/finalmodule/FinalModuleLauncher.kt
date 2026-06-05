package com.example.signassistap.finalmodule

import android.content.Context
import android.content.Intent

/**
 * Use from any screen to open FinalSignAssist features without editing legacy files:
 *
 *   context.startActivity(FinalModuleLauncher.intent(context))
 */
object FinalModuleLauncher {
    fun intent(context: Context): Intent = FinalSignAssistActivity.createIntent(context)
}

package com.example.signassistap.finalmodule.utils

object FinalLetterUtils {

    private const val SIGNS_DIR = "signs"

    fun textToLetters(text: String): List<String> {
        return text
            .lowercase()
            .filter { it.isLetter() }
            .map { ch -> "$SIGNS_DIR/$ch.gif" }
    }

    fun wordGifPath(text: String): String {
        val word = text.lowercase().trim().replace(Regex("\\s+"), " ")
        return "$SIGNS_DIR/$word.gif"
    }
}

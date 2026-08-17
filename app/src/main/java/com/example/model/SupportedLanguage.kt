package com.example.model

import java.util.Locale

/**
 * Supported Indian languages plus English.
 */
enum class SupportedLanguage(
    val code: String,
    val languageName: String,
    val locale: Locale
) {
    ENGLISH("en-IN", "English", Locale("en", "IN")),
    BENGALI("bn-IN", "Bengali", Locale("bn", "IN")),
    HINDI("hi-IN", "Hindi", Locale("hi", "IN")),
    ASSAMESE("as-IN", "Assamese", Locale("as", "IN")),
    GUJARATI("gu-IN", "Gujarati", Locale("gu", "IN")),
    KANNADA("kn-IN", "Kannada", Locale("kn", "IN")),
    MALAYALAM("ml-IN", "Malayalam", Locale("ml", "IN")),
    MARATHI("mr-IN", "Marathi", Locale("mr", "IN")),
    ODIA("or-IN", "Odia", Locale("or", "IN")),
    PUNJABI("pa-IN", "Punjabi", Locale("pa", "IN")),
    TAMIL("ta-IN", "Tamil", Locale("ta", "IN")),
    TELUGU("te-IN", "Telugu", Locale("te", "IN")),
    URDU("ur-IN", "Urdu", Locale("ur", "IN"));

    companion object {
        fun fromCode(code: String): SupportedLanguage {
            val clean = code.lowercase(Locale.ROOT)
            return values().firstOrNull {
                clean.startsWith(it.code.lowercase(Locale.ROOT)) || clean.startsWith(it.locale.language.lowercase(Locale.ROOT))
            } ?: ENGLISH
        }
    }
}

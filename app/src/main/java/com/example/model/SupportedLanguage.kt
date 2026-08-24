package com.example.model

import java.util.Locale

/**
 * Supported Indian languages plus English.
 */
enum class SupportedLanguage(
    val code: String,
    val languageName: String,
    val nativeName: String,
    val locale: Locale
) {
    BENGALI("bn-IN", "Bengali", "বাংলা", Locale("bn", "IN")),
    HINDI("hi-IN", "Hindi", "हिंदी", Locale("hi", "IN")),
    ENGLISH("en-IN", "English (India)", "English", Locale("en", "IN")),
    TELUGU("te-IN", "Telugu", "తెలుగు", Locale("te", "IN")),
    MARATHI("mr-IN", "Marathi", "मराठी", Locale("mr", "IN")),
    TAMIL("ta-IN", "Tamil", "தமிழ்", Locale("ta", "IN")),
    GUJARATI("gu-IN", "Gujarati", "ગુજરાતી", Locale("gu", "IN")),
    KANNADA("kn-IN", "Kannada", "ಕನ್ನಡ", Locale("kn", "IN")),
    MALAYALAM("ml-IN", "Malayalam", "മലയാളം", Locale("ml", "IN")),
    PUNJABI("pa-IN", "Punjabi", "ਪੰਜਾਬੀ", Locale("pa", "IN")),
    ODIA("or-IN", "Odia", "ଓଡ଼ିଆ", Locale("or", "IN")),
    ASSAMESE("as-IN", "Assamese", "অসমীয়া", Locale("as", "IN")),
    URDU("ur-IN", "Urdu", "اردو", Locale("ur", "IN"));

    val displayName: String
        get() = if (nativeName.isNotEmpty() && nativeName != languageName) "$languageName ($nativeName)" else languageName

    companion object {
        val ALL_12_INDIAN_LANGUAGES: List<SupportedLanguage> = listOf(
            BENGALI,
            HINDI,
            ENGLISH,
            TELUGU,
            MARATHI,
            TAMIL,
            GUJARATI,
            KANNADA,
            MALAYALAM,
            PUNJABI,
            ODIA,
            ASSAMESE
        )

        fun fromCode(code: String): SupportedLanguage {
            val clean = code.lowercase(Locale.ROOT)
            return values().firstOrNull {
                clean.startsWith(it.code.lowercase(Locale.ROOT)) || clean.startsWith(it.locale.language.lowercase(Locale.ROOT))
            } ?: ENGLISH
        }
    }
}

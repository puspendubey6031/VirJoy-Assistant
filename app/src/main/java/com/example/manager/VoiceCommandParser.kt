package com.example.manager

import com.example.model.SupportedLanguage

sealed class ParsedVoiceCommand {
    abstract val detectedLanguage: SupportedLanguage

    data class CallContact(
        val targetName: String,
        override val detectedLanguage: SupportedLanguage
    ) : ParsedVoiceCommand()

    data class Unknown(
        val rawText: String,
        override val detectedLanguage: SupportedLanguage
    ) : ParsedVoiceCommand()
}

object VoiceCommandParser {

    /**
     * Parses recognized speech text into structured command and identifies user's language.
     */
    fun parse(rawText: String): ParsedVoiceCommand {
        val trimmed = rawText.trim()
        val detectedLanguage = LanguageManager.detectLanguage(trimmed)

        if (trimmed.isEmpty()) {
            return ParsedVoiceCommand.Unknown(rawText, detectedLanguage)
        }

        // 1. English patterns
        val englishPatterns = listOf(
            Regex("(?i)^(?:please\\s+)?(?:make\\s+a\\s+)?(?:call|phone|dial|ring)\\s+(?:to\\s+)?(.+?)(?:\\s+please)?$"),
            Regex("(?i)^(.+?)\\s+please\\s+call$"),
            Regex("(?i)^call\\s+(.+)$"),
            Regex("(?i)^phone\\s+(.+)$"),
            Regex("(?i)^dial\\s+(.+)$")
        )

        for (pattern in englishPatterns) {
            val match = pattern.matchEntire(trimmed)
            if (match != null) {
                val target = match.groupValues[1].trim()
                val cleaned = BengaliHindiEnglishMatcher.cleanContactQuery(target)
                if (cleaned.isNotEmpty()) {
                    return ParsedVoiceCommand.CallContact(cleaned, detectedLanguage)
                }
            }
        }

        // 2. Bengali patterns (e.g. "রাহুলকে ফোন করো", "রাহুলকে কল কর", "রাহুলকে কল দাও", "রাহুলকে ফোন দাও", "কল করো রাহুলকে")
        val bengaliPatterns = listOf(
            Regex("^(.+?)\\s+(?:কল|ফোন|ডায়াল)\\s*(?:করো|কর|করুন|লাগাও|দাও)$"),
            Regex("^(?:কল|ফোন|ডায়াল)\\s*(?:করো|কর|করুন|লাগাও|দাও)\\s+(.+)$"),
            Regex("^(.+?)\\s*(?:কে|ের)\\s*(?:কল|ফোন)$"),
            Regex("^(.+?)\\s*(?:কল|ফোন)$")
        )

        for (pattern in bengaliPatterns) {
            val match = pattern.matchEntire(trimmed)
            if (match != null) {
                val target = match.groupValues[1].trim()
                val cleaned = BengaliHindiEnglishMatcher.cleanContactQuery(target)
                if (cleaned.isNotEmpty()) {
                    return ParsedVoiceCommand.CallContact(cleaned, detectedLanguage)
                }
            }
        }

        // 3. Hindi patterns (e.g. "राहुल को फोन करो", "राहुल को कॉल करो", "राहुल को फोन लगाओ", "कॉल करो राहुल को")
        val hindiPatterns = listOf(
            Regex("^(.+?)\\s+(?:कॉल|फोन|डायल)\\s*(?:करो|करें|कीजिए|लगाओ|मिलाओ|कर)$"),
            Regex("^(?:कॉल|फोन|डायल)\\s*(?:करो|करें|कीजिए|लगाओ|मिलाओ|कर)\\s+(.+)$"),
            Regex("^(.+?)\\s*(?:को)\\s*(?:कॉल|फोन)$"),
            Regex("^(.+?)\\s*(?:कॉल|फोन)$")
        )

        for (pattern in hindiPatterns) {
            val match = pattern.matchEntire(trimmed)
            if (match != null) {
                val target = match.groupValues[1].trim()
                val cleaned = BengaliHindiEnglishMatcher.cleanContactQuery(target)
                if (cleaned.isNotEmpty()) {
                    return ParsedVoiceCommand.CallContact(cleaned, detectedLanguage)
                }
            }
        }

        // 4. Assamese patterns (e.g. "ৰাহুললৈ কল কৰক", "ৰাহুলক ফোন কৰা")
        val assamesePatterns = listOf(
            Regex("^(.+?)\\s+(?:কল|ফোন)\\s*(?:কৰক|কৰা)$"),
            Regex("^(?:কল|ফোন)\\s*(?:কৰক|কৰা)\\s+(.+)$")
        )
        for (pattern in assamesePatterns) {
            val match = pattern.matchEntire(trimmed)
            if (match != null) {
                val target = match.groupValues[1].trim()
                val cleaned = BengaliHindiEnglishMatcher.cleanContactQuery(target)
                if (cleaned.isNotEmpty()) {
                    return ParsedVoiceCommand.CallContact(cleaned, detectedLanguage)
                }
            }
        }

        // 5. Gujarati patterns (e.g. "રાહુલને કોલ કરો", "રાહુલને ફોન લગાવો")
        val gujaratiPatterns = listOf(
            Regex("^(.+?)\\s+(?:કોલ|ફોન)\\s*(?:કરો|લગાવો)$"),
            Regex("^(?:કોલ|ફોન)\\s*(?:કરો|લગાવો)\\s+(.+)$")
        )
        for (pattern in gujaratiPatterns) {
            val match = pattern.matchEntire(trimmed)
            if (match != null) {
                val target = match.groupValues[1].trim()
                val cleaned = BengaliHindiEnglishMatcher.cleanContactQuery(target)
                if (cleaned.isNotEmpty()) {
                    return ParsedVoiceCommand.CallContact(cleaned, detectedLanguage)
                }
            }
        }

        // 6. Marathi patterns (e.g. "राहुलला कॉल करा", "राहुलला फोन लावा")
        val marathiPatterns = listOf(
            Regex("^(.+?)\\s+(?:कॉल|फोन)\\s*(?:करा|लावा)$"),
            Regex("^(?:कॉल|फोन)\\s*(?:करा|लावा)\\s+(.+)$")
        )
        for (pattern in marathiPatterns) {
            val match = pattern.matchEntire(trimmed)
            if (match != null) {
                val target = match.groupValues[1].trim()
                val cleaned = BengaliHindiEnglishMatcher.cleanContactQuery(target)
                if (cleaned.isNotEmpty()) {
                    return ParsedVoiceCommand.CallContact(cleaned, detectedLanguage)
                }
            }
        }

        // 7. Punjabi patterns (e.g. "ਰਾਹੁਲ ਨੂੰ ਕਾਲ ਕਰੋ", "ਰਾਹੁਲ ਨੂੰ ਫ਼ੋਨ ਲਗਾਓ")
        val punjabiPatterns = listOf(
            Regex("^(.+?)\\s+(?:ਕਾਲ|ਫ਼ੋਨ)\\s*(?:ਕਰੋ|ਲਗਾਓ)$"),
            Regex("^(?:ਕਾਲ|ਫ਼ੋਨ)\\s*(?:ਕਰੋ|ਲਗਾਓ)\\s+(.+)$")
        )
        for (pattern in punjabiPatterns) {
            val match = pattern.matchEntire(trimmed)
            if (match != null) {
                val target = match.groupValues[1].trim()
                val cleaned = BengaliHindiEnglishMatcher.cleanContactQuery(target)
                if (cleaned.isNotEmpty()) {
                    return ParsedVoiceCommand.CallContact(cleaned, detectedLanguage)
                }
            }
        }

        // 8. Odia patterns (e.g. "ରାହୁଲଙ୍କୁ କଲ୍ କରନ୍ତୁ", "ରାହୁଲଙ୍କୁ ଫୋନ୍ କର")
        val odiaPatterns = listOf(
            Regex("^(.+?)\\s+(?:କଲ୍|ଫୋନ୍)\\s*(?:କରନ୍ତୁ|କର)$"),
            Regex("^(?:କଲ୍|ଫୋନ୍)\\s*(?:କରନ୍ତୁ|କର)\\s+(.+)$")
        )
        for (pattern in odiaPatterns) {
            val match = pattern.matchEntire(trimmed)
            if (match != null) {
                val target = match.groupValues[1].trim()
                val cleaned = BengaliHindiEnglishMatcher.cleanContactQuery(target)
                if (cleaned.isNotEmpty()) {
                    return ParsedVoiceCommand.CallContact(cleaned, detectedLanguage)
                }
            }
        }

        // 9. Tamil patterns (e.g. "ராகுலுக்கு போன் செய்", "ராகுலுக்கு கால் பண்ணு")
        val tamilPatterns = listOf(
            Regex("^(.+?)\\s+(?:போன்|கால்)\\s*(?:செய்|பண்ணு|பண்ணுங்க)$"),
            Regex("^(?:போன்|கால்)\\s*(?:செய்|பண்ணு|பண்ணுங்க)\\s+(.+)$")
        )
        for (pattern in tamilPatterns) {
            val match = pattern.matchEntire(trimmed)
            if (match != null) {
                val target = match.groupValues[1].trim()
                val cleaned = BengaliHindiEnglishMatcher.cleanContactQuery(target)
                if (cleaned.isNotEmpty()) {
                    return ParsedVoiceCommand.CallContact(cleaned, detectedLanguage)
                }
            }
        }

        // 10. Telugu patterns (e.g. "రాహుల్ కి కాల్ చేయి", "రాహుల్ కి ఫోన్ చెయ్యి")
        val teluguPatterns = listOf(
            Regex("^(.+?)\\s+(?:కాల్|ఫోన్)\\s*(?:చేయి|చెయ్యి|చేయండి)$"),
            Regex("^(?:కాల్|ఫోన్)\\s*(?:చేయి|చెయ్యి|చేయండి)\\s+(.+)$")
        )
        for (pattern in teluguPatterns) {
            val match = pattern.matchEntire(trimmed)
            if (match != null) {
                val target = match.groupValues[1].trim()
                val cleaned = BengaliHindiEnglishMatcher.cleanContactQuery(target)
                if (cleaned.isNotEmpty()) {
                    return ParsedVoiceCommand.CallContact(cleaned, detectedLanguage)
                }
            }
        }

        // 11. Kannada patterns (e.g. "ರಾಹುಲ್ ಅವರಿಗೆ ಕರೆ ಮಾಡಿ", "ರಾಹುಲ್ ಗೆ ಕಾಲ್ ಮಾಡಿ")
        val kannadaPatterns = listOf(
            Regex("^(.+?)\\s+(?:ಕರೆ|ಕಾಲ್|ಫೋನ್)\\s*(?:ಮಾಡಿ|ಮಾಡು)$"),
            Regex("^(?:ಕರೆ|ಕಾಲ್|ಫೋನ್)\\s*(?:ಮಾಡಿ|ಮಾಡು)\\s+(.+)$")
        )
        for (pattern in kannadaPatterns) {
            val match = pattern.matchEntire(trimmed)
            if (match != null) {
                val target = match.groupValues[1].trim()
                val cleaned = BengaliHindiEnglishMatcher.cleanContactQuery(target)
                if (cleaned.isNotEmpty()) {
                    return ParsedVoiceCommand.CallContact(cleaned, detectedLanguage)
                }
            }
        }

        // 12. Malayalam patterns (e.g. "രാഹുലിനെ വിളിക്കൂ", "രാഹുലിന് കോൾ ചെയ്യുക")
        val malayalamPatterns = listOf(
            Regex("^(.+?)\\s+(?:വിളിക്കൂ|കോൾ ചെയ്യുക|ഫോൺ ചെയ്യുക)$"),
            Regex("^(?:വിളിക്കൂ|കോൾ ചെയ്യുക|ഫോൺ ചെയ്യുക)\\s+(.+)$")
        )
        for (pattern in malayalamPatterns) {
            val match = pattern.matchEntire(trimmed)
            if (match != null) {
                val target = match.groupValues[1].trim()
                val cleaned = BengaliHindiEnglishMatcher.cleanContactQuery(target)
                if (cleaned.isNotEmpty()) {
                    return ParsedVoiceCommand.CallContact(cleaned, detectedLanguage)
                }
            }
        }

        // 13. Urdu patterns (e.g. "راہل کو کال کرو", "راہل کو فون کرو")
        val urduPatterns = listOf(
            Regex("^(.+?)\\s+(?:کال|فون)\\s*(?:کرو|کریں|لگائیں)$"),
            Regex("^(?:کال|فون)\\s*(?:کرو|کریں|لگائیں)\\s+(.+)$")
        )
        for (pattern in urduPatterns) {
            val match = pattern.matchEntire(trimmed)
            if (match != null) {
                val target = match.groupValues[1].trim()
                val cleaned = BengaliHindiEnglishMatcher.cleanContactQuery(target)
                if (cleaned.isNotEmpty()) {
                    return ParsedVoiceCommand.CallContact(cleaned, detectedLanguage)
                }
            }
        }

        // 14. Direct Contact Query: If the input is just a contact name directly
        val cleanedDirectName = BengaliHindiEnglishMatcher.cleanContactQuery(trimmed)
        if (cleanedDirectName.isNotEmpty() && cleanedDirectName.length >= 2) {
            return ParsedVoiceCommand.CallContact(cleanedDirectName, detectedLanguage)
        }

        return ParsedVoiceCommand.Unknown(rawText, detectedLanguage)
    }
}

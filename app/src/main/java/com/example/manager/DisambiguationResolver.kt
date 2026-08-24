package com.example.manager

import com.example.model.PhoneNumberOption
import com.example.model.SupportedLanguage
import java.util.Locale

object DisambiguationResolver {

    private val OPTION_1_REGEX = Regex("""(^|\D)(1|১|one|ek|প্রথম|প্রথমটি|প্রথমটা|पहला|first|1st|option 1|number 1|opt 1|এক নম্বর|১ নম্বর|1 নম্বর|এক নাম্বার|১ নাম্বার|1 নাম্বার|एक नंबर)(\D|$)""", RegexOption.IGNORE_CASE)
    private val OPTION_2_REGEX = Regex("""(^|\D)(2|২|two|do|দ্বিতীয়|দ্বিতীয়|দ্বিতীয়টি|দ্বিতীয়টি|दूसरा|second|2nd|option 2|number 2|opt 2|দুই নম্বর|২ নম্বর|2 নম্বর|দুই নাম্বার|২ নাম্বার|2 নাম্বার|दो नंबर)(\D|$)""", RegexOption.IGNORE_CASE)
    private val OPTION_3_REGEX = Regex("""(^|\D)(3|৩|three|teen|তৃতীয়|তৃতীয়|তৃতীয়টি|तीसरा|third|3rd|option 3|number 3|opt 3|তিন নম্বর|৩ নম্বর|3 নম্বর|তিন নাম্বার|৩ নাম্বার|3 নাম্বার|तीन नंबर)(\D|$)""", RegexOption.IGNORE_CASE)
    private val OPTION_4_REGEX = Regex("""(^|\D)(4|৪|four|chaar|চতুর্থ|चौथा|fourth|4th|option 4|number 4|opt 4|চার নম্বর|৪ নম্বর|4 নম্বর|চার নাম্বার|৪ নাম্বার|4 নাম্বার|चार नंबर)(\D|$)""", RegexOption.IGNORE_CASE)

    /**
     * Resolves the user's spoken answer to one of the provided phone number options.
     * Supports:
     * - Option index words (e.g., "এক নম্বর", "১", "पहলা", "first", "one", "দুই", "second", etc.)
     * - Last 4 digits in English or Indic digits (e.g., "1234", "১২৩৪", "१२३४")
     * - Label matches (e.g., "mobile", "মোবাইল", "office", "অফিস", "home", "বাড়ি", "work", etc.)
     * - Contact name matches
     */
    fun resolveOption(
        spokenText: String,
        options: List<PhoneNumberOption>,
        language: SupportedLanguage = SupportedLanguage.BENGALI
    ): PhoneNumberOption? {
        if (options.isEmpty()) return null
        val cleanSpoken = spokenText.trim().lowercase(Locale.ROOT)
        if (cleanSpoken.isEmpty()) return null

        // 1. Check last 4 digits match first (converting Indic digits to ASCII)
        val normalizedSpokenDigits = normalizeDigits(cleanSpoken)
        if (normalizedSpokenDigits.length >= 3) {
            for (opt in options) {
                val optDigits = normalizeDigits(opt.lastFourDigits)
                if (optDigits.length >= 2 && normalizedSpokenDigits.contains(optDigits)) {
                    return opt
                }
            }
        }

        // 2. Direct index check from number words with boundaries
        val optionIndex = extractOptionIndex(cleanSpoken)
        if (optionIndex != null) {
            val matchedByIndex = options.firstOrNull { it.optionIndex == optionIndex }
            if (matchedByIndex != null) return matchedByIndex
        }

        // 3. Check label matches
        for (opt in options) {
            val labelLower = opt.label.lowercase(Locale.ROOT)
            if (labelLower.isNotEmpty() && matchesLabel(cleanSpoken, labelLower)) {
                return opt
            }
        }

        // 4. Check contact name matches (for multi-contact disambiguation)
        for (opt in options) {
            val nameLower = opt.contactName.lowercase(Locale.ROOT)
            val score = BengaliHindiEnglishMatcher.computeMatchScore(cleanSpoken, nameLower)
            if (score >= 0.65) {
                return opt
            }
        }

        // 5. Fallback check for shorter digits
        if (normalizedSpokenDigits.isNotEmpty()) {
            for (opt in options) {
                val optDigits = normalizeDigits(opt.lastFourDigits)
                if (optDigits.length >= 2 && normalizedSpokenDigits.contains(optDigits)) {
                    return opt
                }
            }
        }

        // Fallback: If only 2 options and user said "first" / "last"
        if (options.size == 2) {
            if (cleanSpoken.contains("first") || cleanSpoken.contains("প্রথম") || cleanSpoken.contains("पहला") || cleanSpoken.contains("1st")) {
                return options[0]
            }
            if (cleanSpoken.contains("second") || cleanSpoken.contains("দ্বিতীয়") || cleanSpoken.contains("দ্বিতীয়") || cleanSpoken.contains("दूसरा") || cleanSpoken.contains("2nd") || cleanSpoken.contains("last")) {
                return options[1]
            }
        }

        return null
    }

    private fun extractOptionIndex(text: String): Int? {
        val t = text.lowercase(Locale.ROOT)
        if (OPTION_1_REGEX.containsMatchIn(t)) return 1
        if (OPTION_2_REGEX.containsMatchIn(t)) return 2
        if (OPTION_3_REGEX.containsMatchIn(t)) return 3
        if (OPTION_4_REGEX.containsMatchIn(t)) return 4
        return null
    }

    private fun matchesLabel(spoken: String, label: String): Boolean {
        if (spoken.contains(label)) return true

        // Mobile
        if (label.contains("mobile") || label.contains("মোবাইল") || label.contains("मोबाइल")) {
            if (spoken.contains("mobile") || spoken.contains("মোবাইল") || spoken.contains("मोबाइल") ||
                spoken.contains("mob") || spoken.contains("মবাইল") || spoken.contains("সেলফোন") ||
                spoken.contains("cell")
            ) {
                return true
            }
        }

        // Office / Work
        if (label.contains("work") || label.contains("office") || label.contains("অফিস") || label.contains("ऑफिस") || label.contains("काम")) {
            if (spoken.contains("office") || spoken.contains("অফিস") || spoken.contains("ऑफिस") ||
                spoken.contains("work") || spoken.contains("কাজ") || spoken.contains("কাম")
            ) {
                return true
            }
        }

        // Home
        if (label.contains("home") || label.contains("বাড়ি") || label.contains("घर") || label.contains("ঘর")) {
            if (spoken.contains("home") || spoken.contains("বাড়ি") || spoken.contains("বাড়ি") ||
                spoken.contains("ঘর") || spoken.contains("घर")
            ) {
                return true
            }
        }

        return false
    }

    fun normalizeDigits(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            when (ch) {
                in '0'..'9' -> sb.append(ch)
                in '০'..'৯' -> sb.append((ch.code - '০'.code + '0'.code).toChar())
                in '०'..'९' -> sb.append((ch.code - '०'.code + '0'.code).toChar())
                in '੦'..'੯' -> sb.append((ch.code - '੦'.code + '0'.code).toChar())
                in '૦'..'૯' -> sb.append((ch.code - '૦'.code + '0'.code).toChar())
                in '୦'..'୯' -> sb.append((ch.code - '୦'.code + '0'.code).toChar())
                in '௦'..'௯' -> sb.append((ch.code - '௦'.code + '0'.code).toChar())
                in '౦'..'౯' -> sb.append((ch.code - '౦'.code + '0'.code).toChar())
                in '೦'..'೯' -> sb.append((ch.code - '೦'.code + '0'.code).toChar())
                in '൦'..'൯' -> sb.append((ch.code - '൦'.code + '0'.code).toChar())
            }
        }
        return sb.toString()
    }

    fun toIndicDigits(digits: String, language: SupportedLanguage): String {
        val offset = when (language) {
            SupportedLanguage.BENGALI, SupportedLanguage.ASSAMESE -> '০'.code - '0'.code
            SupportedLanguage.HINDI, SupportedLanguage.MARATHI -> '०'.code - '0'.code
            SupportedLanguage.GUJARATI -> '૦'.code - '0'.code
            SupportedLanguage.PUNJABI -> '੦'.code - '0'.code
            SupportedLanguage.ODIA -> '୦'.code - '0'.code
            SupportedLanguage.TAMIL -> '௦'.code - '0'.code
            SupportedLanguage.TELUGU -> '౦'.code - '0'.code
            SupportedLanguage.KANNADA -> '೦'.code - '0'.code
            SupportedLanguage.MALAYALAM -> '൦'.code - '0'.code
            else -> 0
        }
        if (offset == 0) return digits

        return digits.map { ch ->
            if (ch in '0'..'9') (ch.code + offset).toChar() else ch
        }.joinToString("")
    }
}

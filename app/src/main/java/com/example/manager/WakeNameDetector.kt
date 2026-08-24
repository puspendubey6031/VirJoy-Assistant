package com.example.manager

import com.example.model.SupportedLanguage
import java.util.Locale

data class WakeWordMatchResult(
    val isWakeWordDetected: Boolean,
    val remainingCommand: String = "",
    val detectedWakeWord: String = ""
)

object WakeNameDetector {

    private val GREETING_PREFIXES = setOf(
        "hey", "hi", "hello", "ok", "okay", "listen", "dear",
        "ওহে", "আরে", "হ্যালো", "শুনুন", "শোনো", "হে",
        "हे", "नमस्ते", "सुनो", "सुनिए", "अरे",
        "హలో", "வணக்கம்", "নমস্কাৰ"
    )

    /**
     * Checks if the spoken text contains the configured wake name.
     * Tolerates variations in casing, punctuation, script/transliteration, and leading greetings.
     */
    fun checkWakeName(rawText: String, configuredWakeName: String): WakeWordMatchResult {
        val trimmed = rawText.trim()
        val cleanConfiguredName = configuredWakeName.trim()
        if (trimmed.isEmpty() || cleanConfiguredName.isEmpty()) {
            return WakeWordMatchResult(isWakeWordDetected = false)
        }

        // Split configured name in case user configured "Ram / রাম" or "Ram, রাম"
        val wakeCandidates = cleanConfiguredName
            .split(Regex("[/,|]"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf(cleanConfiguredName) }

        // Tokenize raw text by punctuation and whitespace
        val tokens = trimmed
            .split(Regex("[,:;!?।\\-\\s]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (tokens.isEmpty()) {
            return WakeWordMatchResult(isWakeWordDetected = false)
        }

        for (candidate in wakeCandidates) {
            val candidateLatin = BengaliHindiEnglishMatcher.transliterateToLatin(candidate).lowercase(Locale.ROOT)
            val candidateLower = candidate.lowercase(Locale.ROOT)

            // 1. Check direct match on first or second token (handling optional greeting prefix)
            var wakeTokenIndex = -1
            for (idx in 0 until minOf(3, tokens.size)) {
                val token = tokens[idx]
                val tokenLower = token.lowercase(Locale.ROOT)
                val tokenLatin = BengaliHindiEnglishMatcher.transliterateToLatin(token).lowercase(Locale.ROOT)

                if (idx == 0 && GREETING_PREFIXES.contains(tokenLower)) {
                    continue
                }

                // Check exact token or transliterated token or phonetic match
                val isExact = tokenLower == candidateLower || tokenLatin == candidateLatin
                val isPhonetic = if (!isExact && candidateLatin.length >= 3 && tokenLatin.length >= 3) {
                    BengaliHindiEnglishMatcher.computeMatchScore(token, candidate) >= 0.80
                } else false

                if (isExact || isPhonetic) {
                    wakeTokenIndex = idx
                    break
                }
            }

            if (wakeTokenIndex != -1) {
                // Wake name detected! Extract remaining command
                val remainingTokens = tokens.drop(wakeTokenIndex + 1)
                val remainingCommand = remainingTokens.joinToString(" ")
                return WakeWordMatchResult(
                    isWakeWordDetected = true,
                    remainingCommand = remainingCommand,
                    detectedWakeWord = candidate
                )
            }

            // 2. Substring matching with regex fallback
            val escapedCandidate = Regex.escape(candidate)
            val regex = Regex("^(?:(?:hey|hi|hello|ok|okay|ওহে|আরে|হ্যালো|हे|नमस्ते|सुनो)\\s+)?$escapedCandidate[\\s,:;!?।\\-]*(.*)$", RegexOption.IGNORE_CASE)
            val match = regex.find(trimmed)
            if (match != null) {
                val remaining = match.groupValues[1].trim()
                return WakeWordMatchResult(
                    isWakeWordDetected = true,
                    remainingCommand = remaining,
                    detectedWakeWord = candidate
                )
            }

            // 3. Latin phonetic substring match on whole string
            val rawLatin = BengaliHindiEnglishMatcher.transliterateToLatin(trimmed).lowercase(Locale.ROOT)
            if (rawLatin.startsWith(candidateLatin)) {
                val remLatin = rawLatin.removePrefix(candidateLatin).trim()
                // Find approximate cut index in original text
                val originalCut = minOf(candidate.length, trimmed.length)
                val remOriginal = trimmed.substring(originalCut).trim().trimStart(',', ':', ';', '!', '?', '।', '-')
                return WakeWordMatchResult(
                    isWakeWordDetected = true,
                    remainingCommand = remOriginal,
                    detectedWakeWord = candidate
                )
            }
        }

        return WakeWordMatchResult(isWakeWordDetected = false)
    }
}

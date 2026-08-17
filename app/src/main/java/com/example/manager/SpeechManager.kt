package com.example.manager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import com.example.model.SupportedLanguage
import com.example.model.VoiceGender
import java.util.Locale

class SpeechManager(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onError: (String) -> Unit,
    private val onRmsChangedCallback: ((Float) -> Unit)? = null
) {
    companion object {
        private const val TAG = "SpeechManager"
        private const val SUPPORTED_LANGUAGES_LIST =
            "bn-IN,hi-IN,en-IN,as-IN,gu-IN,kn-IN,ml-IN,mr-IN,or-IN,pa-IN,ta-IN,te-IN,ur-IN,en-US"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var currentVoiceGender: VoiceGender = VoiceGender.FEMALE
    private var lastSpokenLanguage: SupportedLanguage = SupportedLanguage.ENGLISH

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                val result = textToSpeech?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech?.setLanguage(Locale("en", "IN"))
                }
                applyVoiceGender(currentVoiceGender)
            } else {
                Log.w(TAG, "TextToSpeech initialization failed")
            }
        }
    }

    fun updateVoiceGender(gender: VoiceGender) {
        currentVoiceGender = gender
        applyVoiceGender(gender)
    }

    private fun applyVoiceGender(gender: VoiceGender, targetLocale: Locale? = null) {
        val tts = textToSpeech ?: return
        if (!isTtsInitialized) return

        try {
            when (gender) {
                VoiceGender.MALE -> {
                    tts.setPitch(0.85f)
                    tts.setSpeechRate(0.95f)
                    val voices = tts.voices
                    val maleVoice = voices?.firstOrNull { voice ->
                        val matchesLocale = targetLocale == null || voice.locale.language == targetLocale.language
                        matchesLocale && voice.name.contains("male", ignoreCase = true) && !voice.name.contains("female", ignoreCase = true)
                    }
                    if (maleVoice != null) {
                        tts.voice = maleVoice
                    }
                }
                VoiceGender.FEMALE -> {
                    tts.setPitch(1.15f)
                    tts.setSpeechRate(1.0f)
                    val voices = tts.voices
                    val femaleVoice = voices?.firstOrNull { voice ->
                        val matchesLocale = targetLocale == null || voice.locale.language == targetLocale.language
                        matchesLocale && voice.name.contains("female", ignoreCase = true)
                    }
                    if (femaleVoice != null) {
                        tts.voice = femaleVoice
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not configure voice profile", e)
        }
    }

    /**
     * Speaks text using the user's spoken language if available, falling back safely.
     */
    fun speak(text: String, language: SupportedLanguage = SupportedLanguage.ENGLISH) {
        if (!isTtsInitialized) return
        val tts = textToSpeech ?: return
        lastSpokenLanguage = language

        try {
            val targetLocale = language.locale
            val availability = tts.isLanguageAvailable(targetLocale)
            if (availability >= TextToSpeech.LANG_AVAILABLE) {
                tts.setLanguage(targetLocale)
            } else {
                // Fallback to language without country or Indian English
                val langOnly = Locale(targetLocale.language)
                if (tts.isLanguageAvailable(langOnly) >= TextToSpeech.LANG_AVAILABLE) {
                    tts.setLanguage(langOnly)
                }
            }
            applyVoiceGender(currentVoiceGender, targetLocale)
        } catch (e: Exception) {
            Log.w(TAG, "Error configuring TTS language for ${language.code}", e)
        }

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "VirJoyTTS")
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition is not available on this device.")
            return
        }

        stopListening()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    onListeningStateChanged(true)
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    onRmsChangedCallback?.invoke(rmsdB)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    onListeningStateChanged(false)
                }

                override fun onError(error: Int) {
                    onListeningStateChanged(false)
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please try again."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Please tap mic and try again."
                        else -> "Speech error: $error"
                    }
                    onError(errorMessage)
                }

                override fun onResults(results: Bundle?) {
                    onListeningStateChanged(false)
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val recognized = matches[0]
                        onSpeechResult(recognized)
                    } else {
                        onError("No speech recognized.")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, SUPPORTED_LANGUAGES_LIST)
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf(
                "bn-IN", "hi-IN", "en-IN", "as-IN", "gu-IN", "kn-IN", "ml-IN", "mr-IN", "or-IN", "pa-IN", "ta-IN", "te-IN", "ur-IN"
            ))
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            onListeningStateChanged(false)
            onError("Failed to start speech recognizer: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recognizer", e)
        } finally {
            speechRecognizer = null
            onListeningStateChanged(false)
        }
    }

    fun destroy() {
        stopListening()
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
}

package com.example.manager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
        private const val UTTERANCE_ID = "VirJoyTTS"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var isSpeaking = false
    private var currentVoiceGender: VoiceGender = VoiceGender.FEMALE
    private var currentLanguage: SupportedLanguage = SupportedLanguage.ENGLISH
    private var isWakeMode: Boolean = true
    private var isContinuousListeningEnabled: Boolean = true
    private var isDestroyed: Boolean = false

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                applyLanguageAndVoice(currentLanguage, currentVoiceGender)
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                    }

                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                        currentTtsCompletionCallback?.let { callback ->
                            currentTtsCompletionCallback = null
                            mainHandler.post { callback() }
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                        currentTtsCompletionCallback?.let { callback ->
                            currentTtsCompletionCallback = null
                            mainHandler.post { callback() }
                        }
                    }
                })
            } else {
                Log.w(TAG, "TextToSpeech initialization failed")
            }
        }
    }

    private var currentTtsCompletionCallback: (() -> Unit)? = null

    fun updateLanguage(language: SupportedLanguage) {
        currentLanguage = language
        applyLanguageAndVoice(language, currentVoiceGender)
    }

    fun updateVoiceGender(gender: VoiceGender) {
        currentVoiceGender = gender
        applyLanguageAndVoice(currentLanguage, gender)
    }

    fun setContinuousListeningEnabled(enabled: Boolean) {
        isContinuousListeningEnabled = enabled
        if (!enabled && isWakeMode) {
            stopListening()
        }
    }

    private fun applyLanguageAndVoice(language: SupportedLanguage, gender: VoiceGender) {
        val tts = textToSpeech ?: return
        if (!isTtsInitialized) return

        try {
            val targetLocale = language.locale
            var availability = tts.isLanguageAvailable(targetLocale)
            if (availability >= TextToSpeech.LANG_AVAILABLE) {
                tts.setLanguage(targetLocale)
            } else {
                val langOnly = Locale(targetLocale.language)
                availability = tts.isLanguageAvailable(langOnly)
                if (availability >= TextToSpeech.LANG_AVAILABLE) {
                    tts.setLanguage(langOnly)
                } else {
                    val fallbackLocale = Locale("en", "IN")
                    if (tts.isLanguageAvailable(fallbackLocale) >= TextToSpeech.LANG_AVAILABLE) {
                        tts.setLanguage(fallbackLocale)
                    } else {
                        tts.setLanguage(Locale.getDefault())
                    }
                }
            }

            applyVoiceGender(gender, targetLocale)
        } catch (e: Exception) {
            Log.w(TAG, "Could not configure TTS language/voice profile for ${language.code}", e)
        }
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
                    } ?: voices?.firstOrNull { voice ->
                        voice.name.contains("male", ignoreCase = true) && !voice.name.contains("female", ignoreCase = true)
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
                    } ?: voices?.firstOrNull { voice ->
                        voice.name.contains("female", ignoreCase = true)
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
     * Speaks text using the configured voice profile and notifies completion.
     */
    fun speak(
        text: String,
        language: SupportedLanguage = currentLanguage,
        onDone: (() -> Unit)? = null
    ) {
        if (!isTtsInitialized) {
            onDone?.invoke()
            return
        }
        val tts = textToSpeech ?: run {
            onDone?.invoke()
            return
        }

        // Pause speech recognizer while speaking to prevent hearing oneself
        pauseListeningTemporarily()

        currentTtsCompletionCallback = onDone

        try {
            applyLanguageAndVoice(language, currentVoiceGender)
        } catch (e: Exception) {
            Log.w(TAG, "Error configuring TTS language for ${language.code}", e)
        }

        try {
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID)
            }
            val res = tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID)
            if (res != TextToSpeech.SUCCESS) {
                isSpeaking = false
                currentTtsCompletionCallback = null
                onDone?.invoke()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during TTS speak", e)
            isSpeaking = false
            currentTtsCompletionCallback = null
            onDone?.invoke()
        }
    }

    private fun pauseListeningTemporarily() {
        mainHandler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error pausing recognizer", e)
        }
    }

    fun startWakeListening(language: SupportedLanguage = currentLanguage) {
        isWakeMode = true
        startListeningInternal(language)
    }

    fun startCommandListening(language: SupportedLanguage = currentLanguage) {
        isWakeMode = false
        startListeningInternal(language)
    }

    fun startListening(language: SupportedLanguage = currentLanguage) {
        startCommandListening(language)
    }

    private fun startListeningInternal(language: SupportedLanguage) {
        if (isDestroyed) return
        if (isSpeaking) {
            Log.d(TAG, "Postponing listening while TTS is speaking")
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition is not available on this device.")
            return
        }

        mainHandler.removeCallbacksAndMessages(null)
        stopInternalRecognizer()

        try {
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
                        val isTransient = error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                                error == SpeechRecognizer.ERROR_NO_MATCH ||
                                error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                                error == SpeechRecognizer.ERROR_CLIENT

                        if (isWakeMode && isContinuousListeningEnabled && isTransient && !isDestroyed) {
                            // In hands-free wake mode, silently re-arm listening
                            scheduleRestartListening(language, 300L)
                        } else {
                            val errorMessage = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required for voice activation."
                                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized."
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                                SpeechRecognizer.ERROR_SERVER -> "Server error"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
                                else -> "Speech error: $error"
                            }
                            onError(errorMessage)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        onListeningStateChanged(false)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognized = matches[0]
                            onSpeechResult(recognized)
                        } else {
                            if (isWakeMode && isContinuousListeningEnabled && !isDestroyed) {
                                scheduleRestartListening(language, 300L)
                            } else {
                                onError("No speech recognized.")
                            }
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
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.code)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language.code)
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf(
                    "bn-IN", "hi-IN", "en-IN", "te-IN", "mr-IN", "ta-IN", "gu-IN", "kn-IN", "ml-IN", "pa-IN", "or-IN", "as-IN", "ur-IN"
                ))
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            onListeningStateChanged(false)
            if (isWakeMode && isContinuousListeningEnabled && !isDestroyed) {
                scheduleRestartListening(language, 1000L)
            } else {
                onError("Failed to start speech recognizer: ${e.localizedMessage}")
            }
        }
    }

    fun scheduleRestartListening(language: SupportedLanguage = currentLanguage, delayMs: Long = 300L) {
        if (isDestroyed || !isContinuousListeningEnabled || isSpeaking) return
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (!isDestroyed && !isSpeaking) {
                if (isWakeMode) {
                    startWakeListening(language)
                } else {
                    startCommandListening(language)
                }
            }
        }, delayMs)
    }

    private fun stopInternalRecognizer() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recognizer", e)
        } finally {
            speechRecognizer = null
        }
    }

    fun stopListening() {
        mainHandler.removeCallbacksAndMessages(null)
        stopInternalRecognizer()
        onListeningStateChanged(false)
    }

    fun destroy() {
        isDestroyed = true
        mainHandler.removeCallbacksAndMessages(null)
        stopListening()
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        } finally {
            textToSpeech = null
        }
    }
}

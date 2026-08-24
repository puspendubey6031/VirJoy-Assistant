package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.manager.BengaliHindiEnglishMatcher
import com.example.manager.CallManager
import com.example.manager.ContactManager
import com.example.manager.LanguageManager
import com.example.manager.ParsedVoiceCommand
import com.example.manager.SpeechManager
import com.example.manager.VoiceCommandParser
import com.example.manager.WakeNameDetector
import com.example.model.AssistantListeningMode
import com.example.model.Contact
import com.example.model.ContactMatchResult
import com.example.model.SupportedLanguage
import com.example.model.VoiceGender
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssistantUiState(
    val assistantName: String = "VirJoy Assistant",
    val wakeName: String = "VirJoy",
    val voiceGender: VoiceGender = VoiceGender.FEMALE,
    val selectedLanguage: SupportedLanguage = SupportedLanguage.ENGLISH,
    val listeningMode: AssistantListeningMode = AssistantListeningMode.WAKE_LISTENING,
    val isListening: Boolean = false,
    val recognizedText: String = "",
    val responseText: String = "Say \"VirJoy\" to activate",
    val multipleMatches: List<Contact> = emptyList(),
    val isSettingsOpen: Boolean = false,
    val hasAllPermissions: Boolean = false,
    val rmsLevel: Float = 0f,
    val currentLanguage: SupportedLanguage = SupportedLanguage.ENGLISH,
    val isHandsFreeEnabled: Boolean = true
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val contactManager = ContactManager(context)
    private val callManager = CallManager(context)

    private val _uiState = MutableStateFlow(loadInitialSettings())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private var speechManager: SpeechManager? = null
    private var lastDetectedLanguage: SupportedLanguage = SupportedLanguage.ENGLISH
    private var commandTimeoutJob: Job? = null

    init {
        initSpeechManager()
    }

    private fun loadInitialSettings(): AssistantUiState {
        val prefs = context.getSharedPreferences("virjoy_prefs", Context.MODE_PRIVATE)
        val name = prefs.getString("assistant_name", "VirJoy Assistant") ?: "VirJoy Assistant"
        val wakeName = prefs.getString("wake_name", "VirJoy") ?: "VirJoy"
        val voiceName = prefs.getString("voice_gender", VoiceGender.FEMALE.name) ?: VoiceGender.FEMALE.name
        val langCode = prefs.getString("selected_language", SupportedLanguage.ENGLISH.name) ?: SupportedLanguage.ENGLISH.name
        val isHandsFree = prefs.getBoolean("is_handsfree_enabled", true)

        val gender = try {
            VoiceGender.valueOf(voiceName)
        } catch (e: Exception) {
            VoiceGender.FEMALE
        }
        val language = try {
            SupportedLanguage.valueOf(langCode)
        } catch (e: Exception) {
            SupportedLanguage.fromCode(langCode)
        }

        val initialPrompt = LanguageManager.getWakeIdlePrompt(wakeName, language)

        return AssistantUiState(
            assistantName = name,
            wakeName = wakeName,
            voiceGender = gender,
            selectedLanguage = language,
            currentLanguage = language,
            responseText = initialPrompt,
            listeningMode = AssistantListeningMode.WAKE_LISTENING,
            isHandsFreeEnabled = isHandsFree
        )
    }

    private fun initSpeechManager() {
        speechManager = SpeechManager(
            context = context,
            onSpeechResult = { text ->
                handleSpeechResult(text)
            },
            onListeningStateChanged = { listening ->
                _uiState.update { it.copy(isListening = listening, rmsLevel = if (!listening) 0f else it.rmsLevel) }
            },
            onError = { error ->
                handleSpeechError(error)
            },
            onRmsChangedCallback = { rms ->
                _uiState.update { it.copy(rmsLevel = rms) }
            }
        ).apply {
            updateVoiceGender(_uiState.value.voiceGender)
            updateLanguage(_uiState.value.selectedLanguage)
            setContinuousListeningEnabled(_uiState.value.isHandsFreeEnabled)
        }
    }

    private fun handleSpeechError(error: String) {
        if (_uiState.value.listeningMode == AssistantListeningMode.COMMAND_LISTENING) {
            _uiState.update { it.copy(isListening = false, responseText = error, rmsLevel = 0f) }
            // If command listening failed, schedule return to wake listening
            startCommandTimeoutJob(3000L)
        } else {
            // In wake listening mode, keep UI clean and silently continue wake monitoring
            _uiState.update { it.copy(isListening = false, rmsLevel = 0f) }
            if (_uiState.value.hasAllPermissions && _uiState.value.isHandsFreeEnabled) {
                speechManager?.scheduleRestartListening(_uiState.value.selectedLanguage, 500L)
            }
        }
    }

    fun onMicClicked() {
        if (!_uiState.value.hasAllPermissions) {
            _uiState.update {
                it.copy(responseText = "Please grant Microphone, Contacts, and Call permissions to continue.")
            }
            return
        }

        commandTimeoutJob?.cancel()

        if (_uiState.value.isListening && _uiState.value.listeningMode == AssistantListeningMode.COMMAND_LISTENING) {
            // Cancel active command listening and return to wake listening
            returnToWakeListening()
        } else {
            // Manual activation directly enters COMMAND_LISTENING
            _uiState.update {
                it.copy(
                    listeningMode = AssistantListeningMode.COMMAND_LISTENING,
                    recognizedText = "",
                    responseText = "Listening... Speak now",
                    multipleMatches = emptyList()
                )
            }
            speechManager?.startCommandListening(_uiState.value.selectedLanguage)
            startCommandTimeoutJob(8000L)
        }
    }

    fun handleSpeechResult(rawText: String) {
        val currentMode = _uiState.value.listeningMode
        val preferredLang = _uiState.value.selectedLanguage
        val configuredWake = _uiState.value.wakeName

        if (currentMode == AssistantListeningMode.WAKE_LISTENING) {
            // 1. Lightweight local wake name detection
            val wakeMatch = WakeNameDetector.checkWakeName(rawText, configuredWake)
            if (!wakeMatch.isWakeWordDetected) {
                // Ignore background conversation without wake word
                if (_uiState.value.hasAllPermissions && _uiState.value.isHandsFreeEnabled) {
                    speechManager?.scheduleRestartListening(preferredLang, 300L)
                }
                return
            }

            // Wake name detected!
            _uiState.update {
                it.copy(
                    recognizedText = rawText,
                    multipleMatches = emptyList()
                )
            }

            if (wakeMatch.remainingCommand.isNotBlank()) {
                // Wake name + Command in same utterance (e.g. "রাম, বাবুকে কল করো")
                _uiState.update {
                    it.copy(listeningMode = AssistantListeningMode.COMMAND_LISTENING)
                }
                executeParsedCommand(wakeMatch.remainingCommand)
            } else {
                // Wake name alone (e.g. "রাম" / "Ram")
                // Transition to COMMAND_LISTENING, give audible acknowledgement ("Yes?"), then listen for command
                _uiState.update {
                    it.copy(
                        listeningMode = AssistantListeningMode.COMMAND_LISTENING,
                        responseText = LanguageManager.getListeningForCommandPrompt(preferredLang)
                    )
                }

                val ackMessage = LanguageManager.getWakeAcknowledgementMessage(preferredLang)
                speechManager?.speak(ackMessage, preferredLang) {
                    if (_uiState.value.listeningMode == AssistantListeningMode.COMMAND_LISTENING) {
                        speechManager?.startCommandListening(preferredLang)
                        startCommandTimeoutJob(8000L)
                    }
                }
            }
        } else {
            // In COMMAND_LISTENING mode
            commandTimeoutJob?.cancel()
            _uiState.update {
                it.copy(recognizedText = rawText)
            }
            executeParsedCommand(rawText)
        }
    }

    private fun executeParsedCommand(commandText: String) {
        val preferredLang = _uiState.value.selectedLanguage
        val parsed = VoiceCommandParser.parse(commandText, preferredLang)
        val detectedLanguage = parsed.detectedLanguage
        lastDetectedLanguage = detectedLanguage

        _uiState.update {
            it.copy(
                currentLanguage = detectedLanguage
            )
        }

        viewModelScope.launch {
            when (parsed) {
                is ParsedVoiceCommand.CallContact -> {
                    processCallContact(parsed.targetName, detectedLanguage)
                }
                is ParsedVoiceCommand.Unknown -> {
                    val cleaned = BengaliHindiEnglishMatcher.cleanContactQuery(commandText)
                    val target = if (cleaned.isNotEmpty()) cleaned else commandText
                    processCallContact(target, detectedLanguage)
                }
            }
        }
    }

    private fun processCallContact(targetName: String, language: SupportedLanguage) {
        when (val matchResult = contactManager.findBestContactMatch(targetName)) {
            is ContactMatchResult.SingleMatch -> {
                val contact = matchResult.contact
                val phone = matchResult.phoneNumber
                if (phone.isNotBlank()) {
                    initiateCall(contact.name, phone, language)
                } else {
                    val message = LanguageManager.getNoPhoneNumberMessage(contact.name, language)
                    _uiState.update {
                        it.copy(
                            responseText = message,
                            multipleMatches = emptyList()
                        )
                    }
                    speechManager?.speak(message, language) {
                        returnToWakeListeningDelayed(2000L)
                    }
                }
            }
            is ContactMatchResult.MultipleMatches -> {
                val message = LanguageManager.getMultipleMatchesMessage(language)
                _uiState.update {
                    it.copy(
                        responseText = message,
                        multipleMatches = matchResult.contacts
                    )
                }
                speechManager?.speak(message, language)
                // Keep multiple matches for user selection, then return to wake listening on timeout
                startCommandTimeoutJob(15000L)
            }
            is ContactMatchResult.NoMatch -> {
                val message = LanguageManager.getNoMatchMessage(targetName, language)
                _uiState.update {
                    it.copy(
                        responseText = message,
                        multipleMatches = emptyList()
                    )
                }
                speechManager?.speak(message, language) {
                    returnToWakeListeningDelayed(2000L)
                }
            }
        }
    }

    fun selectContactToCall(contact: Contact, selectedPhone: String? = null) {
        commandTimeoutJob?.cancel()
        val phoneNumber = selectedPhone ?: contact.primaryPhoneNumber
        val lang = lastDetectedLanguage
        if (phoneNumber.isNotEmpty()) {
            initiateCall(contact.name, phoneNumber, lang)
        } else {
            val message = LanguageManager.getNoPhoneNumberMessage(contact.name, lang)
            _uiState.update { it.copy(responseText = message, multipleMatches = emptyList()) }
            speechManager?.speak(message, lang) {
                returnToWakeListeningDelayed(2000L)
            }
        }
    }

    private fun initiateCall(contactName: String, phoneNumber: String, language: SupportedLanguage) {
        val message = LanguageManager.getCallingMessage(contactName, language)
        _uiState.update {
            it.copy(
                responseText = message,
                multipleMatches = emptyList()
            )
        }
        speechManager?.speak(message, language) {
            returnToWakeListeningDelayed(1000L)
        }

        val result = callManager.makePhoneCall(phoneNumber)
        result.onFailure { error ->
            val errorMsg = LanguageManager.getCallFailedMessage(
                contactName,
                error.localizedMessage ?: "Unknown error",
                language
            )
            _uiState.update { it.copy(responseText = errorMsg) }
            returnToWakeListeningDelayed(3000L)
        }
    }

    private fun startCommandTimeoutJob(delayMs: Long = 8000L) {
        commandTimeoutJob?.cancel()
        commandTimeoutJob = viewModelScope.launch {
            delay(delayMs)
            if (_uiState.value.listeningMode == AssistantListeningMode.COMMAND_LISTENING) {
                returnToWakeListening()
            }
        }
    }

    private fun returnToWakeListeningDelayed(delayMs: Long = 2000L) {
        viewModelScope.launch {
            delay(delayMs)
            returnToWakeListening()
        }
    }

    fun returnToWakeListening() {
        commandTimeoutJob?.cancel()
        val wakePrompt = LanguageManager.getWakeIdlePrompt(_uiState.value.wakeName, _uiState.value.selectedLanguage)
        _uiState.update {
            it.copy(
                listeningMode = AssistantListeningMode.WAKE_LISTENING,
                responseText = wakePrompt,
                multipleMatches = emptyList(),
                isListening = false,
                rmsLevel = 0f
            )
        }
        if (_uiState.value.hasAllPermissions && _uiState.value.isHandsFreeEnabled) {
            speechManager?.startWakeListening(_uiState.value.selectedLanguage)
        } else {
            speechManager?.stopListening()
        }
    }

    fun updatePermissionsState(granted: Boolean) {
        _uiState.update { it.copy(hasAllPermissions = granted) }
        if (granted) {
            if (_uiState.value.isHandsFreeEnabled) {
                returnToWakeListening()
            }
        } else {
            speechManager?.stopListening()
            _uiState.update {
                it.copy(
                    listeningMode = AssistantListeningMode.INACTIVE,
                    responseText = "Microphone, Contacts, and Call permissions are required."
                )
            }
        }
    }

    fun openSettings() {
        _uiState.update { it.copy(isSettingsOpen = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(isSettingsOpen = false) }
    }

    fun saveSettings(
        name: String,
        wakeName: String,
        voiceGender: VoiceGender,
        language: SupportedLanguage = _uiState.value.selectedLanguage,
        isHandsFree: Boolean = true
    ) {
        val safeName = name.trim().ifEmpty { "VirJoy Assistant" }
        val safeWakeName = wakeName.trim().ifEmpty { safeName }
        val prefs = context.getSharedPreferences("virjoy_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("assistant_name", safeName)
            .putString("wake_name", safeWakeName)
            .putString("voice_gender", voiceGender.name)
            .putString("selected_language", language.name)
            .putBoolean("is_handsfree_enabled", isHandsFree)
            .apply()

        speechManager?.updateVoiceGender(voiceGender)
        speechManager?.updateLanguage(language)
        speechManager?.setContinuousListeningEnabled(isHandsFree)

        val newPrompt = LanguageManager.getWakeIdlePrompt(safeWakeName, language)

        _uiState.update {
            it.copy(
                assistantName = safeName,
                wakeName = safeWakeName,
                voiceGender = voiceGender,
                selectedLanguage = language,
                currentLanguage = language,
                isHandsFreeEnabled = isHandsFree,
                responseText = newPrompt,
                isSettingsOpen = false
            )
        }

        if (_uiState.value.hasAllPermissions && isHandsFree) {
            speechManager?.startWakeListening(language)
        } else {
            speechManager?.stopListening()
        }
    }

    fun saveSettings(name: String, voiceGender: VoiceGender, language: SupportedLanguage) {
        saveSettings(name, _uiState.value.wakeName, voiceGender, language, _uiState.value.isHandsFreeEnabled)
    }

    fun saveSettings(name: String, voiceGender: VoiceGender) {
        saveSettings(name, _uiState.value.wakeName, voiceGender, _uiState.value.selectedLanguage, _uiState.value.isHandsFreeEnabled)
    }

    override fun onCleared() {
        super.onCleared()
        commandTimeoutJob?.cancel()
        speechManager?.destroy()
    }
}

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
import com.example.model.Contact
import com.example.model.ContactMatchResult
import com.example.model.SupportedLanguage
import com.example.model.VoiceGender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssistantUiState(
    val assistantName: String = "VirJoy Assistant",
    val voiceGender: VoiceGender = VoiceGender.FEMALE,
    val isListening: Boolean = false,
    val recognizedText: String = "",
    val responseText: String = "Tap the microphone to speak.",
    val multipleMatches: List<Contact> = emptyList(),
    val isSettingsOpen: Boolean = false,
    val hasAllPermissions: Boolean = false,
    val rmsLevel: Float = 0f,
    val currentLanguage: SupportedLanguage = SupportedLanguage.ENGLISH
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val contactManager = ContactManager(context)
    private val callManager = CallManager(context)

    private val _uiState = MutableStateFlow(loadInitialSettings())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private var speechManager: SpeechManager? = null
    private var lastDetectedLanguage: SupportedLanguage = SupportedLanguage.ENGLISH

    init {
        initSpeechManager()
    }

    private fun loadInitialSettings(): AssistantUiState {
        val prefs = context.getSharedPreferences("virjoy_prefs", Context.MODE_PRIVATE)
        val name = prefs.getString("assistant_name", "VirJoy Assistant") ?: "VirJoy Assistant"
        val voiceName = prefs.getString("voice_gender", VoiceGender.FEMALE.name) ?: VoiceGender.FEMALE.name
        val gender = try {
            VoiceGender.valueOf(voiceName)
        } catch (e: Exception) {
            VoiceGender.FEMALE
        }
        return AssistantUiState(
            assistantName = name,
            voiceGender = gender
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
                _uiState.update { it.copy(isListening = false, responseText = error, rmsLevel = 0f) }
            },
            onRmsChangedCallback = { rms ->
                _uiState.update { it.copy(rmsLevel = rms) }
            }
        ).apply {
            updateVoiceGender(_uiState.value.voiceGender)
        }
    }

    fun onMicClicked() {
        if (!_uiState.value.hasAllPermissions) {
            _uiState.update {
                it.copy(responseText = "Please grant Microphone, Contacts, and Call permissions to continue.")
            }
            return
        }

        if (_uiState.value.isListening) {
            speechManager?.stopListening()
        } else {
            _uiState.update {
                it.copy(
                    recognizedText = "",
                    responseText = "Listening... Speak now",
                    multipleMatches = emptyList()
                )
            }
            speechManager?.startListening()
        }
    }

    fun handleSpeechResult(rawText: String) {
        val parsed = VoiceCommandParser.parse(rawText)
        val detectedLanguage = parsed.detectedLanguage
        lastDetectedLanguage = detectedLanguage

        _uiState.update {
            it.copy(
                recognizedText = rawText,
                currentLanguage = detectedLanguage
            )
        }

        viewModelScope.launch {
            when (parsed) {
                is ParsedVoiceCommand.CallContact -> {
                    processCallContact(parsed.targetName, detectedLanguage)
                }
                is ParsedVoiceCommand.Unknown -> {
                    val cleaned = BengaliHindiEnglishMatcher.cleanContactQuery(rawText)
                    val target = if (cleaned.isNotEmpty()) cleaned else rawText
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
                    speechManager?.speak(message, language)
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
            }
            is ContactMatchResult.NoMatch -> {
                val message = LanguageManager.getNoMatchMessage(targetName, language)
                _uiState.update {
                    it.copy(
                        responseText = message,
                        multipleMatches = emptyList()
                    )
                }
                speechManager?.speak(message, language)
            }
        }
    }

    fun selectContactToCall(contact: Contact, selectedPhone: String? = null) {
        val phoneNumber = selectedPhone ?: contact.primaryPhoneNumber
        val lang = lastDetectedLanguage
        if (phoneNumber.isNotEmpty()) {
            initiateCall(contact.name, phoneNumber, lang)
        } else {
            val message = LanguageManager.getNoPhoneNumberMessage(contact.name, lang)
            _uiState.update { it.copy(responseText = message, multipleMatches = emptyList()) }
            speechManager?.speak(message, lang)
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
        speechManager?.speak(message, language)

        val result = callManager.makePhoneCall(phoneNumber)
        result.onFailure { error ->
            val errorMsg = LanguageManager.getCallFailedMessage(
                contactName,
                error.localizedMessage ?: "Unknown error",
                language
            )
            _uiState.update { it.copy(responseText = errorMsg) }
        }
    }

    fun updatePermissionsState(granted: Boolean) {
        _uiState.update { it.copy(hasAllPermissions = granted) }
    }

    fun openSettings() {
        _uiState.update { it.copy(isSettingsOpen = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(isSettingsOpen = false) }
    }

    fun saveSettings(name: String, voiceGender: VoiceGender) {
        val safeName = name.trim().ifEmpty { "VirJoy Assistant" }
        val prefs = context.getSharedPreferences("virjoy_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("assistant_name", safeName)
            .putString("voice_gender", voiceGender.name)
            .apply()

        speechManager?.updateVoiceGender(voiceGender)

        _uiState.update {
            it.copy(
                assistantName = safeName,
                voiceGender = voiceGender,
                isSettingsOpen = false
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager?.destroy()
    }
}

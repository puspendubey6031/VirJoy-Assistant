package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.manager.BengaliHindiEnglishMatcher
import com.example.manager.CallManager
import com.example.manager.ContactManager
import com.example.manager.DisambiguationResolver
import com.example.manager.LanguageManager
import com.example.manager.ParsedVoiceCommand
import com.example.manager.SpeechManager
import com.example.manager.VoiceCommandParser
import com.example.manager.WakeNameDetector
import com.example.model.AssistantAvailabilityMode
import com.example.model.AssistantListeningMode
import com.example.model.Contact
import com.example.model.ContactMatchResult
import com.example.model.PhoneNumberOption
import com.example.model.SupportedLanguage
import com.example.model.VoiceGender
import com.example.service.AssistantForegroundService
import com.example.service.AssistantServiceBridge
import com.example.service.ServiceUserAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssistantUiState(
    val assistantName: String = "VirJoy Assistant",
    val wakeName: String = "VirJoy",
    val voiceGender: VoiceGender = VoiceGender.FEMALE,
    val selectedLanguage: SupportedLanguage = SupportedLanguage.ENGLISH,
    val isHandsFreeEnabled: Boolean = true,
    val availabilityMode: AssistantAvailabilityMode = AssistantAvailabilityMode.ACTIVE,
    val scheduleStartHour: Int = 8,
    val scheduleStartMinute: Int = 0,
    val scheduleEndHour: Int = 22,
    val scheduleEndMinute: Int = 0,
    val isListening: Boolean = false,
    val listeningMode: AssistantListeningMode = AssistantListeningMode.INACTIVE,
    val recognizedText: String = "",
    val responseText: String = "Say \"VirJoy\" or tap microphone to speak",
    val currentLanguage: SupportedLanguage = SupportedLanguage.ENGLISH,
    val multipleMatches: List<Contact> = emptyList(),
    val disambiguationOptions: List<PhoneNumberOption> = emptyList(),
    val hasAllPermissions: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val rmsLevel: Float = 0f
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AssistantViewModel"
    }

    private val context: Context get() = getApplication<Application>().applicationContext
    private val contactManager = ContactManager(context)
    private val callManager = CallManager(context)

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private var speechManager: SpeechManager? = null
    private var lastDetectedLanguage: SupportedLanguage = SupportedLanguage.ENGLISH
    private var commandTimeoutJob: Job? = null

    init {
        loadSettings()
        observeServiceBridge()
        initSpeechManager()
    }

    private fun observeServiceBridge() {
        AssistantServiceBridge.serviceState.onEach { svcState ->
            if (svcState.isServiceRunning) {
                _uiState.update { current ->
                    current.copy(
                        assistantName = svcState.assistantName,
                        wakeName = svcState.wakeName,
                        voiceGender = svcState.voiceGender,
                        selectedLanguage = svcState.selectedLanguage,
                        isHandsFreeEnabled = svcState.isHandsFreeEnabled,
                        availabilityMode = svcState.availabilityMode,
                        scheduleStartHour = svcState.scheduleStartHour,
                        scheduleStartMinute = svcState.scheduleStartMinute,
                        scheduleEndHour = svcState.scheduleEndHour,
                        scheduleEndMinute = svcState.scheduleEndMinute,
                        isListening = svcState.isListening,
                        listeningMode = svcState.listeningMode,
                        recognizedText = svcState.recognizedText,
                        responseText = svcState.responseText,
                        currentLanguage = svcState.currentLanguage,
                        multipleMatches = svcState.multipleMatches,
                        disambiguationOptions = svcState.disambiguationOptions,
                        rmsLevel = svcState.rmsLevel
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun loadSettings() {
        val prefs = context.getSharedPreferences("virjoy_prefs", Context.MODE_PRIVATE)
        val assistantName = prefs.getString("assistant_name", "VirJoy Assistant") ?: "VirJoy Assistant"
        val wakeName = prefs.getString("wake_name", "VirJoy") ?: "VirJoy"
        val voiceGenderStr = prefs.getString("voice_gender", VoiceGender.FEMALE.name) ?: VoiceGender.FEMALE.name
        val voiceGender = try {
            VoiceGender.valueOf(voiceGenderStr)
        } catch (e: Exception) {
            VoiceGender.FEMALE
        }
        val languageStr = prefs.getString("selected_language", SupportedLanguage.ENGLISH.name) ?: SupportedLanguage.ENGLISH.name
        val selectedLanguage = try {
            SupportedLanguage.valueOf(languageStr)
        } catch (e: Exception) {
            SupportedLanguage.ENGLISH
        }
        val isHandsFree = prefs.getBoolean("is_handsfree_enabled", true)
        val modeStr = prefs.getString("availability_mode", AssistantAvailabilityMode.ACTIVE.name) ?: AssistantAvailabilityMode.ACTIVE.name
        val availabilityMode = AssistantAvailabilityMode.fromString(modeStr)
        val startH = prefs.getInt("schedule_start_hour", 8)
        val startM = prefs.getInt("schedule_start_minute", 0)
        val endH = prefs.getInt("schedule_end_hour", 22)
        val endM = prefs.getInt("schedule_end_minute", 0)

        _uiState.update {
            it.copy(
                assistantName = assistantName,
                wakeName = wakeName,
                voiceGender = voiceGender,
                selectedLanguage = selectedLanguage,
                currentLanguage = selectedLanguage,
                isHandsFreeEnabled = isHandsFree,
                availabilityMode = availabilityMode,
                scheduleStartHour = startH,
                scheduleStartMinute = startM,
                scheduleEndHour = endH,
                scheduleEndMinute = endM,
                responseText = LanguageManager.getWakeIdlePrompt(wakeName, selectedLanguage)
            )
        }
    }

    private fun initSpeechManager() {
        speechManager = SpeechManager(
            context = context,
            onSpeechResult = { recognizedText ->
                handleSpeechResult(recognizedText)
            },
            onListeningStateChanged = { listening ->
                _uiState.update { it.copy(isListening = listening) }
            },
            onError = { errorMessage ->
                handleSpeechError(errorMessage)
            },
            onRmsChangedCallback = { rms ->
                _uiState.update { it.copy(rmsLevel = rms) }
            }
        )

        speechManager?.updateVoiceGender(_uiState.value.voiceGender)
        speechManager?.updateLanguage(_uiState.value.selectedLanguage)
        speechManager?.setContinuousListeningEnabled(_uiState.value.isHandsFreeEnabled)
    }

    private fun handleSpeechError(errorMessage: String) {
        Log.w(TAG, "Speech error: $errorMessage")
        val currentMode = _uiState.value.listeningMode
        if (currentMode == AssistantListeningMode.COMMAND_LISTENING) {
            returnToWakeListening()
        }
    }

    fun onMicClicked() {
        toggleListening()
    }

    /**
     * Toggles speech listening manually when the microphone button is pressed.
     */
    fun toggleListening() {
        if (!_uiState.value.hasAllPermissions) {
            _uiState.update {
                it.copy(responseText = "Please grant Microphone, Contacts, and Phone permissions first.")
            }
            return
        }

        if (AssistantServiceBridge.serviceState.value.isServiceRunning) {
            AssistantServiceBridge.postAction(ServiceUserAction.ToggleListening)
            return
        }

        commandTimeoutJob?.cancel()

        val currentMode = _uiState.value.listeningMode
        if (_uiState.value.isListening && (currentMode == AssistantListeningMode.COMMAND_LISTENING || currentMode == AssistantListeningMode.DISAMBIGUATION_LISTENING)) {
            // Cancel active listening and return to wake listening
            returnToWakeListening()
        } else {
            // Manual activation directly enters COMMAND_LISTENING
            _uiState.update {
                it.copy(
                    listeningMode = AssistantListeningMode.COMMAND_LISTENING,
                    recognizedText = "",
                    responseText = "Listening... Speak now",
                    multipleMatches = emptyList(),
                    disambiguationOptions = emptyList()
                )
            }
            speechManager?.startCommandListening(_uiState.value.selectedLanguage)
            startCommandTimeoutJob(8000L)
        }
    }

    fun handleSpeechResult(rawText: String) {
        // Immediate cancellation & stop handling (Highest Priority: interrupt TTS & speech immediately)
        if (LanguageManager.isCancelOrStopPhrase(rawText)) {
            commandTimeoutJob?.cancel()
            speechManager?.stopSpeaking()
            speechManager?.stopListening()
            val lang = _uiState.value.selectedLanguage
            val cancelledMsg = LanguageManager.getCancelledMessage(lang)
            _uiState.update {
                it.copy(
                    listeningMode = AssistantListeningMode.WAKE_LISTENING,
                    recognizedText = rawText,
                    responseText = cancelledMsg,
                    multipleMatches = emptyList(),
                    disambiguationOptions = emptyList(),
                    isListening = false,
                    rmsLevel = 0f
                )
            }
            if (_uiState.value.hasAllPermissions && _uiState.value.isHandsFreeEnabled) {
                speechManager?.startWakeListening(_uiState.value.selectedLanguage)
            }
            return
        }

        val currentMode = _uiState.value.listeningMode
        val preferredLang = _uiState.value.selectedLanguage
        val configuredWake = _uiState.value.wakeName

        when (currentMode) {
            AssistantListeningMode.WAKE_LISTENING -> {
                // Lightweight local wake name detection
                val wakeMatch = WakeNameDetector.checkWakeName(rawText, configuredWake)
                if (!wakeMatch.isWakeWordDetected) {
                    // Ignore background conversation without wake word
                    if (_uiState.value.hasAllPermissions && _uiState.value.isHandsFreeEnabled) {
                        speechManager?.scheduleRestartListening(preferredLang, 800L)
                    }
                    return
                }

                // Wake name detected!
                _uiState.update {
                    it.copy(
                        recognizedText = rawText,
                        multipleMatches = emptyList(),
                        disambiguationOptions = emptyList()
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
            }
            AssistantListeningMode.DISAMBIGUATION_LISTENING -> {
                commandTimeoutJob?.cancel()
                _uiState.update { it.copy(recognizedText = rawText) }

                val options = _uiState.value.disambiguationOptions
                val lang = _uiState.value.currentLanguage
                val resolved = DisambiguationResolver.resolveOption(rawText, options, lang)

                if (resolved != null) {
                    initiateCall(resolved.contactName, resolved.number, lang)
                } else {
                    // Check if the user is giving a brand new command instead
                    val parsedNew = VoiceCommandParser.parse(rawText, preferredLang)
                    if (parsedNew is ParsedVoiceCommand.CallContact) {
                        _uiState.update {
                            it.copy(
                                listeningMode = AssistantListeningMode.COMMAND_LISTENING,
                                multipleMatches = emptyList(),
                                disambiguationOptions = emptyList()
                            )
                        }
                        executeParsedCommand(rawText)
                    } else {
                        // Could not match voice option, prompt again
                        val retryMsg = when (lang) {
                            SupportedLanguage.BENGALI -> "বুঝতে পারিনি। ১ নম্বর নাকি ২ নম্বর?"
                            SupportedLanguage.HINDI -> "समझ नहीं आया। एक नंबर या दो नंबर?"
                            else -> "Could not understand. Option 1 or Option 2?"
                        }
                        _uiState.update { it.copy(responseText = retryMsg) }
                        speechManager?.speak(retryMsg, lang) {
                            if (_uiState.value.listeningMode == AssistantListeningMode.DISAMBIGUATION_LISTENING) {
                                speechManager?.startCommandListening(lang)
                                startCommandTimeoutJob(8000L)
                            }
                        }
                    }
                }
            }
            AssistantListeningMode.COMMAND_LISTENING, AssistantListeningMode.INACTIVE -> {
                commandTimeoutJob?.cancel()
                _uiState.update {
                    it.copy(recognizedText = rawText)
                }
                executeParsedCommand(rawText)
            }
        }
    }

    private fun executeParsedCommand(commandText: String) {
        val preferredLang = _uiState.value.selectedLanguage
        val parsed = VoiceCommandParser.parse(commandText, preferredLang)
        val responseLanguage = preferredLang
        lastDetectedLanguage = responseLanguage

        _uiState.update {
            it.copy(
                currentLanguage = responseLanguage
            )
        }

        viewModelScope.launch {
            when (parsed) {
                is ParsedVoiceCommand.CallContact -> {
                    processCallContact(parsed.targetName, responseLanguage)
                }
                is ParsedVoiceCommand.Unknown -> {
                    val cleaned = BengaliHindiEnglishMatcher.cleanContactQuery(commandText)
                    val target = if (cleaned.isNotEmpty()) cleaned else commandText
                    processCallContact(target, responseLanguage)
                }
            }
        }
    }

    private fun processCallContact(targetName: String, language: SupportedLanguage) {
        when (val matchResult = contactManager.findBestContactMatch(targetName, targetName)) {
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
                            multipleMatches = emptyList(),
                            disambiguationOptions = emptyList()
                        )
                    }
                    speechManager?.speak(message, language) {
                        returnToWakeListeningDelayed(2000L)
                    }
                }
            }
            is ContactMatchResult.DisambiguationRequired -> {
                val limitedOptions = matchResult.options.take(3)
                val spokenPrompt = LanguageManager.formatMultiNumberDisambiguationPrompt(
                    matchResult.contactName,
                    limitedOptions,
                    language
                )
                _uiState.update {
                    it.copy(
                        listeningMode = AssistantListeningMode.DISAMBIGUATION_LISTENING,
                        responseText = spokenPrompt,
                        disambiguationOptions = limitedOptions,
                        multipleMatches = emptyList()
                    )
                }
                speechManager?.speak(spokenPrompt, language) {
                    if (_uiState.value.listeningMode == AssistantListeningMode.DISAMBIGUATION_LISTENING) {
                        speechManager?.startCommandListening(language)
                        startCommandTimeoutJob(12000L)
                    }
                }
                speechManager?.startCommandListening(language)
                startCommandTimeoutJob(12000L)
            }
            is ContactMatchResult.MultipleMatches -> {
                val limitedContacts = matchResult.contacts.take(3)
                val options = limitedContacts.mapIndexed { idx, c ->
                    PhoneNumberOption(
                        number = c.primaryPhoneNumber,
                        label = "",
                        lastFourDigits = "",
                        optionIndex = idx + 1,
                        contactName = c.name
                    )
                }
                val spokenPrompt = LanguageManager.formatMultiContactDisambiguationPrompt(
                    targetName,
                    options,
                    language
                )
                _uiState.update {
                    it.copy(
                        listeningMode = AssistantListeningMode.DISAMBIGUATION_LISTENING,
                        responseText = spokenPrompt,
                        disambiguationOptions = options,
                        multipleMatches = limitedContacts
                    )
                }
                speechManager?.speak(spokenPrompt, language) {
                    if (_uiState.value.listeningMode == AssistantListeningMode.DISAMBIGUATION_LISTENING) {
                        speechManager?.startCommandListening(language)
                        startCommandTimeoutJob(12000L)
                    }
                }
                speechManager?.startCommandListening(language)
                startCommandTimeoutJob(12000L)
            }
            is ContactMatchResult.NoMatch -> {
                val message = LanguageManager.getNoMatchMessage(targetName, language)
                _uiState.update {
                    it.copy(
                        responseText = message,
                        multipleMatches = emptyList(),
                        disambiguationOptions = emptyList()
                    )
                }
                speechManager?.speak(message, language) {
                    returnToWakeListeningDelayed(2000L)
                }
            }
        }
    }

    fun selectContactToCall(contact: Contact, selectedPhone: String? = null) {
        if (AssistantServiceBridge.serviceState.value.isServiceRunning) {
            AssistantServiceBridge.postAction(ServiceUserAction.SelectContact(contact, selectedPhone))
            return
        }
        commandTimeoutJob?.cancel()
        val phoneNumber = selectedPhone ?: contact.primaryPhoneNumber
        val lang = lastDetectedLanguage
        if (phoneNumber.isNotEmpty()) {
            initiateCall(contact.name, phoneNumber, lang)
        } else {
            val message = LanguageManager.getNoPhoneNumberMessage(contact.name, lang)
            _uiState.update { it.copy(responseText = message, multipleMatches = emptyList(), disambiguationOptions = emptyList()) }
            speechManager?.speak(message, lang) {
                returnToWakeListeningDelayed(2000L)
            }
        }
    }

    fun selectOptionToCall(option: PhoneNumberOption) {
        if (AssistantServiceBridge.serviceState.value.isServiceRunning) {
            AssistantServiceBridge.postAction(ServiceUserAction.SelectOption(option))
            return
        }
        commandTimeoutJob?.cancel()
        val lang = lastDetectedLanguage
        if (option.number.isNotEmpty()) {
            initiateCall(option.contactName, option.number, lang)
        }
    }

    private fun initiateCall(contactName: String, phoneNumber: String, language: SupportedLanguage) {
        commandTimeoutJob?.cancel()
        speechManager?.stopSpeaking()
        speechManager?.stopListening()

        val message = LanguageManager.getCallingMessage(contactName, language)
        _uiState.update {
            it.copy(
                listeningMode = AssistantListeningMode.WAKE_LISTENING,
                recognizedText = "",
                responseText = message,
                multipleMatches = emptyList(),
                disambiguationOptions = emptyList(),
                isListening = false,
                rmsLevel = 0f
            )
        }

        val result = callManager.makePhoneCall(phoneNumber)
        result.onSuccess {
            if (_uiState.value.hasAllPermissions && _uiState.value.isHandsFreeEnabled) {
                speechManager?.startWakeListening(_uiState.value.selectedLanguage)
            }
        }
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
            val currentMode = _uiState.value.listeningMode
            if (currentMode == AssistantListeningMode.COMMAND_LISTENING || currentMode == AssistantListeningMode.DISAMBIGUATION_LISTENING) {
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
        if (AssistantServiceBridge.serviceState.value.isServiceRunning) {
            AssistantServiceBridge.postAction(ServiceUserAction.ReturnToWakeListening)
            return
        }
        commandTimeoutJob?.cancel()
        val wakePrompt = LanguageManager.getWakeIdlePrompt(_uiState.value.wakeName, _uiState.value.selectedLanguage)
        _uiState.update {
            it.copy(
                listeningMode = AssistantListeningMode.WAKE_LISTENING,
                responseText = wakePrompt,
                multipleMatches = emptyList(),
                disambiguationOptions = emptyList(),
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
            startForegroundServiceIfAllowed()
            if (!AssistantServiceBridge.serviceState.value.isServiceRunning && _uiState.value.isHandsFreeEnabled) {
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

    fun startForegroundServiceIfAllowed() {
        if (_uiState.value.hasAllPermissions) {
            try {
                AssistantForegroundService.startService(context)
            } catch (e: Exception) {
                Log.w(TAG, "Could not start AssistantForegroundService", e)
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
        isHandsFree: Boolean = true,
        availabilityMode: AssistantAvailabilityMode = _uiState.value.availabilityMode,
        scheduleStartHour: Int = _uiState.value.scheduleStartHour,
        scheduleStartMinute: Int = _uiState.value.scheduleStartMinute,
        scheduleEndHour: Int = _uiState.value.scheduleEndHour,
        scheduleEndMinute: Int = _uiState.value.scheduleEndMinute
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
            .putString("availability_mode", availabilityMode.name)
            .putInt("schedule_start_hour", scheduleStartHour)
            .putInt("schedule_start_minute", scheduleStartMinute)
            .putInt("schedule_end_hour", scheduleEndHour)
            .putInt("schedule_end_minute", scheduleEndMinute)
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
                availabilityMode = availabilityMode,
                scheduleStartHour = scheduleStartHour,
                scheduleStartMinute = scheduleStartMinute,
                scheduleEndHour = scheduleEndHour,
                scheduleEndMinute = scheduleEndMinute,
                responseText = newPrompt,
                isSettingsOpen = false
            )
        }

        // Notify running foreground service of new configuration
        AssistantServiceBridge.postAction(ServiceUserAction.ReloadSettings)
        startForegroundServiceIfAllowed()

        if (!AssistantServiceBridge.serviceState.value.isServiceRunning) {
            if (_uiState.value.hasAllPermissions && isHandsFree) {
                speechManager?.startWakeListening(language)
            } else {
                speechManager?.stopListening()
            }
        }
    }

    fun saveSettings(
        name: String,
        wakeName: String,
        voiceGender: VoiceGender,
        language: SupportedLanguage,
        isHandsFree: Boolean
    ) {
        saveSettings(
            name = name,
            wakeName = wakeName,
            voiceGender = voiceGender,
            language = language,
            isHandsFree = isHandsFree,
            availabilityMode = _uiState.value.availabilityMode,
            scheduleStartHour = _uiState.value.scheduleStartHour,
            scheduleStartMinute = _uiState.value.scheduleStartMinute,
            scheduleEndHour = _uiState.value.scheduleEndHour,
            scheduleEndMinute = _uiState.value.scheduleEndMinute
        )
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


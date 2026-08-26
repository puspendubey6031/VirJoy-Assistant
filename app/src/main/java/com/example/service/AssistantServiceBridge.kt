package com.example.service

import com.example.model.AssistantAvailabilityMode
import com.example.model.AssistantListeningMode
import com.example.model.Contact
import com.example.model.PhoneNumberOption
import com.example.model.SupportedLanguage
import com.example.model.VoiceGender
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class ServiceUserAction {
    object ToggleListening : ServiceUserAction()
    data class SelectContact(val contact: Contact, val phoneNumber: String?) : ServiceUserAction()
    data class SelectOption(val option: PhoneNumberOption) : ServiceUserAction()
    object ReloadSettings : ServiceUserAction()
    data class SetAvailabilityMode(val mode: AssistantAvailabilityMode) : ServiceUserAction()
    object ReturnToWakeListening : ServiceUserAction()
}

data class ServiceState(
    val isServiceRunning: Boolean = false,
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
    val rmsLevel: Float = 0f
)

object AssistantServiceBridge {
    private val _serviceState = MutableStateFlow(ServiceState())
    val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

    private val _userActions = MutableSharedFlow<ServiceUserAction>(extraBufferCapacity = 16)
    val userActions: SharedFlow<ServiceUserAction> = _userActions.asSharedFlow()

    fun updateState(transform: (ServiceState) -> ServiceState) {
        _serviceState.value = transform(_serviceState.value)
    }

    fun postAction(action: ServiceUserAction) {
        _userActions.tryEmit(action)
    }
}

package com.example.model

data class Contact(
    val id: String,
    val name: String,
    val phoneNumbers: List<String>
) {
    val primaryPhoneNumber: String
        get() = phoneNumbers.firstOrNull() ?: ""
}

sealed class ContactMatchResult {
    data class SingleMatch(val contact: Contact, val phoneNumber: String) : ContactMatchResult()
    data class MultipleMatches(val contacts: List<Contact>) : ContactMatchResult()
    object NoMatch : ContactMatchResult()
}

enum class VoiceGender {
    MALE,
    FEMALE
}

enum class AssistantListeningMode {
    INACTIVE,
    WAKE_LISTENING,
    COMMAND_LISTENING
}

data class AssistantSettings(
    val assistantName: String = "VirJoy Assistant",
    val wakeName: String = "VirJoy",
    val voiceGender: VoiceGender = VoiceGender.FEMALE,
    val selectedLanguage: SupportedLanguage = SupportedLanguage.ENGLISH,
    val isHandsFreeEnabled: Boolean = true
)

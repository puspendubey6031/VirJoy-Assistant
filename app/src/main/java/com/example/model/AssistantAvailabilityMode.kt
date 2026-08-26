package com.example.model

/**
 * Modes controlling when VirJoy Assistant actively listens for the wake name in the background.
 */
enum class AssistantAvailabilityMode(val displayName: String, val description: String) {
    ACTIVE("Active", "Background wake listening remains active"),
    SLEEP("Sleep", "Completely stop passive wake listening"),
    SCHEDULED("Scheduled", "Active only during scheduled hours"),
    HOURS_24("24 Hours", "Active continuously 24/7");

    companion object {
        fun fromString(value: String): AssistantAvailabilityMode {
            return try {
                valueOf(value)
            } catch (e: Exception) {
                ACTIVE
            }
        }
    }
}

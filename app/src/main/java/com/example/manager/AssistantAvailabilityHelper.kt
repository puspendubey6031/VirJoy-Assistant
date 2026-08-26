package com.example.manager

import com.example.model.AssistantAvailabilityMode
import java.util.Calendar

object AssistantAvailabilityHelper {

    /**
     * Checks if current time is within [startHour:startMinute] to [endHour:endMinute].
     * Properly supports ranges that cross midnight (e.g., 22:00 to 06:00).
     */
    fun isScheduledTimeActive(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        calendar: Calendar = Calendar.getInstance()
    ): Boolean {
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute

        return if (startMinutes <= endMinutes) {
            // Same-day window: e.g. 08:00 to 22:00
            currentMinutes in startMinutes..endMinutes
        } else {
            // Overnight window: e.g. 22:00 to 07:00
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }

    /**
     * Determines whether background listening should be active based on mode and schedule.
     */
    fun isListeningAllowed(
        mode: AssistantAvailabilityMode,
        startHour: Int = 8,
        startMinute: Int = 0,
        endHour: Int = 22,
        endMinute: Int = 0,
        calendar: Calendar = Calendar.getInstance()
    ): Boolean {
        return when (mode) {
            AssistantAvailabilityMode.SLEEP -> false
            AssistantAvailabilityMode.ACTIVE,
            AssistantAvailabilityMode.HOURS_24 -> true
            AssistantAvailabilityMode.SCHEDULED -> isScheduledTimeActive(startHour, startMinute, endHour, endMinute, calendar)
        }
    }
}

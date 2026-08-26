package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.AssistantAvailabilityMode
import com.example.service.AssistantForegroundService

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received broadcast action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val prefs = context.getSharedPreferences("virjoy_prefs", Context.MODE_PRIVATE)
            val modeStr = prefs.getString("availability_mode", AssistantAvailabilityMode.ACTIVE.name) ?: AssistantAvailabilityMode.ACTIVE.name
            val mode = AssistantAvailabilityMode.fromString(modeStr)
            val isHandsFree = prefs.getBoolean("is_handsfree_enabled", true)

            // If Active, Scheduled, or 24 Hours is selected, restore the assistant service
            if (isHandsFree && (mode == AssistantAvailabilityMode.ACTIVE || mode == AssistantAvailabilityMode.SCHEDULED || mode == AssistantAvailabilityMode.HOURS_24)) {
                try {
                    val serviceIntent = Intent(context, AssistantForegroundService::class.java).apply {
                        this.action = AssistantForegroundService.ACTION_START
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                    Log.i(TAG, "Restored AssistantForegroundService on boot (mode: $mode)")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start AssistantForegroundService on boot", e)
                }
            }
        }
    }
}

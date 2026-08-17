package com.example.manager

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat

class CallManager(private val context: Context) {

    companion object {
        private const val TAG = "CallManager"
    }

    /**
     * Initiates a real phone call using Intent.ACTION_CALL.
     * Requires android.permission.CALL_PHONE.
     */
    fun makePhoneCall(phoneNumber: String): Result<Unit> {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "").trim()
        if (cleanNumber.isEmpty()) {
            return Result.failure(IllegalArgumentException("Phone number is invalid or empty"))
        }

        // Check CALL_PHONE permission
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(TAG, "CALL_PHONE permission is missing")
            return Result.failure(SecurityException("CALL_PHONE permission is not granted"))
        }

        return try {
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$cleanNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(callIntent)
            Log.i(TAG, "Real call intent dispatched for number: $cleanNumber")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start call intent", e)
            Result.failure(e)
        }
    }
}

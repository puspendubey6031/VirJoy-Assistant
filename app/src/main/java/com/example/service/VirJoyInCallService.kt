package com.example.service

import android.telecom.Call
import android.telecom.InCallService
import android.util.Log

class VirJoyInCallService : InCallService() {

    companion object {
        private const val TAG = "VirJoyInCallService"

        @Volatile
        var activeCall: Call? = null
            private set
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        activeCall = call

        Log.i(
            TAG,
            "CALL ADDED: state=${call.state}, details=${call.details}"
        )

        call.registerCallback(object : Call.Callback() {

            override fun onStateChanged(
                call: Call,
                state: Int
            ) {
                Log.i(TAG, "CALL STATE CHANGED: $state")

                when (state) {
                    Call.STATE_RINGING -> {
                        Log.i(TAG, "INCOMING CALL IS RINGING")
                    }

                    Call.STATE_ACTIVE -> {
                        Log.i(TAG, "CALL IS ACTIVE")
                    }

                    Call.STATE_DISCONNECTED -> {
                        Log.i(TAG, "CALL DISCONNECTED")
                        if (activeCall == call) {
                            activeCall = null
                        }
                        call.unregisterCallback(this)
                    }
                }
            }
        })

        if (call.state == Call.STATE_RINGING) {
            Log.i(TAG, "INCOMING CALL DETECTED")
        }
    }

    override fun onCallRemoved(call: Call) {
        Log.i(TAG, "CALL REMOVED")

        if (activeCall == call) {
            activeCall = null
        }

        super.onCallRemoved(call)
    }

    override fun onDestroy() {
        activeCall = null
        Log.i(TAG, "VirJoyInCallService destroyed")
        super.onDestroy()
    }
}

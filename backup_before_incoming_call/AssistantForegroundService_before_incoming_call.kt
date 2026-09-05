package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.manager.AssistantAvailabilityHelper
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AssistantForegroundService : Service() {

    companion object {
        private const val TAG = "AssistantService"
        const val CHANNEL_ID = "virjoy_assistant_foreground_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.action.START_SERVICE"
        const val ACTION_STOP = "com.example.action.STOP_SERVICE"
        const val ACTION_TOGGLE_SLEEP = "com.example.action.TOGGLE_SLEEP"
        const val ACTION_RELOAD_SETTINGS = "com.example.action.RELOAD_SETTINGS"

        fun startService(context: Context) {
            val intent = Intent(context, AssistantForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AssistantForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var wakeLock: PowerManager.WakeLock? = null
    private var speechManager: SpeechManager? = null
    private lateinit var contactManager: ContactManager
    private lateinit var callManager: CallManager

    private var assistantName: String = "VirJoy Assistant"
    private var wakeName: String = "VirJoy"
    private var voiceGender: VoiceGender = VoiceGender.FEMALE
    private var selectedLanguage: SupportedLanguage = SupportedLanguage.ENGLISH
    private var isHandsFreeEnabled: Boolean = true
    private var availabilityMode: AssistantAvailabilityMode = AssistantAvailabilityMode.ACTIVE
    private var scheduleStartHour: Int = 8
    private var scheduleStartMinute: Int = 0
    private var scheduleEndHour: Int = 22
    private var scheduleEndMinute: Int = 0

    private var currentListeningMode: AssistantListeningMode = AssistantListeningMode.INACTIVE
    private var currentLanguage: SupportedLanguage = SupportedLanguage.ENGLISH
    private var commandTimeoutJob: Job? = null

    private var telephonyManager: TelephonyManager? = null
    private var isCallActive = false
    private var telephonyCallback: Any? = null
    private var phoneStateListener: Any? = null

    private val scheduleCheckRunnable = object : Runnable {
        override fun run() {
            evaluateAvailabilitySchedule()
            mainHandler.postDelayed(this, 30000L) // Re-check schedule every 30s
        }
    }

    override fun onCreate() {
        super.onCreate()
        contactManager = ContactManager(this)
        callManager = CallManager(this)

        acquireWakeLock()
        createNotificationChannel()
        loadSettingsFromPrefs()
        initSpeechManager()
        registerCallStateListener()

        // Start Foreground immediately
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        observeBridgeUserActions()
        mainHandler.post(scheduleCheckRunnable)

        AssistantServiceBridge.updateState {
            it.copy(
                isServiceRunning = true,
                assistantName = assistantName,
                wakeName = wakeName,
                voiceGender = voiceGender,
                selectedLanguage = selectedLanguage,
                isHandsFreeEnabled = isHandsFreeEnabled,
                availabilityMode = availabilityMode,
                scheduleStartHour = scheduleStartHour,
                scheduleStartMinute = scheduleStartMinute,
                scheduleEndHour = scheduleEndHour,
                scheduleEndMinute = scheduleEndMinute,
                currentLanguage = selectedLanguage,
                responseText = LanguageManager.getWakeIdlePrompt(wakeName, selectedLanguage)
            )
        }

        evaluateAvailabilitySchedule()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_SLEEP -> {
                val newMode = if (availabilityMode == AssistantAvailabilityMode.SLEEP) {
                    AssistantAvailabilityMode.ACTIVE
                } else {
                    AssistantAvailabilityMode.SLEEP
                }
                updateAvailabilityMode(newMode)
            }
            ACTION_RELOAD_SETTINGS -> {
                loadSettingsFromPrefs()
                applySettingsToSpeechManager()
                evaluateAvailabilitySchedule()
            }
            ACTION_START, null -> {
                loadSettingsFromPrefs()
                evaluateAvailabilitySchedule()
            }
        }
        updateNotification()
        return START_STICKY
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "VirJoy:AssistantBackgroundWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(24 * 60 * 60 * 1000L) // 24h safety timeout
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing WakeLock", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VirJoy Assistant Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps VirJoy Assistant hands-free wake word listening active in the background"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleSleepIntent = Intent(this, AssistantForegroundService::class.java).apply {
            action = ACTION_TOGGLE_SLEEP
        }
        val toggleSleepPendingIntent = PendingIntent.getService(
            this,
            1,
            toggleSleepIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AssistantForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = when (availabilityMode) {
            AssistantAvailabilityMode.SLEEP -> "Sleeping (Passive listening paused)"
            AssistantAvailabilityMode.SCHEDULED -> {
                val active = AssistantAvailabilityHelper.isScheduledTimeActive(
                    scheduleStartHour, scheduleStartMinute, scheduleEndHour, scheduleEndMinute
                )
                if (active) "Active (Schedule: %02d:%02d - %02d:%02d)".format(scheduleStartHour, scheduleStartMinute, scheduleEndHour, scheduleEndMinute)
                else "Outside scheduled hours (Paused)"
            }
            AssistantAvailabilityMode.HOURS_24 -> "24 Hours Active (Listening for \"$wakeName\")"
            AssistantAvailabilityMode.ACTIVE -> "Listening for \"$wakeName\""
        }

        val sleepActionTitle = if (availabilityMode == AssistantAvailabilityMode.SLEEP) "Resume" else "Sleep"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VirJoy Assistant is active")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_pause, sleepActionTitle, toggleSleepPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, buildForegroundNotification())
    }

    private fun loadSettingsFromPrefs() {
        val prefs = getSharedPreferences("virjoy_prefs", Context.MODE_PRIVATE)
        assistantName = prefs.getString("assistant_name", "VirJoy Assistant") ?: "VirJoy Assistant"
        wakeName = prefs.getString("wake_name", "VirJoy") ?: "VirJoy"
        val voiceGenderStr = prefs.getString("voice_gender", VoiceGender.FEMALE.name) ?: VoiceGender.FEMALE.name
        voiceGender = try {
            VoiceGender.valueOf(voiceGenderStr)
        } catch (e: Exception) {
            VoiceGender.FEMALE
        }
        val languageStr = prefs.getString("selected_language", SupportedLanguage.ENGLISH.name) ?: SupportedLanguage.ENGLISH.name
        selectedLanguage = try {
            SupportedLanguage.valueOf(languageStr)
        } catch (e: Exception) {
            SupportedLanguage.ENGLISH
        }
        currentLanguage = selectedLanguage
        isHandsFreeEnabled = prefs.getBoolean("is_handsfree_enabled", true)
        val modeStr = prefs.getString("availability_mode", AssistantAvailabilityMode.ACTIVE.name) ?: AssistantAvailabilityMode.ACTIVE.name
        availabilityMode = AssistantAvailabilityMode.fromString(modeStr)
        scheduleStartHour = prefs.getInt("schedule_start_hour", 8)
        scheduleStartMinute = prefs.getInt("schedule_start_minute", 0)
        scheduleEndHour = prefs.getInt("schedule_end_hour", 22)
        scheduleEndMinute = prefs.getInt("schedule_end_minute", 0)

        AssistantServiceBridge.updateState {
            it.copy(
                assistantName = assistantName,
                wakeName = wakeName,
                voiceGender = voiceGender,
                selectedLanguage = selectedLanguage,
                currentLanguage = selectedLanguage,
                isHandsFreeEnabled = isHandsFreeEnabled,
                availabilityMode = availabilityMode,
                scheduleStartHour = scheduleStartHour,
                scheduleStartMinute = scheduleStartMinute,
                scheduleEndHour = scheduleEndHour,
                scheduleEndMinute = scheduleEndMinute
            )
        }
    }

    private fun initSpeechManager() {
        speechManager = SpeechManager(
            context = this,
            onSpeechResult = { text -> handleSpeechResult(text) },
            onListeningStateChanged = { listening ->
                AssistantServiceBridge.updateState { it.copy(isListening = listening) }
            },
            onError = { err -> handleSpeechError(err) },
            onRmsChangedCallback = { rms ->
                AssistantServiceBridge.updateState { it.copy(rmsLevel = rms) }
            }
        )
        applySettingsToSpeechManager()
    }

    private fun applySettingsToSpeechManager() {
        speechManager?.updateVoiceGender(voiceGender)
        speechManager?.updateLanguage(selectedLanguage)
        speechManager?.setContinuousListeningEnabled(isHandsFreeEnabled)
    }

    private fun updateAvailabilityMode(mode: AssistantAvailabilityMode) {
        availabilityMode = mode
        val prefs = getSharedPreferences("virjoy_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("availability_mode", mode.name).apply()

        AssistantServiceBridge.updateState { it.copy(availabilityMode = mode) }
        evaluateAvailabilitySchedule()
        updateNotification()
    }

    private fun evaluateAvailabilitySchedule() {
        if (isCallActive) {
            // Call active guard: Never start recognizer during active phone call
            return
        }

        val allowed = AssistantAvailabilityHelper.isListeningAllowed(
            mode = availabilityMode,
            startHour = scheduleStartHour,
            startMinute = scheduleStartMinute,
            endHour = scheduleEndHour,
            endMinute = scheduleEndMinute
        )

        if (allowed && isHandsFreeEnabled) {
            if (currentListeningMode == AssistantListeningMode.INACTIVE || currentListeningMode == AssistantListeningMode.WAKE_LISTENING) {
                startSilentWakeListening()
            }
        } else {
            // Sleep or outside schedule: pause listening completely
            if (currentListeningMode != AssistantListeningMode.INACTIVE) {
                currentListeningMode = AssistantListeningMode.INACTIVE
                speechManager?.stopListening()
                val prompt = when (availabilityMode) {
                    AssistantAvailabilityMode.SLEEP -> "Assistant is in Sleep mode. Passive wake listening paused."
                    AssistantAvailabilityMode.SCHEDULED -> "Outside scheduled hours (%02d:%02d - %02d:%02d). Passive listening paused.".format(
                        scheduleStartHour, scheduleStartMinute, scheduleEndHour, scheduleEndMinute
                    )
                    else -> "Hands-Free listening is paused."
                }
                AssistantServiceBridge.updateState {
                    it.copy(
                        listeningMode = AssistantListeningMode.INACTIVE,
                        isListening = false,
                        responseText = prompt
                    )
                }
            }
        }
        updateNotification()
    }

    private fun registerCallStateListener() {
        try {
            telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        onPhoneCallStateChanged(state)
                    }
                }
                telephonyCallback = callback
                telephonyManager?.registerTelephonyCallback(mainExecutor, callback)
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        onPhoneCallStateChanged(state)
                    }
                }
                phoneStateListener = listener
                @Suppress("DEPRECATION")
                telephonyManager?.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not register call state listener: ${e.localizedMessage}")
        }
    }

    private fun unregisterCallStateListener() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (telephonyCallback as? TelephonyCallback)?.let {
                    telephonyManager?.unregisterTelephonyCallback(it)
                }
                telephonyCallback = null
            } else {
                @Suppress("DEPRECATION")
                (phoneStateListener as? PhoneStateListener)?.let {
                    telephonyManager?.listen(it, PhoneStateListener.LISTEN_NONE)
                }
                phoneStateListener = null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering call state listener", e)
        }
    }

    private fun onPhoneCallStateChanged(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK, TelephonyManager.CALL_STATE_RINGING -> {
                isCallActive = true
                speechManager?.setCallActive(true)
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (isCallActive) {
                    isCallActive = false
                    speechManager?.setCallActive(false)
                    handleCallEndedResetAndResume()
                }
            }
        }
    }

    private fun handleCallEndedResetAndResume() {
        commandTimeoutJob?.cancel()
        speechManager?.stopSpeaking()
        speechManager?.stopListening()
        currentListeningMode = AssistantListeningMode.WAKE_LISTENING

        AssistantServiceBridge.updateState {
            it.copy(
                listeningMode = AssistantListeningMode.WAKE_LISTENING,
                recognizedText = "",
                responseText = LanguageManager.getWakeIdlePrompt(wakeName, selectedLanguage),
                multipleMatches = emptyList(),
                disambiguationOptions = emptyList(),
                isListening = false,
                rmsLevel = 0f
            )
        }

        evaluateAvailabilitySchedule()
    }

    private fun startSilentWakeListening() {
        commandTimeoutJob?.cancel()
        currentListeningMode = AssistantListeningMode.WAKE_LISTENING
        val prompt = LanguageManager.getWakeIdlePrompt(wakeName, selectedLanguage)

        AssistantServiceBridge.updateState {
            it.copy(
                listeningMode = AssistantListeningMode.WAKE_LISTENING,
                responseText = prompt,
                multipleMatches = emptyList(),
                disambiguationOptions = emptyList(),
                isListening = false,
                rmsLevel = 0f
            )
        }

        speechManager?.startWakeListening(selectedLanguage)
    }

    private fun observeBridgeUserActions() {
        AssistantServiceBridge.userActions.onEach { action ->
            when (action) {
                is ServiceUserAction.ToggleListening -> toggleManualListening()
                is ServiceUserAction.SelectContact -> selectContactToCall(action.contact, action.phoneNumber)
                is ServiceUserAction.SelectOption -> selectOptionToCall(action.option)
                is ServiceUserAction.ReloadSettings -> {
                    loadSettingsFromPrefs()
                    applySettingsToSpeechManager()
                    evaluateAvailabilitySchedule()
                }
                is ServiceUserAction.SetAvailabilityMode -> updateAvailabilityMode(action.mode)
                is ServiceUserAction.ReturnToWakeListening -> evaluateAvailabilitySchedule()
            }
        }.launchIn(serviceScope)
    }

    private fun toggleManualListening() {
        commandTimeoutJob?.cancel()
        if (currentListeningMode == AssistantListeningMode.COMMAND_LISTENING || currentListeningMode == AssistantListeningMode.DISAMBIGUATION_LISTENING) {
            evaluateAvailabilitySchedule()
        } else {
            currentListeningMode = AssistantListeningMode.COMMAND_LISTENING
            AssistantServiceBridge.updateState {
                it.copy(
                    listeningMode = AssistantListeningMode.COMMAND_LISTENING,
                    recognizedText = "",
                    responseText = "Listening... Speak now",
                    multipleMatches = emptyList(),
                    disambiguationOptions = emptyList()
                )
            }
            speechManager?.startCommandListening(selectedLanguage)
            startCommandTimeoutJob(8000L)
        }
    }

    private fun handleSpeechError(errorMessage: String) {
        Log.w(TAG, "Service speech error: $errorMessage")
        if (currentListeningMode == AssistantListeningMode.COMMAND_LISTENING) {
            evaluateAvailabilitySchedule()
        }
    }

    private fun handleSpeechResult(rawText: String) {
        // Immediate cancellation & stop handling (Highest Priority: interrupt TTS & speech immediately)
        if (LanguageManager.isCancelOrStopPhrase(rawText)) {
            commandTimeoutJob?.cancel()
            speechManager?.stopSpeaking()
            speechManager?.stopListening()
            val cancelledMsg = LanguageManager.getCancelledMessage(selectedLanguage)
            currentListeningMode = AssistantListeningMode.WAKE_LISTENING
            AssistantServiceBridge.updateState {
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
            evaluateAvailabilitySchedule()
            return
        }

        when (currentListeningMode) {
            AssistantListeningMode.WAKE_LISTENING -> {
                // Check local wake name
                val wakeMatch = WakeNameDetector.checkWakeName(rawText, wakeName)
                if (!wakeMatch.isWakeWordDetected) {
                    // Check if availability permits passive restart
                    val allowed = AssistantAvailabilityHelper.isListeningAllowed(
                        availabilityMode, scheduleStartHour, scheduleStartMinute, scheduleEndHour, scheduleEndMinute
                    )
                    if (allowed && isHandsFreeEnabled) {
                        speechManager?.scheduleRestartListening(selectedLanguage, 800L)
                    }
                    return
                }

                // Wake name detected!
                AssistantServiceBridge.updateState {
                    it.copy(
                        recognizedText = rawText,
                        multipleMatches = emptyList(),
                        disambiguationOptions = emptyList()
                    )
                }

                if (wakeMatch.remainingCommand.isNotBlank()) {
                    currentListeningMode = AssistantListeningMode.COMMAND_LISTENING
                    AssistantServiceBridge.updateState {
                        it.copy(listeningMode = AssistantListeningMode.COMMAND_LISTENING)
                    }
                    executeParsedCommand(wakeMatch.remainingCommand)
                } else {
                    currentListeningMode = AssistantListeningMode.COMMAND_LISTENING
                    AssistantServiceBridge.updateState {
                        it.copy(
                            listeningMode = AssistantListeningMode.COMMAND_LISTENING,
                            responseText = LanguageManager.getListeningForCommandPrompt(selectedLanguage)
                        )
                    }

                    val ack = LanguageManager.getWakeAcknowledgementMessage(selectedLanguage)
                    speechManager?.speak(ack, selectedLanguage) {
                        if (currentListeningMode == AssistantListeningMode.COMMAND_LISTENING) {
                            speechManager?.startCommandListening(selectedLanguage)
                            startCommandTimeoutJob(8000L)
                        }
                    }
                }
            }
            AssistantListeningMode.DISAMBIGUATION_LISTENING -> {
                commandTimeoutJob?.cancel()
                AssistantServiceBridge.updateState { it.copy(recognizedText = rawText) }

                val options = AssistantServiceBridge.serviceState.value.disambiguationOptions
                val resolved = DisambiguationResolver.resolveOption(rawText, options, currentLanguage)

                if (resolved != null) {
                    initiateCall(resolved.contactName, resolved.number, currentLanguage)
                } else {
                    val parsedNew = VoiceCommandParser.parse(rawText, selectedLanguage)
                    if (parsedNew is ParsedVoiceCommand.CallContact) {
                        currentListeningMode = AssistantListeningMode.COMMAND_LISTENING
                        AssistantServiceBridge.updateState {
                            it.copy(
                                listeningMode = AssistantListeningMode.COMMAND_LISTENING,
                                multipleMatches = emptyList(),
                                disambiguationOptions = emptyList()
                            )
                        }
                        executeParsedCommand(rawText)
                    } else {
                        val retryMsg = when (currentLanguage) {
                            SupportedLanguage.BENGALI -> "বুঝতে পারিনি। ১ নম্বর নাকি ২ নম্বর?"
                            SupportedLanguage.HINDI -> "समझ नहीं आया। एक नंबर या दो नंबर?"
                            else -> "Could not understand. Option 1 or Option 2?"
                        }
                        AssistantServiceBridge.updateState { it.copy(responseText = retryMsg) }
                        speechManager?.speak(retryMsg, currentLanguage) {
                            if (currentListeningMode == AssistantListeningMode.DISAMBIGUATION_LISTENING) {
                                speechManager?.startCommandListening(currentLanguage)
                                startCommandTimeoutJob(8000L)
                            }
                        }
                    }
                }
            }
            AssistantListeningMode.COMMAND_LISTENING, AssistantListeningMode.INACTIVE -> {
                commandTimeoutJob?.cancel()
                AssistantServiceBridge.updateState { it.copy(recognizedText = rawText) }
                executeParsedCommand(rawText)
            }
        }
    }

    private fun executeParsedCommand(commandText: String) {
        val parsed = VoiceCommandParser.parse(commandText, selectedLanguage)
        val responseLang = selectedLanguage
        currentLanguage = responseLang

        AssistantServiceBridge.updateState { it.copy(currentLanguage = responseLang) }

        serviceScope.launch {
            when (parsed) {
                is ParsedVoiceCommand.CallContact -> processCallContact(parsed.targetName, responseLang)
                is ParsedVoiceCommand.Unknown -> {
                    val cleaned = BengaliHindiEnglishMatcher.cleanContactQuery(commandText)
                    val target = if (cleaned.isNotEmpty()) cleaned else commandText
                    processCallContact(target, responseLang)
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
                    AssistantServiceBridge.updateState {
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
                val prompt = LanguageManager.formatMultiNumberDisambiguationPrompt(
                    matchResult.contactName, limitedOptions, language
                )
                currentListeningMode = AssistantListeningMode.DISAMBIGUATION_LISTENING
                AssistantServiceBridge.updateState {
                    it.copy(
                        listeningMode = AssistantListeningMode.DISAMBIGUATION_LISTENING,
                        responseText = prompt,
                        disambiguationOptions = limitedOptions,
                        multipleMatches = emptyList()
                    )
                }
                speechManager?.speak(prompt, language) {
                    if (currentListeningMode == AssistantListeningMode.DISAMBIGUATION_LISTENING) {
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
                val prompt = LanguageManager.formatMultiContactDisambiguationPrompt(targetName, options, language)
                currentListeningMode = AssistantListeningMode.DISAMBIGUATION_LISTENING
                AssistantServiceBridge.updateState {
                    it.copy(
                        listeningMode = AssistantListeningMode.DISAMBIGUATION_LISTENING,
                        responseText = prompt,
                        disambiguationOptions = options,
                        multipleMatches = limitedContacts
                    )
                }
                speechManager?.speak(prompt, language) {
                    if (currentListeningMode == AssistantListeningMode.DISAMBIGUATION_LISTENING) {
                        speechManager?.startCommandListening(language)
                        startCommandTimeoutJob(12000L)
                    }
                }
                speechManager?.startCommandListening(language)
                startCommandTimeoutJob(12000L)
            }
            is ContactMatchResult.NoMatch -> {
                val message = LanguageManager.getNoMatchMessage(targetName, language)
                AssistantServiceBridge.updateState {
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

    private fun selectContactToCall(contact: Contact, selectedPhone: String?) {
        commandTimeoutJob?.cancel()
        val phone = selectedPhone ?: contact.primaryPhoneNumber
        if (phone.isNotEmpty()) {
            initiateCall(contact.name, phone, currentLanguage)
        } else {
            val msg = LanguageManager.getNoPhoneNumberMessage(contact.name, currentLanguage)
            AssistantServiceBridge.updateState {
                it.copy(responseText = msg, multipleMatches = emptyList(), disambiguationOptions = emptyList())
            }
            speechManager?.speak(msg, currentLanguage) {
                returnToWakeListeningDelayed(2000L)
            }
        }
    }

    private fun selectOptionToCall(option: PhoneNumberOption) {
        commandTimeoutJob?.cancel()
        if (option.number.isNotEmpty()) {
            initiateCall(option.contactName, option.number, currentLanguage)
        }
    }

    private fun initiateCall(contactName: String, phoneNumber: String, language: SupportedLanguage) {
        commandTimeoutJob?.cancel()
        speechManager?.stopSpeaking()
        speechManager?.stopListening()

        isCallActive = true
        speechManager?.setCallActive(true)

        val message = LanguageManager.getCallingMessage(contactName, language)
        currentListeningMode = AssistantListeningMode.WAKE_LISTENING
        AssistantServiceBridge.updateState {
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
            // No-op here: Call is active. TelephonyManager CALL_STATE_IDLE will trigger
            // clean reset and resume wake listening when the call finishes.
        }
        result.onFailure { error ->
            isCallActive = false
            speechManager?.setCallActive(false)
            val errorMsg = LanguageManager.getCallFailedMessage(
                contactName, error.localizedMessage ?: "Unknown error", language
            )
            AssistantServiceBridge.updateState { it.copy(responseText = errorMsg) }
            returnToWakeListeningDelayed(3000L)
        }
    }

    private fun startCommandTimeoutJob(delayMs: Long = 8000L) {
        commandTimeoutJob?.cancel()
        commandTimeoutJob = serviceScope.launch {
            delay(delayMs)
            if (currentListeningMode == AssistantListeningMode.COMMAND_LISTENING || currentListeningMode == AssistantListeningMode.DISAMBIGUATION_LISTENING) {
                evaluateAvailabilitySchedule()
            }
        }
    }

    private fun returnToWakeListeningDelayed(delayMs: Long = 2000L) {
        serviceScope.launch {
            delay(delayMs)
            evaluateAvailabilitySchedule()
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        unregisterCallStateListener()
        serviceScope.cancel()
        speechManager?.destroy()
        speechManager = null
        releaseWakeLock()
        AssistantServiceBridge.updateState {
            it.copy(
                isServiceRunning = false,
                isListening = false,
                listeningMode = AssistantListeningMode.INACTIVE
            )
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

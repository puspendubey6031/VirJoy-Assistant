package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Schedule
import com.example.model.AssistantAvailabilityMode
import com.example.model.AssistantListeningMode
import com.example.model.Contact
import com.example.model.PhoneNumberOption
import com.example.model.SupportedLanguage
import com.example.model.VoiceGender
import com.example.viewmodel.AssistantUiState
import com.example.viewmodel.AssistantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirJoyAssistantScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val requiredPermissions = remember {
        val perms = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        perms.toTypedArray()
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = requiredPermissions.all { perm ->
            results[perm] == true || ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
        viewModel.updatePermissionsState(allGranted)
    }

    LaunchedEffect(Unit) {
        val allGranted = requiredPermissions.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
        viewModel.updatePermissionsState(allGranted)
        if (!allGranted) {
            permissionsLauncher.launch(requiredPermissions)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = uiState.assistantName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openSettings() },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!uiState.hasAllPermissions) {
                PermissionRequestBanner(
                    onGrantClick = { permissionsLauncher.launch(requiredPermissions) },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                AssistantContent(
                    uiState = uiState,
                    onMicClicked = { viewModel.onMicClicked() },
                    onContactSelected = { contact -> viewModel.selectContactToCall(contact) },
                    onOptionSelected = { option -> viewModel.selectOptionToCall(option) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (uiState.isSettingsOpen) {
                SettingsDialog(
                    currentName = uiState.assistantName,
                    currentWakeName = uiState.wakeName,
                    currentVoice = uiState.voiceGender,
                    currentLanguage = uiState.selectedLanguage,
                    isHandsFree = uiState.isHandsFreeEnabled,
                    currentAvailabilityMode = uiState.availabilityMode,
                    currentStartHour = uiState.scheduleStartHour,
                    currentStartMinute = uiState.scheduleStartMinute,
                    currentEndHour = uiState.scheduleEndHour,
                    currentEndMinute = uiState.scheduleEndMinute,
                    onDismiss = { viewModel.closeSettings() },
                    onSaveFull = { name, wakeName, voice, lang, handsFree, mode, startH, startM, endH, endM ->
                        viewModel.saveSettings(
                            name = name,
                            wakeName = wakeName,
                            voiceGender = voice,
                            language = lang,
                            isHandsFree = handsFree,
                            availabilityMode = mode,
                            scheduleStartHour = startH,
                            scheduleStartMinute = startM,
                            scheduleEndHour = endH,
                            scheduleEndMinute = endM
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun AssistantContent(
    uiState: AssistantUiState,
    onMicClicked: () -> Unit,
    onContactSelected: (Contact) -> Unit,
    onOptionSelected: (PhoneNumberOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Status indicator badge & Language/Voice/WakeName badge
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            StatusBadge(
                listeningMode = uiState.listeningMode,
                isListening = uiState.isListening,
                wakeName = uiState.wakeName,
                isHandsFree = uiState.isHandsFreeEnabled,
                availabilityMode = uiState.availabilityMode
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Wake: \"${uiState.wakeName}\" • ${uiState.selectedLanguage.displayName} • ${if (uiState.voiceGender == VoiceGender.FEMALE) stringResource(R.string.voice_female) else stringResource(R.string.voice_male)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Center Response / Interaction Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (uiState.recognizedText.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Heard:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"${uiState.recognizedText}\"",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Main Assistant Response Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.responseText,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Disambiguation phone options (Voice-first or tap)
            if (uiState.disambiguationOptions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                DisambiguationOptionsList(
                    options = uiState.disambiguationOptions,
                    onOptionSelected = onOptionSelected
                )
            } else if (uiState.multipleMatches.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                MultipleContactsList(
                    contacts = uiState.multipleMatches,
                    onContactSelected = onContactSelected
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Large Microphone Button Area
        MicButtonSection(
            listeningMode = uiState.listeningMode,
            isListening = uiState.isListening,
            onClick = onMicClicked
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatusBadge(
    listeningMode: AssistantListeningMode,
    isListening: Boolean,
    wakeName: String,
    isHandsFree: Boolean,
    availabilityMode: AssistantAvailabilityMode = AssistantAvailabilityMode.ACTIVE
) {
    val backgroundColor = when {
        availabilityMode == AssistantAvailabilityMode.SLEEP -> MaterialTheme.colorScheme.surfaceVariant
        listeningMode == AssistantListeningMode.COMMAND_LISTENING -> MaterialTheme.colorScheme.errorContainer
        listeningMode == AssistantListeningMode.DISAMBIGUATION_LISTENING -> MaterialTheme.colorScheme.tertiaryContainer
        listeningMode == AssistantListeningMode.WAKE_LISTENING -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when {
        availabilityMode == AssistantAvailabilityMode.SLEEP -> MaterialTheme.colorScheme.onSurfaceVariant
        listeningMode == AssistantListeningMode.COMMAND_LISTENING -> MaterialTheme.colorScheme.onErrorContainer
        listeningMode == AssistantListeningMode.DISAMBIGUATION_LISTENING -> MaterialTheme.colorScheme.onTertiaryContainer
        listeningMode == AssistantListeningMode.WAKE_LISTENING -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val text = when {
        availabilityMode == AssistantAvailabilityMode.SLEEP -> "Sleeping (Passive listening paused)"
        listeningMode == AssistantListeningMode.COMMAND_LISTENING -> "Listening for command..."
        listeningMode == AssistantListeningMode.DISAMBIGUATION_LISTENING -> "Listening: Choose number / option..."
        listeningMode == AssistantListeningMode.WAKE_LISTENING -> {
            if (isHandsFree) "Hands-Free: Listening for \"$wakeName\" (${availabilityMode.displayName})" else "Tap Mic to speak"
        }
        else -> "Paused / Permissions Required"
    }

    val dotColor = when {
        availabilityMode == AssistantAvailabilityMode.SLEEP -> MaterialTheme.colorScheme.outline
        listeningMode == AssistantListeningMode.COMMAND_LISTENING -> MaterialTheme.colorScheme.error
        listeningMode == AssistantListeningMode.DISAMBIGUATION_LISTENING -> MaterialTheme.colorScheme.tertiary
        listeningMode == AssistantListeningMode.WAKE_LISTENING -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        shape = CircleShape,
        color = backgroundColor,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DisambiguationOptionsList(
    options: List<PhoneNumberOption>,
    onOptionSelected: (PhoneNumberOption) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Text(
            text = "Say the number option (e.g. \"1\", \"2\", \"Mobile\") or tap below:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 180.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(options, key = { "${it.contactName}_${it.optionIndex}_${it.number}" }) { option ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("option_item_${option.optionIndex}")
                        .clickable { onOptionSelected(option) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${option.optionIndex}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${option.contactName} (${option.label})",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = option.number,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        FilledIconButton(
                            onClick = { onOptionSelected(option) },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call ${option.number}"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MultipleContactsList(
    contacts: List<Contact>,
    onContactSelected: (Contact) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.multiple_contacts_found),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 180.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(contacts, key = { it.id }) { contact ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_item_${contact.id}")
                        .clickable { onContactSelected(contact) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = contact.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (contact.phoneNumbers.isNotEmpty()) {
                                    Text(
                                        text = contact.primaryPhoneNumber,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        FilledIconButton(
                            onClick = { onContactSelected(contact) },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call ${contact.name}"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MicButtonSection(
    listeningMode: AssistantListeningMode,
    isListening: Boolean,
    onClick: () -> Unit
) {
    val isActiveMode = listeningMode == AssistantListeningMode.COMMAND_LISTENING || listeningMode == AssistantListeningMode.DISAMBIGUATION_LISTENING
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isActiveMode || isListening) 1.15f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            if (isActiveMode || isListening) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            if (isActiveMode) MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                        )
                )
            }

            Surface(
                onClick = onClick,
                modifier = Modifier
                    .size(88.dp)
                    .testTag("mic_button"),
                shape = CircleShape,
                color = if (isActiveMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                shadowElevation = 6.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = if (isActiveMode) "Cancel Command Listening" else "Manual Voice Command",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isActiveMode) "Listening for command..." else "Tap for manual command",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionRequestBanner(
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.permission_required_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.permission_required_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onGrantClick,
                modifier = Modifier.testTag("grant_permissions_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(text = stringResource(R.string.grant_permissions), color = Color.White)
            }
        }
    }
}

@Composable
fun SettingsDialog(
    currentName: String,
    currentWakeName: String = "VirJoy",
    currentVoice: VoiceGender = VoiceGender.FEMALE,
    currentLanguage: SupportedLanguage = SupportedLanguage.ENGLISH,
    isHandsFree: Boolean = true,
    currentAvailabilityMode: AssistantAvailabilityMode = AssistantAvailabilityMode.ACTIVE,
    currentStartHour: Int = 8,
    currentStartMinute: Int = 0,
    currentEndHour: Int = 22,
    currentEndMinute: Int = 0,
    onDismiss: () -> Unit,
    onSaveFull: (
        name: String,
        wakeName: String,
        voice: VoiceGender,
        language: SupportedLanguage,
        handsFree: Boolean,
        mode: AssistantAvailabilityMode,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ) -> Unit
) {
    val context = LocalContext.current
    var nameState by remember { mutableStateOf(currentName) }
    var wakeNameState by remember { mutableStateOf(currentWakeName) }
    var voiceState by remember { mutableStateOf(currentVoice) }
    var languageState by remember { mutableStateOf(currentLanguage) }
    var handsFreeState by remember { mutableStateOf(isHandsFree) }
    var availabilityModeState by remember { mutableStateOf(currentAvailabilityMode) }
    var startHourState by remember { mutableStateOf(currentStartHour) }
    var startMinuteState by remember { mutableStateOf(currentStartMinute) }
    var endHourState by remember { mutableStateOf(currentEndHour) }
    var endMinuteState by remember { mutableStateOf(currentEndMinute) }
    var isLanguageDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Assistant Wake Name / Activation Name Input
                Text(
                    text = stringResource(R.string.wake_name_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = wakeNameState,
                    onValueChange = { wakeNameState = it },
                    placeholder = { Text("e.g. VirJoy, Ram, স্যাম, যদু") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wake_name_input"),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Hands-Free Activation Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.handsfree_toggle_label),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Activates automatically upon hearing wake name",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = handsFreeState,
                        onCheckedChange = { handsFreeState = it },
                        modifier = Modifier.testTag("handsfree_switch")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Assistant Availability (Active, Sleep, Scheduled, 24 Hours)
                Text(
                    text = stringResource(R.string.assistant_availability_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistantAvailabilityMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { availabilityModeState = mode }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = availabilityModeState == mode,
                                onClick = { availabilityModeState = mode },
                                modifier = Modifier.testTag("availability_${mode.name.lowercase()}")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = mode.displayName,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = mode.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // If Scheduled is selected, show time range picker
                    AnimatedVisibility(visible = availabilityModeState == AssistantAvailabilityMode.SCHEDULED) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Scheduled Hours (24h format)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.schedule_start_time),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        TimeNumberSelector(
                                            value = startHourState,
                                            onValueChange = { startHourState = it.coerceIn(0, 23) },
                                            range = 0..23,
                                            label = "H"
                                        )
                                        Text(":")
                                        TimeNumberSelector(
                                            value = startMinuteState,
                                            onValueChange = { startMinuteState = it.coerceIn(0, 59) },
                                            range = 0..59,
                                            label = "M"
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = stringResource(R.string.schedule_end_time),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        TimeNumberSelector(
                                            value = endHourState,
                                            onValueChange = { endHourState = it.coerceIn(0, 23) },
                                            range = 0..23,
                                            label = "H"
                                        )
                                        Text(":")
                                        TimeNumberSelector(
                                            value = endMinuteState,
                                            onValueChange = { endMinuteState = it.coerceIn(0, 59) },
                                            range = 0..59,
                                            label = "M"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Battery Optimization Notice
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.battery_optimization_title),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.battery_optimization_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val appIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(appIntent)
                                }
                            },
                            modifier = Modifier.testTag("battery_settings_button")
                        ) {
                            Text(stringResource(R.string.battery_optimization_action))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Assistant Display Name Input
                Text(
                    text = stringResource(R.string.assistant_name_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = nameState,
                    onValueChange = { nameState = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("assistant_name_input"),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Language Selection (12 Indian Languages)
                Text(
                    text = stringResource(R.string.assistant_language_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { isLanguageDropdownExpanded = true }
                            .testTag("language_selector"),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = languageState.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Language",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isLanguageDropdownExpanded,
                        onDismissRequest = { isLanguageDropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .heightIn(max = 280.dp)
                    ) {
                        SupportedLanguage.ALL_12_INDIAN_LANGUAGES.forEach { lang ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = lang.displayName,
                                        fontWeight = if (lang == languageState) FontWeight.Bold else FontWeight.Normal,
                                        color = if (lang == languageState) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    languageState = lang
                                    isLanguageDropdownExpanded = false
                                },
                                modifier = Modifier.testTag("language_item_${lang.name}")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Voice Selection: Male / Female (Independent setting)
                Text(
                    text = stringResource(R.string.voice_gender),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .testTag("voice_female_radio")
                            .clickable { voiceState = VoiceGender.FEMALE }
                    ) {
                        RadioButton(
                            selected = voiceState == VoiceGender.FEMALE,
                            onClick = { voiceState = VoiceGender.FEMALE }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(R.string.voice_female))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .testTag("voice_male_radio")
                            .clickable { voiceState = VoiceGender.MALE }
                    ) {
                        RadioButton(
                            selected = voiceState == VoiceGender.MALE,
                            onClick = { voiceState = VoiceGender.MALE }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(R.string.voice_male))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel_button))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSaveFull(
                                nameState,
                                wakeNameState,
                                voiceState,
                                languageState,
                                handsFreeState,
                                availabilityModeState,
                                startHourState,
                                startMinuteState,
                                endHourState,
                                endMinuteState
                            )
                        },
                        modifier = Modifier.testTag("save_settings_button")
                    ) {
                        Text(stringResource(R.string.save_button))
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeNumberSelector(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = String.format("%02d", value),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(
                text = "▲",
                modifier = Modifier
                    .clickable {
                        val next = if (value + 1 > range.last) range.first else value + 1
                        onValueChange(next)
                    }
                    .padding(horizontal = 2.dp),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "▼",
                modifier = Modifier
                    .clickable {
                        val prev = if (value - 1 < range.first) range.last else value - 1
                        onValueChange(prev)
                    }
                    .padding(horizontal = 2.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun SettingsDialog(
    currentName: String,
    currentWakeName: String = "VirJoy",
    currentVoice: VoiceGender = VoiceGender.FEMALE,
    currentLanguage: SupportedLanguage = SupportedLanguage.ENGLISH,
    isHandsFree: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (name: String, wakeName: String, voice: VoiceGender, language: SupportedLanguage, handsFree: Boolean) -> Unit
) {
    SettingsDialog(
        currentName = currentName,
        currentWakeName = currentWakeName,
        currentVoice = currentVoice,
        currentLanguage = currentLanguage,
        isHandsFree = isHandsFree,
        currentAvailabilityMode = AssistantAvailabilityMode.ACTIVE,
        currentStartHour = 8,
        currentStartMinute = 0,
        currentEndHour = 22,
        currentEndMinute = 0,
        onDismiss = onDismiss,
        onSaveFull = { name, wakeName, voice, lang, handsFree, _, _, _, _, _ ->
            onSave(name, wakeName, voice, lang, handsFree)
        }
    )
}

@Composable
fun SettingsDialog(
    currentName: String,
    currentVoice: VoiceGender,
    currentLanguage: SupportedLanguage = SupportedLanguage.ENGLISH,
    onDismiss: () -> Unit,
    onSave: (name: String, voice: VoiceGender, language: SupportedLanguage) -> Unit
) {
    SettingsDialog(
        currentName = currentName,
        currentWakeName = currentName,
        currentVoice = currentVoice,
        currentLanguage = currentLanguage,
        isHandsFree = true,
        onDismiss = onDismiss,
        onSave = { name, _, voice, lang, _ -> onSave(name, voice, lang) }
    )
}

@Composable
fun SettingsDialog(
    currentName: String,
    currentVoice: VoiceGender,
    onDismiss: () -> Unit,
    onSave: (String, VoiceGender) -> Unit
) {
    SettingsDialog(
        currentName = currentName,
        currentVoice = currentVoice,
        currentLanguage = SupportedLanguage.ENGLISH,
        onDismiss = onDismiss,
        onSave = { name, voice, _ -> onSave(name, voice) }
    )
}


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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.FilterChip
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.Contact
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
        arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )
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
                    onDismiss = { viewModel.closeSettings() },
                    onSave = { name, wakeName, voice, lang, handsFree ->
                        viewModel.saveSettings(name, wakeName, voice, lang, handsFree)
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
                isHandsFree = uiState.isHandsFreeEnabled
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

            // Multiple contacts selector list (if disambiguation is needed)
            if (uiState.multipleMatches.isNotEmpty()) {
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
    listeningMode: com.example.model.AssistantListeningMode,
    isListening: Boolean,
    wakeName: String,
    isHandsFree: Boolean
) {
    val backgroundColor = when (listeningMode) {
        com.example.model.AssistantListeningMode.COMMAND_LISTENING -> MaterialTheme.colorScheme.errorContainer
        com.example.model.AssistantListeningMode.WAKE_LISTENING -> MaterialTheme.colorScheme.secondaryContainer
        com.example.model.AssistantListeningMode.INACTIVE -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when (listeningMode) {
        com.example.model.AssistantListeningMode.COMMAND_LISTENING -> MaterialTheme.colorScheme.onErrorContainer
        com.example.model.AssistantListeningMode.WAKE_LISTENING -> MaterialTheme.colorScheme.onSecondaryContainer
        com.example.model.AssistantListeningMode.INACTIVE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val text = when (listeningMode) {
        com.example.model.AssistantListeningMode.COMMAND_LISTENING -> "Listening for command..."
        com.example.model.AssistantListeningMode.WAKE_LISTENING -> {
            if (isHandsFree) "Hands-Free: Listening for \"$wakeName\"" else "Tap Mic to speak"
        }
        com.example.model.AssistantListeningMode.INACTIVE -> "Paused / Permissions Required"
    }

    val dotColor = when (listeningMode) {
        com.example.model.AssistantListeningMode.COMMAND_LISTENING -> MaterialTheme.colorScheme.error
        com.example.model.AssistantListeningMode.WAKE_LISTENING -> MaterialTheme.colorScheme.primary
        com.example.model.AssistantListeningMode.INACTIVE -> MaterialTheme.colorScheme.outline
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
                .height(180.dp),
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
    listeningMode: com.example.model.AssistantListeningMode,
    isListening: Boolean,
    onClick: () -> Unit
) {
    val isCommandMode = listeningMode == com.example.model.AssistantListeningMode.COMMAND_LISTENING
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isCommandMode || isListening) 1.15f else 1.0f,
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
            if (isCommandMode || isListening) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            if (isCommandMode) MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
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
                color = if (isCommandMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                shadowElevation = 6.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = if (isCommandMode) "Cancel Command Listening" else "Manual Voice Command",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isCommandMode) "Listening for command..." else "Tap for manual command",
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
    onDismiss: () -> Unit,
    onSave: (name: String, wakeName: String, voice: VoiceGender, language: SupportedLanguage, handsFree: Boolean) -> Unit
) {
    var nameState by remember { mutableStateOf(currentName) }
    var wakeNameState by remember { mutableStateOf(currentWakeName) }
    var voiceState by remember { mutableStateOf(currentVoice) }
    var languageState by remember { mutableStateOf(currentLanguage) }
    var handsFreeState by remember { mutableStateOf(isHandsFree) }
    var isLanguageDropdownExpanded by remember { mutableStateOf(false) }

    val wakePresets = listOf(
        "VirJoy",
        "Ram" to "রাম",
        "Sam" to "স্যাম",
        "Yadu" to "যদু",
        "Madhu" to "মধু"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Wake Name / Activation Name Input
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
                    placeholder = { Text("e.g. Ram, রাম, Sam, Yadu, Madhu") },
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

                Spacer(modifier = Modifier.height(6.dp))

                // Wake Name Quick Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = wakeNameState.equals("VirJoy", ignoreCase = true),
                        onClick = { wakeNameState = "VirJoy" },
                        label = { Text("VirJoy", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = wakeNameState.contains("Ram", ignoreCase = true) || wakeNameState.contains("রাম"),
                        onClick = { wakeNameState = "রাম" },
                        label = { Text("রাম", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = wakeNameState.contains("Sam", ignoreCase = true) || wakeNameState.contains("স্যাম"),
                        onClick = { wakeNameState = "স্যাম" },
                        label = { Text("স্যাম", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = wakeNameState.contains("Yadu", ignoreCase = true) || wakeNameState.contains("যদু"),
                        onClick = { wakeNameState = "যদু" },
                        label = { Text("যদু", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = wakeNameState.contains("Madhu", ignoreCase = true) || wakeNameState.contains("মধু"),
                        onClick = { wakeNameState = "মধু" },
                        label = { Text("মধু", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

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

                Spacer(modifier = Modifier.height(14.dp))

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

                Spacer(modifier = Modifier.height(14.dp))

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
                            .fillMaxWidth(0.75f)
                            .height(260.dp)
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

                Spacer(modifier = Modifier.height(14.dp))

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

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel_button))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(nameState, wakeNameState, voiceState, languageState, handsFreeState) },
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

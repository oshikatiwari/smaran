package net.kibotu.geofencerelay.features.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import net.kibotu.geofencerelay.ui.theme.NerColors

enum class VoiceAssistantMode {
    PATIENT,
    CAREGIVER
}

/**
 * Universal Voice Interaction Overlay for Smaran.
 * Specifically crafted for senior and dementia users:
 * - 100% First-Person POV ("Talk with Smaran", "I am right here with you")
 * - High-contrast tactile 52dp microphone button
 * - Reassuring real-time waveform and spoken voice playback
 * - Zero technical surveillance jargon
 */
@Composable
fun VoiceInteractionOverlay(
    mode: VoiceAssistantMode,
    languageCode: String = "en",
    modifier: Modifier = Modifier,
    onPatientNavigate: ((destination: String) -> Unit)? = null,
    onCaregiverAction: ((CaregiverVoiceAction) -> Unit)? = null
) {
    val context = LocalContext.current
    val voiceManager = remember { VoiceAssistantManager.shared }

    LaunchedEffect(Unit) {
        voiceManager.initialize(context)
    }

    val voiceState by voiceManager.voiceState.collectAsState()
    val transcribedText by voiceManager.transcribedText.collectAsState()
    val spokenResponse by voiceManager.spokenResponse.collectAsState()
    val audioRmsLevel by voiceManager.audioRmsLevel.collectAsState()
    val errorMessage by voiceManager.errorMessage.collectAsState()
    val isWakeWordActive by voiceManager.isWakeWordActive.collectAsState()

    var isExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(voiceManager, languageCode) {
        // Automatically activate ambient wake-word listener on screen load
        voiceManager.startWakeWordListening(context, languageCode)

        voiceManager.onWakeWordTriggered = {
            isExpanded = true
            val greeting = when (languageCode) {
                "hi" -> "हाँ, मैं आपके साथ हूँ। बताइए, मैं आपकी क्या मदद करूँ?"
                "as" -> "হয়, মই আপোনাৰ কাষতেই আছোঁ। কওকচোন, মই কি সহায় কৰিব পাৰোঁ?"
                "lus" -> "Aw, i kiangah ka awm e. Engtin nge ka puih theih che?"
                "kha" -> "Hooid, nga don bad phi. Kumno nga lah ban iarap?"
                else -> "Yes, I am right here with you. How can I help you today?"
            }
            voiceManager.speak(greeting, languageCode) {
                // Seamlessly start listening for user's voice command after greeting finishes!
                startListeningWithHandler(
                    context = context,
                    mode = mode,
                    languageCode = languageCode,
                    voiceManager = voiceManager,
                    onPatientNavigate = onPatientNavigate,
                    onCaregiverAction = onCaregiverAction
                )
            }
        }
    }

    // Audio permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListeningWithHandler(
                context = context,
                mode = mode,
                languageCode = languageCode,
                voiceManager = voiceManager,
                onPatientNavigate = onPatientNavigate,
                onCaregiverAction = onCaregiverAction
            )
        }
    }

    // Gentle pulse animation for mic button
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val isActive = voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING
    val isPatient = mode == VoiceAssistantMode.PATIENT

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Expanded Voice Companion Panel
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically { it / 2 } + fadeIn(),
            exit = slideOutVertically { it / 2 } + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                border = BorderStroke(1.5.dp, NerColors.Primary.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Bar (100% First-Person for Patient)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(NerColors.PrimaryTint),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = NerColors.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isPatient) "Talk with Smaran" else "Voice Assistant",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = NerColors.Charcoal
                                )
                                Text(
                                    text = if (isPatient) "Your Caring Voice Companion" else "Voice Control",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NerColors.Primary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                onClick = {
                                    voiceManager.setWakeWordEnabled(!isWakeWordActive)
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isWakeWordActive) NerColors.SecondaryTint else NerColors.NeutralSoft,
                                border = BorderStroke(1.dp, if (isWakeWordActive) NerColors.Secondary.copy(alpha = 0.4f) else NerColors.NeutralBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Hearing,
                                        contentDescription = null,
                                        tint = if (isWakeWordActive) NerColors.Secondary else NerColors.NeutralMedium,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isWakeWordActive) "Hey Smaran: ON" else "Hey Smaran: OFF",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isWakeWordActive) NerColors.SecondaryDark else NerColors.NeutralMedium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    isExpanded = false
                                    voiceManager.cancelListening()
                                    voiceManager.stopSpeaking()
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(NerColors.NeutralSoft)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = NerColors.Charcoal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Audio Waveform Visualization
                    AudioWaveformBars(
                        isActive = voiceState == VoiceState.LISTENING,
                        isSpeaking = voiceState == VoiceState.SPEAKING,
                        rmsLevel = audioRmsLevel
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status Text - Reassuring & Friendly
                    val statusText = when (voiceState) {
                        VoiceState.LISTENING -> if (isPatient) "I am listening to you... Please speak gently." else "Listening for command..."
                        VoiceState.PROCESSING -> if (isPatient) "Thinking and understanding..." else "Processing..."
                        VoiceState.SPEAKING -> if (isPatient) "Speaking with you..." else "Speaking..."
                        VoiceState.ERROR -> errorMessage ?: "Voice recognition unavailable"
                        VoiceState.IDLE -> if (isPatient) "Tap the orange mic to speak, or pick any question below:" else "Tap mic or select an action:"
                    }

                    Text(
                        text = statusText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (voiceState == VoiceState.ERROR) NerColors.Crimson else NerColors.Charcoal,
                        textAlign = TextAlign.Center
                    )

                    // What user said (Transcribed Speech)
                    if (transcribedText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(NerColors.NeutralSoft)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = NerColors.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "\"$transcribedText\"",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NerColors.Charcoal
                                )
                            }
                        }
                    }

                    // Spoken Answer Card
                    if (spokenResponse.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = NerColors.PrimaryTint),
                            border = BorderStroke(1.dp, NerColors.Primary.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = spokenResponse,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = NerColors.PrimaryDark,
                                        lineHeight = 20.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                IconButton(
                                    onClick = {
                                        voiceManager.speak(spokenResponse, languageCode)
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(NerColors.Primary)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Hear Again",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Friendly 1-Tap Question Suggestions (Fully Localized with Direct Game Redirection)
                    val suggestions = getLocalizedSuggestions(mode, languageCode)

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        items(suggestions) { (title, icon) ->
                            SuggestionChipItem(
                                title = title,
                                icon = icon,
                                onClick = {
                                    executeVoiceQuery(
                                        query = title,
                                        context = context,
                                        mode = mode,
                                        languageCode = languageCode,
                                        voiceManager = voiceManager,
                                        onPatientNavigate = onPatientNavigate,
                                        onCaregiverAction = onCaregiverAction
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Floating Pill Controller Bar (100% First-Person, Friendly & Accessible)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    isExpanded = !isExpanded
                    if (isExpanded && voiceState == VoiceState.IDLE) {
                        handleMicClick(
                            context = context,
                            mode = mode,
                            languageCode = languageCode,
                            voiceManager = voiceManager,
                            permissionLauncher = permissionLauncher,
                            onPatientNavigate = onPatientNavigate,
                            onCaregiverAction = onCaregiverAction
                        )
                    }
                },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isActive) NerColors.PrimaryTint else NerColors.SurfaceWhite
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) NerColors.Primary else NerColors.NeutralBorder
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Tactile 50dp Mic Button
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .scale(if (voiceState == VoiceState.LISTENING) pulseScale else 1.0f)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    if (isActive) NerColors.PrimaryLight else NerColors.Primary,
                                    if (isActive) NerColors.Primary else NerColors.PrimaryDark
                                )
                            )
                        )
                        .clickable {
                            isExpanded = true
                            if (voiceState == VoiceState.LISTENING) {
                                voiceManager.stopListening()
                            } else {
                                handleMicClick(
                                    context = context,
                                    mode = mode,
                                    languageCode = languageCode,
                                    voiceManager = voiceManager,
                                    permissionLauncher = permissionLauncher,
                                    onPatientNavigate = onPatientNavigate,
                                    onCaregiverAction = onCaregiverAction
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (voiceState == VoiceState.LISTENING) Icons.Default.Mic else Icons.Default.MicNone,
                        contentDescription = "Microphone",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // First-Person Informational Prompt
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isPatient) "Talk with Smaran" else "Voice Assistant",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = NerColors.Charcoal
                    )
                    Text(
                        text = when (voiceState) {
                            VoiceState.LISTENING -> "I am listening... Please speak."
                            VoiceState.SPEAKING -> "Speaking with you now..."
                            VoiceState.PROCESSING -> "Understanding..."
                            else -> if (isPatient) "Tap to speak • I am right here for you" else "Tap for voice commands"
                        },
                        fontSize = 12.sp,
                        color = if (isActive) NerColors.Primary else NerColors.NeutralMedium,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                    )
                }

                // Expand/Collapse Chevron Indicator
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = "Expand Assistant",
                        tint = NerColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioWaveformBars(
    isActive: Boolean,
    isSpeaking: Boolean,
    rmsLevel: Float
) {
    val barCount = 7
    val infiniteTransition = rememberInfiniteTransition(label = "waveformAnim")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val animDuration = 400 + (i * 70)
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = 6f,
                targetValue = 26f,
                animationSpec = infiniteRepeatable(
                    animation = tween(animDuration, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$i"
            )

            val currentHeight = when {
                isActive -> (animatedHeight * (0.4f + rmsLevel * 0.8f)).coerceIn(4f, 28f)
                isSpeaking -> (animatedHeight * 0.7f).coerceIn(4f, 24f)
                else -> 4f
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(5.dp)
                    .height(currentHeight.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (isActive || isSpeaking) NerColors.Primary else NerColors.NeutralMedium.copy(alpha = 0.35f)
                    )
            )
        }
    }
}

@Composable
private fun SuggestionChipItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = NerColors.SurfaceWhite,
        border = BorderStroke(1.2.dp, NerColors.Primary.copy(alpha = 0.35f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NerColors.Primary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = NerColors.Charcoal
            )
        }
    }
}

private fun handleMicClick(
    context: Context,
    mode: VoiceAssistantMode,
    languageCode: String,
    voiceManager: VoiceAssistantManager,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    onPatientNavigate: ((String) -> Unit)?,
    onCaregiverAction: ((CaregiverVoiceAction) -> Unit)?
) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
        startListeningWithHandler(
            context = context,
            mode = mode,
            languageCode = languageCode,
            voiceManager = voiceManager,
            onPatientNavigate = onPatientNavigate,
            onCaregiverAction = onCaregiverAction
        )
    } else {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
}

private fun startListeningWithHandler(
    context: Context,
    mode: VoiceAssistantMode,
    languageCode: String,
    voiceManager: VoiceAssistantManager,
    onPatientNavigate: ((String) -> Unit)?,
    onCaregiverAction: ((CaregiverVoiceAction) -> Unit)?
) {
    voiceManager.startListening(context = context, languageCode = languageCode) { recognizedSpeech ->
        executeVoiceQuery(
            query = recognizedSpeech,
            context = context,
            mode = mode,
            languageCode = languageCode,
            voiceManager = voiceManager,
            onPatientNavigate = onPatientNavigate,
            onCaregiverAction = onCaregiverAction
        )
    }
}

private fun executeVoiceQuery(
    query: String,
    context: Context,
    mode: VoiceAssistantMode,
    languageCode: String,
    voiceManager: VoiceAssistantManager,
    onPatientNavigate: ((String) -> Unit)?,
    onCaregiverAction: ((CaregiverVoiceAction) -> Unit)?
) {
    if (mode == VoiceAssistantMode.PATIENT) {
        val response = DementiaVoiceSupportEngine.processPatientVoicePrompt(
            prompt = query,
            context = context,
            languageCode = languageCode
        )

        voiceManager.speak(response.spokenText, languageCode)

        response.suggestedNavigation?.let { destination ->
            onPatientNavigate?.invoke(destination)
        }
    } else {
        val (action, feedback) = VoiceNavigationController.parseCaregiverCommand(query, languageCode)

        voiceManager.speak(feedback, languageCode)
        onCaregiverAction?.invoke(action)
    }
}

private fun getLocalizedSuggestions(
    mode: VoiceAssistantMode,
    languageCode: String
): List<Pair<String, androidx.compose.ui.graphics.vector.ImageVector>> {
    if (mode == VoiceAssistantMode.CAREGIVER) {
        return when (languageCode) {
            "hi" -> listOf(
                "मरीज़ कहाँ है?" to Icons.Default.GpsFixed,
                "मरीज़ का रास्ता" to Icons.Default.Navigation,
                "सुरक्षित घेरा" to Icons.Default.Shield,
                "अलार्म बजाओ" to Icons.Default.Campaign,
                "स्कोरकार्ड देखें" to Icons.Default.Psychology,
                "मरीज़ को कॉल करें" to Icons.Default.Phone
            )
            "as" -> listOf(
                "ৰোগী ক'ত আছে?" to Icons.Default.GpsFixed,
                "ৰোগীৰ পথ আৰু মেপ" to Icons.Default.Navigation,
                "সুৰক্ষিত পৰিসীমা" to Icons.Default.Shield,
                "এলাৰ্ম বজাওক" to Icons.Default.Campaign,
                "মানসিক স্ক'ৰকাৰ্ড" to Icons.Default.Psychology,
                "ৰোগীক ফোন কৰক" to Icons.Default.Phone
            )
            "lus" -> listOf(
                "Damlo awmna khawiah nge?" to Icons.Default.GpsFixed,
                "A awmna kawng" to Icons.Default.Navigation,
                "Himna hmun hungna" to Icons.Default.Shield,
                "Alarm ri tir rawh" to Icons.Default.Campaign,
                "Rilru dinhmun scorecard" to Icons.Default.Psychology,
                "Damlo phone rawh" to Icons.Default.Phone
            )
            "kha" -> listOf(
                "Shano u nongpang u don?" to Icons.Default.GpsFixed,
                "Ka lynti sha u nongpang" to Icons.Default.Navigation,
                "Ka jaka shngain" to Icons.Default.Shield,
                "Pynri ia ka alarm" to Icons.Default.Campaign,
                "Peit ia ka scorecard" to Icons.Default.Psychology,
                "Phone sha u nongpang" to Icons.Default.Phone
            )
            else -> listOf(
                "Where is the patient?" to Icons.Default.GpsFixed,
                "Directions to patient" to Icons.Default.Navigation,
                "Safe Zone settings" to Icons.Default.Shield,
                "Trigger remote alarm" to Icons.Default.Campaign,
                "Check scorecard" to Icons.Default.Psychology,
                "Call patient" to Icons.Default.Phone
            )
        }
    }

    return when (languageCode) {
        "hi" -> listOf(
            "मैं अभी कहाँ हूँ?" to Icons.Default.LocationOn,
            "क्या समय हुआ है?" to Icons.Default.AccessTime,
            "मुझे घर ले चलो" to Icons.Default.Home,
            "मेमोरी मैचिंग" to Icons.Default.SportsEsports,
            "कलर स्ट्रूप" to Icons.Default.Palette,
            "मुझे कौन प्यार करता है?" to Icons.Default.Favorite,
            "मुझे मदद चाहिए" to Icons.Default.Shield
        )
        "as" -> listOf(
            "মই এতিয়া ক'ত আছোঁ?" to Icons.Default.LocationOn,
            "এতিয়া কিমান বাজিছে?" to Icons.Default.AccessTime,
            "মোক ঘৰলৈ লৈ ব'লক" to Icons.Default.Home,
            "মেমৰি মেচিং খেল" to Icons.Default.SportsEsports,
            "ৰং ষ্ট্ৰুপ খেল" to Icons.Default.Palette,
            "মোক কোনে মৰম কৰে?" to Icons.Default.Favorite,
            "মোক সহায় লাগে" to Icons.Default.Shield
        )
        "lus" -> listOf(
            "Khawiah nge ka awm?" to Icons.Default.LocationOn,
            "Dar engzat nge ni tawh?" to Icons.Default.AccessTime,
            "Inah min hruai haw rawh" to Icons.Default.Home,
            "Memory matching infiamna" to Icons.Default.SportsEsports,
            "Color stroop infiamna" to Icons.Default.Palette,
            "Tuin nge min hmangaih?" to Icons.Default.Favorite,
            "Ka mamawh ṭanpuina" to Icons.Default.Shield
        )
        "kha" -> listOf(
            "Hangno nga don mynta?" to Icons.Default.LocationOn,
            "Kaei ka por mynta?" to Icons.Default.AccessTime,
            "Leit sha ing" to Icons.Default.Home,
            "Memory matching ialehkai" to Icons.Default.SportsEsports,
            "Color stroop ialehkai" to Icons.Default.Palette,
            "Uei ba ieid ia nga?" to Icons.Default.Favorite,
            "Nga donkam jingiarap" to Icons.Default.Shield
        )
        else -> listOf(
            "Where am I right now?" to Icons.Default.LocationOn,
            "What time is it?" to Icons.Default.AccessTime,
            "Take me home" to Icons.Default.Home,
            "Memory matching" to Icons.Default.SportsEsports,
            "Color stroop" to Icons.Default.Palette,
            "Who loves me?" to Icons.Default.Favorite,
            "I need help" to Icons.Default.Shield
        )
    }
}


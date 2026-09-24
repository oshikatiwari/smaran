package net.kibotu.geofencerelay.features.ai.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.kibotu.geofencerelay.R
import net.kibotu.geofencerelay.features.ai.history.CognitiveHistoryManager
import net.kibotu.geofencerelay.features.ai.localization.MultilingualManager
import net.kibotu.geofencerelay.features.ai.model.CpsAssessmentResult
import net.kibotu.geofencerelay.features.ai.reminder.GameReminderManager
import net.kibotu.geofencerelay.features.ai.ui.dialogs.*
import net.kibotu.geofencerelay.features.voice.VoiceAssistantManager
import net.kibotu.geofencerelay.features.voice.VoiceAssistantMode
import net.kibotu.geofencerelay.features.voice.VoiceInteractionOverlay
import net.kibotu.geofencerelay.service.TrackerForegroundService
import java.util.*

sealed class SpringboardDestination {
    object Home : SpringboardDestination()
    object Beacon : SpringboardDestination()
    object Exercises : SpringboardDestination()
    object Health : SpringboardDestination()
    object Safety : SpringboardDestination()
    object Voice : SpringboardDestination()
    object Memory : SpringboardDestination()
    object Screening : SpringboardDestination()
}

/**
 * Serene, Mindful Palette inspired by Insight Timer.
 * Specifically crafted for seniors and dementia patients:
 * - Warm zen linen canvas (#FAF9F6)
 * - Soft meditation singing bell amber & bronze accents
 * - Elegant, organic typography and calming breathing room
 */
object CleanWhiteTheme {
    val Background = Color(0xFFFAF9F6)      // Insight Timer Zen warm white linen
    val CardBg = Color(0xFFFFFFFF)          // Pure white card
    val CardBorder = Color(0xFFEBE5D8)      // Warm organic border
    val TextPrimary = Color(0xFF23272F)     // Mindful charcoal
    val TextSecondary = Color(0xFF5A6270)   // Readable warm grey
    val TextMuted = Color(0xFF9CA3AF)       // Subtle hint grey

    // 4 Big Friendly Action Colors with gentle warm tones
    val OrangeBg = Color(0xFFFFFBEB)
    val OrangeBorder = Color(0xFFFDE68A)
    val OrangePrimary = Color(0xFFD97706)   // Warm amber / singing bell

    val GreenBg = Color(0xFFF0FDF4)
    val GreenBorder = Color(0xFFBBF7D0)
    val GreenPrimary = Color(0xFF059669)   // Serene tranquil emerald

    val BlueBg = Color(0xFFEFF6FF)
    val BlueBorder = Color(0xFFBFDBFE)
    val BluePrimary = Color(0xFF2563EB)

    val PurpleBg = Color(0xFFFAF5FF)
    val PurpleBorder = Color(0xFFE9D5FF)
    val PurplePrimary = Color(0xFF7C3AED)
}

/**
 * Ultra-Simple, Accessible White UI for SMARAN.
 * Focused purely on the patient's primary needs:
 * 1. Play Brain Games (Memory cards & colors)
 * 2. Where Am I? (Safe Radar location)
 * 3. Call Family (1-tap dial)
 * 4. Memory Vault (Photos & memories)
 * + Calming voice assistant with "Hey Smaran" wake word.
 */
@Composable
fun SpringboardScreen(
    userEmail: String,
    initialDestination: SpringboardDestination = SpringboardDestination.Home,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current

    // Initialize Multilingual & TTS support
    LaunchedEffect(Unit) {
        MultilingualManager.initTts(context)
    }

    DisposableEffect(Unit) {
        onDispose {
            MultilingualManager.shutdown()
        }
    }

    val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    var activeDestination by remember { mutableStateOf<SpringboardDestination>(initialDestination) }
    var targetGameMode by remember { mutableStateOf(ActiveGameMode.HUB) }
    var selectedLanguageCode by remember {
        mutableStateOf(prefs.getString("selected_language", "en") ?: "en")
    }

    // Persistent cognitive assessment state
    var currentAssessment by remember { mutableStateOf(CognitiveHistoryManager.getLatestAssessment(context)) }
    var showReportCornerPrompt by remember { mutableStateOf(prefs.getBoolean("show_baseline_popup", false)) }

    var isAlarmPopping by remember { mutableStateOf(GameReminderManager.isAlarmFiring(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            isAlarmPopping = GameReminderManager.isAlarmFiring(context)
            kotlinx.coroutines.delay(1200)
        }
    }

    val isServiceRunning by TrackerForegroundService.serviceRunning.collectAsState()
    val isBroadcasting = isServiceRunning || TrackerForegroundService.isRunning(context)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CleanWhiteTheme.Background)
    ) {
        AnimatedContent(
            targetState = activeDestination,
            transitionSpec = {
                if (targetState == SpringboardDestination.Home) {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                } else {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                }
            },
            label = "springboardNav"
        ) { dest ->
            when (dest) {
                SpringboardDestination.Home -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Scrollable content area
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 1. SIMPLE TOP HEADER: Logo + SMARAN + Tagline + Sign Out
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(id = R.drawable.smaran_logo),
                                        contentDescription = "Smaran Logo",
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(CleanWhiteTheme.OrangeBg)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "SMARAN",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 22.sp,
                                            color = CleanWhiteTheme.TextPrimary,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "Where Memories Meet Care",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = CleanWhiteTheme.OrangePrimary
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Language quick switch
                                    TextButton(
                                        onClick = { activeDestination = SpringboardDestination.Voice },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = when (selectedLanguageCode) {
                                                "hi" -> "हिंदी"
                                                "as" -> "অসমীয়া"
                                                "lus" -> "Mizo"
                                                "kha" -> "Khasi"
                                                else -> "English"
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CleanWhiteTheme.BluePrimary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Sign Out
                                    IconButton(
                                        onClick = onSignOut,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(CleanWhiteTheme.CardBg)
                                            .border(1.dp, CleanWhiteTheme.CardBorder, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                            contentDescription = "Sign Out",
                                            tint = CleanWhiteTheme.TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // 2. REASSURING STATUS PILL
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CleanWhiteTheme.CardBg)
                                    .border(1.dp, CleanWhiteTheme.CardBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (isBroadcasting) CleanWhiteTheme.GreenPrimary else CleanWhiteTheme.TextMuted)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isBroadcasting) "Safe Radar Active • Family Connected" else "Radar Standby",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isBroadcasting) CleanWhiteTheme.GreenPrimary else CleanWhiteTheme.TextSecondary
                                    )
                                }

                                Text(
                                    text = "Tap cards below",
                                    fontSize = 11.sp,
                                    color = CleanWhiteTheme.TextMuted
                                )
                            }

                            // Corner pop-up asking if the user wants to see the cognitive report
                            if (showReportCornerPrompt) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = CleanWhiteTheme.CardBg),
                                    border = BorderStroke(1.5.dp, CleanWhiteTheme.OrangePrimary)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "🌿", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Mind Baseline Ready",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = CleanWhiteTheme.TextPrimary
                                                )
                                                Text(
                                                    text = "Would you like to view your report?",
                                                    fontSize = 11.sp,
                                                    color = CleanWhiteTheme.TextSecondary
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            TextButton(
                                                onClick = {
                                                    showReportCornerPrompt = false
                                                    prefs.edit().putBoolean("show_baseline_popup", false).apply()
                                                    activeDestination = SpringboardDestination.Screening
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "View",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = CleanWhiteTheme.OrangePrimary
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    showReportCornerPrompt = false
                                                    prefs.edit().putBoolean("show_baseline_popup", false).apply()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Dismiss",
                                                    tint = CleanWhiteTheme.TextMuted,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            // Insight Timer-inspired Mind Wellness & Roadmap Banner
                            val authPrefs = remember { context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE) }
                            val savedMmse = remember { authPrefs.getFloat("baseline_mmse", prefs.getFloat("baseline_mmse", 27f)) }
                            val savedTier = remember { authPrefs.getString("proficiency_tier", prefs.getString("proficiency_tier", "Intermediate")) ?: "Intermediate" }
                            val savedDiff = remember { authPrefs.getString("recommended_difficulty", prefs.getString("recommended_difficulty", "Medium")) ?: "Medium" }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeDestination = SpringboardDestination.Screening },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CleanWhiteTheme.OrangeBg),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CleanWhiteTheme.OrangeBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "🌿", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Mind Wellness: ${String.format(java.util.Locale.US, "%.1f", savedMmse)} / 30 MMSE",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CleanWhiteTheme.TextPrimary
                                            )
                                            Text(
                                                text = "$savedTier • Plan: $savedDiff",
                                                fontSize = 11.sp,
                                                color = CleanWhiteTheme.OrangePrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Roadmap ➔",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanWhiteTheme.OrangePrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 3. FOUR VERY SIMPLE, LARGE, FRIENDLY CARDS
                            Column(
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // CARD 1: PLAY BRAIN GAMES
                                SimpleAccessibleActionCard(
                                    title = "Play Brain Games",
                                    subtitle = "Gentle memory cards, colors & focus",
                                    icon = Icons.Default.SportsEsports,
                                    iconBgColor = CleanWhiteTheme.OrangeBg,
                                    iconBorderColor = CleanWhiteTheme.OrangeBorder,
                                    iconTintColor = CleanWhiteTheme.OrangePrimary,
                                    onClick = {
                                        targetGameMode = ActiveGameMode.HUB
                                        activeDestination = SpringboardDestination.Exercises
                                    }
                                )

                                // CARD 2: WHERE AM I? (SAFE RADAR)
                                SimpleAccessibleActionCard(
                                    title = "Where Am I?",
                                    subtitle = "Check your current location & home route",
                                    icon = Icons.Default.LocationOn,
                                    iconBgColor = CleanWhiteTheme.GreenBg,
                                    iconBorderColor = CleanWhiteTheme.GreenBorder,
                                    iconTintColor = CleanWhiteTheme.GreenPrimary,
                                    onClick = {
                                        activeDestination = SpringboardDestination.Beacon
                                    }
                                )

                                // CARD 3: CALL FAMILY / CAREGIVER
                                SimpleAccessibleActionCard(
                                    title = "Call Caregiver",
                                    subtitle = "1-tap phone call to your loved ones",
                                    icon = Icons.Default.PhoneInTalk,
                                    iconBgColor = CleanWhiteTheme.BlueBg,
                                    iconBorderColor = CleanWhiteTheme.BlueBorder,
                                    iconTintColor = CleanWhiteTheme.BluePrimary,
                                    onClick = {
                                        val safetyPrefs = context.getSharedPreferences("safety_prefs", Context.MODE_PRIVATE)
                                        val caregiverPhone = safetyPrefs.getString("caregiver_phone", "") ?: ""
                                        try {
                                            val dialNumber = caregiverPhone.ifBlank { "9876543210" }
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$dialNumber"))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            activeDestination = SpringboardDestination.Safety
                                        }
                                    }
                                )

                                // CARD 4: MEMORY VAULT
                                SimpleAccessibleActionCard(
                                    title = "Memory Vault",
                                    subtitle = "View family photos & sweet moments",
                                    icon = Icons.Default.CollectionsBookmark,
                                    iconBgColor = CleanWhiteTheme.PurpleBg,
                                    iconBorderColor = CleanWhiteTheme.PurpleBorder,
                                    iconTintColor = CleanWhiteTheme.PurplePrimary,
                                    onClick = {
                                        activeDestination = SpringboardDestination.Memory
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 4. CLEAN FIRST-PERSON VOICE COMPANION OVERLAY
                            VoiceInteractionOverlay(
                                mode = VoiceAssistantMode.PATIENT,
                                languageCode = selectedLanguageCode,
                                onPatientNavigate = { dest ->
                                    when (dest) {
                                        "beacon" -> activeDestination = SpringboardDestination.Beacon
                                        "exercises" -> {
                                            targetGameMode = ActiveGameMode.HUB
                                            activeDestination = SpringboardDestination.Exercises
                                        }
                                        "game_memory" -> {
                                            targetGameMode = ActiveGameMode.MEMORY_MATCHING
                                            activeDestination = SpringboardDestination.Exercises
                                        }
                                        "game_stroop" -> {
                                            targetGameMode = ActiveGameMode.STROOP_CHALLENGE
                                            activeDestination = SpringboardDestination.Exercises
                                        }
                                        "game_sequence" -> {
                                            targetGameMode = ActiveGameMode.PATTERN_RECOGNITION
                                            activeDestination = SpringboardDestination.Exercises
                                        }
                                        "game_trail" -> {
                                            targetGameMode = ActiveGameMode.TRAIL_MAKING
                                            activeDestination = SpringboardDestination.Exercises
                                        }
                                        "health" -> activeDestination = SpringboardDestination.Health
                                        "safety" -> activeDestination = SpringboardDestination.Safety
                                        "voice" -> activeDestination = SpringboardDestination.Voice
                                        "memory" -> activeDestination = SpringboardDestination.Memory
                                        "home" -> activeDestination = SpringboardDestination.Home
                                        "home_navigate" -> {
                                            val safetyPrefs = context.getSharedPreferences("safety_prefs", Context.MODE_PRIVATE)
                                            val homeLat = safetyPrefs.getFloat("home_latitude", 0f).toDouble()
                                            val homeLon = safetyPrefs.getFloat("home_longitude", 0f).toDouble()
                                            val homeAddr = safetyPrefs.getString("home_address", "") ?: ""
                                            try {
                                                val navUri = if (homeLat != 0.0 && homeLon != 0.0) {
                                                    Uri.parse("geo:$homeLat,$homeLon?q=$homeLat,$homeLon(Home)")
                                                } else {
                                                    Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(homeAddr))
                                                }
                                                context.startActivity(Intent(Intent.ACTION_VIEW, navUri))
                                            } catch (_: Exception) {
                                                activeDestination = SpringboardDestination.Safety
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        // Bottom Navigation Bar: Simple 3-Tab Clean White Dock
                        SimpleBottomBar(
                            selectedIndex = 0,
                            onSelect = { index ->
                                when (index) {
                                    0 -> activeDestination = SpringboardDestination.Home
                                    1 -> {
                                        targetGameMode = ActiveGameMode.HUB
                                        activeDestination = SpringboardDestination.Exercises
                                    }
                                    2 -> activeDestination = SpringboardDestination.Beacon
                                    3 -> activeDestination = SpringboardDestination.Safety
                                }
                            }
                        )
                    }
                }

                SpringboardDestination.Beacon -> {
                    BeaconTrackerPanel(
                        userEmail = userEmail,
                        selectedLanguageCode = selectedLanguageCode,
                        onBack = { activeDestination = SpringboardDestination.Home }
                    )
                }

                SpringboardDestination.Exercises -> {
                    BrainExerciseGamePanel(
                        selectedLanguageCode = selectedLanguageCode,
                        initialGameMode = targetGameMode,
                        onAssessmentUpdated = { updated ->
                            currentAssessment = updated
                            CognitiveHistoryManager.saveAssessment(context, updated)
                        },
                        onBack = {
                            targetGameMode = ActiveGameMode.HUB
                            activeDestination = SpringboardDestination.Home
                        }
                    )
                }

                SpringboardDestination.Health -> {
                    CognitiveHealthPanel(
                        assessment = currentAssessment,
                        selectedLanguageCode = selectedLanguageCode,
                        onLaunchGame = { activeDestination = SpringboardDestination.Exercises },
                        onBack = { activeDestination = SpringboardDestination.Home }
                    )
                }

                SpringboardDestination.Safety -> {
                    SafetyAlertsPanel(
                        assessment = currentAssessment,
                        selectedLanguageCode = selectedLanguageCode,
                        onBack = { activeDestination = SpringboardDestination.Home }
                    )
                }

                SpringboardDestination.Voice -> {
                    VoiceLanguagePanel(
                        selectedLanguageCode = selectedLanguageCode,
                        onLanguageSelected = { code ->
                            selectedLanguageCode = code
                            prefs.edit().putString("selected_language", code).commit()
                        },
                        onBack = { activeDestination = SpringboardDestination.Home }
                    )
                }

                SpringboardDestination.Memory -> {
                    MemoryVaultPanel(
                        selectedLanguageCode = selectedLanguageCode,
                        onBack = { activeDestination = SpringboardDestination.Home }
                    )
                }

                SpringboardDestination.Screening -> {
                    CognitiveScreeningScreen(
                        initialStage = ScreeningStage.RESULTS_ROADMAP,
                        onComplete = {
                            activeDestination = SpringboardDestination.Home
                        }
                    )
                }
            }
        }

        // Clean Reminder Alarm Dialog
        if (isAlarmPopping) {
            AlertDialog(
                onDismissRequest = {},
                properties = androidx.compose.ui.window.DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                title = {
                    Text(
                        text = "🌟 " + MultilingualManager.tr("alarm_title", selectedLanguageCode),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = CleanWhiteTheme.TextPrimary
                    )
                },
                text = {
                    Text(
                        text = MultilingualManager.tr("alarm_desc", selectedLanguageCode),
                        fontSize = 14.sp,
                        color = CleanWhiteTheme.TextSecondary,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            GameReminderManager.dismissAlarm(context)
                            isAlarmPopping = false
                            activeDestination = SpringboardDestination.Exercises
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CleanWhiteTheme.OrangePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = MultilingualManager.tr("alarm_btn_play", selectedLanguageCode),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            GameReminderManager.dismissAlarm(context)
                            GameReminderManager.scheduleNextAlarm(context, 10L)
                            isAlarmPopping = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CleanWhiteTheme.CardBorder)
                    ) {
                        Text(
                            text = MultilingualManager.tr("alarm_btn_snooze", selectedLanguageCode),
                            color = CleanWhiteTheme.TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                containerColor = CleanWhiteTheme.CardBg,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

/**
 * Large, Ultra-Clean Action Card.
 * High tactile visibility, uncluttered, gentle rounded corners.
 */
@Composable
private fun SimpleAccessibleActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconBorderColor: Color,
    iconTintColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CleanWhiteTheme.CardBg),
        border = BorderStroke(1.dp, CleanWhiteTheme.CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBgColor)
                    .border(1.dp, iconBorderColor, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTintColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanWhiteTheme.TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = CleanWhiteTheme.TextSecondary,
                    lineHeight = 17.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = CleanWhiteTheme.TextMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Clean, Simple White Bottom Navigation Dock.
 */
@Composable
private fun SimpleBottomBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        color = CleanWhiteTheme.CardBg,
        border = BorderStroke(1.dp, CleanWhiteTheme.CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleTabItem(
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = selectedIndex == 0,
                onClick = { onSelect(0) }
            )
            SimpleTabItem(
                icon = Icons.Default.SportsEsports,
                label = "Games",
                isSelected = selectedIndex == 1,
                onClick = { onSelect(1) }
            )
            SimpleTabItem(
                icon = Icons.Default.LocationOn,
                label = "Radar",
                isSelected = selectedIndex == 2,
                onClick = { onSelect(2) }
            )
            SimpleTabItem(
                icon = Icons.Default.Shield,
                label = "Safety",
                isSelected = selectedIndex == 3,
                onClick = { onSelect(3) }
            )
        }
    }
}

@Composable
private fun SimpleTabItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (isSelected) CleanWhiteTheme.OrangePrimary else CleanWhiteTheme.TextMuted
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = tint
        )
    }
}
package net.kibotu.geofencerelay.ui.guardian

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import net.kibotu.geofencerelay.features.voice.CaregiverVoiceAction
import net.kibotu.geofencerelay.features.voice.VoiceAssistantMode
import net.kibotu.geofencerelay.features.voice.VoiceInteractionOverlay
import net.kibotu.geofencerelay.ui.theme.*
import net.kibotu.geofencerelay.util.LocationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianScreen(
    googleAccountEmail: String,
    onBack: () -> Unit,
    onSignOut: () -> Unit = onBack,
    vm: GuardianViewModel = viewModel()
) {
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(googleAccountEmail) {
        vm.init(googleAccountEmail)
    }

    val isConnected by vm.isConnected.collectAsState()
    val zone by vm.zone.collectAsState()
    val targetPing by vm.targetPing.collectAsState()
    val latestAlert by vm.latestAlert.collectAsState()
    val isBreached by vm.isBreached.collectAsState()
    val broadcastSuccess by vm.broadcastSuccess.collectAsState()
    val recenterTrigger by vm.recenterTrigger.collectAsState()
    val isPlayingSound by vm.isPlayingSound.collectAsState()
    val latestScorecard by vm.latestScorecard.collectAsState()
    val scorecardsHistory by vm.scorecardsHistory.collectAsState()

    var showZoneEditor by remember { mutableStateOf(false) }
    var showScorecardDialog by remember { mutableStateOf(false) }
    var sliderRadius by remember(zone.radiusMeters) {
        mutableFloatStateOf(zone.radiusMeters.toFloat().coerceIn(50f, 2000f))
    }

    val appPrefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    var selectedLanguageCode by remember {
        mutableStateOf(appPrefs.getString("selected_language", "en") ?: "en")
    }
    var showLanguageMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEA580C)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "S",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "SMARAN GUARDIAN",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                            Text(
                                "Where Memories Meet Care",
                                fontSize = 11.sp,
                                color = Color(0xFFEA580C)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF111827))
                    }
                },
                actions = {
                    // Language Switcher Dropdown Button
                    Box {
                        TextButton(
                            onClick = { showLanguageMenu = true },
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
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                        }

                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            listOf(
                                "en" to "English",
                                "hi" to "हिंदी (Hindi)",
                                "as" to "অসমীয়া (Assamese)",
                                "lus" to "Mizo",
                                "kha" to "Khasi"
                            ).forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, fontWeight = if (code == selectedLanguageCode) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedLanguageCode = code
                                        appPrefs.edit().putString("selected_language", code).commit()
                                        showLanguageMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Online / Connecting Indicator
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isConnected) Color(0xFF16A34A).copy(alpha = 0.15f) else Color(0xFFDC2626).copy(alpha = 0.15f))
                            .clickable { vm.reconnect() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isConnected) "● LIVE RADAR" else "CONNECTING...",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isConnected) Color(0xFF16A34A) else Color(0xFFDC2626)
                        )
                    }

                    // Sign Out Button
                    IconButton(onClick = {
                        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit().clear().commit()
                        onSignOut()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out", tint = Color(0xFF4B5563))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
        ) {
            // Top Woven Ribbon
            NerWovenRibbon(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                height = 12.dp,
                primaryColor = NerColors.Primary,
                secondaryColor = NerColors.Secondary,
                accentColor = NerColors.Marigold
            )
            // Interactive Map View
            OsmMapView(
                modifier = Modifier.fillMaxSize(),
                zone = zone,
                targetPing = targetPing,
                isBreached = isBreached,
                recenterTrigger = recenterTrigger,
                onMapTapped = { lat, lon ->
                    if (showZoneEditor) {
                        vm.updateCenter(lat, lon)
                    }
                }
            )

            // Safe Zone Breach Banner
            AnimatedVisibility(
                visible = isBreached || latestAlert != null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NerColors.Crimson),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SAFE ZONE BREACH DETECTED!", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 14.sp)
                            Text(
                                "Target is outside '${zone.name}' (${LocationUtils.formatDistance(targetPing?.distanceFromCenter ?: 0.0)} from center).",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Floating Recenter Button
            FloatingActionButton(
                onClick = { vm.triggerRecenter() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 270.dp, end = 16.dp),
                containerColor = NerColors.SurfaceWhite,
                contentColor = NerColors.Tertiary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Default.GpsFixed, contentDescription = "Recenter on Device")
            }

            // Bottom IRCTC Clean Control Panel
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NerColors.NeutralBorder))
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Drawer Handle
                    Box(
                        modifier = Modifier
                            .size(36.dp, 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(NerColors.NeutralBorder)
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Device Telemetry Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = targetPing?.deviceName ?: "Locating Beacon...",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = NerColors.Charcoal
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = targetPing?.address ?: "Acquiring live GPS fix...",
                                fontSize = 12.sp,
                                color = NerColors.NeutralMedium,
                                maxLines = 1
                            )
                        }

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isBreached) NerColors.Crimson else NerColors.Secondary
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (isBreached) "BREACH" else "IN ZONE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Distance, Speed, Last Ping row
                    if (targetPing != null) {
                        val lastSeen = LocationUtils.formatTime(targetPing!!.timestamp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(NerColors.CanvasWarm)
                                .border(1.dp, NerColors.NeutralBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Dist: ${LocationUtils.formatDistance(targetPing!!.distanceFromCenter)}",
                                fontSize = 12.sp,
                                color = NerColors.Tertiary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Speed: ${LocationUtils.formatSpeed(targetPing!!.speed)}",
                                fontSize = 12.sp,
                                color = NerColors.NeutralMedium
                            )
                            Text(
                                if (lastSeen == "Just now") "Live Ping" else "Seen: $lastSeen",
                                fontSize = 12.sp,
                                color = if (lastSeen == "Just now") NerColors.Secondary else NerColors.NeutralMedium,
                                fontWeight = if (lastSeen == "Just now") FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Caregiver Voice Assistant Console
                    VoiceInteractionOverlay(
                        mode = VoiceAssistantMode.CAREGIVER,
                        languageCode = selectedLanguageCode,
                        modifier = Modifier.padding(bottom = 6.dp),
                        onCaregiverAction = { action ->
                            when (action) {
                                is CaregiverVoiceAction.RecenterRadar -> {
                                    vm.triggerRecenter()
                                }
                                is CaregiverVoiceAction.OpenDirections -> {
                                    targetPing?.let { ping ->
                                        val uri = "google.navigation:q=${ping.latitude},${ping.longitude}"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                                            setPackage("com.google.android.apps.maps")
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            val webUri = "https://www.google.com/maps/dir/?api=1&destination=${ping.latitude},${ping.longitude}"
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUri)))
                                        }
                                    }
                                }
                                is CaregiverVoiceAction.ToggleSafeZone -> {
                                    showZoneEditor = !showZoneEditor
                                }
                                is CaregiverVoiceAction.PlayAlarm -> {
                                    vm.playSound()
                                }
                                is CaregiverVoiceAction.StopAlarm -> {
                                    vm.stopSound()
                                }
                                is CaregiverVoiceAction.OpenScorecards -> {
                                    showScorecardDialog = true
                                }
                                is CaregiverVoiceAction.CallPatient -> {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_DIAL))
                                    } catch (_: Exception) {}
                                }
                                is CaregiverVoiceAction.SpokenFeedback -> {}
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4 Interactive Action Tiles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 1. Play Sound / Alarm
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (isPlayingSound) vm.stopSound() else vm.playSound()
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlayingSound) NerColors.Crimson else NerColors.Tertiary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play Sound", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                if (isPlayingSound) "Stop Sound" else "Play Sound",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NerColors.Charcoal
                            )
                        }

                        // 2. Directions in Google Maps
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val ping = targetPing
                                    val lat = ping?.latitude ?: zone.latitude
                                    val lon = ping?.longitude ?: zone.longitude
                                    if (lat != 0.0 && lon != 0.0) {
                                        val uri = "google.navigation:q=$lat,$lon"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                                            setPackage("com.google.android.apps.maps")
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            val webUri = "https://www.google.com/maps/dir/?api=1&destination=$lat,$lon"
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUri)))
                                        }
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(NerColors.TertiaryLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Directions, contentDescription = "Directions", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Directions", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NerColors.Charcoal)
                        }

                        // 3. Safe Zone Setup
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    showZoneEditor = !showZoneEditor
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(if (showZoneEditor) NerColors.Primary else NerColors.PrimaryDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Security, contentDescription = "Safe Zone", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Safe Zone", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NerColors.Charcoal)
                        }

                        // 4. Cognitive Scorecard
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    showScorecardDialog = true
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(if (latestScorecard != null) NerColors.Tertiary else NerColors.NeutralMedium),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = "Scorecard", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                if (latestScorecard != null) "${latestScorecard!!.cpsScore.toInt()} CPS" else "Scorecard",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NerColors.Charcoal
                            )
                        }

                        // 5. Recenter & Sync
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    vm.triggerRecenter()
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(NerColors.Tertiary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = "Recenter Radar", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Center Radar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NerColors.Charcoal)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Patient Cognitive Telemetry Card (Live MQTT Telemetry from Tracker)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showScorecardDialog = true },
                        colors = CardDefaults.cardColors(containerColor = NerColors.CanvasWarm),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, NerColors.NeutralBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(NerColors.Tertiary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Psychology,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            "Cognitive Telemetry & Scorecard",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = NerColors.Charcoal
                                        )
                                        Text(
                                            if (latestScorecard != null) "${latestScorecard!!.gameType} • Level: ${latestScorecard!!.difficulty}" else "Awaiting daily brain exercises...",
                                            fontSize = 11.sp,
                                            color = NerColors.NeutralMedium
                                        )
                                    }
                                }

                                if (latestScorecard != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NerColors.Tertiary)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            "${latestScorecard!!.cpsScore.toInt()} CPS",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            if (latestScorecard != null) {
                                Spacer(modifier = Modifier.height(10.dp))

                                if (latestScorecard!!.caregiverSummary.isNotBlank()) {
                                    Text(
                                        text = latestScorecard!!.caregiverSummary,
                                        fontSize = 12.sp,
                                        color = NerColors.Charcoal,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    ScoreSubMetric("Memory", "${latestScorecard!!.memoryRetention.toInt()}%", NerColors.Secondary)
                                    ScoreSubMetric("Reaction", "${latestScorecard!!.reactionLatency.toInt()}%", NerColors.Marigold)
                                    ScoreSubMetric("Executive", "${latestScorecard!!.executiveFunction.toInt()}%", NerColors.Tertiary)
                                    ScoreSubMetric("Accuracy", "${(latestScorecard!!.accuracy * 100).toInt()}%", NerColors.Primary)
                                }

                                if (latestScorecard!!.anomalyDetected) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NerColors.Crimson.copy(alpha = 0.15f))
                                            .padding(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = NerColors.Crimson, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Acute Drop Alert: Performance deviation detected",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NerColors.Crimson
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        "View Detailed History (${scorecardsHistory.size}) →",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NerColors.Tertiary
                                    )
                                }
                            }
                        }
                    }

                    // Collapsible Safe Geofence Editor
                    AnimatedVisibility(visible = showZoneEditor) {
                        Column(
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(NerColors.CanvasWarm)
                                .border(1.dp, NerColors.NeutralBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Geofence Configuration",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = NerColors.Charcoal
                                )
                                Text(
                                    "${sliderRadius.toInt()} m radius",
                                    color = NerColors.Tertiary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                "Tap anywhere on the map to place center coordinate. Adjust boundary radius with the slider below.",
                                fontSize = 11.sp,
                                color = NerColors.NeutralMedium,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Slider(
                                value = sliderRadius,
                                onValueChange = {
                                    sliderRadius = it
                                    vm.updateRadius(it.toDouble())
                                },
                                valueRange = 50f..2000f,
                                colors = SliderDefaults.colors(
                                    thumbColor = NerColors.Primary,
                                    activeTrackColor = NerColors.Primary,
                                    inactiveTrackColor = NerColors.NeutralBorder
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = zone.name,
                                onValueChange = { vm.updateName(it) },
                                label = { Text("Zone Name (e.g., Home, Campus, Work)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { vm.broadcastZone() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NerColors.Secondary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (broadcastSuccess) "Safe Zone Synced to Device! ✓" else "Broadcast Safe Zone to Tracker",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Cognitive Health & Scorecards Detailed History Dialog
            if (showScorecardDialog) {
                AlertDialog(
                    onDismissRequest = { showScorecardDialog = false },
                    title = {
                        Column {
                            NerWovenRibbon(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                height = 10.dp,
                                primaryColor = NerColors.Tertiary,
                                secondaryColor = NerColors.Primary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(NerColors.Tertiary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Psychology, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Patient Cognitive Health", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = NerColors.Charcoal)
                            }
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 440.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (latestScorecard == null) {
                                Text(
                                    "No cognitive scorecards received yet.\n\nWhen the patient plays brain exercises on their phone, live telemetry and daily scorecards will sync here automatically over MQTT.",
                                    fontSize = 13.sp,
                                    color = NerColors.NeutralMedium,
                                    lineHeight = 18.sp
                                )
                            } else {
                                // Latest Session Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = NerColors.CanvasIvory),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, NerColors.NeutralBorder)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    "${latestScorecard!!.gameType} (${latestScorecard!!.difficulty})",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = NerColors.Charcoal
                                                )
                                                Text(latestScorecard!!.dateFormatted, fontSize = 11.sp, color = NerColors.NeutralMedium)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(NerColors.Tertiary)
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Text("${latestScorecard!!.cpsScore.toInt()} CPS", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        if (latestScorecard!!.caregiverSummary.isNotBlank()) {
                                            Text(
                                                "Caregiver Summary: ${latestScorecard!!.caregiverSummary}",
                                                fontSize = 12.sp,
                                                color = NerColors.Charcoal,
                                                lineHeight = 16.sp
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Memory: ${latestScorecard!!.memoryRetention.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NerColors.Secondary)
                                            Text("Reaction: ${latestScorecard!!.reactionLatency.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NerColors.Marigold)
                                            Text("Executive: ${latestScorecard!!.executiveFunction.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NerColors.Tertiary)
                                            Text("Accuracy: ${(latestScorecard!!.accuracy * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NerColors.Primary)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    "Session History (${scorecardsHistory.size} recorded)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = NerColors.Charcoal
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                scorecardsHistory.forEach { card ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, NerColors.NeutralBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${card.gameType} (${card.difficulty})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = NerColors.Charcoal)
                                                Text(card.dateFormatted, fontSize = 10.sp, color = NerColors.NeutralMedium)
                                                if (card.caregiverSummary.isNotBlank()) {
                                                    Text(card.caregiverSummary, fontSize = 10.sp, color = NerColors.NeutralMedium, maxLines = 1)
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(NerColors.Tertiary.copy(alpha = 0.15f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text("${card.cpsScore.toInt()} CPS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NerColors.Tertiary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showScorecardDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = NerColors.Primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Close", color = Color.White)
                        }
                    },
                    containerColor = NerColors.SurfaceWhite,
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ScoreSubMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = color)
        Text(label, fontSize = 10.sp, color = NerColors.NeutralMedium)
    }
}

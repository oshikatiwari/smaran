package net.kibotu.geofencerelay.features.ai.ui.dialogs

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.kibotu.geofencerelay.features.ai.history.CognitiveHistoryManager
import net.kibotu.geofencerelay.features.ai.history.DailyScorecardItem
import net.kibotu.geofencerelay.features.ai.localization.MultilingualManager
import net.kibotu.geofencerelay.features.ai.model.CpsAssessmentResult
import net.kibotu.geofencerelay.features.ai.report.ClinicalReportGenerator
import net.kibotu.geofencerelay.features.ai.risk.CognitiveAnomalyDetector
import net.kibotu.geofencerelay.features.ai.ui.components.IosBackPillButton
import net.kibotu.geofencerelay.features.ai.ui.theme.GoogleColors
import net.kibotu.geofencerelay.ui.theme.*

/**
 * Vibrant Cognitive Assessment Dashboard & Daily Scorecards.
 * - Cognitive age & biological age removed
 * - Visual stats styling adhering to high-contrast design system (Warm Orange, Forest Green, Royal Blue)
 * - Persistent lifetime cognitive scores and longitudinal daily scorecards
 */
@Composable
fun CognitiveHealthPanel(
    assessment: CpsAssessmentResult?,
    selectedLanguageCode: String,
    onLaunchGame: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Cognitive Stats, 1: Daily Scorecards
    val scorecards = remember { CognitiveHistoryManager.getAllScorecards(context) }

    val baselineHistory = remember { CognitiveAnomalyDetector.getSessionHistory(context) }
    val latestSession = baselineHistory.lastOrNull()
    val anomalyReport = remember(latestSession) {
        if (latestSession != null && baselineHistory.size > 1) {
            CognitiveAnomalyDetector.detectAnomalies(
                latestSession.accuracy,
                latestSession.responseTimeMs,
                latestSession.errors,
                baselineHistory.dropLast(1)
            )
        } else {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NerColors.CanvasWarm)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Top Authentic Woven Ribbon
        NerWovenRibbon(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            height = 14.dp,
            primaryColor = NerColors.Tertiary,
            secondaryColor = NerColors.Primary,
            accentColor = NerColors.Marigold
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = MultilingualManager.tr("tile_score_title", selectedLanguageCode),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NerColors.Charcoal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = MultilingualManager.tr("health_subtitle", selectedLanguageCode),
                fontSize = 13.sp,
                color = NerColors.NeutralMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Navigation Switcher Tabs: [Cognitive Overview] [Daily Scorecards]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NerColors.SurfaceWhite)
                    .border(1.dp, NerColors.NeutralBorder, RoundedCornerShape(14.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 0) NerColors.Tertiary else Color.Transparent)
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = if (selectedTab == 0) Color.White else NerColors.NeutralMedium, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cognitive Stats", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 0) Color.White else NerColors.Charcoal)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 1) NerColors.Tertiary else Color.Transparent)
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = if (selectedTab == 1) Color.White else NerColors.NeutralMedium, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Daily Scorecards", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 1) Color.White else NerColors.Charcoal)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // --- TAB 0: COGNITIVE STATS ---
                if (anomalyReport != null && anomalyReport.anomalyDetected) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = GoogleColors.Red.copy(alpha = 0.12f)),
                        border = BorderStroke(1.5.dp, GoogleColors.Red)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(GoogleColors.Red),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Acute Performance Drop Detected",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoogleColors.Red
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = anomalyReport.alerts.firstOrNull()?.message ?: "Performance significantly below baseline.",
                                    fontSize = 12.sp,
                                    color = NerColors.Charcoal,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                if (assessment == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, NerColors.NeutralBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(NerColors.TertiaryTint),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = NerColors.Tertiary, modifier = Modifier.size(46.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(MultilingualManager.tr("lbl_untested", selectedLanguageCode), fontSize = 19.sp, fontWeight = FontWeight.Bold, color = NerColors.Charcoal)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(MultilingualManager.tr("lbl_untested_desc", selectedLanguageCode), fontSize = 13.sp, color = NerColors.NeutralMedium, textAlign = TextAlign.Center, lineHeight = 19.sp)
                            Spacer(modifier = Modifier.height(20.dp))
                            NerPillButton(
                                text = MultilingualManager.tr("btn_start_test", selectedLanguageCode),
                                icon = Icons.Default.SportsEsports,
                                hierarchy = NerButtonHierarchy.Primary,
                                containerColor = NerColors.Primary,
                                onClick = onLaunchGame,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    // Overall CPS Score Card (Cognitive age & biological age removed)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        border = BorderStroke(1.dp, NerColors.NeutralBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(136.dp)
                                    .clip(CircleShape)
                                    .background(NerColors.TertiaryTint)
                                    .border(4.dp, NerColors.Tertiary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${assessment.cpsScore.toInt()}",
                                        fontSize = 46.sp,
                                        fontWeight = FontWeight.Black,
                                        color = NerColors.Tertiary
                                    )
                                    Text(
                                        text = "CPS SCORE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = NerColors.NeutralMedium,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GoogleColors.Green.copy(alpha = 0.15f))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Status: ${assessment.trajectoryStatus}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoogleColors.Green
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Multi-colored Horizontal Progress Stat Bars (Matching Reference UI Kit)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        border = BorderStroke(1.dp, NerColors.NeutralBorder)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BarChart, contentDescription = null, tint = NerColors.Tertiary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cognitive Telemetry Breakdown", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NerColors.Charcoal)
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            // Memory Retention: Royal Blue (#0D47A1)
                            SubScoreRow("Memory Retention Index", assessment.subScores.memoryRetentionIndex, Color(0xFF0D47A1))
                            // Executive Function: Warm Orange (#E65100)
                            SubScoreRow("Executive Function Index", assessment.subScores.executiveFunctionIndex, Color(0xFFE65100))
                            // Reaction Latency: Forest Green (#1B5E20)
                            SubScoreRow("Reaction Latency Score", assessment.subScores.reactionLatencyScore, Color(0xFF1B5E20))
                            // Autobiographical Reminiscence: Marigold
                            SubScoreRow("Autobiographical Reminiscence", assessment.subScores.autobiographicalReminiscence, NerColors.Marigold)
                            // Error Recovery Rate: Plum Maroon
                            SubScoreRow("Error Recovery Rate", assessment.subScores.errorRecoveryRate, NerColors.PlumMaroon)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Biomotor & Acoustic Diagnostics
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        border = BorderStroke(1.dp, NerColors.NeutralBorder)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Biomotor & Speech Acoustics", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NerColors.Charcoal)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Motor Tremor Jitter", fontSize = 11.sp, color = NerColors.NeutralMedium)
                                    Text("${assessment.motorJitterIndex} / 100", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NerColors.Charcoal)
                                    Text(assessment.motorDiagnostic, fontSize = 10.sp, color = GoogleColors.Green)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Speech Hesitation", fontSize = 11.sp, color = NerColors.NeutralMedium)
                                    Text("${assessment.speechHesitationScore} / 100", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NerColors.Charcoal)
                                    Text(assessment.speechDiagnostic, fontSize = 10.sp, color = GoogleColors.Green)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 30 & 90 Days Projections Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        border = BorderStroke(1.dp, NerColors.NeutralBorder)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = NerColors.Secondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${MultilingualManager.tr("health_forecast", selectedLanguageCode)}: ${assessment.trajectoryStatus}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NerColors.Charcoal
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(MultilingualManager.tr("health_30days", selectedLanguageCode), fontSize = 12.sp, color = NerColors.NeutralMedium)
                                    Text("${assessment.projectedCps30Days} CPS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NerColors.Secondary)
                                }
                                Column {
                                    Text(MultilingualManager.tr("health_90days", selectedLanguageCode), fontSize = 12.sp, color = NerColors.NeutralMedium)
                                    Text("${assessment.projectedCps90Days} CPS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NerColors.Tertiary)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = assessment.caregiverReminiscencePlan,
                                fontSize = 12.sp,
                                color = NerColors.NeutralMedium,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    NerPillButton(
                        text = "Share Clinical Diagnostic Report",
                        onClick = {
                            val reportMd = ClinicalReportGenerator.generateMarkdownReport(assessment)
                            ClinicalReportGenerator.shareClinicalReport(context, reportMd, "Senior Participant")
                        },
                        containerColor = NerColors.Tertiary,
                        icon = Icons.Default.Share,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                // --- TAB 1: DAILY SCORECARDS ---
                if (scorecards.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, NerColors.NeutralBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(NerColors.NeutralSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.EventNote, contentDescription = null, tint = NerColors.NeutralMedium, modifier = Modifier.size(36.dp))
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("No Daily Scorecards Yet", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NerColors.Charcoal)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Play your first cognitive game to generate today's scorecard!", fontSize = 13.sp, color = NerColors.NeutralMedium, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(18.dp))
                            NerPillButton(
                                text = "Play a Game",
                                icon = Icons.Default.PlayArrow,
                                containerColor = NerColors.Primary,
                                onClick = onLaunchGame,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    scorecards.forEach { card ->
                        DailyScorecardCard(card = card)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        IosBackPillButton(
            label = MultilingualManager.tr("btn_back", selectedLanguageCode),
            onClick = onBack
        )
    }
}

@Composable
private fun DailyScorecardCard(card: DailyScorecardItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, NerColors.NeutralBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(card.gameType, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NerColors.Charcoal)
                    Text(card.dateFormatted, fontSize = 11.sp, color = NerColors.NeutralMedium)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GoogleColors.Blue.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Level: ${card.difficulty}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoogleColors.Blue)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Score & Stats Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NerColors.CanvasWarm)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${card.cpsScore.toInt()} / 100", fontWeight = FontWeight.Black, fontSize = 15.sp, color = NerColors.Tertiary)
                    Text("CPS Score", fontSize = 10.sp, color = NerColors.NeutralMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(card.accuracy * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GoogleColors.Green)
                    Text("Accuracy", fontSize = 10.sp, color = NerColors.NeutralMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val sec = card.durationMs / 1000
                    Text(String.format("%02d:%02d", sec / 60, sec % 60), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NerColors.Charcoal)
                    Text("Duration", fontSize = 10.sp, color = NerColors.NeutralMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${card.errors}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (card.errors > 0) GoogleColors.Red else GoogleColors.Green)
                    Text("Errors", fontSize = 10.sp, color = NerColors.NeutralMedium)
                }
            }

            if (card.patientMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"${card.patientMessage}\"",
                    fontSize = 11.sp,
                    color = NerColors.NeutralMedium,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun SubScoreRow(
    label: String,
    score: Double,
    tintColor: Color
) {
    val progress = (score / 100.0).coerceIn(0.0, 1.0).toFloat()
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NerColors.Charcoal)
            Text("${score.toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tintColor)
        }
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(NerColors.NeutralSoft)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(tintColor)
            )
        }
    }
}
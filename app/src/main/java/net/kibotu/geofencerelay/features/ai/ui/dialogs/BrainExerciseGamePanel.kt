package net.kibotu.geofencerelay.features.ai.ui.dialogs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kibotu.geofencerelay.features.ai.engine.CpsEngine
import net.kibotu.geofencerelay.features.ai.history.CognitiveHistoryManager
import net.kibotu.geofencerelay.features.ai.history.DailyScorecardItem
import net.kibotu.geofencerelay.features.ai.localization.MultilingualManager
import net.kibotu.geofencerelay.features.ai.model.CpsAssessmentResult
import net.kibotu.geofencerelay.features.ai.model.GameSessionTelemetry
import net.kibotu.geofencerelay.features.ai.reminder.GameReminderManager
import net.kibotu.geofencerelay.features.ai.risk.CognitiveAnomalyDetector
import net.kibotu.geofencerelay.features.ai.service.SmaranAiClient
import net.kibotu.geofencerelay.features.ai.service.SmaranAiSessionAnalysisResponse
import net.kibotu.geofencerelay.features.ai.ui.components.IosBackPillButton
import net.kibotu.geofencerelay.features.ai.ui.theme.GoogleColors
import net.kibotu.geofencerelay.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class ActiveGameMode {
    HUB,
    MEMORY_MATCHING,
    PATTERN_RECOGNITION,
    STROOP_CHALLENGE,
    TRAIL_MAKING
}

data class MatchSymbol(
    val id: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color,
    val name: String
)

data class CardItem(
    val id: Int,
    val symbolItem: MatchSymbol,
    var isFlipped: Boolean = false,
    var isMatched: Boolean = false
)

data class GameSessionMetrics(
    val durationMs: Long,
    val accuracy: Double,
    val attempts: Int,
    val errors: Int,
    val hintsUsed: Int = 0,
    val completionRate: Double = 1.0,
    val gameType: String
)

/**
 * Full-Screen Cognitive Games Hub supporting 4 rich clinical games.
 * Difficulty is autonomously selected and adapted by the AI ML engine.
 */
@Composable
fun BrainExerciseGamePanel(
    selectedLanguageCode: String,
    initialGameMode: ActiveGameMode = ActiveGameMode.HUB,
    onAssessmentUpdated: (CpsAssessmentResult) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var activeMode by remember(initialGameMode) { mutableStateOf(initialGameMode) }

    when (activeMode) {
        ActiveGameMode.HUB -> {
            GameHubSelectionView(
                selectedLanguageCode = selectedLanguageCode,
                onSelectGame = { mode -> activeMode = mode },
                onBack = onBack
            )
        }
        ActiveGameMode.MEMORY_MATCHING -> {
            FullScreenMemoryMatchingGameView(
                selectedLanguageCode = selectedLanguageCode,
                onAssessmentUpdated = { res ->
                    GameReminderManager.recordGamePlayed(context)
                    onAssessmentUpdated(res)
                },
                onBack = { activeMode = ActiveGameMode.HUB }
            )
        }
        ActiveGameMode.PATTERN_RECOGNITION -> {
            FullScreenPatternSequenceGameView(
                selectedLanguageCode = selectedLanguageCode,
                onAssessmentUpdated = { res ->
                    GameReminderManager.recordGamePlayed(context)
                    onAssessmentUpdated(res)
                },
                onBack = { activeMode = ActiveGameMode.HUB }
            )
        }
        ActiveGameMode.STROOP_CHALLENGE -> {
            ColorStroopChallengeGameView(
                selectedLanguageCode = selectedLanguageCode,
                onAssessmentUpdated = { res ->
                    GameReminderManager.recordGamePlayed(context)
                    onAssessmentUpdated(res)
                },
                onBack = { activeMode = ActiveGameMode.HUB }
            )
        }
        ActiveGameMode.TRAIL_MAKING -> {
            AscendingTrailMakingGameView(
                selectedLanguageCode = selectedLanguageCode,
                onAssessmentUpdated = { res ->
                    GameReminderManager.recordGamePlayed(context)
                    onAssessmentUpdated(res)
                },
                onBack = { activeMode = ActiveGameMode.HUB }
            )
        }
    }
}

/**
 * Hub Screen with AI Cognitive Score Banner, 4 Games & Reminder Configuration.
 */
@Composable
private fun GameHubSelectionView(
    selectedLanguageCode: String,
    onSelectGame: (ActiveGameMode) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var reminderInterval by remember { mutableStateOf(GameReminderManager.getReminderInterval(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NerColors.CanvasWarm)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NerWovenRibbon(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                height = 14.dp,
                primaryColor = NerColors.Primary,
                secondaryColor = NerColors.Secondary,
                accentColor = NerColors.Marigold
            )
            Text(
                text = MultilingualManager.tr("games_hub_title", selectedLanguageCode),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NerColors.Charcoal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = MultilingualManager.tr("games_hub_sub", selectedLanguageCode),
                fontSize = 12.sp,
                color = NerColors.NeutralMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Game 1: Memory Match
            val memDiff = remember { SmaranAiClient.getRecommendedDifficulty(context, "memory_matching") }
            GameSelectionCard(
                title = MultilingualManager.tr("game1_name", selectedLanguageCode),
                desc = MultilingualManager.tr("game1_desc", selectedLanguageCode),
                icon = Icons.Default.Style,
                color = GoogleColors.Blue,
                aiDifficulty = memDiff,
                onClick = { onSelectGame(ActiveGameMode.MEMORY_MATCHING) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Game 2: 6-Pad Pattern & Sequence Logic
            val patDiff = remember { SmaranAiClient.getRecommendedDifficulty(context, "pattern_recognition") }
            GameSelectionCard(
                title = MultilingualManager.tr("game2_name", selectedLanguageCode),
                desc = MultilingualManager.tr("game2_desc", selectedLanguageCode),
                icon = Icons.Default.Extension,
                color = GoogleColors.Green,
                aiDifficulty = patDiff,
                onClick = { onSelectGame(ActiveGameMode.PATTERN_RECOGNITION) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Game 3: Color-Word Stroop Challenge
            val stroopDiff = remember { SmaranAiClient.getRecommendedDifficulty(context, "stroop_challenge") }
            GameSelectionCard(
                title = MultilingualManager.tr("game3_title", selectedLanguageCode),
                desc = MultilingualManager.tr("game3_desc", selectedLanguageCode),
                icon = Icons.Default.ColorLens,
                color = GoogleColors.Red,
                aiDifficulty = stroopDiff,
                onClick = { onSelectGame(ActiveGameMode.STROOP_CHALLENGE) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Game 4: Ascending Number Trail Making
            val trailDiff = remember { SmaranAiClient.getRecommendedDifficulty(context, "trail_making") }
            GameSelectionCard(
                title = MultilingualManager.tr("game4_title", selectedLanguageCode),
                desc = MultilingualManager.tr("game4_desc", selectedLanguageCode),
                icon = Icons.Default.Pin,
                color = GoogleColors.Yellow,
                aiDifficulty = trailDiff,
                onClick = { onSelectGame(ActiveGameMode.TRAIL_MAKING) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Game Reminder Interval Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(NerColors.NeutralBorder),
                    width = 1.dp
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Alarm, contentDescription = null, tint = GoogleColors.Red, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(MultilingualManager.tr("lbl_reminder_interval", selectedLanguageCode), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NerColors.Charcoal)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(MultilingualManager.tr("lbl_reminder_interval_desc", selectedLanguageCode), fontSize = 11.sp, color = NerColors.NeutralMedium)

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(
                            Pair(1L, "1 min (Test)"),
                            Pair(60L, "1 hr"),
                            Pair(120L, "2 hrs"),
                            Pair(240L, "4 hrs")
                        ).forEach { pair ->
                            val isSel = reminderInterval == pair.first
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) GoogleColors.Blue else NerColors.NeutralSoft)
                                    .clickable {
                                        reminderInterval = pair.first
                                        GameReminderManager.setReminderInterval(context, pair.first)
                                        android.widget.Toast.makeText(
                                            context,
                                            "Reminder interval set to ${pair.second}. Alarm is armed!",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = pair.second,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color.White else NerColors.Charcoal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            GameReminderManager.triggerTestAlarmInSeconds(context, 3)
                            android.widget.Toast.makeText(
                                context,
                                "Alarm will trigger in 3 seconds! Turn screen OFF or close app now.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoogleColors.Red),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = MultilingualManager.tr("btn_test_alarm", selectedLanguageCode),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        IosBackPillButton(
            label = MultilingualManager.tr("btn_back", selectedLanguageCode),
            onClick = onBack
        )
    }
}

@Composable
private fun GameSelectionCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    aiDifficulty: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(NerColors.NeutralBorder),
            width = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NerColors.Charcoal)
                Spacer(modifier = Modifier.height(2.dp))
                Text(desc, fontSize = 11.sp, color = NerColors.NeutralMedium, lineHeight = 15.sp)
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NerColors.NeutralMedium)
        }
    }
}

/**
 * In-Game Live HUD: Timer, Moves / Attempts, Errors & AI-Selected Difficulty Badge.
 * (Manual selector removed; AI selects the level automatically).
 */
@Composable
private fun GameLiveHud(
    elapsedSeconds: Int,
    attempts: Int,
    errors: Int,
    activeDifficulty: String
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(NerColors.NeutralBorder),
            width = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = GoogleColors.Blue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(timeFormatted, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NerColors.Charcoal)
            }

            // Attempts
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TouchApp, contentDescription = null, tint = NerColors.NeutralMedium, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("$attempts moves", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = NerColors.Charcoal)
            }

            // Errors
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Close, contentDescription = null, tint = if (errors > 0) GoogleColors.Red else GoogleColors.Green, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("$errors err", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = if (errors > 0) GoogleColors.Red else NerColors.Charcoal)
            }

            // AI-Selected Difficulty Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(GoogleColors.Blue.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "AI Level: $activeDifficulty",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoogleColors.Blue
                )
            }
        }
    }
}

/**
 * Post-Game Clinical & AI Result Screen (Mirrors monorepo GameResultMetricsWidget + AI Pipeline).
 */
@Composable
private fun ClinicalGameResultCard(
    metrics: GameSessionMetrics,
    analysis: SmaranAiSessionAnalysisResponse?,
    selectedLanguageCode: String,
    onPlayAgain: (recommendedDiff: String) -> Unit,
    onBackToHub: () -> Unit
) {
    val isCompleted = metrics.completionRate >= 0.8
    val durationSec = metrics.durationMs / 1000
    val durationFormatted = String.format("%02d:%02d", durationSec / 60, durationSec % 60)
    val recLevel = analysis?.recommendedLevel ?: "Medium"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(NerColors.NeutralBorder),
            width = 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Completion Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isCompleted) GoogleColors.Green else GoogleColors.Yellow)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Timelapse,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCompleted) "Well Done!" else "Good Effort!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Metrics Grid (Duration, Accuracy, Attempts, Errors)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem(label = "Duration", value = durationFormatted, icon = Icons.Default.Timer, color = GoogleColors.Blue, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                MetricItem(label = "Accuracy", value = "${(metrics.accuracy * 100).roundToInt()}%", icon = Icons.Default.TrackChanges, color = GoogleColors.Green, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem(label = "Attempts", value = "${metrics.attempts}", icon = Icons.Default.TouchApp, color = NerColors.Charcoal, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                MetricItem(label = "Errors", value = "${metrics.errors}", icon = Icons.Default.Close, color = if (metrics.errors > 0) GoogleColors.Red else GoogleColors.Green, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. SMARAN AI & ML Adaptive Diagnostics Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NerColors.CanvasIvory),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(GoogleColors.Blue.copy(alpha = 0.3f)),
                    width = 1.dp
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = GoogleColors.Blue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SMARAN AI Adaptive Engine", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NerColors.Charcoal)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GoogleColors.Blue)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("AI Next: $recLevel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (analysis != null) {
                        Text(
                            text = "\"${analysis.patientMessage}\"",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = NerColors.Charcoal,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Caregiver Clinical Note: ${analysis.caregiverSummary}",
                            fontSize = 11.sp,
                            color = NerColors.NeutralMedium,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // CPS Score & Sub-scores
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            SubScoreBadge("CPS Score", "${analysis.cpsScore.roundToInt()}/100", GoogleColors.Blue)
                            SubScoreBadge("Memory", "${analysis.memoryRetentionIndex.roundToInt()}%", GoogleColors.Green)
                            SubScoreBadge("Reaction", "${analysis.reactionLatencyScore.roundToInt()}%", GoogleColors.Yellow)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Anomaly Warning Banner if triggered
                        if (analysis.anomalyDetected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GoogleColors.Red.copy(alpha = 0.15f))
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = GoogleColors.Red, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Acute Drop Alert: ${analysis.anomalyMessage}", fontSize = 10.sp, color = GoogleColors.Red, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Action Buttons
            Button(
                onClick = { onPlayAgain(recLevel) },
                colors = ButtonDefaults.buttonColors(containerColor = GoogleColors.Blue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Replay, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play Next Round (AI Level: $recLevel)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onBackToHub,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null, tint = NerColors.Charcoal)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back to Games Hub", fontSize = 14.sp, color = NerColors.Charcoal)
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(NerColors.CanvasWarm)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NerColors.Charcoal)
                Text(label, fontSize = 10.sp, color = NerColors.NeutralMedium)
            }
        }
    }
}

@Composable
private fun SubScoreBadge(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = color)
        Text(label, fontSize = 10.sp, color = NerColors.NeutralMedium)
    }
}

/**
 * Game 1: Multi-Round Memory Matching Game.
 * Difficulty is autonomously chosen by AI.
 */
@Composable
private fun FullScreenMemoryMatchingGameView(
    selectedLanguageCode: String,
    onAssessmentUpdated: (CpsAssessmentResult) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activeDifficulty by remember { mutableStateOf(SmaranAiClient.getRecommendedDifficulty(context, "memory_matching")) }

    val symbolsPool = remember {
        listOf(
            MatchSymbol(1, Icons.Default.Favorite, GoogleColors.Red, "Heart"),
            MatchSymbol(2, Icons.Default.Star, GoogleColors.Yellow, "Star"),
            MatchSymbol(3, Icons.Default.WbSunny, GoogleColors.Blue, "Sun"),
            MatchSymbol(4, Icons.Default.Pets, GoogleColors.Green, "Pet"),
            MatchSymbol(5, Icons.Default.LocalFlorist, Color(0xFFFF4081), "Flower"),
            MatchSymbol(6, Icons.Default.DirectionsCar, Color(0xFF00BCD4), "Car"),
            MatchSymbol(7, Icons.Default.MusicNote, Color(0xFFAB47BC), "Music"),
            MatchSymbol(8, Icons.Default.Park, Color(0xFF4CAF50), "Tree")
        )
    }

    val pairsForDifficulty = when (activeDifficulty.lowercase()) {
        "easy" -> 3
        "hard" -> 6
        else -> 4 // Medium
    }

    var cards by remember(activeDifficulty) {
        val picked = symbolsPool.shuffled().take(pairsForDifficulty)
        val deck = (picked + picked).shuffled().mapIndexed { idx, s ->
            CardItem(id = idx, symbolItem = s)
        }
        mutableStateOf(deck)
    }

    var flippedIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    var totalAttempts by remember { mutableStateOf(0) }
    var totalErrors by remember { mutableStateOf(0) }
    var isGameFinished by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var isBusyChecking by remember { mutableStateOf(false) }
    var sessionAnalysis by remember { mutableStateOf<SmaranAiSessionAnalysisResponse?>(null) }
    var lastMetrics by remember { mutableStateOf<GameSessionMetrics?>(null) }

    LaunchedEffect(isGameFinished) {
        while (!isGameFinished) {
            delay(1000)
            elapsedSeconds++
        }
    }

    fun resetGame(diff: String = activeDifficulty) {
        activeDifficulty = diff
        val pairs = when (diff.lowercase()) {
            "easy" -> 3
            "hard" -> 6
            else -> 4
        }
        val picked = symbolsPool.shuffled().take(pairs)
        cards = (picked + picked).shuffled().mapIndexed { idx, s -> CardItem(id = idx, symbolItem = s) }
        flippedIndices = emptyList()
        totalAttempts = 0
        totalErrors = 0
        elapsedSeconds = 0
        isGameFinished = false
        sessionAnalysis = null
        lastMetrics = null
        isBusyChecking = false
    }

    LaunchedEffect(flippedIndices) {
        if (flippedIndices.size == 2) {
            isBusyChecking = true
            totalAttempts++
            val firstIdx = flippedIndices[0]
            val secondIdx = flippedIndices[1]

            if (cards[firstIdx].symbolItem.id == cards[secondIdx].symbolItem.id) {
                delay(200)
                val updatedCards = cards.mapIndexed { idx, card ->
                    if (idx == firstIdx || idx == secondIdx) card.copy(isMatched = true, isFlipped = true)
                    else card
                }
                cards = updatedCards

                val allMatched = updatedCards.all { it.isMatched }
                if (allMatched) {
                    val durationMs = elapsedSeconds * 1000L
                    val accuracy = if (totalAttempts > 0) (pairsForDifficulty.toDouble() / totalAttempts).coerceIn(0.0, 1.0) else 1.0
                    val telemetry = GameSessionTelemetry(
                        gameType = "memory_matching",
                        accuracy = accuracy,
                        responseTimeMs = durationMs,
                        attempts = totalAttempts,
                        errors = totalErrors,
                        completionRate = 1.0
                    )
                    val result = CpsEngine.analyzeSession(telemetry, selectedLanguageCode)
                    CognitiveAnomalyDetector.recordSessionToHistory(context, telemetry.accuracy, telemetry.responseTimeMs, telemetry.errors)
                    CognitiveHistoryManager.saveAssessment(context, result)

                    val metrics = GameSessionMetrics(
                        durationMs = durationMs,
                        accuracy = accuracy,
                        attempts = totalAttempts,
                        errors = totalErrors,
                        hintsUsed = 0,
                        completionRate = 1.0,
                        gameType = "memory_matching"
                    )
                    lastMetrics = metrics

                    scope.launch {
                        val analysis = SmaranAiClient.analyzeSession(
                            context = context,
                            gameType = "memory_matching",
                            currentDifficulty = activeDifficulty,
                            accuracy = accuracy,
                            completionRate = 1.0,
                            responseTimeMs = durationMs,
                            errors = totalErrors,
                            hintsUsed = 0
                        )
                        sessionAnalysis = analysis

                        // Persist to Lifetime Daily Scorecard
                        val scorecard = DailyScorecardItem(
                            deviceId = net.kibotu.geofencerelay.service.TrackerForegroundService.getDeviceId(context),
                            deviceName = net.kibotu.geofencerelay.service.TrackerForegroundService.getDeviceName(),
                            dateFormatted = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date()),
                            dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                            gameType = "Memory Matching",
                            difficulty = activeDifficulty,
                            cpsScore = analysis.cpsScore,
                            accuracy = accuracy,
                            durationMs = durationMs,
                            attempts = totalAttempts,
                            errors = totalErrors,
                            memoryRetention = analysis.memoryRetentionIndex,
                            reactionLatency = analysis.reactionLatencyScore,
                            executiveFunction = analysis.executiveFunctionIndex,
                            patientMessage = analysis.patientMessage,
                            caregiverSummary = analysis.caregiverSummary,
                            anomalyDetected = analysis.anomalyDetected
                        )
                        CognitiveHistoryManager.recordScorecard(context, scorecard)

                        isGameFinished = true
                        MultilingualManager.speak(analysis.patientMessage, selectedLanguageCode)
                    }
                    onAssessmentUpdated(result)
                    flippedIndices = emptyList()
                    isBusyChecking = false
                } else {
                    flippedIndices = emptyList()
                    isBusyChecking = false
                }
            } else {
                totalErrors++
                delay(700)
                cards = cards.mapIndexed { idx, card ->
                    if (idx == firstIdx || idx == secondIdx) card.copy(isFlipped = false)
                    else card
                }
                flippedIndices = emptyList()
                isBusyChecking = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NerColors.CanvasWarm)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        val resultScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .then(if (isGameFinished) Modifier.verticalScroll(resultScrollState) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = MultilingualManager.tr("game1_name", selectedLanguageCode),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NerColors.Charcoal
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (!isGameFinished) {
                GameLiveHud(elapsedSeconds = elapsedSeconds, attempts = totalAttempts, errors = totalErrors, activeDifficulty = activeDifficulty)
            }

            if (isGameFinished && lastMetrics != null) {
                ClinicalGameResultCard(
                    metrics = lastMetrics!!,
                    analysis = sessionAnalysis,
                    selectedLanguageCode = selectedLanguageCode,
                    onPlayAgain = { recDiff -> resetGame(recDiff) },
                    onBackToHub = onBack
                )
            } else {
                val cols = if (pairsForDifficulty <= 4) 2 else 3
                LazyVerticalGrid(
                    columns = GridCells.Fixed(cols),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(cards) { idx, card ->
                        val cardBg by animateColorAsState(
                            targetValue = if (card.isMatched) GoogleColors.Green.copy(alpha = 0.25f)
                            else if (card.isFlipped) NerColors.NeutralSoft
                            else NerColors.SurfaceWhite,
                            animationSpec = tween(200),
                            label = "cardBg"
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (cols == 2) 115.dp else 95.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(enabled = !card.isFlipped && !card.isMatched && !isBusyChecking) {
                                    if (flippedIndices.size < 2) {
                                        cards = cards.mapIndexed { i, c ->
                                            if (i == idx) c.copy(isFlipped = true) else c
                                        }
                                        flippedIndices = flippedIndices + idx
                                    }
                                },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (card.isMatched) GoogleColors.Green else NerColors.NeutralBorder
                                ),
                                width = 1.5.dp
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (card.isFlipped || card.isMatched) {
                                    Icon(
                                        imageVector = card.symbolItem.icon,
                                        contentDescription = card.symbolItem.name,
                                        tint = card.symbolItem.tint,
                                        modifier = Modifier.size(if (cols == 2) 52.dp else 42.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(GoogleColors.Blue.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = GoogleColors.Blue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!isGameFinished) {
            Spacer(modifier = Modifier.height(10.dp))
            IosBackPillButton(
                label = MultilingualManager.tr("btn_back", selectedLanguageCode),
                onClick = onBack
            )
        }
    }
}

/**
 * Game 2: Multi-Round Cultural Pattern & Sequence Recall.
 * Difficulty is autonomously chosen by AI.
 */
data class PatternPadItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val label: String
)

@Composable
private fun FullScreenPatternSequenceGameView(
    selectedLanguageCode: String,
    onAssessmentUpdated: (CpsAssessmentResult) -> Unit,
    onBack: () -> Unit
) {
    val items = remember {
        listOf(
            PatternPadItem(Icons.Default.Star, GoogleColors.Yellow, "Star"),
            PatternPadItem(Icons.Default.Favorite, GoogleColors.Red, "Heart"),
            PatternPadItem(Icons.Default.Eco, GoogleColors.Green, "Nature"),
            PatternPadItem(Icons.Default.WbSunny, GoogleColors.Blue, "Sun"),
            PatternPadItem(Icons.Default.MusicNote, Color(0xFFAB47BC), "Music"),
            PatternPadItem(Icons.Default.DirectionsCar, Color(0xFF00BCD4), "Car")
        )
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var activeDifficulty by remember { mutableStateOf(SmaranAiClient.getRecommendedDifficulty(context, "pattern_recognition")) }

    val seqLength = when (activeDifficulty.lowercase()) {
        "easy" -> 3
        "hard" -> 5
        else -> 4
    }

    fun generateSequence(len: Int): List<Int> = (1..len).map { (0..5).random() }

    var sequence by remember(activeDifficulty) { mutableStateOf(generateSequence(seqLength)) }
    var highlightedIndex by remember { mutableStateOf<Int?>(null) }
    var userTappedHighlightIdx by remember { mutableStateOf<Int?>(null) }
    var isShowingSequence by remember { mutableStateOf(true) }
    var userTappedSteps by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isFinished by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var totalAttempts by remember { mutableStateOf(0) }
    var totalErrors by remember { mutableStateOf(0) }
    var sessionAnalysis by remember { mutableStateOf<SmaranAiSessionAnalysisResponse?>(null) }
    var lastMetrics by remember { mutableStateOf<GameSessionMetrics?>(null) }

    LaunchedEffect(isFinished) {
        while (!isFinished) {
            delay(1000)
            elapsedSeconds++
        }
    }

    fun playSequence(diff: String = activeDifficulty) {
        activeDifficulty = diff
        val len = when (diff.lowercase()) {
            "easy" -> 3
            "hard" -> 5
            else -> 4
        }
        sequence = generateSequence(len)
        highlightedIndex = null
        userTappedHighlightIdx = null
        userTappedSteps = emptyList()
        isShowingSequence = true
        isFinished = false
        sessionAnalysis = null
        lastMetrics = null
        elapsedSeconds = 0
        totalAttempts = 0
        totalErrors = 0
    }

    LaunchedEffect(isShowingSequence, sequence) {
        if (isShowingSequence) {
            delay(500)
            for (step in sequence) {
                highlightedIndex = step
                delay(550)
                highlightedIndex = null
                delay(200)
            }
            isShowingSequence = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NerColors.CanvasWarm)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        val resultScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .then(if (isFinished) Modifier.verticalScroll(resultScrollState) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = MultilingualManager.tr("game2_name", selectedLanguageCode),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NerColors.Charcoal
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (!isFinished) {
                GameLiveHud(elapsedSeconds = elapsedSeconds, attempts = totalAttempts, errors = totalErrors, activeDifficulty = activeDifficulty)

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    sequence.indices.forEach { stepIdx ->
                        val isDone = stepIdx < userTappedSteps.size
                        val isCurrent = stepIdx == userTappedSteps.size && !isShowingSequence
                        val dotColor = when {
                            isDone -> GoogleColors.Green
                            isCurrent -> GoogleColors.Blue
                            else -> Color.LightGray
                        }
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isCurrent) 12.dp else 10.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isFinished && lastMetrics != null) {
                ClinicalGameResultCard(
                    metrics = lastMetrics!!,
                    analysis = sessionAnalysis,
                    selectedLanguageCode = selectedLanguageCode,
                    onPlayAgain = { recDiff -> playSequence(recDiff) },
                    onBackToHub = onBack
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(items) { idx, item ->
                        val isLitBySystem = highlightedIndex == idx
                        val isLitByUser = userTappedHighlightIdx == idx
                        val isLit = isLitBySystem || isLitByUser

                        val cardBg by animateColorAsState(
                            targetValue = if (isLit) item.color.copy(alpha = 0.45f) else NerColors.SurfaceWhite,
                            animationSpec = tween(150),
                            label = "padColor"
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(115.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(enabled = !isShowingSequence) {
                                    totalAttempts++
                                    userTappedHighlightIdx = idx
                                    coroutineScope.launch {
                                        delay(250)
                                        if (userTappedHighlightIdx == idx) userTappedHighlightIdx = null
                                    }

                                    val nextExpected = sequence[userTappedSteps.size]
                                    if (idx == nextExpected) {
                                        val newSteps = userTappedSteps + idx
                                        userTappedSteps = newSteps
                                        if (newSteps.size == sequence.size) {
                                            val durationMs = elapsedSeconds * 1000L
                                            val acc = if (totalAttempts > 0) (sequence.size.toDouble() / totalAttempts).coerceIn(0.0, 1.0) else 1.0
                                            val telemetry = GameSessionTelemetry(
                                                gameType = "pattern_recognition",
                                                accuracy = acc,
                                                responseTimeMs = durationMs,
                                                attempts = totalAttempts,
                                                errors = totalErrors,
                                                completionRate = 1.0
                                            )
                                            val result = CpsEngine.analyzeSession(telemetry, selectedLanguageCode)
                                            CognitiveAnomalyDetector.recordSessionToHistory(context, telemetry.accuracy, telemetry.responseTimeMs, telemetry.errors)
                                            CognitiveHistoryManager.saveAssessment(context, result)

                                            val metrics = GameSessionMetrics(
                                                durationMs = durationMs,
                                                accuracy = acc,
                                                attempts = totalAttempts,
                                                errors = totalErrors,
                                                hintsUsed = 0,
                                                completionRate = 1.0,
                                                gameType = "pattern_recognition"
                                            )
                                            lastMetrics = metrics

                                            coroutineScope.launch {
                                                val analysis = SmaranAiClient.analyzeSession(
                                                    context = context,
                                                    gameType = "pattern_recognition",
                                                    currentDifficulty = activeDifficulty,
                                                    accuracy = acc,
                                                    completionRate = 1.0,
                                                    responseTimeMs = durationMs,
                                                    errors = totalErrors,
                                                    hintsUsed = 0
                                                )
                                                sessionAnalysis = analysis

                                                // Record Daily Scorecard
                                                val scorecard = DailyScorecardItem(
                                                    deviceId = net.kibotu.geofencerelay.service.TrackerForegroundService.getDeviceId(context),
                                                    deviceName = net.kibotu.geofencerelay.service.TrackerForegroundService.getDeviceName(),
                                                    dateFormatted = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date()),
                                                    dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                                    gameType = "Pattern Sequence",
                                                    difficulty = activeDifficulty,
                                                    cpsScore = analysis.cpsScore,
                                                    accuracy = acc,
                                                    durationMs = durationMs,
                                                    attempts = totalAttempts,
                                                    errors = totalErrors,
                                                    memoryRetention = analysis.memoryRetentionIndex,
                                                    reactionLatency = analysis.reactionLatencyScore,
                                                    executiveFunction = analysis.executiveFunctionIndex,
                                                    patientMessage = analysis.patientMessage,
                                                    caregiverSummary = analysis.caregiverSummary,
                                                    anomalyDetected = analysis.anomalyDetected
                                                )
                                                CognitiveHistoryManager.recordScorecard(context, scorecard)

                                                isFinished = true
                                                MultilingualManager.speak(analysis.patientMessage, selectedLanguageCode)
                                            }
                                            onAssessmentUpdated(result)
                                        }
                                    } else {
                                        totalErrors++
                                        coroutineScope.launch {
                                            delay(500)
                                            userTappedSteps = emptyList()
                                            isShowingSequence = true
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isLit) 8.dp else 2.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isLit) item.color else NerColors.NeutralBorder
                                ),
                                width = if (isLit) 3.dp else 1.dp
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (isLit) Color.White else item.color,
                                    modifier = Modifier.size(52.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!isFinished) {
            Spacer(modifier = Modifier.height(10.dp))
            IosBackPillButton(
                label = MultilingualManager.tr("btn_back", selectedLanguageCode),
                onClick = onBack
            )
        }
    }
}

/**
 * Game 3: Color-Word Stroop Challenge.
 * Difficulty is autonomously chosen by AI.
 */
@Composable
private fun ColorStroopChallengeGameView(
    selectedLanguageCode: String,
    onAssessmentUpdated: (CpsAssessmentResult) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var activeDifficulty by remember { mutableStateOf(SmaranAiClient.getRecommendedDifficulty(context, "stroop_challenge")) }

    val colorOptions = listOf(
        Triple("BLUE", MultilingualManager.tr("color_blue", selectedLanguageCode), GoogleColors.Blue),
        Triple("RED", MultilingualManager.tr("color_red", selectedLanguageCode), GoogleColors.Red),
        Triple("GREEN", MultilingualManager.tr("color_green", selectedLanguageCode), GoogleColors.Green),
        Triple("YELLOW", MultilingualManager.tr("color_yellow", selectedLanguageCode), GoogleColors.Yellow)
    )

    val totalRounds = when (activeDifficulty.lowercase()) {
        "easy" -> 5
        "hard" -> 12
        else -> 8
    }

    var currentWordIndex by remember { mutableStateOf(0) }
    var currentInkColorIndex by remember { mutableStateOf(1) }
    var scoreCount by remember { mutableStateOf(0) }
    var currentRound by remember { mutableStateOf(1) }
    var totalErrors by remember { mutableStateOf(0) }
    var isFinished by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var sessionAnalysis by remember { mutableStateOf<SmaranAiSessionAnalysisResponse?>(null) }
    var lastMetrics by remember { mutableStateOf<GameSessionMetrics?>(null) }

    LaunchedEffect(isFinished) {
        while (!isFinished) {
            delay(1000)
            elapsedSeconds++
        }
    }

    fun resetStroop(diff: String = activeDifficulty) {
        activeDifficulty = diff
        scoreCount = 0
        currentRound = 1
        totalErrors = 0
        elapsedSeconds = 0
        isFinished = false
        sessionAnalysis = null
        lastMetrics = null
        currentWordIndex = (0..3).random()
        var ink = (0..3).random()
        while (ink == currentWordIndex) ink = (0..3).random()
        currentInkColorIndex = ink
    }

    fun nextStroopQuestion(userChoiceInk: Int) {
        if (userChoiceInk == currentInkColorIndex) {
            scoreCount++
        } else {
            totalErrors++
        }

        if (currentRound >= totalRounds) {
            val durationMs = elapsedSeconds * 1000L
            val accuracy = (scoreCount.toDouble() / totalRounds).coerceIn(0.0, 1.0)
            val telemetry = GameSessionTelemetry(
                gameType = "stroop_challenge",
                accuracy = accuracy,
                responseTimeMs = durationMs,
                attempts = totalRounds,
                errors = totalErrors,
                completionRate = 1.0
            )
            val result = CpsEngine.analyzeSession(telemetry, selectedLanguageCode)
            CognitiveAnomalyDetector.recordSessionToHistory(context, telemetry.accuracy, telemetry.responseTimeMs, telemetry.errors)
            CognitiveHistoryManager.saveAssessment(context, result)

            val metrics = GameSessionMetrics(
                durationMs = durationMs,
                accuracy = accuracy,
                attempts = totalRounds,
                errors = totalErrors,
                hintsUsed = 0,
                completionRate = 1.0,
                gameType = "stroop_challenge"
            )
            lastMetrics = metrics

            coroutineScope.launch {
                val analysis = SmaranAiClient.analyzeSession(
                    context = context,
                    gameType = "stroop_challenge",
                    currentDifficulty = activeDifficulty,
                    accuracy = accuracy,
                    completionRate = 1.0,
                    responseTimeMs = durationMs,
                    errors = totalErrors,
                    hintsUsed = 0
                )
                sessionAnalysis = analysis

                // Record Daily Scorecard
                val scorecard = DailyScorecardItem(
                    deviceId = net.kibotu.geofencerelay.service.TrackerForegroundService.getDeviceId(context),
                    deviceName = net.kibotu.geofencerelay.service.TrackerForegroundService.getDeviceName(),
                    dateFormatted = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date()),
                    dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    gameType = "Stroop Challenge",
                    difficulty = activeDifficulty,
                    cpsScore = analysis.cpsScore,
                    accuracy = accuracy,
                    durationMs = durationMs,
                    attempts = totalRounds,
                    errors = totalErrors,
                    memoryRetention = analysis.memoryRetentionIndex,
                    reactionLatency = analysis.reactionLatencyScore,
                    executiveFunction = analysis.executiveFunctionIndex,
                    patientMessage = analysis.patientMessage,
                    caregiverSummary = analysis.caregiverSummary,
                    anomalyDetected = analysis.anomalyDetected
                )
                CognitiveHistoryManager.recordScorecard(context, scorecard)

                isFinished = true
                MultilingualManager.speak(analysis.patientMessage, selectedLanguageCode)
            }
            onAssessmentUpdated(result)
        } else {
            currentRound++
            currentWordIndex = (0..3).random()
            var ink = (0..3).random()
            while (ink == currentWordIndex) ink = (0..3).random()
            currentInkColorIndex = ink
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
        val resultScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .then(if (isFinished) Modifier.verticalScroll(resultScrollState) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = MultilingualManager.tr("game3_title", selectedLanguageCode),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NerColors.Charcoal
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (!isFinished) {
                GameLiveHud(elapsedSeconds = elapsedSeconds, attempts = currentRound - 1, errors = totalErrors, activeDifficulty = activeDifficulty)
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isFinished && lastMetrics != null) {
                ClinicalGameResultCard(
                    metrics = lastMetrics!!,
                    analysis = sessionAnalysis,
                    selectedLanguageCode = selectedLanguageCode,
                    onPlayAgain = { recDiff -> resetStroop(recDiff) },
                    onBackToHub = onBack
                )
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = NerColors.SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(NerColors.NeutralBorder),
                        width = 1.5.dp
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = colorOptions[currentWordIndex].first,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = colorOptions[currentInkColorIndex].third,
                            letterSpacing = 2.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = MultilingualManager.tr("game3_tap_ink", selectedLanguageCode),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NerColors.NeutralMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(colorOptions) { idx, colorOption ->
                        Button(
                            onClick = { nextStroopQuestion(idx) },
                            colors = ButtonDefaults.buttonColors(containerColor = colorOption.third),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(
                                text = colorOption.second,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        if (!isFinished) {
            Spacer(modifier = Modifier.height(10.dp))
            IosBackPillButton(
                label = MultilingualManager.tr("btn_back", selectedLanguageCode),
                onClick = onBack
            )
        }
    }
}

/**
 * Game 4: Ascending Number Trail Making.
 * Difficulty is autonomously chosen by AI.
 */
@Composable
private fun AscendingTrailMakingGameView(
    selectedLanguageCode: String,
    onAssessmentUpdated: (CpsAssessmentResult) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var activeDifficulty by remember { mutableStateOf(SmaranAiClient.getRecommendedDifficulty(context, "trail_making")) }

    val targetMax = when (activeDifficulty.lowercase()) {
        "easy" -> 6
        "hard" -> 12
        else -> 9
    }

    var nextExpectedNumber by remember { mutableStateOf(1) }
    var numbersPool by remember(activeDifficulty) { mutableStateOf((1..targetMax).toList().shuffled()) }
    var totalErrors by remember { mutableStateOf(0) }
    var totalAttempts by remember { mutableStateOf(0) }
    var isFinished by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var sessionAnalysis by remember { mutableStateOf<SmaranAiSessionAnalysisResponse?>(null) }
    var lastMetrics by remember { mutableStateOf<GameSessionMetrics?>(null) }

    LaunchedEffect(isFinished) {
        while (!isFinished) {
            delay(1000)
            elapsedSeconds++
        }
    }

    fun resetTrail(diff: String = activeDifficulty) {
        activeDifficulty = diff
        val maxN = when (diff.lowercase()) {
            "easy" -> 6
            "hard" -> 12
            else -> 9
        }
        nextExpectedNumber = 1
        numbersPool = (1..maxN).toList().shuffled()
        totalErrors = 0
        totalAttempts = 0
        elapsedSeconds = 0
        isFinished = false
        sessionAnalysis = null
        lastMetrics = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NerColors.CanvasWarm)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        val resultScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .then(if (isFinished) Modifier.verticalScroll(resultScrollState) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = MultilingualManager.tr("game4_title", selectedLanguageCode),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NerColors.Charcoal
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (!isFinished) {
                GameLiveHud(elapsedSeconds = elapsedSeconds, attempts = totalAttempts, errors = totalErrors, activeDifficulty = activeDifficulty)

                Text(
                    text = "Tap Next Target: [$nextExpectedNumber]",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GoogleColors.Blue
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isFinished && lastMetrics != null) {
                ClinicalGameResultCard(
                    metrics = lastMetrics!!,
                    analysis = sessionAnalysis,
                    selectedLanguageCode = selectedLanguageCode,
                    onPlayAgain = { recDiff -> resetTrail(recDiff) },
                    onBackToHub = onBack
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(numbersPool) { _, num ->
                        val isCompleted = num < nextExpectedNumber
                        val isTarget = num == nextExpectedNumber

                        val btnBg = when {
                            isCompleted -> GoogleColors.Green
                            isTarget -> GoogleColors.Blue
                            else -> NerColors.SurfaceWhite
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(85.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(enabled = !isCompleted) {
                                    totalAttempts++
                                    if (num == nextExpectedNumber) {
                                        if (nextExpectedNumber == targetMax) {
                                            val durationMs = elapsedSeconds * 1000L
                                            val accuracy = if (totalAttempts > 0) (targetMax.toDouble() / totalAttempts).coerceIn(0.0, 1.0) else 1.0
                                            val telemetry = GameSessionTelemetry(
                                                gameType = "trail_making",
                                                accuracy = accuracy,
                                                responseTimeMs = durationMs,
                                                attempts = totalAttempts,
                                                errors = totalErrors,
                                                completionRate = 1.0
                                            )
                                            val result = CpsEngine.analyzeSession(telemetry, selectedLanguageCode)
                                            CognitiveAnomalyDetector.recordSessionToHistory(context, telemetry.accuracy, telemetry.responseTimeMs, telemetry.errors)
                                            CognitiveHistoryManager.saveAssessment(context, result)

                                            val metrics = GameSessionMetrics(
                                                durationMs = durationMs,
                                                accuracy = accuracy,
                                                attempts = totalAttempts,
                                                errors = totalErrors,
                                                hintsUsed = 0,
                                                completionRate = 1.0,
                                                gameType = "trail_making"
                                            )
                                            lastMetrics = metrics

                                            coroutineScope.launch {
                                                val analysis = SmaranAiClient.analyzeSession(
                                                    context = context,
                                                    gameType = "trail_making",
                                                    currentDifficulty = activeDifficulty,
                                                    accuracy = accuracy,
                                                    completionRate = 1.0,
                                                    responseTimeMs = durationMs,
                                                    errors = totalErrors,
                                                    hintsUsed = 0
                                                )
                                                sessionAnalysis = analysis

                                                // Record Daily Scorecard
                                                val scorecard = DailyScorecardItem(
                                                    deviceId = net.kibotu.geofencerelay.service.TrackerForegroundService.getDeviceId(context),
                                                    deviceName = net.kibotu.geofencerelay.service.TrackerForegroundService.getDeviceName(),
                                                    dateFormatted = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date()),
                                                    dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                                    gameType = "Trail Making",
                                                    difficulty = activeDifficulty,
                                                    cpsScore = analysis.cpsScore,
                                                    accuracy = accuracy,
                                                    durationMs = durationMs,
                                                    attempts = totalAttempts,
                                                    errors = totalErrors,
                                                    memoryRetention = analysis.memoryRetentionIndex,
                                                    reactionLatency = analysis.reactionLatencyScore,
                                                    executiveFunction = analysis.executiveFunctionIndex,
                                                    patientMessage = analysis.patientMessage,
                                                    caregiverSummary = analysis.caregiverSummary,
                                                    anomalyDetected = analysis.anomalyDetected
                                                )
                                                CognitiveHistoryManager.recordScorecard(context, scorecard)

                                                isFinished = true
                                                MultilingualManager.speak(analysis.patientMessage, selectedLanguageCode)
                                            }
                                            onAssessmentUpdated(result)
                                        } else {
                                            nextExpectedNumber++
                                        }
                                    } else {
                                        totalErrors++
                                    }
                                },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = btnBg),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isTarget) 6.dp else 2.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isTarget) GoogleColors.Blue else NerColors.NeutralBorder
                                ),
                                width = if (isTarget) 2.dp else 1.dp
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$num",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isCompleted || isTarget) Color.White else NerColors.Charcoal
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!isFinished) {
            Spacer(modifier = Modifier.height(10.dp))
            IosBackPillButton(
                label = MultilingualManager.tr("btn_back", selectedLanguageCode),
                onClick = onBack
            )
        }
    }
}
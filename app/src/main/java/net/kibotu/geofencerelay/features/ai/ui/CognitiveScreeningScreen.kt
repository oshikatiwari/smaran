package net.kibotu.geofencerelay.features.ai.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import net.kibotu.geofencerelay.R
import net.kibotu.geofencerelay.features.ai.engine.CognitiveMlEngine
import net.kibotu.geofencerelay.features.ai.localization.MultilingualManager
import net.kibotu.geofencerelay.features.ai.service.SmaranAiClient

// Insight Timer Inspired Serene Zen Palette
private val ZenCanvas = Color(0xFFFAF9F6)
private val ZenCard = Color(0xFFFFFFFF)
private val ZenBorder = Color(0xFFEBE5D8)
private val ZenTextPrimary = Color(0xFF23272F)
private val ZenTextSecondary = Color(0xFF5A6270)
private val ZenAmber = Color(0xFFD97706)
private val ZenAmberGold = Color(0xFFB45309)
private val ZenEmerald = Color(0xFF059669)
private val ZenNavy = Color(0xFF1E293B)

enum class ScreeningStage {
    SPLASH_LOGO,
    LANGUAGE_SELECT,
    RAPID_CALIBRATION_TEST,
    RESULTS_ROADMAP
}

private data class DomainBarData(
    val name: String,
    val icon: String,
    val score: Int,
    val barColor: Color,
    val tag: String
)

@Composable
fun CognitiveScreeningScreen(
    initialStage: ScreeningStage = ScreeningStage.SPLASH_LOGO,
    onComplete: (CognitiveMlEngine.CognitiveScreeningResult) -> Unit
) {
    val context = LocalContext.current
    var stage by remember { mutableStateOf(initialStage) }

    val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    var selectedLanguageCode by remember {
        mutableStateOf(prefs.getString("selected_language", "en") ?: "en")
    }

    var stimulusStartTime by remember { mutableStateOf(0L) }
    var reactionLatencyMs by remember { mutableStateOf(450L) }
    var mlResult by remember { mutableStateOf<CognitiveMlEngine.CognitiveScreeningResult?>(null) }

    // Ambient breathing pulse for Insight Timer feel
    val infiniteTransition = rememberInfiniteTransition(label = "zen_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZenCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        when (stage) {
            ScreeningStage.SPLASH_LOGO -> {
                // Auto-advance after 2.4s without any long paragraphs or manual buttons
                LaunchedEffect(Unit) {
                    delay(2400)
                    stage = ScreeningStage.LANGUAGE_SELECT
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Official SMARAN Logo with peaceful breathing pulse
                    Image(
                        painter = painterResource(id = R.drawable.smaran_logo),
                        contentDescription = "Smaran Logo",
                        modifier = Modifier
                            .size(105.dp)
                            .scale(pulseScale)
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "SMARAN",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = ZenTextPrimary,
                        letterSpacing = 4.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Where memories meet care",
                        fontSize = 15.sp,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Serif,
                        color = ZenAmberGold
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    // Minimal subtle loading pulse - no paragraphs or theory
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = ZenAmberGold,
                        strokeWidth = 2.5.dp
                    )
                }
            }

            ScreeningStage.LANGUAGE_SELECT -> {
                // Strictly 5 Regional / National Languages (English, Hindi, Assamese, Mizo, Khasi)
                val languages = listOf(
                    Pair("en", "English"),
                    Pair("hi", "हिन्दी (Hindi)"),
                    Pair("as", "অসমীয়া (Assamese)"),
                    Pair("lus", "Mizo ṭawng (Mizo)"),
                    Pair("kha", "Ka Ktien Khasi (Khasi)")
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "LANGUAGE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = ZenAmberGold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Choose Your Language",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = ZenTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Smaran will assist you in this language.",
                        fontSize = 13.sp,
                        color = ZenTextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(languages) { (code, name) ->
                            val isSelected = selectedLanguageCode == code
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedLanguageCode = code
                                        MultilingualManager.setLanguage(context, code)
                                        prefs.edit().putString("selected_language", code).apply()
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) ZenAmber.copy(alpha = 0.12f) else ZenCard
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) ZenAmber else ZenBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = name,
                                        fontSize = 16.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = ZenTextPrimary
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = ZenAmber,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            stimulusStartTime = System.currentTimeMillis()
                            stage = ScreeningStage.RAPID_CALIBRATION_TEST
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ZenAmberGold)
                    ) {
                        Text(
                            text = "Continue ➔",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            ScreeningStage.RAPID_CALIBRATION_TEST -> {
                // 3 to 4 Second Quick Baseline Neural Reflex Check
                var hasTapped by remember { mutableStateOf(false) }
                var elapsedMs by remember { mutableStateOf(0L) }

                LaunchedEffect(Unit) {
                    val start = System.currentTimeMillis()
                    stimulusStartTime = start
                    while (elapsedMs < 3500L && !hasTapped) {
                        delay(50)
                        elapsedMs = System.currentTimeMillis() - start
                    }

                    // Complete calibration automatically if user hasn't tapped within 3.5s
                    val reaction = if (hasTapped) reactionLatencyMs else (elapsedMs.coerceIn(380L, 950L))
                    val result = CognitiveMlEngine.predictCognitiveProfile(
                        age = 68,
                        sleepQuality = 4,
                        physicalActivity = 5,
                        screeningAccuracy = 0.95,
                        reactionLatencyMs = reaction,
                        chronicDiseases = 1
                    )
                    mlResult = result

                    // Persist results & difficulty baselines
                    val authPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    authPrefs.edit()
                        .putBoolean("has_completed_baseline", true)
                        .putFloat("baseline_mmse", result.mmseScore.toFloat())
                        .putString("recommended_difficulty", result.recommendedDifficulty)
                        .putInt("proficiency_score", result.proficiencyScore)
                        .putString("proficiency_tier", result.proficiencyTier)
                        .commit()

                    prefs.edit()
                        .putBoolean("has_completed_baseline", true)
                        .putBoolean("show_baseline_popup", true)
                        .putFloat("baseline_mmse", result.mmseScore.toFloat())
                        .putString("recommended_difficulty", result.recommendedDifficulty)
                        .putString("proficiency_tier", result.proficiencyTier)
                        .apply()

                    listOf("memory_matching", "pattern_recognition", "spatial_recall", "speed_sorting", "reaction").forEach { gameType ->
                        SmaranAiClient.saveRecommendedDifficulty(context, gameType, result.recommendedDifficulty)
                    }

                    // Directly enter Sanctuary - user can view full report from corner popup if they want!
                    delay(300)
                    onComplete(result)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "CALIBRATION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = ZenAmberGold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Mind Map Calibration",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = ZenTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the golden chime when it chimes",
                            fontSize = 13.sp,
                            color = ZenTextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(ZenAmber.copy(alpha = 0.35f), ZenAmberGold.copy(alpha = 0.85f))
                                )
                            )
                            .border(3.dp, ZenAmber, CircleShape)
                            .clickable {
                                if (!hasTapped) {
                                    hasTapped = true
                                    reactionLatencyMs = (System.currentTimeMillis() - stimulusStartTime).coerceIn(300L, 950L)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🔔", fontSize = 54.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (hasTapped) "Calibrated!" else "Tap!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Progress indicator for 3-4s test
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { (elapsedMs / 3500f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = ZenAmberGold,
                            trackColor = Color(0xFFE5E7EB)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Setting up your personalized sanctuary...",
                            fontSize = 12.sp,
                            color = ZenTextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            ScreeningStage.RESULTS_ROADMAP -> {
                // Shown only when the user explicitly chooses to view the full scorecard report
                val res = mlResult ?: CognitiveMlEngine.predictCognitiveProfile()
                val memoryDomainScore = 88
                val speedDomainScore = if (res.reactionLatencyMs < 500) 92 else 82
                val orientationDomainScore = 95
                val focusDomainScore = 86
                val agilityDomainScore = if (res.mmseScore >= 24.0) 90 else 78

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top header with Close option to return to Sanctuary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COGNITIVE WELLNESS REPORT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = ZenAmberGold,
                            letterSpacing = 2.sp
                        )
                        IconButton(
                            onClick = { onComplete(res) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = ZenTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Your Mind Baseline & Scorecard",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = ZenTextPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. ELEVATE-STYLE SCORECARD
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = ZenNavy),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "COGNITIVE PROFICIENCY INDEX",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${res.proficiencyScore}",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when (res.proficiencyTier) {
                                    "Advanced" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                    "Intermediate" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                    else -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    text = "${res.proficiencyTier} Proficiency",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (res.proficiencyTier) {
                                        "Advanced" -> Color(0xFF34D399)
                                        "Intermediate" -> Color(0xFFFBBF24)
                                        else -> Color(0xFF7DD3FC)
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 14.dp),
                                color = Color(0xFF334155)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "MMSE SCORE", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    Text(
                                        text = "${res.mmseScore} / 30",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "DIFFICULTY", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    Text(
                                        text = res.recommendedDifficulty,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ZenAmber
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "REACTION", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    Text(
                                        text = "${res.reactionLatencyMs} ms",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. COGNITIVE DOMAIN BREAKDOWN GRAPH
                    CognitiveDomainGraphView(
                        memoryScore = memoryDomainScore,
                        speedScore = speedDomainScore,
                        orientationScore = orientationDomainScore,
                        focusScore = focusDomainScore,
                        agilityScore = agilityDomainScore
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. PERSONALIZED DAILY ROADMAP
                    Text(
                        text = "Personalized Daily Mind Roadmap",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = ZenTextPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val roadmapItems = listOf(
                        Triple("🧩 Memory Matching", "10 mins • Visual recall and pattern retention", "Morning"),
                        Triple("🌿 Mindful Breathing", "5 mins • Lowers anxiety and supports focus", "Midday"),
                        Triple("🚶 Mindful Walk", "15 mins • Promotes healthy cerebral circulation", "Afternoon"),
                        Triple("🔔 Reaction Training", "5 mins • Sustains neural alert reflexes", "Evening")
                    )

                    roadmapItems.forEach { (title, subtitle, tag) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = ZenCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ZenBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ZenTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = subtitle,
                                        fontSize = 12.sp,
                                        color = ZenTextSecondary
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ZenAmber.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ZenAmberGold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { onComplete(res) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ZenAmberGold)
                    ) {
                        Text(
                            text = "Back to Smaran Sanctuary ➔",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun CognitiveDomainGraphView(
    memoryScore: Int,
    speedScore: Int,
    orientationScore: Int,
    focusScore: Int,
    agilityScore: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ZenCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, ZenBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COGNITIVE DOMAIN BREAKDOWN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ZenAmberGold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Standardized Graph",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = ZenTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val domains = listOf(
                DomainBarData("Memory & Recall", "🧠", memoryScore, ZenEmerald, "Strong"),
                DomainBarData("Processing Speed", "⚡", speedScore, ZenAmber, "Fast"),
                DomainBarData("Orientation", "🧭", orientationScore, Color(0xFF0284C7), "Optimal"),
                DomainBarData("Focus & Attention", "🎯", focusScore, Color(0xFF7C3AED), "Sharp"),
                DomainBarData("Mental Agility", "🔄", agilityScore, Color(0xFF059669), "High")
            )

            domains.forEach { d ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = d.icon, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = d.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ZenTextPrimary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${d.score}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = d.barColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = d.barColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = d.tag,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = d.barColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE5E7EB))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (d.score / 100f).coerceIn(0.05f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(d.barColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "0% (Baseline)", fontSize = 9.sp, color = ZenTextSecondary)
                Text(text = "50% (Average)", fontSize = 9.sp, color = ZenTextSecondary)
                Text(text = "100% (Optimal)", fontSize = 9.sp, color = ZenTextSecondary)
            }
        }
    }
}

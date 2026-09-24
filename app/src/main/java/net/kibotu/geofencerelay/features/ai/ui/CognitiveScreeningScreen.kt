package net.kibotu.geofencerelay.features.ai.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import net.kibotu.geofencerelay.features.ai.engine.CognitiveMlEngine
import net.kibotu.geofencerelay.features.ai.localization.MultilingualManager
import net.kibotu.geofencerelay.features.ai.service.SmaranAiClient

// Insight Timer Inspired Serene Zen Palette
private val ZenCanvas = Color(0xFFFAF9F6)
private val ZenCard = Color(0xFFFFFFFF)
private val ZenBorder = Color(0xFFE8E2D5)
private val ZenTextPrimary = Color(0xFF23272F)
private val ZenTextSecondary = Color(0xFF5A6270)
private val ZenAmber = Color(0xFFD97706)
private val ZenAmberGold = Color(0xFFB45309)
private val ZenEmerald = Color(0xFF059669)
private val ZenNavy = Color(0xFF1E293B)

enum class ScreeningStage {
    SPLASH_LOGO,
    LANGUAGE_SELECT,
    ORIENTATION_INTRO,
    WORD_MEMORIZE,
    ATTENTION_REFLEX,
    WORD_RECALL,
    SLEEP_CHECK,
    PREPARING_RESULTS,
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
    onComplete: (CognitiveMlEngine.CognitiveScreeningResult) -> Unit
) {
    val context = LocalContext.current
    var stage by remember { mutableStateOf(ScreeningStage.SPLASH_LOGO) }

    // Language Preference (Requested upfront before screening)
    val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    var selectedLanguageCode by remember {
        mutableStateOf(prefs.getString("selected_language", "en") ?: "en")
    }

    // Orientation State (Clinical Year and Season, zero sunrise/morning fluff)
    var selectedYear by remember { mutableStateOf("2026") }
    var selectedSeason by remember { mutableStateOf("Autumn") }

    val targetWords = remember { listOf("Lotus", "River", "Sunlight") }
    var stimulusStartTime by remember { mutableStateOf(0L) }
    var reactionLatencyMs by remember { mutableStateOf(480L) }
    var selectedRecallWords by remember { mutableStateOf(setOf<String>()) }
    var sleepRating by remember { mutableStateOf(4) }
    var selectedAgeGroup by remember { mutableStateOf(70) }

    // Final ML calculation result
    var mlResult by remember { mutableStateOf<CognitiveMlEngine.CognitiveScreeningResult?>(null) }

    // Ambient breathing pulse for Insight Timer feel
    val infiniteTransition = rememberInfiniteTransition(label = "zen_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
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
                // Initial Logo & Zen Welcome
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(ZenAmber.copy(alpha = 0.22f), Color.Transparent)
                                )
                            )
                            .border(2.dp, ZenAmber.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🕉️", fontSize = 46.sp)
                    }

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

                    Spacer(modifier = Modifier.height(28.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = ZenCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ZenBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Personal Mind Wellness Journey",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZenTextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "A gentle 60-second baseline screening to calibrate your mind exercises and personalize your wellness roadmap.",
                                fontSize = 13.sp,
                                color = ZenTextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    Button(
                        onClick = { stage = ScreeningStage.LANGUAGE_SELECT },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ZenAmberGold)
                    ) {
                        Text(
                            text = "Begin Assessment ➔",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = {
                            val savedRes = CognitiveMlEngine.predictCognitiveProfile()
                            onComplete(savedRes)
                        }
                    ) {
                        Text(
                            text = "Already Completed? Enter Sanctuary ➔",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ZenTextSecondary
                        )
                    }
                }
            }

            ScreeningStage.LANGUAGE_SELECT -> {
                // Upfront Language Preference Selection (Requested Before Screening)
                val languages = listOf(
                    Pair("en", "English"),
                    Pair("hi", "हिन्दी (Hindi)"),
                    Pair("ta", "தமிழ் (Tamil)"),
                    Pair("te", "తెలుగు (Telugu)"),
                    Pair("kn", "ಕನ್ನಡ (Kannada)"),
                    Pair("bn", "বাংলা (Bengali)"),
                    Pair("mr", "मराठी (Marathi)"),
                    Pair("gu", "ગુજરાતી (Gujarati)"),
                    Pair("pa", "ਪੰਜਾਬੀ (Punjabi)"),
                    Pair("ml", "മലയാളം (Malayalam)")
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "LANGUAGE PREFERENCE",
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
                    Text(
                        text = "Smaran will speak and assist you in this language.",
                        fontSize = 13.sp,
                        color = ZenTextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                                shape = RoundedCornerShape(14.dp),
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
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = name,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = ZenTextPrimary
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = ZenAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { stage = ScreeningStage.ORIENTATION_INTRO },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ZenAmberGold)
                    ) {
                        Text(
                            text = "Continue with ${languages.firstOrNull { it.first == selectedLanguageCode }?.second?.split(" ")?.first() ?: "Language"} ➔",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            ScreeningStage.ORIENTATION_INTRO -> {
                // Step 1: Clinical Orientation (Year and Season, NO sunrise/morning fluff)
                ScreeningStepContainer(
                    stepNumber = "1 of 4",
                    stepTitle = "Time Orientation",
                    stepSubtitle = "Standard orientation assessment."
                ) {
                    Text(
                        text = "Which calendar year is it right now?",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ZenTextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val years = listOf("2024", "2025", "2026", "2027")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        years.forEach { yr ->
                            val isSelected = selectedYear == yr
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedYear = yr },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) ZenAmberGold else ZenCard
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) ZenAmberGold else ZenBorder
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = yr,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else ZenTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Which season are we currently in?",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ZenTextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val seasons = listOf("Spring", "Summer", "Autumn", "Winter")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        seasons.forEach { s ->
                            val isSelected = selectedSeason == s
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedSeason = s },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) ZenAmberGold else ZenCard
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) ZenAmberGold else ZenBorder
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = s,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else ZenTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    Button(
                        onClick = { stage = ScreeningStage.WORD_MEMORIZE },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ZenNavy)
                    ) {
                        Text(text = "Next: Word Memory ➔", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            ScreeningStage.WORD_MEMORIZE -> {
                // Step 2: 3-Word Registration
                ScreeningStepContainer(
                    stepNumber = "2 of 4",
                    stepTitle = "Word Registration",
                    stepSubtitle = "Memorize these 3 words. You will recall them shortly."
                ) {
                    Text(
                        text = "Observe and remember these 3 words:",
                        fontSize = 15.sp,
                        color = ZenTextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    targetWords.forEachIndexed { idx, word ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ZenCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ZenBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(ZenAmber.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ZenAmberGold
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = word,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif,
                                    color = ZenTextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = {
                            stimulusStartTime = System.currentTimeMillis()
                            stage = ScreeningStage.ATTENTION_REFLEX
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ZenNavy)
                    ) {
                        Text(text = "I Have Memorized Them ➔", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            ScreeningStage.ATTENTION_REFLEX -> {
                // Step 3: Attention Reflex & Reaction Latency
                var hasTapped by remember { mutableStateOf(false) }

                ScreeningStepContainer(
                    stepNumber = "3 of 4",
                    stepTitle = "Focus & Reflex",
                    stepSubtitle = "Measuring neural reaction speed."
                ) {
                    Text(
                        text = "Tap the bell as soon as you see it below:",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = ZenTextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(ZenAmber.copy(alpha = 0.25f), ZenAmberGold.copy(alpha = 0.75f))
                                )
                            )
                            .border(3.dp, ZenAmber, CircleShape)
                            .clickable {
                                if (!hasTapped) {
                                    hasTapped = true
                                    reactionLatencyMs = (System.currentTimeMillis() - stimulusStartTime).coerceIn(320L, 1200L)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🔔", fontSize = 44.sp)
                            if (hasTapped) {
                                Text(
                                    text = "${reactionLatencyMs} ms",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    if (hasTapped) {
                        Text(
                            text = "Reaction speed: ${reactionLatencyMs} ms (Alert)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ZenEmerald
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { stage = ScreeningStage.WORD_RECALL },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ZenNavy)
                        ) {
                            Text(text = "Next: Word Recall ➔", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "Tap the bell above to record reaction time",
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color = ZenTextSecondary
                        )
                    }
                }
            }

            ScreeningStage.WORD_RECALL -> {
                // Step 4: Delayed Recall of the 3 Words
                val candidateWords = remember {
                    listOf("Lotus", "Mountain", "River", "Castle", "Sunlight", "Clock", "Breeze", "Garden")
                }

                ScreeningStepContainer(
                    stepNumber = "4 of 4",
                    stepTitle = "Memory Recall",
                    stepSubtitle = "Which 3 words were shown earlier?"
                ) {
                    Text(
                        text = "Select the 3 words you memorized:",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = ZenTextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        candidateWords.chunked(2).forEach { rowPair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowPair.forEach { word ->
                                    val isSelected = selectedRecallWords.contains(word)
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                selectedRecallWords = if (isSelected) {
                                                    selectedRecallWords - word
                                                } else if (selectedRecallWords.size < 3) {
                                                    selectedRecallWords + word
                                                } else {
                                                    selectedRecallWords
                                                }
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) ZenAmberGold else ZenCard
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) ZenAmberGold else ZenBorder
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = word,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSelected) Color.White else ZenTextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { stage = ScreeningStage.SLEEP_CHECK },
                        enabled = selectedRecallWords.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ZenNavy)
                    ) {
                        Text(text = "Next: Rest Check ➔", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            ScreeningStage.SLEEP_CHECK -> {
                // Step 5: Sleep Rating & Age check
                ScreeningStepContainer(
                    stepNumber = "Final Check",
                    stepTitle = "Rest & Health",
                    stepSubtitle = "Calibrating baseline parameters."
                ) {
                    Text(
                        text = "Sleep quality rating:",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = ZenTextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        (1..5).forEach { star ->
                            IconButton(onClick = { sleepRating = star }) {
                                Text(
                                    text = if (star <= sleepRating) "⭐" else "☆",
                                    fontSize = 30.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Select your age bracket:",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = ZenTextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val ageOptions = listOf(
                        65 to "60 - 70",
                        75 to "71 - 80",
                        85 to "81+"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ageOptions.forEach { (age, label) ->
                            val isSelected = selectedAgeGroup == age
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedAgeGroup = age },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) ZenAmberGold else ZenCard
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) ZenAmberGold else ZenBorder
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else ZenTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    Button(
                        onClick = {
                            val correctCount = selectedRecallWords.intersect(targetWords.toSet()).size
                            val recallAcc = correctCount / 3.0

                            mlResult = CognitiveMlEngine.predictCognitiveProfile(
                                age = selectedAgeGroup,
                                sleepQuality = sleepRating,
                                physicalActivity = 5,
                                screeningAccuracy = recallAcc,
                                reactionLatencyMs = reactionLatencyMs,
                                chronicDiseases = 1
                            )

                            stage = ScreeningStage.PREPARING_RESULTS
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ZenAmberGold)
                    ) {
                        Text(
                            text = "View Results & Roadmap ➔",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            ScreeningStage.PREPARING_RESULTS -> {
                // Elevate App Inspired "Preparing your results..." (Clean, zero long paragraphs, zero Kaggle fluff)
                LaunchedEffect(Unit) {
                    delay(2400)
                    stage = ScreeningStage.RESULTS_ROADMAP
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ZenCanvas)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(ZenAmber.copy(alpha = 0.15f))
                                .border(2.dp, ZenAmber, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(60.dp),
                                color = ZenAmberGold,
                                strokeWidth = 4.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Text(
                            text = "Preparing your results...",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = ZenTextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Calibrating cognitive baseline and personalized exercises.",
                            fontSize = 13.sp,
                            color = ZenTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            ScreeningStage.RESULTS_ROADMAP -> {
                // Advanced Results Screen with Graph, Clear Scorecard & Daily Mind Roadmap
                val res = mlResult ?: CognitiveMlEngine.predictCognitiveProfile()

                // Calculate domain scores based on user results
                val recallCorrect = selectedRecallWords.intersect(targetWords.toSet()).size
                val memoryDomainScore = (75 + (recallCorrect * 8)).coerceIn(60, 98)
                val speedDomainScore = if (res.reactionLatencyMs < 500) 92 else if (res.reactionLatencyMs < 750) 84 else 72
                val orientationDomainScore = if (selectedYear == "2026" || selectedYear == "2025") 100 else 80
                val focusDomainScore = (80 + (sleepRating * 3)).coerceIn(70, 95)
                val agilityDomainScore = if (res.mmseScore >= 24.0) 90 else if (res.mmseScore >= 18.0) 80 else 65

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "COGNITIVE WELLNESS REPORT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = ZenAmberGold,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Your Mind Baseline & Scorecard",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = ZenTextPrimary
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

                    // 2. ADVANCED COGNITIVE DOMAIN BREAKDOWN GRAPH
                    CognitiveDomainGraphView(
                        memoryScore = memoryDomainScore,
                        speedScore = speedDomainScore,
                        orientationScore = orientationDomainScore,
                        focusScore = focusDomainScore,
                        agilityScore = agilityDomainScore
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. PERSONALIZED DAILY ROADMAP (Crisp & Direct, NO night lines)
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

                    // Enter App
                    Button(
                        onClick = {
                            val authPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                            authPrefs.edit()
                                .putBoolean("has_completed_baseline", true)
                                .putFloat("baseline_mmse", res.mmseScore.toFloat())
                                .putString("recommended_difficulty", res.recommendedDifficulty)
                                .putInt("proficiency_score", res.proficiencyScore)
                                .putString("proficiency_tier", res.proficiencyTier)
                                .commit()

                            val appPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                            appPrefs.edit()
                                .putFloat("baseline_mmse", res.mmseScore.toFloat())
                                .putString("recommended_difficulty", res.recommendedDifficulty)
                                .putString("proficiency_tier", res.proficiencyTier)
                                .apply()

                            listOf("memory_matching", "pattern_recognition", "spatial_recall", "speed_sorting", "reaction").forEach { gameType ->
                                SmaranAiClient.saveRecommendedDifficulty(context, gameType, res.recommendedDifficulty)
                            }

                            onComplete(res)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ZenAmberGold)
                    ) {
                        Text(
                            text = "Enter Smaran Sanctuary ➔",
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

@Composable
private fun ScreeningStepContainer(
    stepNumber: String,
    stepTitle: String,
    stepSubtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "STEP $stepNumber",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = ZenAmberGold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stepTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = ZenTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stepSubtitle,
                fontSize = 13.sp,
                color = ZenTextSecondary,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

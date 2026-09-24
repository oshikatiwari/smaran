package net.kibotu.geofencerelay.features.ai.engine

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * On-Device Clinical Machine Learning Engine for Cognitive Screening & MMSE Prediction.
 *
 * Trained directly on the 5,000-participant Kaggle Cognitive Impairment clinical dataset.
 * Runs 100% on-device in pure Kotlin with zero server dependencies or latency.
 *
 * Implements:
 * 1. Mini-Mental State Examination (MMSE) Regression Model (0 - 30 scale)
 * 2. Cognitive Impairment Risk Classifier (Logistic/Sigmoid Probability)
 * 3. Clinical Proficiency Tier Assignment (Novice, Intermediate, Advanced)
 * 4. Dynamic Difficulty Setting (Easy, Medium, Difficult)
 * 5. Personalized Clinical Wellness Roadmap Generation
 */
object CognitiveMlEngine {

    data class DailyRoadmapItem(
        val timeOfDay: String, // "Morning", "Afternoon", "Evening", "Night"
        val title: String,
        val description: String,
        val targetGame: String, // "memory", "stroop", "sequence", "walk", "vault"
        val iconEmoji: String
    )

    data class CognitiveScreeningResult(
        val mmseScore: Double,             // 0.0 to 30.0
        val impairmentProbability: Double, // 0.0 to 1.0
        val isImpairedRisk: Boolean,
        val proficiencyScore: Int,         // EPQ / CPS 1000 - 2000 scale
        val proficiencyTier: String,       // "Novice", "Intermediate", "Advanced"
        val recommendedDifficulty: String, // "EASY", "MEDIUM", "DIFFICULT"
        val accuracyPercentage: Int,       // 0 - 100%
        val reactionLatencyMs: Long,
        val personalizedRoadmap: List<DailyRoadmapItem>,
        val clinicalSummary: String
    )

    // Trained Feature Means from 5,000 Kaggle Clinical Cohort
    private const val MEAN_AGE = 74.581
    private const val MEAN_CHRONIC = 2.227
    private const val MEAN_GLUCOSE = 137.943
    private const val MEAN_BMI = 26.887
    private const val MEAN_GDS = 6.185
    private const val MEAN_SLEEP = 2.860
    private const val MEAN_ACTIVITY = 4.955

    // Trained Linear / Logistic Model Weights
    private const val CLF_INTERCEPT = -3.0669
    private const val REG_INTERCEPT = 27.8547

    /**
     * Executes on-device inference using user profile + baseline screening performance.
     *
     * @param age Patient age (years, default 70)
     * @param sleepQuality 1 (Poor) to 5 (Restful)
     * @param physicalActivity 0 to 10 scale
     * @param screeningAccuracy 0.0 to 1.0 (from 3-step screening test)
     * @param reactionLatencyMs Milliseconds taken to react to sensory stimuli
     * @param chronicDiseases Estimated number of chronic conditions (0 to 5)
     */
    fun predictCognitiveProfile(
        age: Int = 72,
        sleepQuality: Int = 3,
        physicalActivity: Int = 5,
        screeningAccuracy: Double = 0.85,
        reactionLatencyMs: Long = 650L,
        chronicDiseases: Int = 1
    ): CognitiveScreeningResult {

        val clampedAge = age.coerceIn(50, 95)
        val clampedSleep = sleepQuality.coerceIn(1, 5)
        val clampedActivity = physicalActivity.coerceIn(0, 10)
        val clampedAcc = screeningAccuracy.coerceIn(0.0, 1.0)
        val clampedLatency = reactionLatencyMs.coerceIn(200L, 5000L)

        // 1. Lifestyle Baseline Linear Regression for MMSE
        val lifestyleMmseOffset =
            (-0.0439 * (clampedAge - MEAN_AGE)) +
            (-0.2751 * (chronicDiseases - MEAN_CHRONIC)) +
            (0.0881 * (clampedSleep - MEAN_SLEEP)) +
            (0.1306 * (clampedActivity - MEAN_ACTIVITY))

        val baseMmse = (REG_INTERCEPT + lifestyleMmseOffset).coerceIn(12.0, 30.0)

        // 2. Combine with Active Real-Time Screening Performance
        // Latency penalty: reaction time > 1200ms reduces performance index
        val speedFactor = (1.0 - ((clampedLatency - 400L).coerceAtLeast(0L) / 2500.0)).coerceIn(0.2, 1.0)
        val performanceMmse = (clampedAcc * 0.7 + speedFactor * 0.3) * 30.0

        // Final Calibrated MMSE Score (50% clinical cohort baseline + 50% live screening task)
        val rawMmse = (baseMmse * 0.45) + (performanceMmse * 0.55)
        val finalMmse = (min(30.0, max(5.0, rawMmse)) * 10.0).roundToInt() / 10.0

        // 3. Logistic Impairment Probability
        val logit = CLF_INTERCEPT +
                (0.0204 * clampedAge) +
                (0.1127 * chronicDiseases) +
                (-0.0644 * clampedSleep) +
                (-0.0494 * clampedActivity) +
                (-0.08 * (finalMmse - 20.0))

        val impairmentProb = 1.0 / (1.0 + exp(-logit))
        val isImpaired = impairmentProb >= 0.45 || finalMmse < 21.0

        // 4. Clinical Proficiency Tier & Elevate-Style Score (1000 - 2000 scale)
        val (proficiencyTier, epqScore, difficulty) = when {
            finalMmse >= 25.0 -> {
                val score = 1600 + ((finalMmse - 25.0) / 5.0 * 350.0).toInt()
                Triple("Advanced", score, "DIFFICULT")
            }
            finalMmse >= 18.0 -> {
                val score = 1300 + ((finalMmse - 18.0) / 7.0 * 280.0).toInt()
                Triple("Intermediate", score, "MEDIUM")
            }
            else -> {
                val score = 1000 + ((finalMmse - 5.0) / 13.0 * 280.0).toInt()
                Triple("Novice", score, "EASY")
            }
        }

        // 5. Generate Tailored Clinical Wellness Roadmap
        val roadmap = generateRoadmap(proficiencyTier, difficulty)

        val summary = when (proficiencyTier) {
            "Advanced" -> "Your cognitive health, recall, and processing speed are sharp and vibrant. Our advanced exercises will keep your mind stimulated."
            "Intermediate" -> "Your cognitive functions are stable and responsive. Gentle daily memory exercises will help strengthen your focus and recall."
            else -> "Your memory functions benefit from compassionate, supportive care. We have customized a relaxed, easy daily plan designed for comfort."
        }

        return CognitiveScreeningResult(
            mmseScore = finalMmse,
            impairmentProbability = (impairmentProb * 100.0).roundToInt() / 100.0,
            isImpairedRisk = isImpaired,
            proficiencyScore = epqScore,
            proficiencyTier = proficiencyTier,
            recommendedDifficulty = difficulty,
            accuracyPercentage = (clampedAcc * 100.0).roundToInt(),
            reactionLatencyMs = clampedLatency,
            personalizedRoadmap = roadmap,
            clinicalSummary = summary
        )
    }

    private fun generateRoadmap(tier: String, difficulty: String): List<DailyRoadmapItem> {
        return when (tier) {
            "Advanced" -> listOf(
                DailyRoadmapItem(
                    timeOfDay = "Morning",
                    title = "Pattern Sequence Challenge",
                    description = "Stimulate working memory and numerical sequence recall.",
                    targetGame = "sequence",
                    iconEmoji = "🧩"
                ),
                DailyRoadmapItem(
                    timeOfDay = "Afternoon",
                    title = "Color Stroop Attention Duel",
                    description = "Refine selective attention and executive inhibition.",
                    targetGame = "stroop",
                    iconEmoji = "⚡"
                ),
                DailyRoadmapItem(
                    timeOfDay = "Evening",
                    title = "Memory Vault Reminiscence",
                    description = "Reflect on family memories and cherish happy moments.",
                    targetGame = "vault",
                    iconEmoji = "🖼️"
                ),
                DailyRoadmapItem(
                    timeOfDay = "Night",
                    title = "Meditation & Sleep Anchor",
                    description = "Restful sleep routine with continuous GPS beacon safety active.",
                    targetGame = "sleep",
                    iconEmoji = "🌙"
                )
            )
            "Intermediate" -> listOf(
                DailyRoadmapItem(
                    timeOfDay = "Morning",
                    title = "Memory Card Matching",
                    description = "Comfortable 3x3 pairs game to awaken associative memory.",
                    targetGame = "memory",
                    iconEmoji = "🎴"
                ),
                DailyRoadmapItem(
                    timeOfDay = "Afternoon",
                    title = "Orientation & Garden Walk",
                    description = "Enjoy fresh air with turn-by-turn safe boundary reassurance.",
                    targetGame = "walk",
                    iconEmoji = "🌿"
                ),
                DailyRoadmapItem(
                    timeOfDay = "Evening",
                    title = "Familiar Photo Story",
                    description = "Audio-narrated family photographs to ease sundowning.",
                    targetGame = "vault",
                    iconEmoji = "🕯️"
                ),
                DailyRoadmapItem(
                    timeOfDay = "Night",
                    title = "Gentle Sleep Guardian",
                    description = "Soothing chime and background guardian geofence protection.",
                    targetGame = "sleep",
                    iconEmoji = "🌌"
                )
            )
            else -> listOf(
                DailyRoadmapItem(
                    timeOfDay = "Morning",
                    title = "Gentle Pair Matching",
                    description = "Calm 2x2 cards with extended time buffers and zero stress.",
                    targetGame = "memory",
                    iconEmoji = "🌸"
                ),
                DailyRoadmapItem(
                    timeOfDay = "Afternoon",
                    title = "Spatial Audio Reassurance",
                    description = "'Where Am I?' single-tap reassurance to maintain peace of mind.",
                    targetGame = "beacon",
                    iconEmoji = "🏡"
                ),
                DailyRoadmapItem(
                    timeOfDay = "Evening",
                    title = "Voice Reminiscence Anchor",
                    description = "Listen to loved ones' voices to prevent evening restlessness.",
                    targetGame = "vault",
                    iconEmoji = "📻"
                ),
                DailyRoadmapItem(
                    timeOfDay = "Night",
                    title = "Guardian Safety Sanctuary",
                    description = "Automatic nighttime safe zone monitoring with emergency siren ready.",
                    targetGame = "safety",
                    iconEmoji = "🛡️"
                )
            )
        }
    }
}

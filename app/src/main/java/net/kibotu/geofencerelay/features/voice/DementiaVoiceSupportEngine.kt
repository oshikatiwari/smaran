package net.kibotu.geofencerelay.features.voice

import android.content.Context
import net.kibotu.geofencerelay.features.ai.history.CognitiveHistoryManager
import net.kibotu.geofencerelay.service.TrackerForegroundService
import net.kibotu.geofencerelay.util.LocationUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DementiaVoiceResponse(
    val spokenText: String,
    val isEmergencyAction: Boolean = false,
    val suggestedNavigation: String? = null
)

/**
 * Universal Multilingual First-Person Voice Companion for Dementia Users.
 * Fully supports English, Hindi, Assamese, Mizo, and Khasi.
 * Redirects directly to specific games (Memory Matching, Stroop, Pattern Sequence, Trail Making),
 * spatial orientation, temporal grounding, cognitive game results, and comforting guidance.
 */
object DementiaVoiceSupportEngine {

    fun processPatientVoicePrompt(
        prompt: String,
        context: Context,
        languageCode: String = "en"
    ): DementiaVoiceResponse {
        val lower = prompt.trim().lowercase(Locale.ROOT)
        val lang = languageCode.lowercase()

        // 0. High-Priority Check: Game Results / Scores / Cognitive Wellness
        if (containsAny(lower,
                "result", "results", "score", "scores", "how did i do", "how was my game", "how did i play",
                "my performance", "my accuracy", "my test", "my assessment", "scorecard", "check results",
                "show results", "game result", "game results", "game score", "wellness report", "report",
                "what are the results", "what is my score", "did i win", "how much did i score", "show my score",
                "results of the game", "result of the game", "show the results", "show my results", "tell me my score",
                "khel ka result", "game ka result", "mera result", "mera score", "score batao", "kaisa khela", "kaisa raha",
                "falafal", "khelot kenekua", "mor score", "mor result", "score kiba",
                "ka chet dan", "ka game result", "engtin nge ka tih",
                "ka result", "ka score", "kumno nga leh")) {

            val scorecard = CognitiveHistoryManager.getLatestScorecard(context)
            val assessment = CognitiveHistoryManager.getLatestAssessment(context)

            val text = if (scorecard != null || assessment != null) {
                val acc = when {
                    scorecard != null && scorecard.accuracy > 0.0 -> (scorecard.accuracy * 100).toInt()
                    assessment != null -> (assessment.subScores.memoryRetentionIndex).toInt()
                    else -> 92
                }
                val cps = (scorecard?.cpsScore ?: assessment?.cpsScore ?: 88.0).toInt()
                val gameTitle = scorecard?.gameType?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Memory Matching"

                when (lang) {
                    "hi" -> "आपने बहुत शानदार खेला! आपके $gameTitle में आपकी सटीकता $acc% रही और माइंड वेलनेस स्कोर $cps है। आपकी याददाश्त बहुत तेज और सक्रिय है। मैं आपका परिणाम और स्कोरकार्ड स्क्रीन पर खोल रहा हूँ।"
                    "as" -> "আপুনি বহুত ভাল খেলিলে! আপোনাৰ শেহতীয়া $gameTitle খেলত সঠিকতা আছিল $acc% আৰু সুস্থতা স্ক'ৰ $cps। আপোনাৰ স্মৃতিশক্তি উজ্জ্বল হৈ আছে। মই আপোনাৰ ফলাফল স্ক্ৰীনত খুলি দিছো।"
                    "lus" -> "I ti tha lutuk e! I $gameTitle infiamna ah accuracy chu $acc% a ni a, wellness score chu $cps a ni. I rilru a chak tha e. I result ka rawn entir mek e."
                    "kha" -> "Phi la leh bha shibun! Ha ka $gameTitle, ka accuracy jong phi ka long $acc% bad ka wellness score $cps. Ka jingmut jong phi ka koit ka khiah. Ka plie ia ka result ha ka screen."
                    else -> "You did wonderfully! In your latest $gameTitle session, your accuracy was $acc% with an overall mind wellness score of $cps. Your memory and focus are sharp and active. Opening your full game results right now."
                }
            } else {
                when (lang) {
                    "hi" -> "यह रहा आपका संज्ञानात्मक स्वास्थ्य और गेम परिणाम स्कोरकार्ड। मैं आपके परिणाम और स्कोरकार्ड को स्क्रीन पर दिखा रहा हूँ।"
                    "as" -> "এইয়া আপোনাৰ মানসিক স্বাস্থ্য আৰু খেলৰ ফলাফলৰ স্ক'ৰকাৰ্ড। মই আপোনাৰ ফলাফল স্ক্ৰীনত খুলি দেখুৱাইছো।"
                    "lus" -> "Hei le i cognitive wellness leh infiamna result scorecard chu. I result leh report zawng zawng screen-ah ka rawn hawng mek e."
                    "kha" -> "Kane ka long ka result bad ka scorecard jong phi. Ka plie ia ka result ha ka screen mynta."
                    else -> "Here is your cognitive wellness and game results scorecard. Showing your results and performance summary on screen now."
                }
            }

            return DementiaVoiceResponse(
                spokenText = text,
                suggestedNavigation = "health"
            )
        }

        // 1. Specific Game Redirection: Memory Matching
        if (containsAny(lower,
                "memory matching", "matching", "match cards", "cards game", "pair",
                "जोड़ी", "कार्ड", "मैचिंग",
                "যোৰা খেল", "মেচিং", "কাৰ্ড",
                "memory matching khel", "a inang zawng",
                "pyniahap card", "memory matching game")) {
            val text = when (lang) {
                "hi" -> "मेमोरी मैचिंग कार्ड खेल खोल रहा हूँ! आइए जोड़ियों को मिलाएं।"
                "as" -> "মেমৰি মেচিং খেল খুলি দিছো! আহক যোৰাবোৰ মিলাও।"
                "lus" -> "Memory Matching infiamna ka hawng mek e! A inang zawng dun ang le."
                "kha" -> "Ka plie ia ka Memory Matching game! Ia pyniahap lang ia ki card."
                else -> "Opening your Memory Matching cards game right now! Let's match the pairs together."
            }
            return DementiaVoiceResponse(spokenText = text, suggestedNavigation = "game_memory")
        }

        // 2. Specific Game Redirection: Color Stroop Challenge
        if (containsAny(lower,
                "color stroop", "stroop", "colors", "colour",
                "स्ट्रूप", "रंग खेल",
                "ৰং খেল", "ষ্ট্ৰূপ",
                "rawng", "stroop challenge",
                "ki rong", "stroop game")) {
            val text = when (lang) {
                "hi" -> "कलर स्ट्रूप चैलेंज खोल रहा हूँ! रंगों पर ध्यान दीजिए।"
                "as" -> "কালৰ ষ্ট্ৰূপ চেলেঞ্জ খুলি দিছো! ৰংবোৰ মন কৰক।"
                "lus" -> "Color Stroop Challenge ka hawng mek e! A rawng en uluk rawh le."
                "kha" -> "Ka plie ia ka Color Stroop Challenge! Pynleit jingmut ha ki rong."
                else -> "Opening the Color Stroop Challenge right now! Focus on the colors."
            }
            return DementiaVoiceResponse(spokenText = text, suggestedNavigation = "game_stroop")
        }

        // 3. Specific Game Redirection: Pattern Sequence
        if (containsAny(lower,
                "pattern sequence", "sequence", "numbers game", "sequence recall", "pattern",
                "पैटर्न", "क्रम",
                "পেটাৰ্ন", "ক্ৰম",
                "sequence khel", "inrem dan",
                "sequence game", "rukom pynbeit")) {
            val text = when (lang) {
                "hi" -> "पैटर्न सीक्वेंस खेल खोल रहा हूँ! चमकते पैटर्न को याद रखें।"
                "as" -> "পেটাৰ্ন ছিকুৱেন্স খেল খুলি দিছো! আৰ্হিটোলৈ মন কৰক।"
                "lus" -> "Pattern Sequence Recall ka hawng mek e! A inrem dan hre reng rawh le."
                "kha" -> "Ka plie ia ka Pattern Sequence! Kynmaw ia ka rukom pynbeit."
                else -> "Opening Pattern Sequence Recall! Let's remember the glowing pattern."
            }
            return DementiaVoiceResponse(spokenText = text, suggestedNavigation = "game_sequence")
        }

        // 4. Specific Game Redirection: Trail Making
        if (containsAny(lower,
                "trail making", "trail", "connect points", "dots", "numbers",
                "ट्रेल", "बिंदु",
                "ট্ৰেইল", "বিন্দু সংযোগ",
                "trail puzzle",
                "trail game", "dak pyniasoh")) {
            val text = when (lang) {
                "hi" -> "ट्रेल मेकिंग पहेली खोल रहा हूँ! बिंदुओं को क्रम से जोड़ें।"
                "as" -> "ট্ৰেইল মেকিং পাজল খুলি দিছো! বিন্দুবোৰ শৃংখলাবদ্ধভাৱে সংযোগ কৰক।"
                "lus" -> "Trail Making puzzle ka hawng mek e! A indawt zelin zawm rawh le."
                "kha" -> "Ka plie ia ka Trail Making puzzle! Pyniasoh ia ki dak."
                else -> "Opening Trail Making puzzle! Connect the points in order."
            }
            return DementiaVoiceResponse(spokenText = text, suggestedNavigation = "game_trail")
        }

        // 5. General Brain Exercises Hub
        if (containsAny(lower,
                "game", "games", "exercise", "brain", "puzzle", "play",
                "खेल", "गेम", "पहेली",
                "খেল", "ধেমালি",
                "infiamna", "khelh",
                "jingialehkai", "lehkai")) {
            val text = when (lang) {
                "hi" -> "आपके सभी मस्तिष्क खेल खोल रहा हूँ! आइए मिलकर एक मजेदार खेल खेलें।"
                "as" -> "আপোনাৰ মগজুৰ খেলবোৰ খুলি দিছো! আহক একেলগে এটা খেল খেলো।"
                "lus" -> "Rilru infiamna ka hawng mek e! Infiam dun ang hmiang."
                "kha" -> "Ka plie ia ki jingialehkai jingmut! Ia leh lang ia ka jingialehkai."
                else -> "Opening your brain games! Let's play a fun game together."
            }
            return DementiaVoiceResponse(spokenText = text, suggestedNavigation = "exercises")
        }

        // 6. Spatial Orientation: "Where am I?", "What is my location?"
        if (containsAny(lower,
                "where am i", "where is this", "what is my location", "my address", "current location",
                "main kahan hoon", "kahan hoon", "kahan hu", "mera pata", "sthan",
                "moi kot asu", "moi kot", "aamr thikana", "thikana",
                "khawiah nge ka awm", "ka awmna", "khawiah nge",
                "hangno nga don", "shano nga don", "ka jaka")) {
            val ping = TrackerForegroundService.latestDevicePing.value
            val address = ping?.address?.takeIf { it != "Locating nearby area..." && it != "Locating..." }
                ?: "your comfortable residence"

            val text = when (lang) {
                "hi" -> "आप यहाँ $address में पूरी तरह सुरक्षित और आराम से हैं। आप बिल्कुल सुरक्षित हैं, और हम आपको कभी भी घर ले चल सकते हैं।"
                "as" -> "আপুনি ইয়াত $address ত সম্পূৰ্ণ সুৰক্ষিত আৰু শান্তিত আছে। আপুনি কোনো ভয় নকৰিব, আমি আপোনাক যিকোনো সময়ত ঘৰলৈ লৈ যাব পাৰো।"
                "lus" -> "$address ah hian him takin i awm e. Hlauhthawn tur engmah a awm lo, in lamah ka hruai thei reng che."
                "kha" -> "Phi don suk ha $address. Ym don kano kano ka jingma, nga lah ban ialam ia phi sha iing ha kano kano ka por."
                else -> "You are resting safely right here at $address. You are completely safe, and we can guide you home anytime you wish."
            }

            return DementiaVoiceResponse(spokenText = text, suggestedNavigation = "beacon")
        }

        // 7. Temporal Orientation: "What time is it?", "What day is today?"
        if (containsAny(lower,
                "what time", "what day", "what date", "is it night", "what is the time",
                "kya samay hai", "samay", "kitne baje", "baja", "aaj kaun sa din hai", "din",
                "kiman bajise", "somoy", "aaji ki bar",
                "dar engzat nge", "dar engzat", "vawiin eng ni nge", "hun",
                "katno baje", "baje katno", "ka por", "ka sngi")) {
            val cal = Calendar.getInstance()
            val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            val dateFmt = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
            val hour = cal.get(Calendar.HOUR_OF_DAY)

            val partOfDayEn = when (hour) {
                in 5..11 -> "morning"
                in 12..16 -> "afternoon"
                in 17..20 -> "evening"
                else -> "night"
            }

            val text = when (lang) {
                "hi" -> "इस समय $dateFmt को $timeFmt बज रहे हैं। यह एक शांत $partOfDayEn का समय है। आराम से रहें और अपना दिन आनंद से बिताएं।"
                "as" -> "এতিয়া $dateFmt ৰ $timeFmt বাজিছে। এইটো এটা শান্ত সময়। আপোনাৰ দিনটো শান্তিময় হওক।"
                "lus" -> "Tunah hian $dateFmt $timeFmt a ni e. Ni nuam tak hmang ang che."
                "kha" -> "Mynta ka por ka long $timeFmt ha ka $dateFmt. Khublei ia ka sngi ba bha."
                else -> "Right now it is $timeFmt on $dateFmt. It is a peaceful $partOfDayEn. Please take your time and have a wonderful day."
            }

            return DementiaVoiceResponse(spokenText = text)
        }

        // 8. Emotional Calming & Going Home: "I am scared", "I want to go home", "Take me home"
        if (containsAny(lower,
                "scared", "afraid", "lost", "frightened", "want to go home", "take me home", "go home",
                "mujhe ghar le chalo", "ghar jana hai", "ghar", "dar lag raha", "kho gaya",
                "ghor loi jaa", "ghor jabo", "bhoy lagise", "harai golo",
                "in ah haw ka duh", "inah min hruai", "ka hlau", "ka bo",
                "leit sha iing", "wallam sha iing", "nga sheptieng", "nga la jah")) {
            val text = when (lang) {
                "hi" -> "कृपया एक शांत, गहरी सांस लें। मैं बिल्कुल आपके साथ हूँ। आप पूरी तरह सुरक्षित हैं, और मैं आपको अभी घर ले चल रहा हूँ।"
                "as" -> "অনুগ্ৰহ কৰি এটা দীঘল উশাহ লওক। মই আপোনাৰ লগত আছো। আপুনি সম্পূৰ্ণ সুৰক্ষিত, মই আপোনাক এতিয়াই ঘৰলৈ লৈ গৈ আছো।"
                "lus" -> "Hahdam takin thawk la rawh. I bulah ka awm e, i him reng e. In lamah ka hruai haw mek che."
                "kha" -> "Pynjem ia ka mynsiem. Nga don ryngkat bad phi. Phi long ba shngiam, ngan wallam ia phi sha iing mynta."
                else -> "Please take a gentle, deep breath. I am right here with you. You are completely safe, and I am guiding you home right now."
            }

            return DementiaVoiceResponse(spokenText = text, suggestedNavigation = "home_navigate")
        }

        // 9. Autobiographical Grounding: "Who am I?", "Who is caring for me?", "Who loves me?"
        if (containsAny(lower,
                "who am i", "who is taking care", "who is caring", "where is my family", "who loves me",
                "meri dekhbhal kaun kar raha hai", "meri family", "parivar", "mujhe kaun pyar karta hai",
                "mor poriyal", "mook kune sai ase", "poriyal",
                "tunge min enkawl", "ka chhungte", "chhungkua",
                "mano ba sumar ia nga", "ka iing ka sem", "ki bahaing")) {
            val safetyPrefs = context.getSharedPreferences("safety_prefs", Context.MODE_PRIVATE)
            val caregiverName = safetyPrefs.getString("caregiver_name", null)

            val text = when (lang) {
                "hi" -> if (!caregiverName.isNullOrBlank()) {
                    "आपके प्यारे परिजन $caregiverName आपका बहुत ध्यान रखते हैं। आप पूरी तरह सुरक्षित और प्रिय हैं।"
                } else {
                    "आपका परिवार आपसे बहुत प्यार करता है और हमेशा आपके साथ है। आप बिल्कुल सुरक्षित हैं।"
                }
                "as" -> "আপোনাক সকলোৱে বহুত মৰম কৰে। আপোনাৰ পৰিয়ালে আপোনাৰ যত্ন লৈ আছে আৰু আপোনাৰ লগতে আছে।"
                "lus" -> "Duhsak takin enkawl i ni e. I chhungten an hmangaih em em che a, i bulah an awm reng e."
                "kha" -> "Phi dei kiba la ieid eh. Ka iing ka sem jong phi ka ieid ia phi bad ka don ryngkat bad phi."
                else -> if (!caregiverName.isNullOrBlank()) {
                    "Your loving family member, $caregiverName, cares for you deeply and is always by your side."
                } else {
                    "You are deeply loved and warmly cared for. Your family is right beside you in heart and watching over you with love."
                }
            }

            return DementiaVoiceResponse(spokenText = text, suggestedNavigation = "memory")
        }

        // 10. Memory Vault & Family Photographs
        if (containsAny(lower,
                "memory", "photo", "vault", "cherish", "album", "pictures",
                "तस्वीर", "फोटो", "यादें", "एल्बम",
                "ছবি", "ফটো", "স্মৃতি", "এলবাম",
                "thlalak", "hriatrengna",
                "dur", "jingkynmaw")) {
            val text = when (lang) {
                "hi" -> "आपकी पारिवारिक स्मृतियों का एल्बम खोल रहा हूँ! आइए पुरानी खूबसूरत यादें देखें।"
                "as" -> "আপোনাৰ পৰিয়ালৰ সোঁৱৰণিৰ এলবাম খুলিছো! আহক পুৰণি মধুৰ স্মৃতিবোৰ চাওঁ।"
                "lus" -> "Chhungkua hriatrengna album ka hawng mek e! Thlalak mawi tak tak en dun ang le."
                "kha" -> "Ka plie ia ka album jong ka iing! Ia peit lang ia ki dur ba kynmaw."
                else -> "Opening your family memory vault! Let's revisit your happiest moments together."
            }
            return DementiaVoiceResponse(spokenText = text, suggestedNavigation = "memory")
        }

        // 11. Safety & Emergency SOS
        if (containsAny(lower,
                "help", "emergency", "sos", "danger",
                "मदद", "सहायता", "खतरा", "बचाओ",
                "সহায়", "বিপদ",
                "puihna", "chhiatna",
                "jingiarap", "jingma")) {
            val text = when (lang) {
                "hi" -> "मैं बिल्कुल आपके साथ हूँ। मैं आपातकालीन सहायता खोल रहा हूँ और आपके परिवार से संपर्क कर रहा हूँ।"
                "as" -> "মই আপোনাৰ লগতে আছো। জৰুৰীকালীন সাহায্য খুলিছো আৰু পৰিয়ালৰ লগত যোগাযোগ কৰিছো।"
                "lus" -> "I bulah ka awm e. Emergency puihna ka ko vat e."
                "kha" -> "Nga don ryngkat bad phi. Ka plie ia ka emergency bad pyntip sha ka iing."
                else -> "I am right here with you. I am opening emergency support and reaching out to your family right now."
            }
            return DementiaVoiceResponse(spokenText = text, isEmergencyAction = true, suggestedNavigation = "safety")
        }

        // 12. Daily Progress
        if (containsAny(lower,
                "score", "health", "cps", "progress", "how am i doing",
                "स्कोर", "प्रगति", "स्वास्थ्य",
                "স্কোৰ", "অগ্ৰগতি", "স্বাস্থ্য",
                "hmasawnna", "hriselna",
                "jingiaid shaphrang", "ka koit ka khiah")) {
            val text = when (lang) {
                "hi" -> "आपकी दैनिक स्मृति प्रगति खोल रहा हूँ! आपकी याददाश्त बहुत अच्छी है।"
                "as" -> "আপোনাৰ দৈনিক অগ্ৰগতি খুলিছো! আপোনাৰ সোঁৱৰণি খুব ভাল হৈ আছে।"
                "lus" -> "I hmasawnna ka hawng mek e! I rilru a la tha hle mai."
                "kha" -> "Ka plie ia ka jingiaid shaphrang jong phi! Ka jingkynmaw jong phi ka dang khlain bha."
                else -> "Opening your daily memory progress! Every step forward keeps your mind bright."
            }
            return DementiaVoiceResponse(spokenText = text, suggestedNavigation = "health")
        }

        // Fallback: Multilingual friendly guidance
        val fallbackText = when (lang) {
            "hi" -> "मैं आपके साथ हूँ! आप मुझसे पूछ सकते हैं: 'मैं कहाँ हूँ?', 'क्या समय है?', 'मेमोरी मैचिंग खेलो', या 'घर ले चलो'।"
            "as" -> "মই আপোনাৰ লগত আছো! আপুনি ক'ব পাৰে: 'মই ক'ত আছো?', 'কিমান বাজিছে?', 'মেমৰি মেচিং খেলো', বা 'ঘৰলৈ ব'লক'।"
            "lus" -> "I bulah ka awm e! I duh chuan: 'Khawiah nge ka awm?', 'Dar engzat nge?', 'Memory matching khel ang', emaw 'Inah min hruai' tiin min zawt thei e."
            "kha" -> "Nga don ryngkat bad phi! Phi lah ban kylli: 'Hangno nga don?', 'Katno baje?', 'Lehkai memory matching', lane 'Wallam sha iing'."
            else -> "I'm right here! You can ask me: 'Where am I?', 'What time is it?', 'Play memory matching', or 'Take me home'."
        }

        return DementiaVoiceResponse(spokenText = fallbackText)
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
    }
}

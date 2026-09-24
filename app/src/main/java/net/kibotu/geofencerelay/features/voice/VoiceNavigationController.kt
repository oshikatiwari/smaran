package net.kibotu.geofencerelay.features.voice

import java.util.Locale

sealed class CaregiverVoiceAction {
    object RecenterRadar : CaregiverVoiceAction()
    object OpenDirections : CaregiverVoiceAction()
    object ToggleSafeZone : CaregiverVoiceAction()
    object PlayAlarm : CaregiverVoiceAction()
    object StopAlarm : CaregiverVoiceAction()
    object OpenScorecards : CaregiverVoiceAction()
    object CallPatient : CaregiverVoiceAction()
    data class SpokenFeedback(val message: String) : CaregiverVoiceAction()
}

/**
 * Universal Voice Navigation Controller parsing natural speech commands into app actions
 * for both Patient Assistive Access and Caregiver Guardian Console.
 * Fully supports all 5 languages: English, Hindi, Assamese, Mizo, and Khasi.
 */
object VoiceNavigationController {

    fun parseCaregiverCommand(
        prompt: String,
        languageCode: String = "en"
    ): Pair<CaregiverVoiceAction, String> {
        val lower = prompt.trim().lowercase(Locale.ROOT)
        val lang = languageCode.lowercase(Locale.ROOT)

        // 1. Recenter / Locate patient ("Where is the patient?")
        if (containsAny(lower,
                "where is", "locate", "recenter", "center", "find patient", "radar", "track", "position",
                "kahan hai", "kahan he", "kaha hai", "location", "dhoondo", "dhundo", "mariz",
                "kot ase", "kot asu", "kot", "thikana", "rogi", "rogi kot",
                "khawiah nge", "damlo", "damlo awmna", "zawng rawh",
                "shano don", "u nongpang", "peit radar")) {
            val feedback = when (lang) {
                "hi" -> "रडार को मरीज़ के नवीनतम स्थान पर केंद्रित कर रहा हूँ।"
                "as" -> "ৰাডাৰখন ৰোগীৰ শেহতীয়া স্থানলৈ কেন্দ্ৰীভূত কৰা হৈছে।"
                "lus" -> "Radar chu damlo awmna tharah dah rem mek a ni."
                "kha" -> "Pynkylla ia ka radar sha ka jaka ba don u nongpang."
                else -> "Recentering radar view on the patient's latest GPS position."
            }
            return Pair(CaregiverVoiceAction.RecenterRadar, feedback)
        }

        // 2. Directions / Navigation to patient
        if (containsAny(lower,
                "direction", "navigate", "route", "maps", "go to patient", "how to reach",
                "rasta", "raasta", "disha", "naksha", "kaise pahuche", "pahucho",
                "poth", "dixa", "kenekoi jam", "rasta",
                "kawng", "kal dan", "hruaina",
                "ka lynti", "kumno ban leit")) {
            val feedback = when (lang) {
                "hi" -> "मरीज़ तक पहुंचने के लिए नेविगेशन मैप खोल रहा हूँ।"
                "as" -> "ৰোগীৰ ওচৰলৈ যাবলৈ নেভিগেচন মেপ খুলি থকা হৈছে।"
                "lus" -> "Damlo awmna pan tur kawng hruaina ka hawng mek e."
                "kha" -> "Plie ia ka lynti ban leit sha u nongpang."
                else -> "Opening turn-by-turn navigation directly to the patient's coordinates."
            }
            return Pair(CaregiverVoiceAction.OpenDirections, feedback)
        }

        // 3. Safe Zone Setup
        if (containsAny(lower,
                "safe zone", "geofence", "boundary", "perimeter", "radius", "fence", "zone",
                "surakshit", "ghera", "seema", "dayra",
                "porisima", "surakshita",
                "himna hmun", "hungna", "ramri",
                "jaka shngain", "ka pud")) {
            val feedback = when (lang) {
                "hi" -> "सुरक्षित घेरा सीमा सेटिंग खोल रहा हूँ।"
                "as" -> "সুৰক্ষিত পৰিসীমা সংস্থাপন খুলি থকা হৈছে।"
                "lus" -> "Himna hmun hungna siam remna ka hawng mek e."
                "kha" -> "Plie ia ka rukom pynbeit ia ka jaka shngain."
                else -> "Opening safe zone perimeter configuration."
            }
            return Pair(CaregiverVoiceAction.ToggleSafeZone, feedback)
        }

        // 4. Remote Siren / Sound
        if (containsAny(lower,
                "play sound", "ring", "alarm", "siren", "beep", "sound on",
                "alarm bajao", "aawaz bajao", "ghanti bajao", "sound chalu",
                "alarm bojaok", "aawaz korok", "ghonti",
                "alarm ri tir", "sound ri tir", "ti ri rawh",
                "tylliat alarm", "pynri alarm")) {
            val feedback = when (lang) {
                "hi" -> "मरीज़ के फोन पर अलार्म बजा रहा हूँ।"
                "as" -> "ৰোগীৰ ফোনত এলাৰ্ম বজাই থকা হৈছে।"
                "lus" -> "Damlo phone ah alarm a ri mek e."
                "kha" -> "Pynri ia ka alarm ha ka phone u nongpang."
                else -> "Triggering remote locator alarm on the patient's phone."
            }
            return Pair(CaregiverVoiceAction.PlayAlarm, feedback)
        }
        if (containsAny(lower,
                "stop sound", "silence", "stop alarm", "sound off", "quiet", "mute",
                "alarm band", "aawaz band", "shant",
                "alarm bondho", "aawaz bondho",
                "alarm ti tawp", "ti tawp rawh",
                "sangeh alarm", "sngap")) {
            val feedback = when (lang) {
                "hi" -> "अलार्म की आवाज़ बंद कर दी गई है।"
                "as" -> "এলাৰ্মৰ আৱাজ বন্ধ কৰা হৈছে।"
                "lus" -> "Alarm tih tawp a ni ta."
                "kha" -> "La pynsangeh ia ka alarm."
                else -> "Stopping remote alarm sound."
            }
            return Pair(CaregiverVoiceAction.StopAlarm, feedback)
        }

        // 5. Cognitive Scorecard / Health History
        if (containsAny(lower,
                "scorecard", "score", "cognitive", "cps", "history", "assessment", "games", "health",
                "report", "swasthya", "parinam", "khel score",
                "swasthya", "porinam", "falafal",
                "result", "dinhmun",
                "ka jinglong", "ka result")) {
            val feedback = when (lang) {
                "hi" -> "मरीज़ का संज्ञानात्मक स्वास्थ्य स्कोरकार्ड खोल रहा हूँ।"
                "as" -> "ৰোগীৰ মানসিক স্বাস্থ্যৰ স্ক'ৰকাৰ্ড খুলি থকা হৈছে।"
                "lus" -> "Damlo rilru dinhmun entirna scorecard ka hawng mek e."
                "kha" -> "Plie ia ka scorecard jong u nongpang."
                else -> "Opening the patient's cognitive performance scorecards and history."
            }
            return Pair(CaregiverVoiceAction.OpenScorecards, feedback)
        }

        // 6. Call Patient
        if (containsAny(lower,
                "call", "phone", "dial", "contact", "reach",
                "call karo", "phone karo", "baat karao",
                "call korok", "phone korok",
                "phone rawh", "be rawh",
                "phone ia u nongpang")) {
            val feedback = when (lang) {
                "hi" -> "मरीज़ को कॉल करने के लिए डायलर खोल रहा हूँ।"
                "as" -> "ৰোগীক ফোন কৰিবলৈ ডায়েলৰ খুলি থকা হৈছে।"
                "lus" -> "Damlo biak nan phone dialer ka hawng mek e."
                "kha" -> "Plie ia ka dialer ban phone sha u nongpang."
                else -> "Opening phone dialer to reach the patient."
            }
            return Pair(CaregiverVoiceAction.CallPatient, feedback)
        }

        // Default explanation
        val defaultFeedback = when (lang) {
            "hi" -> "आदेश समझा गया। आप कह सकते हैं: 'मरीज़ कहाँ है?', 'रास्ता', 'सुरक्षित घेरा', 'अलार्म बजाओ', या 'स्कोरकार्ड'।"
            "as" -> "নিৰ্দেশ বুজি পালোঁ। আপুনি ক'ব পাৰে: 'ৰোগী ক'ত আছে?', 'পথ', 'সুৰক্ষিত পৰিসীমা', 'এলাৰ্ম বজাওক', বা 'স্ক'ৰকাৰ্ড'।"
            "lus" -> "Thupek hriat a ni e. I sawi thei: 'Damlo khawiah nge?', 'Kawng', 'Himna hmun', 'Alarm ri tir rawh', emaw 'Scorecard' tiin."
            "kha" -> "La sngewthuh ia ka jingbthah. Phi lah ban ong: 'Shano u nongpang?', 'Ka lynti', 'Ka jaka shngain', 'Pynri alarm', lane 'Peit scorecard'."
            else -> "Command recognized. You can say: 'Where is the patient?', 'Directions', 'Safe Zone', 'Play Alarm', or 'Check Scorecard'."
        }

        return Pair(
            CaregiverVoiceAction.SpokenFeedback(defaultFeedback),
            defaultFeedback
        )
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
    }
}

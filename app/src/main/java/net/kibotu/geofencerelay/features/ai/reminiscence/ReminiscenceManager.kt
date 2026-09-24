package net.kibotu.geofencerelay.features.ai.reminiscence

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class MemoryCard(
    val id: String,
    val title: String,
    val category: String,          // Theme
    val question: String,
    val cueText: String,
    val iconEmoji: String,
    val options: List<String>,
    val correctIndex: Int,
    val isCustom: Boolean = false
)

object ReminiscenceManager {

    val defaultThemes = listOf(
        Pair("Family & Grandchildren", "👨‍👩‍👧‍👦"),
        Pair("Hometown & Childhood", "🏡"),
        Pair("Traditional Food & Recipes", "🍛"),
        Pair("Festivals & Celebrations", "🌸"),
        Pair("Brahmaputra & Tea Hills", "🌿"),
        Pair("Favorite Songs & Hobbies", "🎵")
    )

    fun getDefaultThemes(langCode: String): List<Pair<String, String>> {
        return when (langCode) {
            "hi" -> listOf(
                Pair("परिवार और बच्चे", "👨‍👩‍👧‍👦"),
                Pair("बचपन और पैतृक गाँव", "🏡"),
                Pair("पारंपरिक व्यंजन और स्वाद", "🍛"),
                Pair("त्योहार और उत्सव", "🌸"),
                Pair("पहाड़ और चाय के बागान", "🌿"),
                Pair("प्रिय गीत और संगीत", "🎵")
            )
            "as" -> listOf(
                Pair("পৰিয়াল আৰু নাতি-নাতিনী", "👨‍👩‍👧‍👦"),
                Pair("আপোন গাঁও আৰু শৈশৱ", "🏡"),
                Pair("পৰম্পৰাগত খাদ্য আৰু সোৱাদ", "🍛"),
                Pair("ব'হাগ বিহু আৰু উৎসৱ", "🌸"),
                Pair("ব্ৰহ্মপুত্ৰ আৰু সেউজ চাহ বাগিচা", "🌿"),
                Pair("লোকগীত আৰু সুৰ", "🎵")
            )
            "lus" -> listOf(
                Pair("Chhungkua leh Tu leh Fate", "👨‍👩‍👧‍👦"),
                Pair("Naupan Lai leh Khua", "🏡"),
                Pair("Hnam Chaw leh Hmeh Tui", "🍛"),
                Pair("Chapchar Kut leh Cheraw", "🌸"),
                Pair("Mizoram Tlang leh Thingpui Hmun", "🌿"),
                Pair("Hla Mawi leh Zai", "🎵")
            )
            "kha" -> listOf(
                Pair("Ka Kmie, Kpa bad Ki Khun", "👨‍👩‍👧‍👦"),
                Pair("Ka Shnong Trai bad Por Khynnah", "🏡"),
                Pair("Ki Bam Tynrai bad Ja Shongknor", "🍛"),
                Pair("Ka Shad Suk Mynsiem bad Lehniam", "🌸"),
                Pair("Ki Lum Khasi bad Ki Wah Kordor", "🌿"),
                Pair("Ki Jingrwai Tynrai", "🎵")
            )
            else -> defaultThemes
        }
    }

    fun getDefaultCards(langCode: String): List<MemoryCard> {
        return when (langCode) {
            "hi" -> listOf(
                MemoryCard(
                    id = "hi_card_1",
                    title = "बच्चों का आगमन",
                    category = "परिवार और बच्चे",
                    question = "सप्ताहांत में परिवार के कौन से प्यारे सदस्य मिलने आए थे?",
                    cueText = "आपके प्यारे पोते-पोती।",
                    iconEmoji = "👨‍👩‍👧‍👦",
                    options = listOf("अर्जुन और मीरा", "डाकिया", "सब्ज़ी विक्रेता", "दुकानदार"),
                    correctIndex = 0
                ),
                MemoryCard(
                    id = "hi_card_2",
                    title = "महान नदी",
                    category = "पहाड़ और चाय के बागान",
                    question = "हमारे क्षेत्र की सबसे विशाल और शांत बहने वाली नदी कौन सी है?",
                    cueText = "विशाल ब्रह्मपुत्र नदी।",
                    iconEmoji = "🌿",
                    options = listOf("ब्रह्मपुत्र नदी", "गंगा नदी", "यमुना नदी", "नर्मदा नदी"),
                    correctIndex = 0
                ),
                MemoryCard(
                    id = "hi_card_3",
                    title = "चाय के बागान",
                    category = "पारंपरिक व्यंजन और स्वाद",
                    question = "सुबह की ताज़गी के लिए वादियों की कौन सी प्रसिद्ध चाय पी जाती है?",
                    cueText = "हरी पत्तियों की ताज़ा चाय।",
                    iconEmoji = "🍛",
                    options = listOf("असम की ताज़ा चाय", "शीतल पेय", "नींबू पानी", "सादा पानी"),
                    correctIndex = 0
                )
            )
            "as" -> listOf(
                MemoryCard(
                    id = "as_card_1",
                    title = "নাতি-নাতিনীৰ মৰম",
                    category = "পৰিয়াল আৰু নাতি-নাতিনী",
                    question = "দেওবাৰে ঘৰলৈ কোন মৰমৰ নাতি-নাতিনী আহিছিল?",
                    cueText = "আপোনাৰ বৰ মৰমৰ অৰ্জুন আৰু মীৰা।",
                    iconEmoji = "👨‍👩‍👧‍👦",
                    options = listOf("অৰ্জুন আৰু মীৰা", "ডাকোৱাল", "শাক বেপাৰী", "প্ৰতিবেশী"),
                    correctIndex = 0
                ),
                MemoryCard(
                    id = "as_card_2",
                    title = "মহাভাৰতী লুইত",
                    category = "ব্ৰহ্মপুত্ৰ আৰু সেউজ চাহ বাগিচা",
                    question = "আমাৰ অসমৰ বুকুৰে বৈ যোৱা মৰমৰ নদীখন কি?",
                    cueText = "চিৰপ্ৰবাহী ব্ৰহ্মপুত্ৰ।",
                    iconEmoji = "🌿",
                    options = listOf("ব্ৰহ্মপুত্ৰ নদী", "গংগা নদী", "যমুনা নদী", "নৰ্মদা নদী"),
                    correctIndex = 0
                ),
                MemoryCard(
                    id = "as_card_3",
                    title = "ৰঙালী বিহু",
                    category = "ব'হাগ বিহু আৰু উৎসৱ",
                    question = "অসমীয়া জাতিৰ প্ৰাণ আৰু চেনেহৰ বসন্ত উৎসৱটো কি?",
                    cueText = "ঢোল, পেঁপা আৰু বিহুৱান।",
                    iconEmoji = "🌸",
                    options = listOf("ব'হাগ বিহু", "দেৱালী", "হোলী", "ঈদ"),
                    correctIndex = 0
                )
            )
            "lus" -> listOf(
                MemoryCard(
                    id = "lus_card_1",
                    title = "Tu leh Fate Tlawhna",
                    category = "Chhungkua leh Tu leh Fate",
                    question = "Kar tawpah tu nge lo tlawh che u?",
                    cueText = "I tu duhtak te kha an ni.",
                    iconEmoji = "👨‍👩‍👧‍👦",
                    options = listOf("Arjun leh Meera", "Lehkha thawn tute", "Bazar mi", "Thenawm"),
                    correctIndex = 0
                ),
                MemoryCard(
                    id = "lus_card_2",
                    title = "Chapchar Kut",
                    category = "Chapchar Kut leh Cheraw",
                    question = "Mizoram hnam kut ropui leh hlimawm ber kha eng nge ni?",
                    cueText = "Cheraw lam nen a thleng thin.",
                    iconEmoji = "🌸",
                    options = listOf("Chapchar Kut", "Pawl Kut", "Mim Kut", "Kut Hla"),
                    correctIndex = 0
                ),
                MemoryCard(
                    id = "lus_card_3",
                    title = "Mizoram Tlang",
                    category = "Mizoram Tlang leh Thingpui Hmun",
                    question = "Kan ram tlang mawi leh boruak thianghlim tak kha khawi nge?",
                    cueText = "Tlang sang leh zofate tlang.",
                    iconEmoji = "🌿",
                    options = listOf("Reiek Tlang", "Tuirial", "Champhai", "Tlawng"),
                    correctIndex = 0
                )
            )
            "kha" -> listOf(
                MemoryCard(
                    id = "kha_card_1",
                    title = "Ka Jingwan Ki Khun",
                    category = "Ka Kmie, Kpa bad Ki Khun",
                    question = "Ha ka sngi U Blei kiei kiba wan jngoh ia phi?",
                    cueText = "Ki khun ksiew ba ieid jong phi.",
                    iconEmoji = "👨‍👩‍👧‍👦",
                    options = listOf("U Arjun bad Ka Meera", "U nongpan buh", "U nongdie jhur", "Ki paralok"),
                    correctIndex = 0
                ),
                MemoryCard(
                    id = "kha_card_2",
                    title = "Ka Shad Suk Mynsiem",
                    category = "Ka Shad Suk Mynsiem bad Lehniam",
                    question = "Kano ka lehniam bad ka shad tynrai kaba kmen tam jong ki Khasi?",
                    cueText = "Ka shad tynrai ha Weiking.",
                    iconEmoji = "🌸",
                    options = listOf("Ka Shad Suk Mynsiem", "Ka Rongkhli", "Ka Behdeinkhlam", "Ka Wangala"),
                    correctIndex = 0
                ),
                MemoryCard(
                    id = "kha_card_3",
                    title = "Ki Lum Shillong",
                    category = "Ki Lum Khasi bad Ki Wah Kordor",
                    question = "Kano ka jaka kaba thiang bad kaba pyngngad ha Ri Khasi?",
                    cueText = "Ki lum ba jyrngam.",
                    iconEmoji = "🌿",
                    options = listOf("U Lum Shillong", "Ka Sohra", "Ka Dawki", "U Lum Symper"),
                    correctIndex = 0
                )
            )
            else -> listOf(
                MemoryCard(
                    id = "en_card_1",
                    title = "Grandchildren Visit",
                    category = "Family & Grandchildren",
                    question = "Who visited our family during the weekend?",
                    cueText = "Think of your lovely grandchildren.",
                    iconEmoji = "👨‍👩‍👧‍👦",
                    options = listOf("Arjun & Meera", "Postman", "Market grocer", "Office friend"),
                    correctIndex = 0
                ),
                MemoryCard(
                    id = "en_card_2",
                    title = "Brahmaputra River",
                    category = "Brahmaputra & Tea Hills",
                    question = "Which grand river flows peacefully through our region?",
                    cueText = "The majestic river.",
                    iconEmoji = "🌿",
                    options = listOf("Brahmaputra River", "Ganges River", "Yamuna River", "Godavari River"),
                    correctIndex = 0
                ),
                MemoryCard(
                    id = "en_card_3",
                    title = "Assam Tea Aroma",
                    category = "Traditional Food & Recipes",
                    question = "What warm cup of tea is famous across our hills and valleys?",
                    cueText = "Grown in our lush green gardens.",
                    iconEmoji = "🍛",
                    options = listOf("Fresh Assam Tea", "Cold Soda", "Lemon Juice", "Plain Water"),
                    correctIndex = 0
                )
            )
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    fun loadAllCards(context: Context, langCode: String = "en"): List<MemoryCard> {
        val prefs = context.getSharedPreferences("reminiscence_prefs", Context.MODE_PRIVATE)
        val customCardsJson = prefs.getString("custom_cards", null)
        val customCards: List<MemoryCard> = if (!customCardsJson.isNullOrBlank()) {
            try {
                json.decodeFromString<List<MemoryCard>>(customCardsJson)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val defaults = getDefaultCards(langCode)
        return customCards + defaults
    }

    fun saveCustomCard(context: Context, card: MemoryCard) {
        val prefs = context.getSharedPreferences("reminiscence_prefs", Context.MODE_PRIVATE)
        val customCardsJson = prefs.getString("custom_cards", null)
        val existing: MutableList<MemoryCard> = if (!customCardsJson.isNullOrBlank()) {
            try {
                json.decodeFromString<List<MemoryCard>>(customCardsJson).toMutableList()
            } catch (_: Exception) {
                mutableListOf()
            }
        } else {
            mutableListOf()
        }
        existing.add(card)
        val serialized = json.encodeToString(existing)
        prefs.edit().putString("custom_cards", serialized).apply()
    }

    fun clearAllCards(context: Context) {
        val prefs = context.getSharedPreferences("reminiscence_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("custom_cards").apply()
    }
}

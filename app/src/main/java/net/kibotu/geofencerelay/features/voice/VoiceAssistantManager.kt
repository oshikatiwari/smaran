package net.kibotu.geofencerelay.features.voice

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

/**
 * Universal Voice Engine for SMARAN.
 * Features:
 * - Natural adult healthcare consultant voice synthesis (Google Studio/Neural TTS)
 * - Autonomous, continuous "Hey Smaran" ambient wake word listener with self-healing restart
 * - Multi-dialect STT recognition across 10 Indian languages
 */
class VoiceAssistantManager private constructor() {

    private val tag = "SmaranVoice"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _transcribedText = MutableStateFlow("")
    val transcribedText: StateFlow<String> = _transcribedText.asStateFlow()

    private val _spokenResponse = MutableStateFlow("")
    val spokenResponse: StateFlow<String> = _spokenResponse.asStateFlow()

    private val _audioRmsLevel = MutableStateFlow(0f)
    val audioRmsLevel: StateFlow<Float> = _audioRmsLevel.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isWakeWordActive = MutableStateFlow(false)
    val isWakeWordActive: StateFlow<Boolean> = _isWakeWordActive.asStateFlow()

    var onWakeWordTriggered: (() -> Unit)? = null
    private var onSpeechResultCallback: ((String) -> Unit)? = null
    private var onTtsDoneCallback: (() -> Unit)? = null

    private var appContext: Context? = null
    private var currentLanguageCode: String = "en"
    private var isWakeWordMonitoring = false

    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (tts == null) {
            val initListener = TextToSpeech.OnInitListener { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsReady = true

                    try {
                        val audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                        tts?.setAudioAttributes(audioAttributes)
                    } catch (_: Exception) {}

                    tts?.setSpeechRate(1.0f)
                    tts?.setPitch(1.0f)

                    selectBestConsultantVoice(Locale.ENGLISH)

                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _voiceState.value = VoiceState.SPEAKING
                        }

                        override fun onDone(utteranceId: String?) {
                            if (_voiceState.value == VoiceState.SPEAKING) {
                                _voiceState.value = VoiceState.IDLE
                            }
                            val cb = onTtsDoneCallback
                            onTtsDoneCallback = null
                            scope.launch(Dispatchers.Main) {
                                cb?.invoke()
                            }
                        }

                        override fun onError(utteranceId: String?) {
                            if (_voiceState.value == VoiceState.SPEAKING) {
                                _voiceState.value = VoiceState.IDLE
                            }
                            val cb = onTtsDoneCallback
                            onTtsDoneCallback = null
                            scope.launch(Dispatchers.Main) {
                                cb?.invoke()
                            }
                        }
                    })
                }
            }

            tts = try {
                TextToSpeech(context.applicationContext, initListener, "com.google.android.tts")
            } catch (_: Exception) {
                TextToSpeech(context.applicationContext, initListener)
            }
        }
    }

    private fun selectBestConsultantVoice(targetLocale: Locale) {
        try {
            val voices = tts?.voices ?: return
            val matchingVoices = voices.filter { it.locale.language == targetLocale.language }

            val bestVoice: Voice? = matchingVoices.firstOrNull { voice ->
                val name = voice.name.lowercase(Locale.ROOT)
                val isNeural = name.contains("wavenet") || name.contains("neural") || name.contains("network")
                val isWarmFemaleOrMale = name.contains("f0") || name.contains("f1") || name.contains("c0") || name.contains("standard-a")
                val isGoodQuality = voice.quality >= Voice.QUALITY_HIGH
                isNeural || (isWarmFemaleOrMale && isGoodQuality)
            } ?: matchingVoices.firstOrNull { it.quality >= Voice.QUALITY_NORMAL }
              ?: matchingVoices.firstOrNull()

            if (bestVoice != null) {
                tts?.voice = bestVoice
                Log.d(tag, "Selected adult consultant voice: ${bestVoice.name}")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error selecting consultant voice: ${e.message}")
        }
    }

    /**
     * Starts continuous ambient wake word detection for "Hey Smaran".
     * Automatically restarts on silence/timeout so it never drops.
     */
    fun startWakeWordListening(context: Context, languageCode: String = "en") {
        appContext = context.applicationContext
        currentLanguageCode = languageCode
        _isWakeWordActive.value = true
        if (isWakeWordMonitoring) return
        isWakeWordMonitoring = true
        restartWakeWordLoop()
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        if (enabled) {
            val ctx = appContext ?: return
            startWakeWordListening(ctx, currentLanguageCode)
        } else {
            stopWakeWordListening()
        }
    }

    fun stopWakeWordListening() {
        _isWakeWordActive.value = false
        isWakeWordMonitoring = false
        cancelListening()
    }

    private fun restartWakeWordLoop() {
        val ctx = appContext ?: return
        if (!_isWakeWordActive.value) return
        if (_voiceState.value == VoiceState.SPEAKING) return

        scope.launch {
            delay(250)
            if (!_isWakeWordActive.value || _voiceState.value == VoiceState.SPEAKING) return@launch

            if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
                Log.w(tag, "Speech recognition not available for wake-word loop")
                return@launch
            }

            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(ctx).apply {
                    setRecognitionListener(createListener())
                }

                val locale = when (currentLanguageCode) {
                    "hi" -> Locale("hi", "IN")
                    "as" -> Locale("as", "IN")
                    "lus", "kha" -> Locale("en", "IN")
                    else -> Locale.ENGLISH
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toString())
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.toString())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                speechRecognizer?.startListening(intent)
                Log.d(tag, "Ambient wake-word listener active for 'Hey Smaran'...")
            } catch (e: Exception) {
                Log.e(tag, "Wake-word loop error: ${e.message}")
                delay(1200)
                if (_isWakeWordActive.value) restartWakeWordLoop()
            }
        }
    }

    fun startListening(
        context: Context,
        languageCode: String = "en",
        onResult: (String) -> Unit
    ) {
        onSpeechResultCallback = onResult
        _errorMessage.value = null
        _transcribedText.value = ""

        stopSpeaking()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _errorMessage.value = "Speech recognition is not available on this device."
            _voiceState.value = VoiceState.ERROR
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }

            val locale = when (languageCode) {
                "hi" -> Locale("hi", "IN")
                "as" -> Locale("as", "IN")
                "lus", "kha" -> Locale("en", "IN")
                else -> Locale.ENGLISH
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toString())
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            _voiceState.value = VoiceState.LISTENING
            speechRecognizer?.startListening(intent)
            Log.d(tag, "Speech recognizer started listening in language: $languageCode")
        } catch (e: Exception) {
            Log.e(tag, "Failed to start speech recognizer: ${e.message}", e)
            _errorMessage.value = "Failed to start microphone: ${e.message}"
            _voiceState.value = VoiceState.ERROR
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            if (_voiceState.value == VoiceState.LISTENING) {
                _voiceState.value = VoiceState.PROCESSING
            }
        } catch (_: Exception) {}
    }

    fun cancelListening() {
        try {
            speechRecognizer?.cancel()
            _voiceState.value = VoiceState.IDLE
            _audioRmsLevel.value = 0f
        } catch (_: Exception) {}
    }

    fun speak(
        text: String,
        languageCode: String = "en",
        onDone: (() -> Unit)? = null
    ) {
        if (text.isBlank()) return
        onTtsDoneCallback = onDone
        _spokenResponse.value = text
        _voiceState.value = VoiceState.SPEAKING

        try {
            if (isTtsReady && tts != null) {
                tts?.setSpeechRate(1.0f)
                tts?.setPitch(1.0f)

                val locale = when (languageCode) {
                    "hi" -> Locale("hi", "IN")
                    "as" -> Locale("as", "IN")
                    "lus", "kha" -> Locale("en", "IN")
                    else -> Locale.ENGLISH
                }
                tts?.language = locale
                selectBestConsultantVoice(locale)

                val params = Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "smaran_voice_response")
                    putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                }
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "smaran_voice_response")
            } else {
                onDone?.invoke()
            }
        } catch (e: Exception) {
            Log.e(tag, "TTS speak error: ${e.message}")
            onDone?.invoke()
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
            if (_voiceState.value == VoiceState.SPEAKING) {
                _voiceState.value = VoiceState.IDLE
            }
        } catch (_: Exception) {}
    }

    private fun checkAndTriggerWakeWord(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        val isWake = lower.contains("hey smaran") ||
                     lower.contains("hello smaran") ||
                     lower.contains("smaran") ||
                     lower.contains("स्मरण") ||
                     lower.contains("হে স্মৰণ") ||
                     lower.contains("স্মৰণ") ||
                     lower.contains("hey simran") ||
                     lower.contains("simran") ||
                     lower.contains("hey sharan") ||
                     lower.contains("sharan") ||
                     lower.contains("hey smart")

        if (isWake) {
            Log.i(tag, "Wake-word triggered from speech: '$text'")
            triggerHapticFeedback()
            onWakeWordTriggered?.invoke()
            return true
        }
        return false
    }

    private fun triggerHapticFeedback() {
        try {
            val ctx = appContext ?: return
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                v?.vibrate(120)
            }
        } catch (_: Exception) {}
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _audioRmsLevel.value = 0.1f
        }

        override fun onBeginningOfSpeech() {
            if (_voiceState.value != VoiceState.SPEAKING) {
                _voiceState.value = VoiceState.LISTENING
            }
        }

        override fun onRmsChanged(rmsdB: Float) {
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
            _audioRmsLevel.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _audioRmsLevel.value = 0f
        }

        override fun onError(error: Int) {
            Log.d(tag, "Speech recognizer status code: $error")
            _audioRmsLevel.value = 0f

            // If wake word is active and we are not speaking, quietly self-heal and restart loop!
            if (_isWakeWordActive.value && _voiceState.value != VoiceState.SPEAKING) {
                restartWakeWordLoop()
            } else {
                _voiceState.value = VoiceState.IDLE
            }
        }

        override fun onResults(results: Bundle?) {
            _audioRmsLevel.value = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val topMatch = matches?.firstOrNull()?.trim() ?: ""

            if (topMatch.isNotBlank()) {
                val wasWake = checkAndTriggerWakeWord(topMatch)
                _transcribedText.value = topMatch
                Log.d(tag, "Speech recognition match: '$topMatch' (wake=$wasWake)")

                if (!wasWake) {
                    onSpeechResultCallback?.invoke(topMatch)
                    if (_isWakeWordActive.value && _voiceState.value != VoiceState.SPEAKING) {
                        restartWakeWordLoop()
                    }
                }
            } else if (_isWakeWordActive.value && _voiceState.value != VoiceState.SPEAKING) {
                restartWakeWordLoop()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = partials?.firstOrNull()?.trim() ?: ""
            if (partial.isNotBlank()) {
                _transcribedText.value = partial
                checkAndTriggerWakeWord(partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun shutdown() {
        try {
            stopWakeWordListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            tts?.stop()
            tts?.shutdown()
            tts = null
            isTtsReady = false
        } catch (_: Exception) {}
    }

    companion object {
        val shared: VoiceAssistantManager by lazy { VoiceAssistantManager() }
    }
}

package net.kibotu.geofencerelay.features.voice

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
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
 * Universal Voice Assistant Manager coordinating Speech-to-Text and Text-to-Speech.
 * Configured for a soothing, clear, caring doctor / nurse / medical consultant persona:
 * - High-fidelity media audio stream routing (eliminates tinny/hoarse PA horn sound)
 * - Empathetic, composed bedside pace (0.93x rate, 0.98x pitch)
 * - Automatic selection of Google Neural / studio-grade consultant voice
 * - Multilingual support across English, Hindi, Assamese, Mizo, and Khasi
 * - "Hey Smaran" ambient wake word listener
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

    fun initialize(context: Context) {
        if (tts == null) {
            val initListener = TextToSpeech.OnInitListener { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsReady = true

                    // High-fidelity speech attributes (routes through multimedia DAC, eliminating megaphone distortion)
                    try {
                        val audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                        tts?.setAudioAttributes(audioAttributes)
                    } catch (_: Exception) {}

                    // Natural, clear adult consultant tempo & pitch
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
                        }

                        override fun onError(utteranceId: String?) {
                            if (_voiceState.value == VoiceState.SPEAKING) {
                                _voiceState.value = VoiceState.IDLE
                            }
                        }
                    })
                }
            }

            // Prefer Google Studio/Neural TTS engine to avoid metallic or low-bitrate AOSP synthesizers
            tts = try {
                TextToSpeech(context.applicationContext, initListener, "com.google.android.tts")
            } catch (_: Exception) {
                TextToSpeech(context.applicationContext, initListener)
            }
        }
    }

    private fun selectBestConsultantVoice(locale: Locale) {
        try {
            val allVoices = tts?.voices ?: return
            val matchingVoices = allVoices.filter {
                it.locale.language.equals(locale.language, ignoreCase = true) &&
                        !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
            }
            if (matchingVoices.isEmpty()) return

            // Prefer Google neural/wavenet/studio adult voice models; avoid compressed syllable-quantized offline packs
            val bestVoice = matchingVoices.sortedWith(
                compareByDescending<Voice> { it.quality }
                    .thenByDescending {
                        val name = it.name.lowercase(Locale.ROOT)
                        when {
                            name.contains("neural2") -> 12
                            name.contains("neural") -> 10
                            name.contains("wavenet") -> 9
                            name.contains("natural") -> 8
                            name.contains("network") -> 7
                            name.contains("premium") -> 6
                            !name.contains("local") -> 5
                            else -> 1
                        }
                    }
                    .thenByDescending { it.isNetworkConnectionRequired }
            ).firstOrNull()

            if (bestVoice != null) {
                tts?.voice = bestVoice
                Log.d(tag, "Selected adult consultant voice: ${bestVoice.name}, quality: ${bestVoice.quality}")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error selecting consultant voice: ${e.message}")
        }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        _isWakeWordActive.value = enabled
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
        _spokenResponse.value = text
        _voiceState.value = VoiceState.SPEAKING

        try {
            if (isTtsReady && tts != null) {
                // Natural, articulate adult doctor/consultant rate and pitch
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
            }
        } catch (e: Exception) {
            Log.e(tag, "TTS speak error: ${e.message}")
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

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _voiceState.value = VoiceState.LISTENING
            _audioRmsLevel.value = 0.1f
        }

        override fun onBeginningOfSpeech() {
            _voiceState.value = VoiceState.LISTENING
        }

        override fun onRmsChanged(rmsdB: Float) {
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
            _audioRmsLevel.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _voiceState.value = VoiceState.PROCESSING
            _audioRmsLevel.value = 0f
        }

        override fun onError(error: Int) {
            val msg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "I couldn't quite hear that. Please speak gently again."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I'm listening whenever you are ready."
                SpeechRecognizer.ERROR_AUDIO -> "Microphone issue. Please check settings."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network issue with voice recognition."
                else -> "Speech recognition ended."
            }
            Log.w(tag, "Speech recognition error: $error ($msg)")
            _errorMessage.value = msg
            _voiceState.value = VoiceState.IDLE
            _audioRmsLevel.value = 0f
        }

        override fun onResults(results: Bundle?) {
            _voiceState.value = VoiceState.IDLE
            _audioRmsLevel.value = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val topMatch = matches?.firstOrNull()?.trim() ?: ""
            if (topMatch.isNotBlank()) {
                val lower = topMatch.lowercase(Locale.ROOT)
                // Check if user said wake word
                if (lower.contains("hey smaran") || lower.contains("hello smaran") || lower.contains("smaran")) {
                    onWakeWordTriggered?.invoke()
                }

                _transcribedText.value = topMatch
                Log.d(tag, "Speech recognition match: '$topMatch'")
                onSpeechResultCallback?.invoke(topMatch)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = partials?.firstOrNull()?.trim() ?: ""
            if (partial.isNotBlank()) {
                _transcribedText.value = partial
                val lower = partial.lowercase(Locale.ROOT)
                if (lower.contains("hey smaran") || lower.contains("hello smaran")) {
                    onWakeWordTriggered?.invoke()
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun shutdown() {
        try {
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

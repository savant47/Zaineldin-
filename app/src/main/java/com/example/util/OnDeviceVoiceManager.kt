package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.model.ChalkboardStep
import com.example.data.model.LanguageMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * محرك الصوت المحلي المجاني بالكامل (On-Device TTS & STT)
 * لا يستهلك أي تكاليف API خارجية، ويدعم التبديل الفوري بين العربية والإنجليزية.
 */
class OnDeviceVoiceManager(context: Context) {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _voiceStatusMessage = MutableStateFlow("محرك الصوت المحلي جاهز (مجاني بالكامل بدون تكلفة API)")
    val voiceStatusMessage: StateFlow<String> = _voiceStatusMessage.asStateFlow()

    private val arabicLocale = Locale.forLanguageTag("ar-EG")
    private val englishLocale = Locale.US

    init {
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        try {
            // تفضيل محرك Google Speech Services الرسمي لأنه يحتوي على الأصوات العصبية الفصحى المحسنة
            tts = TextToSpeech(appContext, { status ->
                if (status == TextToSpeech.SUCCESS) {
                    _isTtsReady.value = true
                    // ضبط وتيرة وطبقة الصوت لتكون دافئة وطبيعية وأشبه بمدرس ودود
                    tts?.setPitch(1.0f)
                    tts?.setSpeechRate(0.88f)
                    
                    // اختيار أفضل نبرة صوتية متوفرة للعربية (تفضيل الأصوات عالية الجودة / غير الروبوتية)
                    configureBestAvailableVoice()

                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                        }
                    })
                } else {
                    _isTtsReady.value = false
                    _voiceStatusMessage.value = "محرك النطق الصوتي غير مفعل على هذا الجهاز"
                }
            }, "com.google.android.tts")
        } catch (e: Exception) {
            // في حال عدم توفر حزمة جوجل، العودة للمحرك الافتراضي في النظام
            try {
                tts = TextToSpeech(appContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        _isTtsReady.value = true
                        tts?.setPitch(1.0f)
                        tts?.setSpeechRate(0.88f)
                        configureBestAvailableVoice()
                    }
                }
            } catch (_: Exception) {
                _isTtsReady.value = false
                _voiceStatusMessage.value = "تعذر تهيئة القارئ الصوتي المحلي"
            }
        }
    }

    private fun configureBestAvailableVoice() {
        try {
            val engine = tts ?: return
            val voices = engine.voices ?: return
            // البحث عن صوت عربي عالي الجودة لا يحتاج اتصال شبكة بطيء أو روبوتي
            val arabicVoice = voices.firstOrNull { voice ->
                voice.locale.language == "ar" &&
                    (voice.name.contains("natural", ignoreCase = true) ||
                        voice.name.contains("wavenet", ignoreCase = true) ||
                        voice.name.contains("neural", ignoreCase = true) ||
                        voice.quality >= android.speech.tts.Voice.QUALITY_VERY_HIGH)
            } ?: voices.firstOrNull { it.locale.language == "ar" && !it.isNetworkConnectionRequired }
            
            if (arabicVoice != null) {
                engine.voice = arabicVoice
            }
        } catch (_: Exception) {
            // Fallback gracefully
        }
    }

    /**
     * قراءة السبورة الذكية باللغتين (يتحدث الإنجليزية ثم يشرح بالعربية) أو حسب لغة المادة
     */
    fun speakChalkboardExplanation(
        greeting: String,
        steps: List<ChalkboardStep>,
        question: String,
        isBilingualSubject: Boolean,
        languageMode: LanguageMode
    ) {
        val engine = tts ?: return
        if (!_isTtsReady.value) {
            _voiceStatusMessage.value = "جاري تجهيز القارئ الصوتي المحلي..."
            return
        }

        engine.stop()
        _isSpeaking.value = true

        // 1. قراءة التحية بالعربية
        speakSegmentInternal(engine, greeting, arabicLocale, TextToSpeech.QUEUE_FLUSH)

        // 2. قراءة خطوات الشرح على السبورة
        for (step in steps) {
            if (isBilingualSubject && languageMode != LanguageMode.ARABIC && step.englishLine.isNotBlank()) {
                speakSegmentInternal(engine, step.englishLine, englishLocale, TextToSpeech.QUEUE_ADD)
            } else if (!isBilingualSubject && step.englishLine.isNotBlank()) {
                speakSegmentInternal(engine, step.englishLine, arabicLocale, TextToSpeech.QUEUE_ADD)
            }

            if (languageMode != LanguageMode.ENGLISH && step.arabicExplanation.isNotBlank()) {
                speakSegmentInternal(engine, step.arabicExplanation, arabicLocale, TextToSpeech.QUEUE_ADD)
            }
        }

        // 3. قراءة السؤال الختامي
        val questionLocale = if (isBilingualSubject && languageMode == LanguageMode.ENGLISH) {
            englishLocale
        } else {
            arabicLocale
        }
        speakSegmentInternal(engine, question, questionLocale, TextToSpeech.QUEUE_ADD)
        _voiceStatusMessage.value = "يقوم المدرس الآن بقراءة الشرح من السبورة..."
    }

    fun speakSingleText(text: String, preferEnglish: Boolean) {
        val engine = tts ?: return
        if (!_isTtsReady.value || text.isBlank()) return
        engine.stop()
        _isSpeaking.value = true
        val locale = if (preferEnglish) englishLocale else arabicLocale
        speakSegmentInternal(engine, text, locale, TextToSpeech.QUEUE_FLUSH)
    }

    private fun speakSegmentInternal(
        engine: TextToSpeech,
        text: String,
        locale: Locale,
        queueMode: Int
    ) {
        val cleanText = text.replace("*", "").replace("#", "").trim()
        if (cleanText.isBlank()) return
        val availability = engine.isLanguageAvailable(locale)
        if (availability >= TextToSpeech.LANG_AVAILABLE) {
            engine.language = locale
        } else if (locale.language == "ar") {
            engine.language = Locale.forLanguageTag("ar")
        } else {
            engine.language = Locale.ENGLISH
        }
        val params = Bundle()
        val utteranceId = UUID.randomUUID().toString()
        engine.speak(cleanText, queueMode, params, utteranceId)
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
        _voiceStatusMessage.value = "تم إيقاف القراءة الصوتية"
    }

    fun startListening(
        recognizeEnglish: Boolean,
        onResult: (String) -> Unit
    ) {
        stopSpeaking()
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            _voiceStatusMessage.value =
                "خدمة التعرف الصوتي غير متوفرة على هذا المحاكي - يمكنك كتابة إجابة زين الدين في مربع المحادثة بالأسفل"
            return
        }

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
            }

            val langTag = if (recognizeEnglish) "en-US" else "ar-EG"
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _partialTranscript.value = ""
                    _voiceStatusMessage.value = if (recognizeEnglish) {
                        "تحدث الآن بالإنجليزية يا زين الدين... المدرس يستمع إليك"
                    } else {
                        "تحدث الآن بالعربية يا زين الدين... المدرس يستمع إليك"
                    }
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _voiceStatusMessage.value =
                        "لم يتم التقاط صوت واضح - اضغط على المايكروفون مرة أخرى أو اكتب إجابتك"
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val recognized = matches?.firstOrNull()?.trim().orEmpty()
                    if (recognized.isNotBlank()) {
                        _partialTranscript.value = recognized
                        _voiceStatusMessage.value = "سمعك المدرس: \"$recognized\""
                        onResult(recognized)
                    } else {
                        _voiceStatusMessage.value = "لم يتم التعرف على الكلام، حاول مرة أخرى يا بطل!"
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (partial.isNotBlank()) {
                        _partialTranscript.value = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            _voiceStatusMessage.value = "تعذر تشغيل المايكروفون المحلي: ${e.localizedMessage ?: ""}"
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
        _isListening.value = false
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
    }
}

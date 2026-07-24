package com.example.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class TextToSpeechManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            // Default to Bengali or English if Bengali not supported
            val bnLocale = Locale("bn", "BD")
            val result = tts?.setLanguage(bnLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.US
            }

            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
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
        }
    }

    fun speak(text: String) {
        if (!isInitialized) return
        stop()

        // Clean text for speaking (remove markdown code blocks & asterisks)
        val cleanText = text
            .replace(Regex("```[\\s\\S]*?```"), "কোড ব্লক অমিট করা হলো")
            .replace(Regex("[`*#_]"), "")

        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "ChatGPT_TTS_ID")
        _isSpeaking.value = true
    }

    fun stop() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

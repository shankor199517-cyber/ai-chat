package com.example.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

sealed class SpeechState {
    object Idle : SpeechState()
    object Listening : SpeechState()
    data class Success(val text: String) : SpeechState()
    data class Error(val message: String) : SpeechState()
}

class SpeechRecognizerManager(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    fun startListening(languageCode: String = "bn-BD") {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _speechState.value = SpeechState.Error("ভয়েস ইনপুট সিস্টেমে উপলব্ধ নয়।")
            return
        }

        stopListening()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _speechState.value = SpeechState.Listening
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _speechState.value = SpeechState.Idle
                }

                override fun onError(error: Int) {
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "কোনো কথা শোনা যায়নি।"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কথা বলতে সময় শেষ।"
                        SpeechRecognizer.ERROR_AUDIO -> "অডিও রিকোর্ডিং এরর।"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "মাইক্রোফোন পারমিশন দিন।"
                        else -> "ভয়েস ইনপুট এরর: $error"
                    }
                    _speechState.value = SpeechState.Error(msg)
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        _recognizedText.value = text
                        _speechState.value = SpeechState.Success(text)
                    } else {
                        _speechState.value = SpeechState.Idle
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        _recognizedText.value = matches[0]
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            speechRecognizer?.startListening(intent)
            _speechState.value = SpeechState.Listening
        } catch (e: Exception) {
            _speechState.value = SpeechState.Error("ভয়েস চালু করা যায়নি: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            // ignore
        }
        _speechState.value = SpeechState.Idle
    }

    fun resetState() {
        _speechState.value = SpeechState.Idle
        _recognizedText.value = ""
    }
}

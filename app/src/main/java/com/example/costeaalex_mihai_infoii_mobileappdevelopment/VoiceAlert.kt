package com.example.costeaalex_mihai_infoii_mobileappdevelopment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class VoiceAlert(
    private val context: Context,
    private val onAlertDetected: () -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null

    fun start() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val heard = matches?.firstOrNull()?.lowercase() ?: ""
                android.util.Log.d("VoiceAlert", "Heard: $heard")

                if ("alert" in heard || "help" in heard || "emergency" in heard) {
                    onAlertDetected()
                } else {
                    // keep listening if nothing matched
                    listen()
                }
            }

            override fun onError(error: Int) {
                android.util.Log.d("VoiceAlert", "Error: $error, restarting...")
                listen() // restart on error so it keeps listening
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        listen()
    }

    private fun listen() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stop() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
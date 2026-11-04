package com.local.daveai

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class OpenAiTtsManager(private val context: Context) {

    companion object {
        private const val TAG = "OpenAiTtsManager"
        private const val API_KEY = "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"
        private const val TTS_URL = "https://api.openai.com/v1/audio/speech"
    }

    private var mediaPlayer: MediaPlayer? = null

    suspend fun speak(text: String, voice: String = "alloy"): Boolean = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext false
        try {
            val url = URL(TTS_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $API_KEY")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
            }

            val jsonBody = JSONObject().apply {
                put("model", "tts-1")
                put("input", text)
                put("voice", voice) // Options: alloy, echo, fable, onyx, nova, shimmer
            }

            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val audioFile = File.createTempFile("dave_speech_", ".mp3", context.cacheDir)
                connection.inputStream.use { input ->
                    FileOutputStream(audioFile).use { output ->
                        input.copyTo(output)
                    }
                }

                playAudioFile(audioFile)
                true
            } else {
                val errorMsg = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e(TAG, "OpenAI TTS API error: ${connection.responseCode} - $errorMsg")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate or play OpenAI TTS speech", e)
            false
        }
    }

    private fun playAudioFile(file: File) {
        try {
            stop()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    release()
                    mediaPlayer = null
                    try { file.delete() } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio file", e)
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        }
    }
}

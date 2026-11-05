package com.local.daveai

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class OpenAiTtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "OpenAiTtsManager"
        private const val API_KEY = "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"
        private const val TTS_URL = "https://api.openai.com/v1/audio/speech"
    }

    private var mediaPlayer: MediaPlayer? = null
    @Volatile
    private var isPlaying = false

    private val audioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private var fallbackTts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    @Volatile
    private var isFallbackInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = fallbackTts?.setLanguage(Locale.UK)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                fallbackTts?.setLanguage(Locale.US)
            }
            fallbackTts?.setAudioAttributes(audioAttributes)
            isFallbackInitialized = true
        } else {
            Log.e(TAG, "Initialization of fallback TextToSpeech failed")
        }
    }

    suspend fun speak(text: String, voice: String = "alloy"): Boolean = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext false
        stop()
        isPlaying = true

        val chunks = splitTextIntoChunks(text, maxChunkSize = 800)
        var openAiSuccess = false

        for (chunk in chunks) {
            if (!isPlaying) break
            val audioFile = fetchTtsAudio(chunk, voice)
            if (audioFile != null) {
                openAiSuccess = true
                if (!isPlaying) {
                    try { audioFile.delete() } catch (_: Exception) {}
                    break
                }
                playAudioFileSynchronously(audioFile)
            } else {
                break
            }
        }

        // Fallback to native TextToSpeech if OpenAI TTS API is unreachable or fails
        if (!openAiSuccess && isPlaying && isFallbackInitialized) {
            withContext(Dispatchers.Main) {
                fallbackTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "DaveSpeech_${System.currentTimeMillis()}")
            }
        }

        isPlaying = false
        true
    }

    private fun splitTextIntoChunks(text: String, maxChunkSize: Int = 800): List<String> {
        val result = mutableListOf<String>()
        val paragraphs = text.split("\n")
        val currentChunk = StringBuilder()

        for (paragraph in paragraphs) {
            val trimmedPara = paragraph.trim()
            if (trimmedPara.isEmpty()) continue

            if (currentChunk.length + trimmedPara.length + 1 <= maxChunkSize) {
                if (currentChunk.isNotEmpty()) currentChunk.append(" ")
                currentChunk.append(trimmedPara)
            } else {
                val sentences = trimmedPara.split(Regex("(?<=[.!?])\\s+"))
                for (sentence in sentences) {
                    if (currentChunk.length + sentence.length + 1 <= maxChunkSize) {
                        if (currentChunk.isNotEmpty()) currentChunk.append(" ")
                        currentChunk.append(sentence)
                    } else {
                        if (currentChunk.isNotEmpty()) {
                            result.add(currentChunk.toString())
                            currentChunk.clear()
                        }
                        if (sentence.length > maxChunkSize) {
                            sentence.chunked(maxChunkSize).forEach { sub ->
                                result.add(sub)
                            }
                        } else {
                            currentChunk.append(sentence)
                        }
                    }
                }
            }
        }

        if (currentChunk.isNotEmpty()) {
            result.add(currentChunk.toString())
        }

        return result
    }

    private fun fetchTtsAudio(chunkText: String, voice: String): File? {
        return try {
            val url = URL(TTS_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $API_KEY")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 30000
            }

            val jsonBody = JSONObject().apply {
                put("model", "tts-1")
                put("input", chunkText)
                put("voice", voice)
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
                audioFile
            } else {
                val errorMsg = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e(TAG, "OpenAI TTS API error: ${connection.responseCode} - $errorMsg")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch TTS audio chunk", e)
            null
        }
    }

    private suspend fun playAudioFileSynchronously(file: File) = withContext(Dispatchers.Main) {
        val deferred = CompletableDeferred<Unit>()
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    release()
                    mediaPlayer = null
                    try { file.delete() } catch (_: Exception) {}
                    if (deferred.isActive) deferred.complete(Unit)
                }
                setOnErrorListener { _, _, _ ->
                    release()
                    mediaPlayer = null
                    try { file.delete() } catch (_: Exception) {}
                    if (deferred.isActive) deferred.complete(Unit)
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio file", e)
            try { file.delete() } catch (_: Exception) {}
            if (deferred.isActive) deferred.complete(Unit)
        }
        deferred.await()
    }

    fun stop() {
        isPlaying = false
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            fallbackTts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech player", e)
        }
    }

    fun shutdown() {
        stop()
        try {
            fallbackTts?.shutdown()
            fallbackTts = null
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down fallback TextToSpeech", e)
        }
    }
}

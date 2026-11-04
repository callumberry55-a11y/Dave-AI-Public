package com.local.daveai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class DaveWebSearchManager {

    companion object {
        private const val TAG = "DaveWebSearchManager"
    }

    suspend fun search(query: String): String = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext ""
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val urlString = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseString)
                val abstractText = json.optString("AbstractText", "")
                val heading = json.optString("Heading", "")

                val resultsBuilder = StringBuilder()
                if (abstractText.isNotBlank()) {
                    resultsBuilder.append("Heading: $heading\nSummary: $abstractText\n")
                }

                val relatedTopics = json.optJSONArray("RelatedTopics")
                if (relatedTopics != null) {
                    for (i in 0 until minOf(relatedTopics.length(), 3)) {
                        val topic = relatedTopics.optJSONObject(i)
                        val text = topic?.optString("Text", "")
                        if (!text.isNullOrBlank()) {
                            resultsBuilder.append("- $text\n")
                        }
                    }
                }

                val result = resultsBuilder.toString().trim()
                if (result.isNotBlank()) {
                    "Web Search Results for \"$query\":\n$result"
                } else {
                    "No direct web search summary found for \"$query\"."
                }
            } else {
                "Web search request failed with response code ${connection.responseCode}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing web search", e)
            "Error performing web search: ${e.localizedMessage}"
        }
    }
}

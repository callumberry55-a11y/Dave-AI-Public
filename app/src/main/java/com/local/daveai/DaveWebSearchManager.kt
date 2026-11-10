package com.local.daveai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

data class WebSearchResult(
    val formattedText: String,
    val sources: List<WebSource>
)

class DaveWebSearchManager {

    companion object {
        private const val TAG = "DaveWebSearchManager"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    suspend fun searchWithSources(query: String): WebSearchResult = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext WebSearchResult("", emptyList())

        // Check C++ native cache first
        val cachedText = DaveWebContextNative.getCachedResult(query)
        if (!cachedText.isNullOrBlank()) {
            try {
                Log.d(TAG, "Native C++ Web Context cache hit for query: $query")
            } catch (_: Throwable) {}
            val parsedSources = parseSourcesFromFormattedText(cachedText)
            return@withContext WebSearchResult(cachedText, parsedSources)
        }

        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")

            // 1. Try DuckDuckGo Instant Answer API with structured [Retrieved Web Data] schema
            val apiResult = searchApi(encodedQuery)
            if (apiResult.second.isNotEmpty()) {
                val nativeRanked = DaveWebContextNative.processAndRank(query, apiResult.second)
                val finalFormatted = nativeRanked ?: "[Retrieved Web Data]\n${apiResult.first}"
                val finalSources = parseSourcesFromFormattedText(finalFormatted).ifEmpty { apiResult.second }
                return@withContext WebSearchResult(finalFormatted, finalSources)
            }

            // 2. Fallback to HTML Search Snippet Extraction
            val htmlResult = searchHtmlFallback(encodedQuery)
            if (htmlResult.second.isNotEmpty()) {
                val nativeRanked = DaveWebContextNative.processAndRank(query, htmlResult.second)
                val finalFormatted = nativeRanked ?: "[Retrieved Web Data]\n${apiResult.first}"
                val finalSources = parseSourcesFromFormattedText(finalFormatted).ifEmpty { htmlResult.second }
                return@withContext WebSearchResult(finalFormatted, finalSources)
            }

            WebSearchResult("No direct web search summary found for \"$query\".", emptyList())
        } catch (e: Exception) {
            Log.e(TAG, "Error performing web search", e)
            WebSearchResult("Error performing web search: ${e.localizedMessage}", emptyList())
        }
    }

    fun parseSourcesFromFormattedText(text: String): List<WebSource> {
        if (text.isBlank() || !text.contains("URL:")) return emptyList()
        val sources = mutableListOf<WebSource>()
        val urlRegex = Regex("URL:\\s*(https?://\\S+)")
        val matches = urlRegex.findAll(text)
        for (match in matches) {
            val url = match.groupValues[1].trim()
            if (url.isNotBlank() && sources.none { it.url == url }) {
                val title = extractDomainTitle(url)
                sources.add(WebSource(title = title, url = url))
            }
        }
        return sources
    }

    suspend fun search(query: String): String {
        return searchWithSources(query).formattedText
    }

    private fun searchApi(encodedQuery: String): Pair<String, List<WebSource>> {
        return try {
            val urlString = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"
            val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseString)
                val abstractText = json.optString("AbstractText", "")
                val abstractUrl = json.optString("AbstractURL", "")

                val resultsBuilder = StringBuilder()
                val sourcesList = mutableListOf<WebSource>()
                var sourceIndex = 1

                if (abstractText.isNotBlank()) {
                    val link = if (abstractUrl.isNotBlank()) abstractUrl else "https://duckduckgo.com/?q=$encodedQuery"
                    val title = extractDomainTitle(link)
                    resultsBuilder.append("Source $sourceIndex:\nURL: $link\nSnippet: $abstractText\n\n")
                    sourcesList.add(WebSource(title, link))
                    sourceIndex++
                }

                val relatedTopics = json.optJSONArray("RelatedTopics")
                if (relatedTopics != null) {
                    for (i in 0 until minOf(relatedTopics.length(), 3)) {
                        val topic = relatedTopics.optJSONObject(i)
                        val text = topic?.optString("Text", "")
                        val firstUrl = topic?.optString("FirstURL", "")
                        if (!text.isNullOrBlank()) {
                            val link = if (!firstUrl.isNullOrBlank()) firstUrl else "https://duckduckgo.com/?q=$encodedQuery"
                            val title = extractDomainTitle(link)
                            resultsBuilder.append("Source $sourceIndex:\nURL: $link\nSnippet: $text\n\n")
                            if (sourcesList.none { it.url == link }) {
                                sourcesList.add(WebSource(title, link))
                            }
                            sourceIndex++
                        }
                    }
                }

                Pair(resultsBuilder.toString().trim(), sourcesList)
            } else {
                Pair("", emptyList())
            }
        } catch (e: Exception) {
            Log.d(TAG, "API search failed: ${e.message}")
            Pair("", emptyList())
        }
    }

    private fun searchHtmlFallback(encodedQuery: String): Pair<String, List<WebSource>> {
        return try {
            val urlString = "https://html.duckduckgo.com/html/?q=$encodedQuery"
            val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val html = connection.inputStream.bufferedReader().use { it.readText() }
                val resultsBuilder = StringBuilder()
                val sourcesList = mutableListOf<WebSource>()

                val resultRegex = Regex("<a\\s+class=\"result__url\"\\s+href=\"([^\"]+)\"[^>]*>(.*?)</a>.*?<a\\s+class=\"result__snippet\"[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
                var sourceIndex = 1
                val matches = resultRegex.findAll(html)
                for (match in matches) {
                    if (sourceIndex > 3) break
                    val rawUrl = match.groupValues[1].trim()
                    val url = cleanDuckUrl(rawUrl)
                    val rawSnippet = match.groupValues[3]
                    val cleanSnippet = rawSnippet.replace(Regex("<[^>]*>"), "").replace("&quot;", "\"").replace("&amp;", "&").trim()

                    if (url.isNotBlank() && cleanSnippet.isNotBlank()) {
                        val title = extractDomainTitle(url)
                        resultsBuilder.append("Source $sourceIndex:\nURL: $url\nSnippet: $cleanSnippet\n\n")
                        if (sourcesList.none { it.url == url }) {
                            sourcesList.add(WebSource(title, url))
                        }
                        sourceIndex++
                    }
                }

                if (resultsBuilder.isEmpty()) {
                    val snippetRegex = Regex("<a\\s+class=\"result__snippet\"[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
                    val snippetMatches = snippetRegex.findAll(html)
                    for (match in snippetMatches.take(3)) {
                        val cleanSnippet = match.groupValues[1].replace(Regex("<[^>]*>"), "").replace("&quot;", "\"").replace("&amp;", "&").trim()
                        if (cleanSnippet.isNotBlank()) {
                            val fallbackUrl = "https://duckduckgo.com/?q=$encodedQuery"
                            resultsBuilder.append("Source $sourceIndex:\nURL: $fallbackUrl\nSnippet: $cleanSnippet\n\n")
                            if (sourcesList.none { it.url == fallbackUrl }) {
                                sourcesList.add(WebSource("duckduckgo.com", fallbackUrl))
                            }
                            sourceIndex++
                        }
                    }
                }

                Pair(resultsBuilder.toString().trim(), sourcesList)
            } else {
                Pair("", emptyList())
            }
        } catch (e: Exception) {
            Log.d(TAG, "HTML fallback search failed: ${e.message}")
            Pair("", emptyList())
        }
    }

    private fun cleanDuckUrl(rawUrl: String): String {
        return try {
            if (rawUrl.contains("uddg=")) {
                val extracted = rawUrl.substringAfter("uddg=").substringBefore("&")
                URLDecoder.decode(extracted, "UTF-8")
            } else if (rawUrl.startsWith("//")) {
                "https:$rawUrl"
            } else if (rawUrl.startsWith("http")) {
                rawUrl
            } else {
                "https://" + rawUrl.trimStart('/')
            }
        } catch (_: Exception) {
            rawUrl
        }
    }

    private fun extractDomainTitle(url: String): String {
        return try {
            val uri = URI(url)
            val host = uri.host ?: url
            host.removePrefix("www.").removePrefix("m.")
        } catch (_: Exception) {
            url.take(25)
        }
    }
}

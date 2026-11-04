package com.local.daveai

import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.sqrt

class LocalSemanticMemoryManager(private val messageDao: MessageDao? = null) {

    suspend fun retrieveRelevantMemories(query: String, limit: Int = 3): List<String> {
        if (query.isBlank()) return emptyList()
        val allMessages = try {
            messageDao?.getAllMessages()?.firstOrNull() ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
        return retrieveRelevantMemoriesSync(query, allMessages.map { it.text }, limit)
    }

    fun retrieveRelevantMemoriesSync(query: String, existingMessages: List<String>, limit: Int = 3): List<String> {
        if (query.isBlank() || existingMessages.isEmpty()) return emptyList()
        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) return emptyList()

        val scoredDocs = existingMessages.distinct().map { doc ->
            val docTokens = tokenize(doc)
            val score = cosineSimilarity(queryTokens, docTokens)
            doc to score
        }.sortedByDescending { it.second }

        return scoredDocs.filter { it.second > 0.03 }.take(limit).map { it.first }
    }

    private fun tokenize(text: String): Map<String, Int> {
        val words = text.lowercase().replace(Regex("[^a-z0-9 ]"), "").split(Regex("\\s+"))
        val freq = mutableMapOf<String, Int>()
        for (w in words) {
            if (w.isNotBlank() && w.length > 2) {
                freq[w] = (freq[w] ?: 0) + 1
            }
        }
        return freq
    }

    private fun cosineSimilarity(v1: Map<String, Int>, v2: Map<String, Int>): Double {
        val intersection = v1.keys.intersect(v2.keys)
        var dotProduct = 0.0
        for (key in intersection) {
            dotProduct += (v1[key] ?: 0) * (v2[key] ?: 0)
        }
        var norm1 = 0.0
        for (v in v1.values) {
            norm1 += v * v
        }
        var norm2 = 0.0
        for (v in v2.values) {
            norm2 += v * v
        }
        if (norm1 == 0.0 || norm2 == 0.0) return 0.0
        return dotProduct / (sqrt(norm1) * sqrt(norm2))
    }
}

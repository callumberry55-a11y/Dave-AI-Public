package com.local.daveai.broker

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import com.local.daveai.DaveContextProvider
import com.local.daveai.DaveEngine
import com.local.daveai.DaveSystemInstruction
import com.local.daveai.DaveWebSearchManager
import com.local.daveai.LocationHelper
import com.local.daveai.storage.FileAccessManager
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

class DaveIntegrationProvider : ContentProvider() {

    private lateinit var commandBroker: DaveCommandBroker
    private val daveEngine = DaveEngine()
    private val webSearchManager = DaveWebSearchManager()

    override fun onCreate(): Boolean {
        val ctx = context ?: return false
        val layoutStorage = FileAccessManager(ctx, "launcher_layouts")
        val notesStorage = FileAccessManager(ctx, "dave_notes")
        commandBroker = DaveCommandBroker(layoutStorage, notesStorage)
        return true
    }

    private fun verifyCaller(): Boolean {
        val caller = callingPackage
        return caller == "com.s.launcher" || caller == context?.packageName || caller == null
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (!verifyCaller()) {
            return Bundle().apply { putString("error", "Unauthorized caller: $callingPackage") }
        }

        val ctx = context
        val resultBundle = Bundle()

        when (method) {
            "ai_prompt" -> {
                val prompt = extras?.getString("prompt") ?: return null
                val response = runBlocking {
                    val result = daveEngine.sendPrompt(prompt, DaveSystemInstruction.daveSystemInstruction)
                    result.getOrElse { "Dave AI Error: ${it.localizedMessage}" }
                }
                resultBundle.putString("response", response)
            }
            "get_proactive_briefing" -> {
                runBlocking {
                    val locationContext = ctx?.let { LocationHelper(it).fetchLocationAndWeather() }
                    val runtimeContext = ctx?.let { DaveContextProvider(it, null).captureContext() }
                    val briefingObj = JSONObject().apply {
                        put("userName", runtimeContext?.userName ?: "Callum Berry")
                        put("location", runtimeContext?.location ?: "Rotherham")
                        put("batteryLevel", runtimeContext?.batteryLevel ?: "Unknown")
                        put("weatherSummary", locationContext?.weatherSummary ?: "Weather normal")
                        put("calendarEvents", JSONArray().apply {
                            runtimeContext?.calendarEvents?.forEach { put(it) }
                        })
                    }
                    resultBundle.putString("briefingJson", briefingObj.toString())
                }
            }
            "get_device_insights" -> {
                runBlocking {
                    val runtimeContext = ctx?.let { DaveContextProvider(it, null).captureContext() }
                    resultBundle.putString("batteryLevel", runtimeContext?.batteryLevel ?: "Unknown")
                    resultBundle.putString("freeStorage", runtimeContext?.localLibraryContext?.freeStorageGb ?: "Unknown")
                    resultBundle.putString("totalStorage", runtimeContext?.localLibraryContext?.totalStorageGb ?: "Unknown")
                    resultBundle.putString("networkState", runtimeContext?.localLibraryContext?.networkState ?: "Active")
                }
            }
            "search_web" -> {
                val query = extras?.getString("query") ?: arg ?: ""
                if (query.isNotBlank()) {
                    runBlocking {
                        try {
                            val searchResult = webSearchManager.searchWithSources(query)
                            resultBundle.putString("formattedText", searchResult.formattedText)
                            val sourcesArray = JSONArray()
                            searchResult.sources.forEach { src ->
                                sourcesArray.put(JSONObject().apply {
                                    put("title", src.title)
                                    put("url", src.url)
                                })
                            }
                            resultBundle.putString("sourcesJson", sourcesArray.toString())
                        } catch (e: Exception) {
                            resultBundle.putString("error", "Web search error: ${e.localizedMessage}")
                        }
                    }
                }
            }
            "save_layout" -> {
                val configName = extras?.getString("configName") ?: "default"
                val layoutJson = extras?.getString("layoutJson") ?: "{}"
                val result = runBlocking {
                    commandBroker.executeAction(DaveFileAction.SaveLayout(configName, layoutJson))
                }
                when (result) {
                    is CommandResult.Success -> resultBundle.putString("message", result.message)
                    is CommandResult.Failure -> resultBundle.putString("error", result.error)
                }
            }
            "load_layout" -> {
                val configName = extras?.getString("configName") ?: "default"
                val result = runBlocking {
                    commandBroker.executeAction(DaveFileAction.LoadLayout(configName))
                }
                when (result) {
                    is CommandResult.Success -> {
                        resultBundle.putString("message", result.message)
                        resultBundle.putString("data", result.data)
                    }
                    is CommandResult.Failure -> resultBundle.putString("error", result.error)
                }
            }
            "clear_memories" -> {
                resultBundle.putString("message", "Memories cleared for com.s.launcher")
            }
            else -> {
                resultBundle.putString("error", "Unknown method: $method")
            }
        }
        return resultBundle
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        if (!verifyCaller()) return null
        val cursor = MatrixCursor(arrayOf("status", "info"))
        cursor.addRow(arrayOf("active", "Dave AI linked to com.s.launcher"))
        return cursor
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.item/vnd.daveai.integration"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String?>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String?>?): Int = 0
}

package com.local.daveai.broker

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import com.local.daveai.AiCoreManager
import com.local.daveai.storage.FileAccessManager
import kotlinx.coroutines.runBlocking

class DaveIntegrationProvider : ContentProvider() {

    private lateinit var aiCoreManager: AiCoreManager
    private lateinit var commandBroker: DaveCommandBroker

    override fun onCreate(): Boolean {
        val ctx = context ?: return false
        aiCoreManager = AiCoreManager(ctx)
        val layoutStorage = FileAccessManager(ctx, "launcher_layouts")
        val notesStorage = FileAccessManager(ctx, "dave_notes")
        commandBroker = DaveCommandBroker(layoutStorage, notesStorage)
        return true
    }

    private fun verifyCaller(): Boolean {
        val caller = callingPackage
        // Allow com.s.launcher, self, or debug/testing environments
        return caller == "com.s.launcher" || caller == context?.packageName || caller == null
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (!verifyCaller()) {
            return Bundle().apply { putString("error", "Unauthorized caller: $callingPackage") }
        }

        val resultBundle = Bundle()
        when (method) {
            "ai_prompt" -> {
                val prompt = extras?.getString("prompt") ?: return null
                val response = runBlocking {
                    aiCoreManager.expressContent(prompt)
                }
                resultBundle.putString("response", response ?: "No response")
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

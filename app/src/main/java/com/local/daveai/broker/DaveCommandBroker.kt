package com.local.daveai.broker

import com.local.daveai.storage.FileAccessManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface DaveFileAction {
    data class SaveLayout(val configName: String, val layoutJson: String) : DaveFileAction
    data class LoadLayout(val configName: String) : DaveFileAction
    data class SaveNote(val noteTitle: String, val content: String) : DaveFileAction
    data class DeleteConfig(val targetName: String) : DaveFileAction
    data object ListFiles : DaveFileAction
}

sealed interface CommandResult {
    data class Success(val message: String, val data: String? = null) : CommandResult
    data class Failure(val error: String) : CommandResult
}

class DaveCommandBroker(
    private val layoutStorage: FileAccessManager,
    private val notesStorage: FileAccessManager
) {

    suspend fun executeAction(action: DaveFileAction): CommandResult = withContext(Dispatchers.IO) {
        try {
            when (action) {
                is DaveFileAction.SaveLayout -> {
                    val fileName = sanitizeFileName(action.configName) + ".json"
                    val success = layoutStorage.writeAtomic(fileName, action.layoutJson)
                    if (success) {
                        CommandResult.Success("Layout saved successfully: $fileName")
                    } else {
                        CommandResult.Failure("Disk write failure while saving $fileName")
                    }
                }

                is DaveFileAction.LoadLayout -> {
                    val fileName = sanitizeFileName(action.configName) + ".json"
                    val content = layoutStorage.readAtomic(fileName)
                    if (content != null) {
                        CommandResult.Success("Layout loaded", content)
                    } else {
                        CommandResult.Failure("Configuration not found: $fileName")
                    }
                }

                is DaveFileAction.SaveNote -> {
                    val fileName = sanitizeFileName(action.noteTitle) + ".txt"
                    val success = notesStorage.writeAtomic(fileName, action.content)
                    if (success) {
                        CommandResult.Success("Note persisted: $fileName")
                    } else {
                        CommandResult.Failure("Failed saving note: $fileName")
                    }
                }

                is DaveFileAction.DeleteConfig -> {
                    val fileName = sanitizeFileName(action.targetName) + ".json"
                    val deleted = layoutStorage.deleteFile(fileName)
                    if (deleted) {
                        CommandResult.Success("Deleted configuration: $fileName")
                    } else {
                        CommandResult.Failure("Failed to remove file: $fileName")
                    }
                }

                is DaveFileAction.ListFiles -> {
                    val files = layoutStorage.listManagedFiles()
                    CommandResult.Success("Files retrieved", files.joinToString(", "))
                }
            }
        } catch (e: Exception) {
            CommandResult.Failure("Broker execution error: ${e.localizedMessage}")
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }
}

package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatFolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

sealed class FolderSaveStatus {
    object Idle : FolderSaveStatus()
    object Saving : FolderSaveStatus()
    data class Success(val message: String = "Folder saved") : FolderSaveStatus()
    data class Failure(val error: String) : FolderSaveStatus()
}

@HiltViewModel
class ChatFoldersViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _chatFolders = MutableStateFlow<List<ChatFolder>>(emptyList())
    val chatFolders: StateFlow<List<ChatFolder>> = _chatFolders.asStateFlow()

    private val _saveStatus = MutableStateFlow<FolderSaveStatus>(FolderSaveStatus.Idle)
    val saveStatus: StateFlow<FolderSaveStatus> = _saveStatus.asStateFlow()

    init {
        loadFolders()
    }

    private fun loadFolders() {
        viewModelScope.launch {
            val json = settingsRepository.chatFoldersJson.firstOrNull()
            if (!json.isNullOrBlank()) {
                try {
                    val parsedFolders = Json.decodeFromString<List<ChatFolder>>(json)
                    _chatFolders.value = parsedFolders
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun addFolder(name: String) {
        val newFolder = ChatFolder(
            id = UUID.randomUUID().toString(),
            name = name
        )
        val updatedList = _chatFolders.value + newFolder
        _chatFolders.value = updatedList
        _saveStatus.value = FolderSaveStatus.Saving
        viewModelScope.launch {
            try {
                saveFolders(updatedList)
                _saveStatus.value = FolderSaveStatus.Success()
            } catch (e: Exception) {
                // Revert the optimistic update so the UI reflects persisted state
                _chatFolders.value = _chatFolders.value.filter { it.id != newFolder.id }
                _saveStatus.value = FolderSaveStatus.Failure(e.message ?: "Failed to save folder")
            }
        }
    }

    fun renameFolder(folderId: String, newName: String) {
        val originalFolder = _chatFolders.value.firstOrNull { it.id == folderId } ?: return
        val originalName = originalFolder.name
        val updatedList = _chatFolders.value.map { if (it.id == folderId) it.copy(name = newName) else it }
        _chatFolders.value = updatedList
        _saveStatus.value = FolderSaveStatus.Saving
        viewModelScope.launch {
            try {
                saveFolders(updatedList)
                _saveStatus.value = FolderSaveStatus.Success()
            } catch (e: Exception) {
                // Revert the optimistic update on failure
                _chatFolders.value = _chatFolders.value.map { if (it.id == folderId) it.copy(name = originalName) else it }
                _saveStatus.value = FolderSaveStatus.Failure(e.message ?: "Failed to rename folder")
            }
        }
    }

    fun reorderFolders(from: Int, to: Int) {
        val list = _chatFolders.value.toMutableList()
        list.add(to, list.removeAt(from))
        _chatFolders.value = list
        _saveStatus.value = FolderSaveStatus.Saving
        viewModelScope.launch {
            try {
                saveFolders(list)
                _saveStatus.value = FolderSaveStatus.Success()
            } catch (e: Exception) {
                _saveStatus.value = FolderSaveStatus.Failure(e.message ?: "Failed to save folder order")
            }
        }
    }

    fun removeFolder(folderId: String) {
        val updatedList = _chatFolders.value.filter { it.id != folderId }
        _chatFolders.value = updatedList
        _saveStatus.value = FolderSaveStatus.Saving
        viewModelScope.launch {
            try {
                saveFolders(updatedList)
                _saveStatus.value = FolderSaveStatus.Success()
            } catch (e: Exception) {
                _saveStatus.value = FolderSaveStatus.Failure(e.message ?: "Failed to remove folder")
            }
        }
    }

    fun clearSaveStatus() {
        _saveStatus.value = FolderSaveStatus.Idle
    }

    private suspend fun saveFolders(folders: List<ChatFolder>) {
        val json = Json.encodeToString(folders)
        settingsRepository.setChatFoldersJson(json)
    }
}
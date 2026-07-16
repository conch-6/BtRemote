package com.atharok.btremote.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atharok.btremote.data.dlna.DlnaController
import com.atharok.btremote.domain.entity.dlna.DlnaDevice
import com.atharok.btremote.domain.entities.settings.CastDevice
import com.atharok.btremote.domain.entities.settings.CastLink
import com.atharok.btremote.domain.entities.settings.CastSettings
import com.atharok.btremote.domain.repositories.DataStoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class CastViewModel(
    private val dataStoreRepository: DataStoreRepository,
    private val dlnaController: DlnaController = DlnaController()
) : ViewModel() {

    private val _castSettings = MutableStateFlow(CastSettings())
    val castSettings: StateFlow<CastSettings> = _castSettings.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DlnaDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DlnaDevice>> = _discoveredDevices.asStateFlow()

    private val _castResult = MutableStateFlow<CastResult?>(null)
    val castResult: StateFlow<CastResult?> = _castResult.asStateFlow()

    init {
        viewModelScope.launch {
            dataStoreRepository.getCastSettings().collect {
                _castSettings.value = it
            }
        }
    }

    fun addLink(url: String) {
        viewModelScope.launch {
            val current = _castSettings.value
            val newLink = CastLink(id = UUID.randomUUID().toString(), url = url.trim())
            val updated = current.copy(
                links = current.links + newLink,
                selectedLinkId = current.selectedLinkId ?: newLink.id
            )
            dataStoreRepository.saveCastSettings(updated)
        }
    }

    fun deleteLink(linkId: String) {
        viewModelScope.launch {
            val current = _castSettings.value
            val updatedLinks = current.links.filter { it.id != linkId }
            val updated = current.copy(
                links = updatedLinks,
                selectedLinkId = when {
                    updatedLinks.isEmpty() -> null
                    current.selectedLinkId == linkId -> updatedLinks.first().id
                    else -> current.selectedLinkId
                }
            )
            dataStoreRepository.saveCastSettings(updated)
        }
    }

    fun selectLink(linkId: String) {
        viewModelScope.launch {
            val current = _castSettings.value
            if (current.selectedLinkId != linkId) {
                dataStoreRepository.saveCastSettings(current.copy(selectedLinkId = linkId))
            }
        }
    }

    fun clearCastResult() {
        _castResult.value = null
    }

    fun clearDiscoveredDevices() {
        _discoveredDevices.value = emptyList()
    }

    fun cancelSearchOrCast() {
        _isSearching.value = false
        _discoveredDevices.value = emptyList()
    }

    fun startCast() {
        viewModelScope.launch {
            val settings = _castSettings.value
            val selectedLink = settings.links.find { it.id == settings.selectedLinkId }
                ?: return@launch

            val savedDevice = settings.savedDevice
            if (savedDevice != null) {
                castToDevice(
                    DlnaDevice(name = savedDevice.name, controlUrl = savedDevice.controlUrl),
                    selectedLink.url
                )
            } else {
                discoverAndSelectDevice(selectedLink.url)
            }
        }
    }

    fun selectDeviceAndCast(device: DlnaDevice) {
        viewModelScope.launch {
            val settings = _castSettings.value
            val selectedLink = settings.links.find { it.id == settings.selectedLinkId }
                ?: return@launch

            _discoveredDevices.value = emptyList()
            dataStoreRepository.saveCastSettings(
                settings.copy(savedDevice = CastDevice(device.name, device.controlUrl))
            )
            castToDevice(device, selectedLink.url)
        }
    }

    private suspend fun discoverAndSelectDevice(streamUri: String) {
        _isSearching.value = true
        _discoveredDevices.value = emptyList()
        try {
            val devices = dlnaController.discoverMediaRenderers()
            if (_isSearching.value) {
                _discoveredDevices.value = devices
                if (devices.isEmpty()) {
                    _castResult.value = CastResult.Error("未找到 DLNA/UPnP 设备")
                }
            }
        } catch (e: Exception) {
            if (_isSearching.value) {
                _castResult.value = CastResult.Error("搜索设备失败：${e.localizedMessage}")
            }
        } finally {
            _isSearching.value = false
        }
    }

    private suspend fun castToDevice(device: DlnaDevice, streamUri: String) {
        _isSearching.value = true
        try {
            val result = dlnaController.castToDevice(device, streamUri)
            if (_isSearching.value) {
                _castResult.value = if (result.isSuccess) {
                    CastResult.Success
                } else {
                    CastResult.Error(result.exceptionOrNull()?.localizedMessage ?: "投屏失败")
                }
            }
        } catch (e: Exception) {
            if (_isSearching.value) {
                _castResult.value = CastResult.Error(e.localizedMessage ?: "投屏失败")
            }
        } finally {
            _isSearching.value = false
        }
    }

    fun forgetSavedDevice() {
        viewModelScope.launch {
            val current = _castSettings.value
            dataStoreRepository.saveCastSettings(current.copy(savedDevice = null))
        }
    }

    sealed class CastResult {
        data object Success : CastResult()
        data class Error(val message: String) : CastResult()
    }
}

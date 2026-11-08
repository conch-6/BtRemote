package com.btremote.teny.domain.usecases

import com.btremote.teny.domain.entities.remoteInput.keyboard.virtualKeyboard.VirtualKeyboardLayout
import com.btremote.teny.domain.entities.settings.RemoteSettings
import com.btremote.teny.domain.repositories.BluetoothRepository
import com.btremote.teny.domain.repositories.DataStoreRepository
import kotlinx.coroutines.flow.Flow

class RemoteUseCase(
    private val bluetoothRepository: BluetoothRepository,
    private val dataStoreRepository: DataStoreRepository
) {

    // ---- Settings ----

    val remoteSettingsFlow: Flow<RemoteSettings> get() = dataStoreRepository.remoteSettingsFlow

    suspend fun saveUseReadingMode(useReadingMode: Boolean) {
        dataStoreRepository.saveUseReadingMode(useReadingMode)
    }

    // ---- Connection ----

    fun disconnectDevice(): Boolean {
        return bluetoothRepository.disconnectDevice()
    }

    // ---- Send ----

    fun sendReport(id: Int, bytes: ByteArray): Boolean {
        return bluetoothRepository.sendReport(id, bytes)
    }

    suspend fun sendTextReport(
        text: String,
        virtualKeyboardLayout: VirtualKeyboardLayout,
        shouldSendEnter: Boolean
    ): Boolean {
        return bluetoothRepository.sendTextReport(text, virtualKeyboardLayout, shouldSendEnter)
    }
}
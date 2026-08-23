package com.btremote.teny.domain.usecases

import com.btremote.teny.domain.entities.DeviceHidConnectionState
import com.btremote.teny.domain.entities.settings.AppearanceSettings
import com.btremote.teny.domain.repositories.BluetoothRepository
import com.btremote.teny.domain.repositories.DataStoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class AppScopeUseCase(
    private val bluetoothRepository: BluetoothRepository,
    private val dataStoreRepository: DataStoreRepository
) {

    // ---- App Appearance ----

    val appearanceSettingsFlow: Flow<AppearanceSettings> get() = dataStoreRepository.appearanceSettingsFlow

    // ---- Local Device Name ----

    fun getLocalDeviceName(): String = bluetoothRepository.getLocalDeviceName()

    // ---- Connection States ----

    fun isBluetoothSupported(): Boolean {
        return bluetoothRepository.isBluetoothSupported()
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothRepository.isBluetoothEnabled()
    }

    fun isBluetoothServiceRunning(): StateFlow<Boolean> {
        return bluetoothRepository.isBluetoothServiceRunning()
    }

    fun isBluetoothHidProfileRegistered(): StateFlow<Boolean> {
        return bluetoothRepository.isBluetoothHidProfileRegistered()
    }

    fun getDeviceHidConnectionState(): StateFlow<DeviceHidConnectionState> {
        return bluetoothRepository.getDeviceHidConnectionState()
    }
}
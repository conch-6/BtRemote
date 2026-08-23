package com.btremote.teny.domain.usecases

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.btremote.teny.domain.entities.DeviceEntity
import com.btremote.teny.domain.repositories.BluetoothRepository

class DeviceDiscoveryUseCase(
    private val bluetoothRepository: BluetoothRepository,
) {

    // ---- Connection ----

    fun connectDevice(deviceAddress: String): Boolean {
        return bluetoothRepository.connectDevice(deviceAddress)
    }

    fun disconnectDevice(): Boolean {
        return bluetoothRepository.disconnectDevice()
    }

    // ---- Discover Devices ----

    fun getScannedDevices(): SnapshotStateList<DeviceEntity> = bluetoothRepository.getScannedDevices()

    fun registerBluetoothScannerReceiver() {
        bluetoothRepository.registerBluetoothScannerReceiver()
    }

    fun unregisterBluetoothScannerReceiver() {
        bluetoothRepository.unregisterBluetoothScannerReceiver()
    }

    fun startDiscovery(): Boolean = bluetoothRepository.startDiscovery()

    fun cancelDiscovery(): Boolean = bluetoothRepository.cancelDiscovery()
}
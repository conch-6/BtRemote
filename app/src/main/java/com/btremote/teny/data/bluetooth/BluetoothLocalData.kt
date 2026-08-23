package com.btremote.teny.data.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.Context
import com.btremote.teny.R
import com.btremote.teny.common.extensions.unpair
import com.btremote.teny.common.utils.checkBluetoothConnectPermission
import com.btremote.teny.domain.entities.DeviceEntity
import com.btremote.teny.domain.resources.Result

class BluetoothLocalData(
    private val context: Context,
    private val adapter: BluetoothAdapter?
) {

    // ---- About local device ----

    fun getLocalDeviceName(): String = if(checkBluetoothConnectPermission(context)) {
        adapter?.name ?: context.getString(R.string.unknown)
    } else context.getString(R.string.unknown)

    // ---- Get Bonded Devices ----

    fun getBondedDevices(): List<DeviceEntity> {
        val deviceEntities = mutableListOf<DeviceEntity>()
        if (checkBluetoothConnectPermission(context)) {
            adapter?.bondedDevices?.forEach { device ->
                deviceEntities.add(
                    DeviceEntity(
                        name = device.name ?: "null",
                        macAddress = device.address ?: "null",
                        category = device.bluetoothClass.majorDeviceClass
                    )
                )
            }
        }
        return deviceEntities
    }

    // ---- Unpair ----

    fun unpairDevice(address: String?): Result<Boolean> {
        return if (address == null) {
            Result(
                value = false,
                message = context.getString(R.string.unpair_device_failure)
            )
        } else {
            val isUnpaired = adapter?.getRemoteDevice(address)?.unpair() == true

            val messageRes =
                if (isUnpaired) R.string.unpair_device_successful else R.string.unpair_device_failure

            Result(
                value = isUnpaired,
                message = context.getString(messageRes)
            )
        }
    }
}
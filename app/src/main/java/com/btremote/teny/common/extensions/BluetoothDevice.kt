package com.btremote.teny.common.extensions

import android.bluetooth.BluetoothDevice

fun BluetoothDevice.unpair(): Boolean = try {
    javaClass.getMethod("removeBond").invoke(this)
    true
} catch (e: Exception) { false }
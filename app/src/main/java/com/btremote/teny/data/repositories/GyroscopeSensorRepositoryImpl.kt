package com.btremote.teny.data.repositories

import com.btremote.teny.data.sensor.GyroscopeSensor
import com.btremote.teny.domain.repositories.GyroscopeSensorRepository
import kotlinx.coroutines.flow.StateFlow

class GyroscopeSensorRepositoryImpl(private val sensor: GyroscopeSensor): GyroscopeSensorRepository {

    override fun startListening() {
        sensor.startListening()
    }

    override fun stopListening() {
        sensor.stopListening()
    }

    override fun getDisplayRotation(): Int = sensor.getDisplayRotation()

    override fun getPositions(): StateFlow<Triple<Float, Float, Float>> = sensor.gyroscopePositionsState
}
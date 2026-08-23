package com.btremote.teny.domain.usecases

import com.btremote.teny.domain.repositories.GyroscopeSensorRepository
import kotlinx.coroutines.flow.StateFlow

class GyroscopeSensorUseCase(private val repository: GyroscopeSensorRepository) {

    fun startListening() {
        repository.startListening()
    }

    fun stopListening() {
        repository.stopListening()
    }

    fun getDisplayRotation(): Int = repository.getDisplayRotation()

    fun getPositions(): StateFlow<Triple<Float, Float, Float>> = repository.getPositions()
}
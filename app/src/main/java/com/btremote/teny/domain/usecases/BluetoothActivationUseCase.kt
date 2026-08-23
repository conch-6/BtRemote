package com.btremote.teny.domain.usecases

import com.btremote.teny.domain.repositories.DataStoreRepository
import kotlinx.coroutines.flow.Flow

class BluetoothActivationUseCase(
    private val dataStoreRepository: DataStoreRepository
) {
    fun hideBluetoothActivationButton(): Flow<Boolean> = dataStoreRepository.hideBluetoothActivationButton()
}
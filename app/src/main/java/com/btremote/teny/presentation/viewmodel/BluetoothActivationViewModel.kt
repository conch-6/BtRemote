package com.btremote.teny.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.btremote.teny.domain.usecases.BluetoothActivationUseCase
import kotlinx.coroutines.flow.Flow

class BluetoothActivationViewModel(
    private val useCase: BluetoothActivationUseCase
): ViewModel() {
    val hideBluetoothActivationButton: Flow<Boolean> get() = useCase.hideBluetoothActivationButton()
}
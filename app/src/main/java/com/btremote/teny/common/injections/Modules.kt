package com.btremote.teny.common.injections

import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import com.btremote.teny.R
import com.btremote.teny.common.utils.bluetoothHidDescriptor
import com.btremote.teny.data.bluetooth.BluetoothHidCore
import com.btremote.teny.data.bluetooth.BluetoothLocalData
import com.btremote.teny.data.bluetooth.BluetoothScanner
import com.btremote.teny.data.bluetooth.BluetoothStatus
import com.btremote.teny.data.dataStore.SettingsDataStore
import com.btremote.teny.data.repositories.BluetoothRepositoryImpl
import com.btremote.teny.data.repositories.DataStoreRepositoryImpl
import com.btremote.teny.data.repositories.GyroscopeSensorRepositoryImpl
import com.btremote.teny.data.sensor.GyroscopeSensor
import com.btremote.teny.domain.entities.remoteInput.keyboard.advancedKeyboard.ArabicAdvancedKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.advancedKeyboard.BulgarianAdvancedKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.advancedKeyboard.CzechAdvancedKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.advancedKeyboard.EnglishUKAdvancedKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.advancedKeyboard.EnglishUSAdvancedKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.advancedKeyboard.FrenchAdvancedKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.advancedKeyboard.GermanAdvancedKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.advancedKeyboard.GreekAdvancedKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.advancedKeyboard.HebrewAdvancedKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.advancedKeyboard.PersianAdvancedKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.advancedKeyboard.PolishAdvancedKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.advancedKeyboard.PortugueseAdvancedKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.advancedKeyboard.PortugueseBRAdvancedKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.advancedKeyboard.RussianAdvancedKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.advancedKeyboard.SpanishAdvancedKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.advancedKeyboard.TurkishAdvancedKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.advancedKeyboard.UkrainianAdvancedKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.virtualKeyboard.ArabicVirtualKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.virtualKeyboard.BulgarianVirtualKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.virtualKeyboard.CzechVirtualKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.virtualKeyboard.EnglishUKVirtualKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.virtualKeyboard.EnglishUSVirtualKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.virtualKeyboard.FrenchVirtualKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.virtualKeyboard.GermanVirtualKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.virtualKeyboard.GreekVirtualKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.virtualKeyboard.HebrewVirtualKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.virtualKeyboard.PersianVirtualKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.virtualKeyboard.PolishVirtualKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.virtualKeyboard.PortugueseBRVirtualKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.virtualKeyboard.PortugueseVirtualKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.virtualKeyboard.RussianVirtualKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.virtualKeyboard.SpanishVirtualKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.virtualKeyboard.TurkishVirtualKeyboardLayout
import com.btremote.teny.domain.entities.remoteInput.keyboard.virtualKeyboard.UkrainianVirtualKeyboardLayout
import com.btremote.teny.domain.repositories.BluetoothRepository
import com.btremote.teny.domain.repositories.DataStoreRepository
import com.btremote.teny.domain.repositories.GyroscopeSensorRepository
import com.btremote.teny.domain.usecases.AppScopeUseCase
import com.btremote.teny.domain.usecases.BluetoothActivationUseCase
import com.btremote.teny.domain.usecases.BluetoothHidServiceUseCase
import com.btremote.teny.domain.usecases.DeviceDiscoveryUseCase
import com.btremote.teny.domain.usecases.DeviceSelectionUseCase
import com.btremote.teny.domain.usecases.GyroscopeSensorUseCase
import com.btremote.teny.domain.usecases.RemoteUseCase
import com.btremote.teny.domain.usecases.SettingsUseCase
import com.btremote.teny.data.dlna.DlnaController
import com.btremote.teny.presentation.viewmodel.AppScopeViewModel
import com.btremote.teny.presentation.viewmodel.BluetoothActivationViewModel
import com.btremote.teny.presentation.viewmodel.CastViewModel
import com.btremote.teny.presentation.viewmodel.DeviceDiscoveryViewModel
import com.btremote.teny.presentation.viewmodel.DeviceSelectionViewModel
import com.btremote.teny.presentation.viewmodel.GyroscopeSensorViewModel
import com.btremote.teny.presentation.viewmodel.RemoteViewModel
import com.btremote.teny.presentation.viewmodel.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.util.Locale

val appModules by lazy {
    listOf<Module>(androidModule, viewModelModule, useCaseModule, repositoryModule, dataModule)
}

private val androidModule: Module = module {
    single<SensorManager> {
        androidContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    single<DisplayManager> {
        androidContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    single<BluetoothManager> {
        androidContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    single<BluetoothHidDeviceAppSdpSettings> {
        BluetoothHidDeviceAppSdpSettings(
            androidContext().getString(R.string.app_name),
            "Bluetooth HID Device",
            "Atharok",
            BluetoothHidDevice.SUBCLASS2_UNCATEGORIZED,
            bluetoothHidDescriptor
        )
    }

    single { EnglishUSVirtualKeyboardLayout() }
    single { EnglishUKVirtualKeyboardLayout() }
    single { SpanishVirtualKeyboardLayout() }
    single { FrenchVirtualKeyboardLayout() }
    single { GermanVirtualKeyboardLayout() }
    single { RussianVirtualKeyboardLayout() }
    single { CzechVirtualKeyboardLayout() }
    single { PolishVirtualKeyboardLayout() }
    single { PortugueseVirtualKeyboardLayout() }
    single { PortugueseBRVirtualKeyboardLayout() }
    single { GreekVirtualKeyboardLayout() }
    single { TurkishVirtualKeyboardLayout() }
    single { HebrewVirtualKeyboardLayout() }
    single { BulgarianVirtualKeyboardLayout() }
    single { UkrainianVirtualKeyboardLayout() }
    single { PersianVirtualKeyboardLayout() }
    single { ArabicVirtualKeyboardLayout() }

    single { EnglishUSAdvancedKeyboardLayout(context = androidContext()) }
    single { EnglishUKAdvancedKeyboardLayout(context = androidContext()) }
    single { SpanishAdvancedKeyboardLayout(context = androidContext()) }
    single { FrenchAdvancedKeyboardLayout(context = androidContext()) }
    single { GermanAdvancedKeyboardLayout(context = androidContext()) }
    single { RussianAdvancedKeyboardLayout(context = androidContext()) }
    single { CzechAdvancedKeyboardLayout(context = androidContext()) }
    single { PolishAdvancedKeyboardLayout(context = androidContext()) }
    single { PortugueseAdvancedKeyboardLayout(context = androidContext()) }
    single { PortugueseBRAdvancedKeyboardLayout(context = androidContext()) }
    single { GreekAdvancedKeyboardLayout(context = androidContext()) }
    single { TurkishAdvancedKeyboardLayout(context = androidContext()) }
    single { HebrewAdvancedKeyboardLayout(context = androidContext()) }
    single { BulgarianAdvancedKeyboardLayout(context = androidContext()) }
    single { UkrainianAdvancedKeyboardLayout(context = androidContext()) }
    single { PersianAdvancedKeyboardLayout(context = androidContext()) }
    single { ArabicAdvancedKeyboardLayout(context = androidContext()) }

    factory<Locale> {
        androidContext().resources.configuration.locales[0]
    }
}

private val viewModelModule: Module = module {
    viewModel {
        AppScopeViewModel(
            useCase = get<AppScopeUseCase>()
        )
    }

    viewModel {
        GyroscopeSensorViewModel(
            useCase = get<GyroscopeSensorUseCase>()
        )
    }

    viewModel {
        SettingsViewModel(
            useCase = get<SettingsUseCase>()
        )
    }

    viewModel {
        BluetoothActivationViewModel(
            useCase = get<BluetoothActivationUseCase>()
        )
    }

    viewModel {
        DeviceSelectionViewModel(
            useCase = get<DeviceSelectionUseCase>()
        )
    }

    viewModel {
        DeviceDiscoveryViewModel(
            useCase = get<DeviceDiscoveryUseCase>()
        )
    }

    viewModel {
        RemoteViewModel(
            useCase = get<RemoteUseCase>()
        )
    }

    viewModel {
        CastViewModel(
            dataStoreRepository = get<DataStoreRepository>(),
            dlnaController = get<DlnaController>()
        )
    }
}

private val useCaseModule: Module = module {
    single {
        AppScopeUseCase(
            bluetoothRepository = get<BluetoothRepository>(),
            dataStoreRepository = get<DataStoreRepository>()
        )
    }

    single {
        GyroscopeSensorUseCase(
            repository = get<GyroscopeSensorRepository>()
        )
    }

    single {
        BluetoothHidServiceUseCase(
            bluetoothRepository = get<BluetoothRepository>(),
            dataStoreRepository = get<DataStoreRepository>()
        )
    }

    single {
        SettingsUseCase(
            dataStoreRepository = get<DataStoreRepository>()
        )
    }

    single {
        BluetoothActivationUseCase(
            dataStoreRepository = get<DataStoreRepository>()
        )
    }

    single {
        DeviceSelectionUseCase(
            bluetoothRepository = get<BluetoothRepository>(),
            dataStoreRepository = get<DataStoreRepository>()
        )
    }

    single {
        DeviceDiscoveryUseCase(
            bluetoothRepository = get<BluetoothRepository>()
        )
    }

    single {
        RemoteUseCase(
            bluetoothRepository = get<BluetoothRepository>(),
            dataStoreRepository = get<DataStoreRepository>()
        )
    }
}

private val repositoryModule: Module = module {
    single<GyroscopeSensorRepository> {
        GyroscopeSensorRepositoryImpl(
            sensor = get<GyroscopeSensor>()
        )
    }

    single<BluetoothRepository> {
        BluetoothRepositoryImpl(
            bluetoothStatus = get<BluetoothStatus>(),
            bluetoothScanner = get<BluetoothScanner>(),
            bluetoothLocalData = get<BluetoothLocalData>(),
            bluetoothHidCore = get<BluetoothHidCore>()
        )
    }

    single<DataStoreRepository> {
        DataStoreRepositoryImpl(
            settingsDataStore = get<SettingsDataStore>()
        )
    }
}

private val dataModule: Module = module {
    single {
        GyroscopeSensor(
            sensorManager = get<SensorManager>(),
            displayManager = get<DisplayManager>()
        )
    }

    single {
        BluetoothStatus(
            adapter = get<BluetoothManager>().adapter
        )
    }

    single {
        BluetoothScanner(
            context = androidContext(),
            adapter = get<BluetoothManager>().adapter
        )
    }

    single {
        BluetoothLocalData(
            context = androidContext(),
            adapter = get<BluetoothManager>().adapter
        )
    }

    single {
        BluetoothHidCore(
            context = androidContext(),
            adapter = get<BluetoothManager>().adapter,
            hidSettings = get<BluetoothHidDeviceAppSdpSettings>()
        )
    }

    single {
        SettingsDataStore(
            context = androidContext()
        )
    }

    single {
        DlnaController()
    }
}
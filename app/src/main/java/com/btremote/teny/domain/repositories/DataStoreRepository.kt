package com.btremote.teny.domain.repositories

import com.btremote.teny.domain.entities.RemoteNavigationEntity
import com.btremote.teny.domain.entities.remoteInput.VolumePhysicalButtonAction
import com.btremote.teny.domain.entities.remoteInput.keyboard.KeyboardLanguage
import com.btremote.teny.domain.entities.settings.AppearanceSettings
import com.btremote.teny.domain.entities.settings.CastSettings
import com.btremote.teny.domain.entities.settings.RemoteSettings
import com.btremote.teny.domain.entities.settings.ThemeEntity
import kotlinx.coroutines.flow.Flow

interface DataStoreRepository {

    // ---- Appearance ----

    val appearanceSettingsFlow: Flow<AppearanceSettings>

    suspend fun saveTheme(themeEntity: ThemeEntity)

    suspend fun saveUseDynamicColors(useDynamicColors: Boolean)

    suspend fun saveUseBlackColorForDarkTheme(useBlackColorForDarkTheme: Boolean)

    suspend fun saveUseFullScreen(useFullScreen: Boolean)

    // ---- Remote ----

    val remoteSettingsFlow: Flow<RemoteSettings>

    suspend fun saveMouseSpeed(mouseSpeed: Float)

    suspend fun saveMouseScrollSpeed(scrollSpeed: Float)

    suspend fun saveInvertMouseScrollingDirection(invertScrollingDirection: Boolean)

    suspend fun saveUseGyroscope(useGyroscope: Boolean)

    suspend fun saveUseReadingMode(useReadingMode: Boolean)

    suspend fun saveKeyboardLanguage(language: KeyboardLanguage)

    suspend fun saveMustClearInputField(mustClearInputField: Boolean)

    suspend fun saveUseAdvancedKeyboard(useAdvancedKeyboard: Boolean)

    suspend fun saveUseAdvancedKeyboardIntegrated(useAdvancedKeyboardIntegrated: Boolean)

    suspend fun saveUseMinimalistRemote(useMinimalistRemote: Boolean)

    suspend fun saveRemoteNavigation(remoteNavigationEntity: RemoteNavigationEntity)

    suspend fun saveUseEnterForSelection(useEnterForSelection: Boolean)

    suspend fun saveUseMouseNavigationByDefault(useMouseNavigationByDefault: Boolean)

    suspend fun saveVolumePhysicalButtonActionUp(volumePhysicalButtonAction: VolumePhysicalButtonAction)

    suspend fun saveVolumePhysicalButtonActionDown(volumePhysicalButtonAction: VolumePhysicalButtonAction)

    // ---- Others ----

    fun getFavoriteDevices(): Flow<List<String>>
    suspend fun saveFavoriteDevices(macAddresses: List<String>)

    fun getAutoConnectDeviceAddressFlow(): Flow<String>
    suspend fun saveAutoConnectDeviceAddress(macAddress: String)

    fun showRemoteButtonsInNotification(): Flow<Boolean>
    suspend fun saveShowRemoteButtonsInNotification(show: Boolean)

    fun hideBluetoothActivationButton(): Flow<Boolean>
    suspend fun saveHideBluetoothActivationButton(hide: Boolean)

    // ---- Cast ----

    fun getCastSettings(): Flow<CastSettings>
    suspend fun saveCastSettings(castSettings: CastSettings)
}
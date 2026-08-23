package com.btremote.teny.ui.screens

import android.bluetooth.BluetoothHidDevice
import android.content.Context
import android.content.res.Configuration
import android.view.InputDevice
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btremote.teny.R
import com.btremote.teny.common.utils.AppIcons
import com.btremote.teny.common.utils.KEYBOARD_REPORT_ID
import com.btremote.teny.common.utils.REMOTE_INPUT_NONE
import com.btremote.teny.common.utils.REMOTE_REPORT_ID
import com.btremote.teny.common.utils.getKeyboardLayout
import com.btremote.teny.domain.entities.DeviceHidConnectionState
import com.btremote.teny.domain.entities.RemoteNavigationEntity
import com.btremote.teny.domain.entities.remoteInput.MouseAction
import com.btremote.teny.domain.entities.remoteInput.RemoteButtonProperties
import com.btremote.teny.domain.entities.remoteInput.VolumePhysicalButtonAction
import com.btremote.teny.domain.entities.remoteInput.keyboard.KeyboardLanguage
import com.btremote.teny.domain.entities.remoteInput.keyboard.virtualKeyboard.VirtualKeyboardLayout
import com.btremote.teny.domain.entities.settings.RemoteSettings
import com.btremote.teny.presentation.services.BluetoothHidService
import com.btremote.teny.presentation.viewmodel.RemoteViewModel
import com.btremote.teny.ui.components.AppScaffold
import com.btremote.teny.ui.components.BasicDropdownMenuItem
import com.btremote.teny.ui.components.BasicIconButton
import com.btremote.teny.ui.components.FadeAnimatedContent
import com.btremote.teny.ui.components.LoadingDialog
import com.btremote.teny.ui.components.MoreOverflowMenu
import com.btremote.teny.ui.theme.surfaceElevationMedium
import com.btremote.teny.ui.views.RemoteScreenHelpModalBottomSheet
import com.btremote.teny.ui.views.keyboard.AdvancedKeyboard
import com.btremote.teny.ui.views.keyboard.AdvancedKeyboardModalBottomSheet
import com.btremote.teny.ui.views.keyboard.VirtualKeyboardModalBottomSheet
import com.btremote.teny.ui.views.mouse.MousePadLayout
import com.btremote.teny.ui.views.remote.CastDialog
import com.btremote.teny.ui.views.remote.MinimalistRemoteView
import com.btremote.teny.ui.views.remote.MoreButtonsDialog
import com.btremote.teny.ui.views.remote.RemoteView
import com.btremote.teny.ui.views.remote.TVChannelDialog
import com.btremote.teny.ui.views.remoteNavigation.RemoteDirectionalPadNavigation
import com.btremote.teny.ui.views.remoteNavigation.RemoteSwipeNavigation
import org.koin.androidx.compose.koinViewModel

private enum class NavigationToggle {
    DIRECTION,
    MOUSE
}

@Composable
fun RemoteScreen(
    bluetoothDeviceHidConnectionState: DeviceHidConnectionState,
    closeApp: () -> Unit,
    navigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    remoteViewModel: RemoteViewModel = koinViewModel(),
    context: Context = LocalContext.current
) {
    val configuration = LocalConfiguration.current

    val focusRequester = remember { FocusRequester() }

    val remoteSettings by remoteViewModel
        .remoteSettingsFlow.collectAsStateWithLifecycle(RemoteSettings())

    // Remote
    var navigationToggle by rememberSaveable(remoteSettings.useMouseNavigationByDefault) { mutableStateOf(if(remoteSettings.useMouseNavigationByDefault) NavigationToggle.MOUSE else NavigationToggle.DIRECTION) }

    // Keyboard
    var isKeyboardVisible: Boolean by rememberSaveable { mutableStateOf(false) }

    // More Buttons
    var isMoreButtonsVisible: Boolean by rememberSaveable { mutableStateOf(false) }

    // Cast
    var isCastDialogVisible: Boolean by rememberSaveable { mutableStateOf(false) }

    // Help
    var isHelpBottomSheetVisible: Boolean by remember { mutableStateOf(false) }

    // Fullscreen
    var isFullscreen: Boolean by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = true, onBack = closeApp)

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    StatelessRemoteScreen(
        deviceName = bluetoothDeviceHidConnectionState.deviceName,
        isLandscapeMode = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE,
        topBarActions = {
            TopBarActions(
                navigateToSettings = navigateToSettings,
                disconnectDevice = {
                    remoteViewModel.disconnectDevice()
                },
                navigationToggle = navigationToggle,
                onNavigationToggleChanged = { navigationToggle = it },
                useAdvancedKeyboardIntegrated = remoteSettings.useAdvancedKeyboard && remoteSettings.useAdvancedKeyboardIntegrated,
                isKeyboardVisible = isKeyboardVisible,
                onKeyboardVisibleChanged = { isKeyboardVisible = it },
                isMoreButtonsVisible = isMoreButtonsVisible,
                onMoreButtonsVisibleChanged = { isMoreButtonsVisible = it },
                isCastDialogVisible = isCastDialogVisible,
                onCastDialogVisibleChanged = { isCastDialogVisible = it },
                isHelpBottomSheetVisible = isHelpBottomSheetVisible,
                onHelpBottomSheetVisibleChanged = { isHelpBottomSheetVisible = it },
                isFullscreen = isFullscreen,
                onFullscreenChanged = { isFullscreen = it }
            )
        },
        remoteLayout = {
            RemoteLayout(
                useMinimalistRemote = remoteSettings.useMinimalistRemote,
                isAdvancedKeyboardVisible = remoteSettings.useAdvancedKeyboard && remoteSettings.useAdvancedKeyboardIntegrated && isKeyboardVisible,
                onMoreButtonsVisibleChanged = { isMoreButtonsVisible = !isMoreButtonsVisible },
                keyboardLanguage = remoteSettings.keyboardLanguage,
                sendRemoteKeyReport = remoteViewModel.sendRemoteReport,
                sendKeyboardKeyReport = remoteViewModel.sendKeyboardReport
            )
        },
        navigationLayout = {
            NavigationLayout(
                remoteSettings = remoteSettings,
                sendRemoteKeyReport = remoteViewModel.sendRemoteReport,
                sendKeyboardKeyReport = remoteViewModel.sendKeyboardReport,
                sendMouseKeyReport = remoteViewModel.sendMouseReport,
                navigationToggle = navigationToggle,
                isFullscreen = isFullscreen
            )
        },
        overlayView = {
            // Dialog / ModalBottomSheet
            when {
                bluetoothDeviceHidConnectionState.state == BluetoothHidDevice.STATE_DISCONNECTING -> {
                    LoadingDialog(
                        title = stringResource(id = R.string.connection),
                        message = stringResource(id = R.string.bluetooth_device_disconnecting_message, bluetoothDeviceHidConnectionState.deviceName),
                        buttonText = stringResource(id = R.string.disconnect),
                        onButtonClick = {
                            remoteViewModel.disconnectDevice()
                            BluetoothHidService.stop(context) // force disconnection
                        }
                    )
                }
                isHelpBottomSheetVisible -> {
                    RemoteScreenHelpModalBottomSheet(
                        onDismissRequest = { isHelpBottomSheetVisible = false },
                        modifier = modifier
                    )
                }
                isKeyboardVisible && (!remoteSettings.useAdvancedKeyboard || !remoteSettings.useAdvancedKeyboardIntegrated) -> {
                    KeyboardModalBottomSheet(
                        useAdvancedKeyboard = remoteSettings.useAdvancedKeyboard,
                        keyboardLanguage = remoteSettings.keyboardLanguage,
                        mustClearInputField = remoteSettings.mustClearInputField,
                        sendKeyboardKeyReport = remoteViewModel.sendKeyboardReport,
                        sendTextReport = remoteViewModel.sendTextReport,
                        onShowKeyboardChanged = { isKeyboardVisible = it }
                    )
                }
                isMoreButtonsVisible -> {
                    MoreButtonsDialog(
                        sendRemoteKeyReport = remoteViewModel.sendRemoteReport,
                        sendKeyboardKeyReport = remoteViewModel.sendKeyboardReport,
                        onDismissRequest = { isMoreButtonsVisible = false }
                    )
                }
                isCastDialogVisible -> {
                    CastDialog(
                        onDismissRequest = { isCastDialogVisible = false }
                    )
                }
            }
        },
        isFullscreen = isFullscreen,
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->

                // ---- Physical Buttons Handler ----

                val nativeEvent = event.nativeKeyEvent

                // Volume Physical Buttons

                val isHidReportSent = handlePhysicalVolumeButton(
                    event = nativeEvent,
                    volumePhysicalButtonActionUp = remoteSettings.volumePhysicalButtonActionUp,
                    volumePhysicalButtonActionDown = remoteSettings.volumePhysicalButtonActionDown,
                    sendHidReport = { id: Int, bytes: ByteArray ->
                        remoteViewModel.sendReport(id, bytes)
                    }
                )

                if(isHidReportSent) return@onPreviewKeyEvent true

                // Gamepad Physical Buttons

                val isFromGamepad = nativeEvent.device?.sources?.let { sources ->
                    sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                            sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
                } ?: false

                if (!isFromGamepad) return@onPreviewKeyEvent false

                handlePhysicalGamepadButtonAction(
                    event = nativeEvent,
                    remoteSettings = remoteSettings,
                    sendHidReport = { id: Int, bytes: ByteArray ->
                        remoteViewModel.sendReport(id, bytes)
                    }
                )
            }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatelessRemoteScreen(
    deviceName: String,
    isLandscapeMode: Boolean,
    topBarActions: @Composable (RowScope.() -> Unit),
    remoteLayout: @Composable () -> Unit,
    navigationLayout: @Composable () -> Unit,
    overlayView: @Composable () -> Unit,
    isFullscreen: Boolean,
    modifier: Modifier = Modifier
) {
    AppScaffold(
        title = deviceName,
        modifier = modifier,
        scrollBehavior = null,
        topBarActions = topBarActions
    ) { innerPadding ->

        if(isLandscapeMode) {
            RemoteLandscapeView(
                remoteLayout = remoteLayout,
                navigationLayout = navigationLayout,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            RemotePortraitView(
                remoteLayout = remoteLayout,
                navigationLayout = navigationLayout,
                isFullscreen = isFullscreen,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }

        // Dialog / ModalBottomSheet
        overlayView()
    }
}

@Composable
private fun RemoteLandscapeView(
    remoteLayout: @Composable () -> Unit,
    navigationLayout: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var rowSize by remember { mutableStateOf(Size.Zero) }

    Row(
        modifier = modifier.onGloballyPositioned { layoutCoordinates ->
            rowSize = layoutCoordinates.size.toSize() },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = with(LocalDensity.current) { (0.5f * rowSize.width).toDp() })
                .align(Alignment.CenterVertically)
                .padding(
                    start = dimensionResource(id = R.dimen.padding_large),
                    top = dimensionResource(id = R.dimen.padding_large),
                    bottom = dimensionResource(id = R.dimen.padding_large)
                ),
        ) {
            navigationLayout()
        }

        Box(
            modifier = Modifier.align(Alignment.CenterVertically),
            contentAlignment = Alignment.Center
        ) {
            remoteLayout()
        }
    }
}

@Composable
private fun RemotePortraitView(
    remoteLayout: @Composable () -> Unit,
    navigationLayout: @Composable () -> Unit,
    isFullscreen: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if(!isFullscreen) {
            Box(
                modifier = Modifier
                    .heightIn(
                        max = with(LocalConfiguration.current) {
                            if (screenHeightDp >= screenWidthDp * 1.9f)
                                screenWidthDp.dp
                            else
                                (screenHeightDp * 0.50).dp
                        }
                    )
                    .align(Alignment.CenterHorizontally),
            ) {
                remoteLayout()
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .then(
                    if(isFullscreen) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier.padding(
                            start = dimensionResource(id = R.dimen.padding_large),
                            end = dimensionResource(id = R.dimen.padding_large),
                            bottom = dimensionResource(id = R.dimen.padding_large)
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            navigationLayout()
        }
    }
}

@Composable
private fun RemoteLayout(
    useMinimalistRemote: Boolean,
    isAdvancedKeyboardVisible: Boolean,
    onMoreButtonsVisibleChanged: () -> Unit,
    keyboardLanguage: KeyboardLanguage,
    sendRemoteKeyReport: (ByteArray) -> Unit,
    sendKeyboardKeyReport: (ByteArray) -> Unit
) {
    FadeAnimatedContent(targetState = isAdvancedKeyboardVisible) {
        if (it) {
            AdvancedKeyboard(
                keyboardLanguage = keyboardLanguage,
                sendKeyboardKeyReport = sendKeyboardKeyReport,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.padding_large)),
                keyElevation = surfaceElevationMedium()
            )
        } else {
            RemoteButtonsLayouts(
                useMinimalistRemote = useMinimalistRemote,
                onMoreButtonsVisibleChanged = onMoreButtonsVisibleChanged,
                sendRemoteKeyReport = sendRemoteKeyReport,
                sendKeyboardKeyReport = sendKeyboardKeyReport
            )
        }
    }
}

@Composable
private fun RemoteButtonsLayouts(
    useMinimalistRemote: Boolean,
    onMoreButtonsVisibleChanged: () -> Unit,
    sendRemoteKeyReport: (ByteArray) -> Unit,
    sendKeyboardKeyReport: (ByteArray) -> Unit
) {
    if (useMinimalistRemote) {
        var isTVChannelButtonsVisible: Boolean by remember { mutableStateOf(false) }

        MinimalistRemoteView(
            sendRemoteKeyReport = sendRemoteKeyReport,
            onTVChannelButtonsChanged = {
                isTVChannelButtonsVisible = !isTVChannelButtonsVisible
            },
            onMoreButtonsVisibleChanged = onMoreButtonsVisibleChanged,
            modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_normal))
        )

        if (isTVChannelButtonsVisible) {
            TVChannelDialog(
                sendRemoteKeyReport = sendRemoteKeyReport,
                sendKeyboardKeyReport = sendKeyboardKeyReport,
                onDismissRequest = {
                    isTVChannelButtonsVisible = false
                }
            )
        }
    } else {
        RemoteView(
            sendRemoteKeyReport = sendRemoteKeyReport,
            sendKeyboardKeyReport = sendKeyboardKeyReport,
            modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_normal))
        )
    }
}

@Composable
private fun NavigationLayout(
    remoteSettings: RemoteSettings,
    sendRemoteKeyReport: (ByteArray) -> Unit,
    sendKeyboardKeyReport: (ByteArray) -> Unit,
    sendMouseKeyReport: (input: MouseAction, x: Float, y: Float, wheel: Float) -> Unit,
    navigationToggle: NavigationToggle,
    isFullscreen: Boolean
) {
    FadeAnimatedContent(targetState = navigationToggle) {
        when(it) {
            NavigationToggle.DIRECTION -> {
                RemotePadLayout(
                    remoteNavigationMode = remoteSettings.remoteNavigationEntity,
                    sendRemoteKeyReport = sendRemoteKeyReport,
                    sendKeyboardKeyReport = sendKeyboardKeyReport,
                    useEnterForSelection = remoteSettings.useEnterForSelection,
                    modifier = if(isFullscreen) Modifier.fillMaxSize() else Modifier
                )
            }

            NavigationToggle.MOUSE -> {
                MousePadLayout(
                    mouseSpeed = remoteSettings.mouseSpeed,
                    scrollSpeed = remoteSettings.mouseScrollSpeed,
                    shouldInvertMouseScrollingDirection = remoteSettings.shouldInvertMouseScrollingDirection,
                    useGyroscope = remoteSettings.useGyroscope,
                    sendMouseInput = sendMouseKeyReport,
                    isFullscreen = isFullscreen,
                    modifier = if(isFullscreen) Modifier.fillMaxSize() else Modifier
                )
            }
        }
    }
}

@Composable
private fun RemotePadLayout(
    remoteNavigationMode: RemoteNavigationEntity,
    sendRemoteKeyReport: (ByteArray) -> Unit,
    sendKeyboardKeyReport: (ByteArray) -> Unit,
    useEnterForSelection: Boolean,
    modifier: Modifier = Modifier
) {
    if(remoteNavigationMode == RemoteNavigationEntity.D_PAD) {
        RemoteDirectionalPadNavigation(
            sendRemoteKeyReport = sendRemoteKeyReport,
            sendKeyboardKeyReport = sendKeyboardKeyReport,
            useEnterForSelection = useEnterForSelection,
            modifier = modifier.aspectRatio(1f)
        )
    } else {
        RemoteSwipeNavigation(
            sendRemoteKeyReport = sendRemoteKeyReport,
            sendKeyboardKeyReport = sendKeyboardKeyReport,
            useEnterForSelection = useEnterForSelection,
            modifier = modifier
        )
    }
}

@Composable
private fun KeyboardModalBottomSheet(
    useAdvancedKeyboard: Boolean,
    keyboardLanguage: KeyboardLanguage,
    mustClearInputField: Boolean,
    sendKeyboardKeyReport: (ByteArray) -> Unit,
    sendTextReport: (String, VirtualKeyboardLayout, Boolean) -> Unit,
    onShowKeyboardChanged: (Boolean) -> Unit,
) {
    if (useAdvancedKeyboard) {
        AdvancedKeyboardModalBottomSheet(
            keyboardLanguage = keyboardLanguage,
            sendKeyboardKeyReport = sendKeyboardKeyReport,
            onShowKeyboardBottomSheetChanged = onShowKeyboardChanged
        )
    } else {
        var virtualKeyboardLayout: VirtualKeyboardLayout by remember {
            mutableStateOf(getKeyboardLayout(keyboardLanguage))
        }

        LaunchedEffect(keyboardLanguage) {
            virtualKeyboardLayout = getKeyboardLayout(keyboardLanguage)
        }

        VirtualKeyboardModalBottomSheet(
            mustClearInputField = mustClearInputField,
            sendKeyboardKeyReport = sendKeyboardKeyReport,
            sendTextReport = { text: String, shouldSendEnter: Boolean ->
                sendTextReport(text, virtualKeyboardLayout, shouldSendEnter)
            },
            onShowKeyboardBottomSheetChanged = onShowKeyboardChanged
        )
    }
}

@Composable
private fun TopBarActions(
    navigateToSettings: () -> Unit,
    disconnectDevice: () -> Unit,
    navigationToggle: NavigationToggle,
    onNavigationToggleChanged: (NavigationToggle) -> Unit,
    useAdvancedKeyboardIntegrated: Boolean,
    isKeyboardVisible: Boolean,
    onKeyboardVisibleChanged: (Boolean) -> Unit,
    isMoreButtonsVisible: Boolean,
    onMoreButtonsVisibleChanged: (Boolean) -> Unit,
    isCastDialogVisible: Boolean,
    onCastDialogVisibleChanged: (Boolean) -> Unit,
    isHelpBottomSheetVisible: Boolean,
    onHelpBottomSheetVisibleChanged: (Boolean) -> Unit,
    isFullscreen: Boolean,
    onFullscreenChanged: (Boolean) -> Unit
) {
    FadeAnimatedContent(targetState = navigationToggle) {
        when (it) {
            NavigationToggle.MOUSE -> {
                BasicIconButton(
                    onClick = {
                        onNavigationToggleChanged(NavigationToggle.DIRECTION)
                    },
                    icon = AppIcons.Controller,
                    contentDescription = stringResource(id = R.string.direction_arrows),
                )
            }

            NavigationToggle.DIRECTION -> {
                BasicIconButton(
                    onClick = {
                        onNavigationToggleChanged(NavigationToggle.MOUSE)
                    },
                    icon = AppIcons.Mouse,
                    contentDescription = stringResource(id = R.string.mouse),
                )
            }
        }
    }

    if(useAdvancedKeyboardIntegrated) {
        FadeAnimatedContent(targetState = isKeyboardVisible) {
            when (it) {
                true -> {
                    // Remote
                    BasicIconButton(
                        onClick = { onKeyboardVisibleChanged(false) },
                        icon = AppIcons.RemoteControl,
                        contentDescription = stringResource(id = R.string.remote)
                    )
                }

                false -> {
                    // Keyboard
                    BasicIconButton(
                        onClick = { onKeyboardVisibleChanged(true) },
                        icon = AppIcons.Keyboard,
                        contentDescription = stringResource(id = R.string.keyboard)
                    )
                }
            }
        }
    } else {
        // Keyboard
        BasicIconButton(
            onClick = { onKeyboardVisibleChanged(!isKeyboardVisible) },
            icon = AppIcons.Keyboard,
            contentDescription = stringResource(id = R.string.keyboard)
        )
    }

    // Fullscreen
    FadeAnimatedContent(targetState = isFullscreen) {
        when (it) {
            true -> {
                BasicIconButton(
                    onClick = { onFullscreenChanged(false) },
                    icon = AppIcons.FullscreenExit,
                    contentDescription = stringResource(id = R.string.exit_full_screen)
                )
            }

            false -> {
                BasicIconButton(
                    onClick = { onFullscreenChanged(true) },
                    icon = AppIcons.Fullscreen,
                    contentDescription = stringResource(id = R.string.full_screen)
                )
            }
        }
    }

    MoreOverflowMenu { closeDropdownMenu: () -> Unit ->

        // More Buttons
        BasicDropdownMenuItem(
            text = stringResource(id = R.string.more_buttons),
            icon = AppIcons.MoreHoriz,
            onClick = {
                closeDropdownMenu()
                onMoreButtonsVisibleChanged(!isMoreButtonsVisible)
            }
        )

        // Disconnect
        BasicDropdownMenuItem(
            text = stringResource(id = R.string.disconnect),
            icon = AppIcons.Disconnect,
            onClick = {
                closeDropdownMenu()
                disconnectDevice()
            }
        )

        // Cast
        BasicDropdownMenuItem(
            text = stringResource(id = R.string.cast_screen),
            icon = AppIcons.Cast,
            onClick = {
                closeDropdownMenu()
                onCastDialogVisibleChanged(!isCastDialogVisible)
            }
        )

        // Divider
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.padding_medium))
        )

        // Help
        BasicDropdownMenuItem(
            text = stringResource(id = R.string.help),
            icon = AppIcons.Help,
            onClick = {
                closeDropdownMenu()
                onHelpBottomSheetVisibleChanged(!isHelpBottomSheetVisible)
            }
        )

        // Settings
        BasicDropdownMenuItem(
            text = stringResource(id = R.string.settings),
            icon = AppIcons.Settings,
            onClick = {
                closeDropdownMenu()
                navigateToSettings()
            }
        )
    }
}

// ---- Physical Buttons ----

private fun handlePhysicalGamepadButtonAction(
    event: KeyEvent,
    remoteSettings: RemoteSettings,
    sendHidReport: (Int, ByteArray) -> Unit
): Boolean {
    var id = REMOTE_REPORT_ID
    val input: ByteArray = when (event.keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> RemoteButtonProperties.MenuUpButton.input
        KeyEvent.KEYCODE_DPAD_DOWN -> RemoteButtonProperties.MenuDownButton.input
        KeyEvent.KEYCODE_DPAD_LEFT -> RemoteButtonProperties.MenuLeftButton.input
        KeyEvent.KEYCODE_DPAD_RIGHT -> RemoteButtonProperties.MenuRightButton.input
        KeyEvent.KEYCODE_BUTTON_A -> {
            if(remoteSettings.useEnterForSelection) {
                id = KEYBOARD_REPORT_ID
                RemoteButtonProperties.KeyboardEnterButton.input
            } else {
                RemoteButtonProperties.MenuPickButton.input
            }
        }
        KeyEvent.KEYCODE_BUTTON_B -> RemoteButtonProperties.BackButton.input
        else -> return false
    }

    return doPhysicalGamepadButtonAction(
        event = event,
        id = id,
        input = input,
        sendHidReport = sendHidReport
    )
}

private fun doPhysicalGamepadButtonAction(
    event: KeyEvent,
    id: Int,
    input: ByteArray,
    sendHidReport: (Int, ByteArray) -> Unit
): Boolean {
    when(event.action) {
        KeyEvent.ACTION_DOWN -> sendHidReport(id, input)
        KeyEvent.ACTION_UP -> sendHidReport(id, REMOTE_INPUT_NONE)
        else -> return false
    }
    return true
}

private fun handlePhysicalVolumeButton(
    event: KeyEvent,
    volumePhysicalButtonActionUp: VolumePhysicalButtonAction,
    volumePhysicalButtonActionDown: VolumePhysicalButtonAction,
    sendHidReport: (Int, ByteArray) -> Unit
): Boolean {
    val volumePhysicalButtonAction: VolumePhysicalButtonAction = when(event.keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP -> volumePhysicalButtonActionUp
        KeyEvent.KEYCODE_VOLUME_DOWN -> volumePhysicalButtonActionDown
        else -> return false
    }

    return sendPhysicalVolumeButtonAction(
        event = event,
        volumePhysicalButtonAction = volumePhysicalButtonAction,
        sendHidReport = sendHidReport
    )
}

private fun sendPhysicalVolumeButtonAction(
    event: KeyEvent,
    volumePhysicalButtonAction: VolumePhysicalButtonAction,
    sendHidReport: (Int, ByteArray) -> Unit
): Boolean {
    if (volumePhysicalButtonAction == VolumePhysicalButtonAction.DEFAULT) return false

    when(event.action) {
        KeyEvent.ACTION_DOWN -> {
            sendHidReport(
                volumePhysicalButtonAction.hidReportId, volumePhysicalButtonAction.pressedBytes
            )
        }

        KeyEvent.ACTION_UP -> {
            sendHidReport(
                volumePhysicalButtonAction.hidReportId, volumePhysicalButtonAction.releasedBytes
            )
        }

        else -> return false
    }

    return true
}
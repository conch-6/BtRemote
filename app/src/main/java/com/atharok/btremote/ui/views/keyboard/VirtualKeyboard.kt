package com.atharok.btremote.ui.views.keyboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.atharok.btremote.R
import com.atharok.btremote.common.utils.AppIcons
import com.atharok.btremote.domain.entities.remoteInput.RemoteButtonProperties
import com.atharok.btremote.ui.components.BasicIconButton
import com.atharok.btremote.ui.components.customButtons.RemoteIconSurfaceButton
import com.atharok.btremote.ui.theme.surfaceElevationHigh

@Composable
fun VirtualKeyboardModalBottomSheet(
    mustClearInputField: Boolean,
    sendKeyboardKeyReport: (ByteArray) -> Unit,
    sendTextReport: (String, Boolean) -> Unit,
    onShowKeyboardBottomSheetChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    KeyboardModalBottomSheet(
        onShowKeyboardBottomSheetChanged = onShowKeyboardBottomSheetChanged,
        windowInsets = WindowInsets.ime,
        modifier = modifier
    ) {
        val focusRequester = remember { FocusRequester() }
        val textState = remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        StatelessKeyboardView(
            mustClearInputField = mustClearInputField,
            focusRequester = focusRequester,
            text = textState.value,
            onTextChange = {
                textState.value = it
            },
            sendKeyboardKeyReport = sendKeyboardKeyReport,
            sendTextReport = sendTextReport,
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.padding_medium))
                .padding(bottom = dimensionResource(id = R.dimen.padding_max))
        )
    }
}

@Composable
private fun StatelessKeyboardView(
    mustClearInputField: Boolean,
    focusRequester: FocusRequester,
    text: String,
    onTextChange: (String) -> Unit,
    sendKeyboardKeyReport: (ByteArray) -> Unit,
    sendTextReport: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_medium))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(unbounded = true),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_small)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(4f)
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        sendTextReport(text, true)
                        if(mustClearInputField) {
                            onTextChange("")
                        }
                    }
                )
            )

            BasicIconButton(
                onClick = {
                    sendTextReport(text, false)
                    if(mustClearInputField) {
                        onTextChange("")
                    }
                },
                icon = AppIcons.Send,
                contentDescription = stringResource(id = R.string.send),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = CircleShape
            )

        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            AdditionalKeyboardKeysLayout(
                sendKeyboardKeyReport = sendKeyboardKeyReport,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(unbounded = true)
            )
        }
    }
}

@Composable
private fun AdditionalKeyboardKeysLayout(
    sendKeyboardKeyReport: (ByteArray) -> Unit,
    modifier: Modifier = Modifier,
    spaceBetweenButtons: Dp = dimensionResource(id = R.dimen.padding_small)
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spaceBetweenButtons)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            VirtualKeyboardButton(
                properties = RemoteButtonProperties.KeyboardSpaceBarButton,
                sendKeyboardKeyReport = sendKeyboardKeyReport,
                modifier = Modifier.weight(3f)
            )

            VirtualKeyboardButton(
                properties = RemoteButtonProperties.KeyboardUpButton,
                sendKeyboardKeyReport = sendKeyboardKeyReport,
                modifier = Modifier.weight(1f).padding(horizontal = spaceBetweenButtons)
            )

            VirtualKeyboardButton(
                properties = RemoteButtonProperties.KeyboardPrintScreenButton,
                sendKeyboardKeyReport = sendKeyboardKeyReport,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            VirtualKeyboardButton(
                properties = RemoteButtonProperties.KeyboardBackspaceButton,
                sendKeyboardKeyReport = sendKeyboardKeyReport,
                modifier = Modifier.weight(1f)
            )

            VirtualKeyboardButton(
                properties = RemoteButtonProperties.KeyboardEnterButton,
                sendKeyboardKeyReport = sendKeyboardKeyReport,
                modifier = Modifier.weight(1f).padding(horizontal = spaceBetweenButtons)
            )

            VirtualKeyboardButton(
                properties = RemoteButtonProperties.KeyboardLeftButton,
                sendKeyboardKeyReport = sendKeyboardKeyReport,
                modifier = Modifier.weight(1f)
            )

            VirtualKeyboardButton(
                properties = RemoteButtonProperties.KeyboardDownButton,
                sendKeyboardKeyReport = sendKeyboardKeyReport,
                modifier = Modifier.weight(1f).padding(horizontal = spaceBetweenButtons)
            )

            VirtualKeyboardButton(
                properties = RemoteButtonProperties.KeyboardRightButton,
                sendKeyboardKeyReport = sendKeyboardKeyReport,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun VirtualKeyboardButton(
    properties: RemoteButtonProperties,
    sendKeyboardKeyReport: (ByteArray) -> Unit,
    modifier: Modifier = Modifier
) {
    RemoteIconSurfaceButton(
        properties = properties,
        sendKeyReport = sendKeyboardKeyReport,
        modifier = modifier,
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.keyboard_key_corner_radius)),
        elevation = surfaceElevationHigh(),
        iconFillFraction = 1f,
        iconPadding = dimensionResource(id = R.dimen.padding_medium)
    )
}
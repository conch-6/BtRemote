package com.atharok.btremote.ui.views.remote

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atharok.btremote.R
import com.atharok.btremote.common.utils.AppIcons
import com.atharok.btremote.domain.entities.settings.CastLink
import com.atharok.btremote.domain.entities.settings.CastSettings
import com.atharok.btremote.presentation.viewmodel.CastViewModel
import com.atharok.btremote.ui.components.BasicIconButton
import com.atharok.btremote.ui.components.ListDialog
import com.atharok.btremote.ui.components.LoadingDialog
import com.atharok.btremote.ui.components.MaterialButton
import com.atharok.btremote.ui.components.SimpleDialog
import com.atharok.btremote.ui.components.TemplateDialog
import com.atharok.btremote.ui.components.TextLarge
import com.atharok.btremote.ui.components.TextNormal
import com.atharok.btremote.ui.components.TextNormalSecondary
import org.koin.androidx.compose.koinViewModel

@Composable
fun CastDialog(
    castViewModel: CastViewModel = koinViewModel(),
    onDismissRequest: () -> Unit
) {
    val castSettings by castViewModel.castSettings.collectAsStateWithLifecycle()
    val isSearching by castViewModel.isSearching.collectAsStateWithLifecycle()
    val discoveredDevices by castViewModel.discoveredDevices.collectAsStateWithLifecycle()
    val castResult by castViewModel.castResult.collectAsStateWithLifecycle()

    var showAddLinkDialog by remember { mutableStateOf(false) }
    var linkToDelete by remember { mutableStateOf<CastLink?>(null) }

    CastDialogsLayer(
        castViewModel = castViewModel,
        castSettings = castSettings,
        isSearching = isSearching,
        discoveredDevices = discoveredDevices,
        castResult = castResult,
        showAddLinkDialog = showAddLinkDialog,
        onShowAddLinkDialogChanged = { showAddLinkDialog = it },
        linkToDelete = linkToDelete,
        onLinkToDeleteChanged = { linkToDelete = it },
        onDismissRequest = onDismissRequest
    )

    TemplateDialog(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextLarge(text = stringResource(R.string.cast_screen))
                BasicIconButton(
                    onClick = { showAddLinkDialog = true },
                    icon = AppIcons.Add,
                    contentDescription = stringResource(R.string.add_cast_link)
                )
            }
        },
        content = {
            CastDialogContent(
                castSettings = castSettings,
                onSelectLink = { castViewModel.selectLink(it) },
                onLinkLongClick = { linkToDelete = it },
                onCastClick = { castViewModel.startCast() },
                onReselectDevice = { castViewModel.forgetSavedDevice() }
            )
        },
        dismissButtonText = stringResource(R.string.close),
        onDismissRequest = onDismissRequest
    )
}

@Composable
private fun CastDialogsLayer(
    castViewModel: CastViewModel,
    castSettings: CastSettings,
    isSearching: Boolean,
    discoveredDevices: List<com.atharok.btremote.domain.entity.dlna.DlnaDevice>,
    castResult: CastViewModel.CastResult?,
    showAddLinkDialog: Boolean,
    onShowAddLinkDialogChanged: (Boolean) -> Unit,
    linkToDelete: CastLink?,
    onLinkToDeleteChanged: (CastLink?) -> Unit,
    onDismissRequest: () -> Unit
) {
    if (showAddLinkDialog) {
        AddLinkDialog(
            onConfirm = { url ->
                castViewModel.addLink(url)
                onShowAddLinkDialogChanged(false)
            },
            onDismissRequest = { onShowAddLinkDialogChanged(false) }
        )
    }

    linkToDelete?.let { link ->
        SimpleDialog(
            confirmButtonText = stringResource(R.string.delete),
            dismissButtonText = stringResource(R.string.cancel),
            onConfirmation = {
                castViewModel.deleteLink(link.id)
                onLinkToDeleteChanged(null)
            },
            onDismissRequest = { onLinkToDeleteChanged(null) },
            dialogTitle = stringResource(R.string.delete_cast_link_title),
            dialogText = stringResource(R.string.delete_cast_link_message, link.url)
        )
    }

    if (isSearching && discoveredDevices.isEmpty()) {
        LoadingDialog(
            title = stringResource(R.string.cast_screen),
            message = stringResource(
                if (castSettings.savedDevice != null) R.string.casting_in_progress else R.string.searching_dlna_devices
            ),
            buttonText = stringResource(R.string.cancel),
            onButtonClick = { castViewModel.cancelSearchOrCast() }
        )
    } else if (discoveredDevices.isNotEmpty()) {
        ListDialog(
            confirmButtonText = stringResource(R.string.cast_screen),
            dismissButtonText = stringResource(R.string.cancel),
            onConfirmation = { index ->
                castViewModel.selectDeviceAndCast(discoveredDevices[index])
            },
            onDismissRequest = { castViewModel.clearDiscoveredDevices() },
            dialogTitle = stringResource(R.string.select_device),
            dialogMessage = stringResource(R.string.select_dlna_device_message),
            items = discoveredDevices.map { it.name },
            defaultItemIndex = 0
        )
    }

    castResult?.let { result ->
        when (result) {
            is CastViewModel.CastResult.Success -> {
                SimpleDialog(
                    confirmButtonText = null,
                    dismissButtonText = stringResource(R.string.ok),
                    onConfirmation = {},
                    onDismissRequest = {
                        castViewModel.clearCastResult()
                        onDismissRequest()
                    },
                    dialogTitle = stringResource(R.string.cast_screen),
                    dialogText = stringResource(R.string.cast_success_message)
                )
            }
            is CastViewModel.CastResult.Error -> {
                SimpleDialog(
                    confirmButtonText = stringResource(R.string.retry),
                    dismissButtonText = stringResource(R.string.close),
                    onConfirmation = {
                        castViewModel.clearCastResult()
                        castViewModel.startCast()
                    },
                    onDismissRequest = { castViewModel.clearCastResult() },
                    dialogTitle = stringResource(R.string.cast_failed_title),
                    dialogText = result.message
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CastDialogContent(
    castSettings: CastSettings,
    onSelectLink: (String) -> Unit,
    onLinkLongClick: (CastLink) -> Unit,
    onCastClick: () -> Unit,
    onReselectDevice: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = LocalWindowInfo.current.containerDpSize.height * 3 / 5)
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            if (castSettings.links.isEmpty()) {
                TextNormalSecondary(
                    text = stringResource(R.string.no_cast_link_message),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(R.dimen.padding_large)),
                    textAlign = TextAlign.Center
                )
            } else {
                castSettings.links.forEach { link ->
                    LinkItem(
                        link = link,
                        isSelected = link.id == castSettings.selectedLinkId,
                        onSelect = { onSelectLink(link.id) },
                        onLongClick = { onLinkLongClick(link) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_large)))

        castSettings.savedDevice?.let { device ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextNormalSecondary(
                    text = stringResource(R.string.saved_device_label, device.name),
                    modifier = Modifier.weight(1f)
                )
                BasicIconButton(
                    onClick = onReselectDevice,
                    icon = AppIcons.Refresh,
                    contentDescription = stringResource(R.string.reselect_device)
                )
            }
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))
        }

        MaterialButton(
            onClick = onCastClick,
            enabled = castSettings.links.isNotEmpty() && castSettings.selectedLinkId != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextNormal(text = stringResource(R.string.cast_screen))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LinkItem(
    link: CastLink,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onSelect,
                onLongClick = onLongClick
            )
            .padding(vertical = dimensionResource(R.dimen.padding_small)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextNormal(
            text = link.url,
            modifier = Modifier
                .weight(1f)
                .padding(end = dimensionResource(R.dimen.padding_medium)),
            maxLines = 2
        )
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonColors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.onSurface,
                disabledSelectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledUnselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
    HorizontalDivider()
}

@Composable
private fun AddLinkDialog(
    onConfirm: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var textState by remember { mutableStateOf(TextFieldValue("")) }
    val isValid = textState.text.isNotBlank()

    TemplateDialog(
        title = { TextLarge(text = stringResource(R.string.add_cast_link)) },
        content = {
            TextField(
                value = textState,
                onValueChange = { textState = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { TextNormalSecondary(text = stringResource(R.string.cast_link_hint)) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                )
            )
        },
        confirmButtonText = stringResource(R.string.add),
        onConfirmation = {
            if (isValid) {
                onConfirm(textState.text)
            }
        },
        dismissButtonText = stringResource(R.string.cancel),
        onDismissRequest = onDismissRequest
    )
}

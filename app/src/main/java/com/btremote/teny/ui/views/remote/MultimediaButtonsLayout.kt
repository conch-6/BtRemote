package com.btremote.teny.ui.views.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.btremote.teny.domain.entities.remoteInput.RemoteButtonProperties
import com.btremote.teny.ui.components.DefaultElevatedCard
import com.btremote.teny.ui.components.customButtons.RemoteIconRawButton
import com.btremote.teny.ui.theme.surfaceElevationMedium

@Composable
fun MultimediaButtonsLayout(
    sendRemoteKeyReport: (ByteArray) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    elevation: Dp = surfaceElevationMedium()
) {
    DefaultElevatedCard(
        modifier = modifier,
        shape = shape,
        elevation = elevation
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Absolute.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RemoteIconRawButton(
                properties = RemoteButtonProperties.RewindButton,
                sendKeyReport = sendRemoteKeyReport,
                modifier = Modifier.weight(24f),
                shape = shape,
                iconFillFraction = 0.6f
            )

            RemoteIconRawButton(
                properties = RemoteButtonProperties.PlayPauseButton,
                sendKeyReport = sendRemoteKeyReport,
                modifier = Modifier.weight(40f),
                shape = shape,
                iconFillFraction = 0.6f
            )

            RemoteIconRawButton(
                properties = RemoteButtonProperties.ForwardButton,
                sendKeyReport = sendRemoteKeyReport,
                modifier = Modifier.weight(24f),
                shape = shape,
                iconFillFraction = 0.6f
            )
        }
    }
}
package com.btremote.teny.ui.views.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
fun VerticalButtonsLayout(
    topButtonProperties: RemoteButtonProperties,
    bottomButtonProperties: RemoteButtonProperties,
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
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            RemoteIconRawButton(
                properties = topButtonProperties,
                sendKeyReport = sendRemoteKeyReport,
                modifier = Modifier
                    .aspectRatio(1f)
                    .weight(
                        weight = 1f,
                        fill = false
                    ),
                shape = shape
            )

            RemoteIconRawButton(
                properties = bottomButtonProperties,
                sendKeyReport = sendRemoteKeyReport,
                modifier = Modifier
                    .aspectRatio(1f)
                    .weight(
                        weight = 1f,
                        fill = false
                    ),
                shape = shape
            )
        }
    }
}
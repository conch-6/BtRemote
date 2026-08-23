package com.btremote.teny.ui.components.customButtons

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.btremote.teny.common.utils.REMOTE_INPUT_NONE
import com.btremote.teny.domain.entities.remoteInput.RemoteButtonProperties
import com.btremote.teny.ui.theme.surfaceElevationMedium
import com.btremote.teny.ui.theme.surfaceElevationShadow

// ---- Surface Button ----

@Composable
fun EmptySurfaceButton(
    touchDown: () -> Unit,
    touchUp: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    elevation: Dp = surfaceElevationMedium(),
    shadowElevation: Dp = surfaceElevationShadow(),
) {
    SurfaceButton(
        touchDown = touchDown,
        touchUp = touchUp,
        modifier = modifier,
        shape = shape,
        elevation = elevation,
        shadowElevation = shadowElevation
    ) {
        Spacer(
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun RemoteEmptySurfaceButton(
    properties: RemoteButtonProperties,
    sendKeyReport: (ByteArray) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    elevation: Dp = surfaceElevationMedium(),
    shadowElevation: Dp = surfaceElevationShadow()
) {
    EmptySurfaceButton(
        touchDown = { sendKeyReport(properties.input) },
        touchUp = { sendKeyReport(REMOTE_INPUT_NONE) },
        modifier = modifier,
        shape = shape,
        elevation = elevation,
        shadowElevation = shadowElevation
    )
}
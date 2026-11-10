package com.btremote.teny.ui.views.mouse

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.btremote.teny.R
import com.btremote.teny.common.utils.AppIcons
import com.btremote.teny.common.utils.Ref
import com.btremote.teny.domain.entities.remoteInput.MouseAction
import com.btremote.teny.ui.components.DefaultElevatedCard
import com.btremote.teny.ui.components.customButtons.EmptySurfaceButton

@Composable
fun MousePadLayout(
    mouseSpeed: Float,
    scrollSpeed: Float,
    shouldInvertMouseScrollingDirection: Boolean,
    useGyroscope: Boolean,
    sendMouseInput: (MouseAction, Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false
) {
    val mouseAction: Ref<MouseAction> = remember { Ref(MouseAction.NONE) }

    if(useGyroscope) {
        MouseGyroscope(
            mouseSpeed = mouseSpeed,
            onMousePositionChange = { x: Float, y: Float ->
                sendMouseInput(mouseAction.value, x, y, 0f)
            }
        )
    }

    Column(modifier) {
        Row(
            modifier = Modifier
                .weight(0.8f)
        ) {
            MousePad(
                mouseSpeed = mouseSpeed,
                scrollSpeed = scrollSpeed,
                sendMouseAction = {
                    mouseAction.value = it
                    sendMouseInput(mouseAction.value, 0f, 0f, 0f)
                },
                sendMousePosition = { x: Float, y: Float ->
                    sendMouseInput(mouseAction.value, x, y, 0f)
                },
                sendMouseScroll = { wheel: Float ->
                    val mouseWheel = wheel * if(shouldInvertMouseScrollingDirection) -1f else 1f
                    sendMouseInput(mouseAction.value, 0f, 0f, mouseWheel)
                },
                shape = if(isFullscreen) {
                    RoundedCornerShape(0.dp)
                } else {
                    RoundedCornerShape(
                        topStart = dimensionResource(id = R.dimen.card_corner_radius),
                        topEnd = 0.dp,
                        bottomEnd = 0.dp,
                        bottomStart = 0.dp
                    )
                },
                modifier = Modifier
                    .weight(0.85f)
            )

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = dimensionResource(id = R.dimen.padding_min)),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )

            ScrollWheelLayout(
                scrollSpeed = scrollSpeed,
                sendMouseScroll = {
                    val mouseWheel = it * if(shouldInvertMouseScrollingDirection) -1f else 1f
                    sendMouseInput(mouseAction.value, 0f, 0f, mouseWheel)
                },
                modifier = Modifier
                    .weight(0.15f)
            )
        }

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = dimensionResource(id = R.dimen.padding_min)),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )

        MouseButtonsLayout(
            sendMouseAction = {
                mouseAction.value = it
                sendMouseInput(mouseAction.value, 0f, 0f, 0f)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.2f)
        )
    }
}

// ---- Mouse buttons ----

@Composable
private fun MouseButtonsLayout(
    sendMouseAction: (MouseAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.Absolute.Left) {

        // Start
        EmptySurfaceButton(
            touchDown = { sendMouseAction(MouseAction.MOUSE_CLICK_LEFT) },
            touchUp = { sendMouseAction(MouseAction.NONE) },
            modifier = Modifier
                .weight(0.38f)
                .fillMaxHeight()
                .absolutePadding(right = dimensionResource(id = R.dimen.padding_min)),
            shape = AbsoluteRoundedCornerShape(
                topLeft = 0.dp,
                topRight = 0.dp,
                bottomRight = 0.dp,
                bottomLeft = dimensionResource(id = R.dimen.card_corner_radius)
            )
        )

        // Center
        EmptySurfaceButton(
            touchDown = { sendMouseAction(MouseAction.MOUSE_CLICK_MIDDLE) },
            touchUp = { sendMouseAction(MouseAction.NONE) },
            modifier = Modifier
                .weight(0.24f)
                .fillMaxHeight()
                .padding(
                    horizontal = dimensionResource(id = R.dimen.padding_min)
                ),
            shape = RectangleShape
        )

        // End
        EmptySurfaceButton(
            touchDown = { sendMouseAction(MouseAction.MOUSE_CLICK_RIGHT) },
            touchUp = { sendMouseAction(MouseAction.NONE) },
            modifier = Modifier
                .weight(0.38f)
                .fillMaxHeight()
                .absolutePadding(left = dimensionResource(id = R.dimen.padding_min)),
            shape = AbsoluteRoundedCornerShape(
                topLeft = 0.dp,
                topRight = 0.dp,
                bottomRight = dimensionResource(id = R.dimen.card_corner_radius),
                bottomLeft = 0.dp
            )
        )
    }
}

// ---- Scroll wheel touchpad ----

// The controlled device quantises wheel reports into fixed-size ticks (one count is one
// "scroll factor" step of roughly a text line), so smoothness comes from sending OFTEN,
// not from sending big counts: while swiping, each pointer event emits at most one tick,
// and at swipe pace the next tick arrives before the previous step finished scrolling -
// a continuous glide instead of the old hold-button's 100ms-per-tick studder. No finger
// movement means no pointer events, which means no scrolling: the page only moves while
// the finger does.
private const val SCROLL_WHEEL_PX_PER_COUNT = 64f

@Composable
private fun ScrollWheelLayout(
    scrollSpeed: Float,
    sendMouseScroll: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollAccumulator = remember { Ref(0f) }

    DefaultElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(topEnd = dimensionResource(id = R.dimen.card_corner_radius))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(scrollSpeed) {
                    awaitEachGesture {
                        awaitFirstDown()
                        // Each swipe starts from scratch, so a leftover fraction cannot
                        // make the next touch jump by a whole tick.
                        scrollAccumulator.value = 0f
                        while (true) {
                            val event = awaitPointerEvent()
                            val inputChanges = event.changes

                            if (inputChanges.all { !it.pressed }) break

                            // Vertical only: horizontal travel on the strip is ignored,
                            // and extra fingers (doWheel-style scrolling) are skipped.
                            if (inputChanges.size == 1) {
                                val deltaY = inputChanges.first().positionChange().y
                                if (deltaY != 0f) {
                                    scrollAccumulator.value += deltaY * scrollSpeed / SCROLL_WHEEL_PX_PER_COUNT
                                    val counts = scrollAccumulator.value.toInt().coerceIn(-1, 1)
                                    if (counts != 0) {
                                        sendMouseScroll(counts.toFloat())
                                        scrollAccumulator.value -= counts
                                    }
                                }
                            }

                            inputChanges.forEach { it.consume() }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = AppIcons.MouseScrollUp,
                    contentDescription = stringResource(id = R.string.mouse_wheel_up),
                    modifier = Modifier.fillMaxHeight(0.35f),
                    tint = LocalContentColor.current.copy(alpha = 0.35f)
                )
                Icon(
                    imageVector = AppIcons.MouseScrollDown,
                    contentDescription = stringResource(id = R.string.mouse_wheel_down),
                    modifier = Modifier.fillMaxHeight(0.35f),
                    tint = LocalContentColor.current.copy(alpha = 0.35f)
                )
            }
        }
    }
}

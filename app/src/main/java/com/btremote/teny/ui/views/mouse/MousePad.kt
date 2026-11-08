package com.btremote.teny.ui.views.mouse

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import com.btremote.teny.common.utils.Ref
import com.btremote.teny.common.utils.TAP_DRIFT_MAX_PX
import com.btremote.teny.common.utils.TAP_DURATION_MAX
import com.btremote.teny.domain.entities.remoteInput.MouseAction
import com.btremote.teny.ui.components.DefaultElevatedCard

// A single HID mouse report carries 8-bit signed relative values (-127..127), so drag
// deltas are capped to that range and the cursor re-centering move is split into chunks
// that each fit into one report.
private const val HID_RELATIVE_MAX = 127f
private const val READING_RETURN_STEP = 100f

@Composable
fun MousePad(
    mouseSpeed: Float,
    scrollSpeed: Float,
    useReadingMode: Boolean,
    sendMouseAction: (input: MouseAction) -> Unit,
    sendMousePosition: (Float, Float) -> Unit,
    sendMouseScroll: (Float) -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    val tapStartTimestamp = remember { Ref(0L) }
    val tapFingerCount = remember { Ref(0) }
    val tapAccumulatedDrift = remember { Ref(0f) }
    val scrollAccumulator = remember { Ref(0f) }

    // Reading mode keeps the cursor close to where each swipe began so it can never
    // drift into a screen edge and stop scrolling halfway through a page.
    val readingDragActive = remember { Ref(false) }
    val readingDrift = remember { Ref(0f) }
    val readingAccX = remember { Ref(0f) }
    val readingAccY = remember { Ref(0f) }

    DefaultElevatedCard(
        modifier = modifier,
        shape = shape
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(mouseSpeed, scrollSpeed, useReadingMode) {
                    awaitEachGesture {
                        awaitFirstDown()
                        readingDragActive.value = false
                        readingDrift.value = 0f
                        readingAccX.value = 0f
                        readingAccY.value = 0f
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val inputChanges = event.changes

                                if (useReadingMode) {
                                    doReadingScroll(
                                        mouseSpeed = mouseSpeed,
                                        inputChanges = inputChanges,
                                        dragActive = readingDragActive,
                                        drift = readingDrift,
                                        accX = readingAccX,
                                        accY = readingAccY,
                                        sendMouseAction = sendMouseAction,
                                        sendMousePosition = sendMousePosition
                                    )
                                } else {
                                    doMove(
                                        mouseSpeed = mouseSpeed,
                                        inputChanges = inputChanges,
                                        sendMousePosition = sendMousePosition
                                    )
                                }

                                doTap(
                                    inputChanges = inputChanges,
                                    tapStartTimestamp = tapStartTimestamp.value,
                                    onTapStartTimestampChange = { tapStartTimestamp.value = it },
                                    tapFingerCount = tapFingerCount.value,
                                    onTapFingerCountChange = { tapFingerCount.value = it },
                                    tapAccumulatedDrift = tapAccumulatedDrift.value,
                                    onTapAccumulatedDriftChange = { tapAccumulatedDrift.value = it },
                                    sendMouseAction = sendMouseAction
                                )

                                doWheel(
                                    inputChanges = inputChanges,
                                    scrollSpeed = scrollSpeed,
                                    scrollAccumulator = scrollAccumulator,
                                    sendMouseScroll = sendMouseScroll
                                )

                                inputChanges.forEach { it.consume() }

                            }
                        } finally {
                            // If a gesture was interrupted before a clean lift, never leave the
                            // left button held down on the controlled device.
                            if (readingDragActive.value) {
                                finishReadingDrag(
                                    readingDragActive, readingDrift, readingAccX, readingAccY,
                                    sendMouseAction, sendMousePosition
                                )
                            }
                        }
                    }
                }
        )
    }
}

// ---- Actions ----

private fun doMove(
    mouseSpeed: Float,
    inputChanges: List<PointerInputChange>,
    sendMousePosition: (Float, Float) -> Unit
) {
    if(inputChanges.size != 1) return
    val delta: Offset = inputChanges.first().positionChange()
    sendMousePosition(delta.x * mouseSpeed, delta.y * mouseSpeed)
}

private fun doTap(
    inputChanges: List<PointerInputChange>,
    tapStartTimestamp: Long,
    onTapStartTimestampChange: (Long) -> Unit,
    tapFingerCount: Int,
    onTapFingerCountChange: (Int) -> Unit,
    tapAccumulatedDrift: Float,
    onTapAccumulatedDriftChange: (Float) -> Unit,
    sendMouseAction: (input: MouseAction) -> Unit
) {
    // Store the maximum number of fingers that simultaneously touched the pad, used to determine
    // the tap action (1 finger = left click, 2 fingers = right click, 3 fingers = middle click).
    if (inputChanges.size > tapFingerCount) {
        onTapFingerCountChange(inputChanges.size)
    }

    // If a first finger touches the pad, store the timestamp of the initial contact.
    val isFingerDown = inputChanges.any { it.changedToDown() }
    if (isFingerDown && tapStartTimestamp == 0L) {
        onTapStartTimestampChange(System.currentTimeMillis())
        return
    }

    // Track how much the fingers have moved on the pad. If they moved too much, the gesture will
    // not be considered a tap.
    onTapAccumulatedDriftChange(
        tapAccumulatedDrift + inputChanges.maxOf { it.positionChange().getDistance() }
    )

    // If all fingers are lifted, check if the gesture qualifies as a tap
    val isAllFingersUp = inputChanges.none { it.pressed }
    if (isAllFingersUp) {
        val tapDuration = System.currentTimeMillis() - tapStartTimestamp

        if (tapDuration < TAP_DURATION_MAX && tapAccumulatedDrift < TAP_DRIFT_MAX_PX) {
            when (tapFingerCount) {
                1 -> sendMouseAction(MouseAction.MOUSE_CLICK_LEFT)
                2 -> sendMouseAction(MouseAction.MOUSE_CLICK_RIGHT)
                3 -> sendMouseAction(MouseAction.MOUSE_CLICK_MIDDLE)
            }
            sendMouseAction(MouseAction.NONE)
        }

        // Reset state
        onTapStartTimestampChange(0L)
        onTapFingerCountChange(0)
        onTapAccumulatedDriftChange(0f)
    }
}

private fun doWheel(
    inputChanges: List<PointerInputChange>,
    scrollSpeed: Float,
    scrollAccumulator: Ref<Float>,
    sendMouseScroll: (Float) -> Unit
) {
    if(inputChanges.size != 2) return

    // Average the delta Y across all fingers using positionChange() for consistency
    val avgDeltaY = inputChanges
        .map { it.positionChange().y }
        .average()
        .toFloat()

    if (avgDeltaY == 0f) return // Nothing to scroll

    // Accumulate movement until a full scroll step is reached
    scrollAccumulator.value += avgDeltaY

    val steps = (scrollAccumulator.value / 20f).toInt()
    if (steps != 0) {
        sendMouseScroll(steps * scrollSpeed)
        sendMouseScroll(0f)
        scrollAccumulator.value -= steps * 20f
    }
}

// ---- Reading mode ----

/**
 * Replaces plain cursor movement while reading mode is on.
 *
 * A single-finger swipe becomes a continuous drag: the left button is held automatically
 * and the finger movement is sent as relative mouse movement. The controlled device reads
 * that as a smooth touch-drag (page scroll) instead of the steppy wheel notches. A short
 * tap stays a normal click, because doTap still decides taps and its drift stays under
 * the tap threshold.
 *
 * When the finger lifts (or a second finger joins), the button is released FIRST and then
 * the accumulated displacement is sent back with the button up. A button-up mouse move
 * only repositions the cursor and does NOT scroll, so the cursor returns to roughly where
 * the swipe began: every swipe starts from the same spot and never runs into a screen edge.
 */
private fun doReadingScroll(
    mouseSpeed: Float,
    inputChanges: List<PointerInputChange>,
    dragActive: Ref<Boolean>,
    drift: Ref<Float>,
    accX: Ref<Float>,
    accY: Ref<Float>,
    sendMouseAction: (MouseAction) -> Unit,
    sendMousePosition: (Float, Float) -> Unit
) {
    // Multi-finger gestures keep their original handling (doTap / doWheel).
    if (inputChanges.size != 1) {
        finishReadingDrag(dragActive, drift, accX, accY, sendMouseAction, sendMousePosition)
        return
    }

    val change = inputChanges.first()

    // Match doTap: the first down event starts the gesture but carries no movement. Skipping it
    // keeps this drift identical to doTap's tapAccumulatedDrift, so a drag can NEVER be mistaken
    // for a tap and fire a stray click right when the left button is auto-held.
    if (change.changedToDown()) return

    if (!change.pressed) {
        // Finger lifted: end the drag (or simply reset state when it stayed a tap).
        finishReadingDrag(dragActive, drift, accX, accY, sendMouseAction, sendMousePosition)
        return
    }

    val delta: Offset = change.positionChange()
    drift.value += delta.getDistance()

    if (!dragActive.value) {
        // Below the tap threshold this could still be a tap: send nothing and let doTap decide.
        if (drift.value < TAP_DRIFT_MAX_PX) return
        // The gesture is a drag: hold the left button for its whole duration.
        dragActive.value = true
        sendMouseAction(MouseAction.MOUSE_CLICK_LEFT)
    }

    val dx = (delta.x * mouseSpeed).coerceIn(-HID_RELATIVE_MAX, HID_RELATIVE_MAX)
    val dy = (delta.y * mouseSpeed).coerceIn(-HID_RELATIVE_MAX, HID_RELATIVE_MAX)
    if (dx != 0f || dy != 0f) {
        sendMousePosition(dx, dy)
        accX.value += dx
        accY.value += dy
    }
}

private fun finishReadingDrag(
    dragActive: Ref<Boolean>,
    drift: Ref<Float>,
    accX: Ref<Float>,
    accY: Ref<Float>,
    sendMouseAction: (MouseAction) -> Unit,
    sendMousePosition: (Float, Float) -> Unit
) {
    if (dragActive.value) {
        // Release the button before re-centering, so the return move does not scroll.
        sendMouseAction(MouseAction.NONE)

        var remainingX = accX.value
        var remainingY = accY.value
        while (remainingX != 0f || remainingY != 0f) {
            val stepX = remainingX.coerceIn(-READING_RETURN_STEP, READING_RETURN_STEP)
            val stepY = remainingY.coerceIn(-READING_RETURN_STEP, READING_RETURN_STEP)
            sendMousePosition(-stepX, -stepY)
            remainingX -= stepX
            remainingY -= stepY
        }
        dragActive.value = false
    }
    drift.value = 0f
    accX.value = 0f
    accY.value = 0f
}

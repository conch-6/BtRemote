package com.btremote.teny.ui.views.mouse

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
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// A single HID mouse report carries 8-bit signed relative values (-127..127), so drag
// deltas are capped to that range and the re-centering move is split into chunks that
// each fit into one report.
private const val HID_RELATIVE_MAX = 127f
private const val HID_REPORT_CHUNK_MAX = 127

// Absolute cursor re-centering for reading mode. A relative mouse cannot address a
// position - the controlled device never tells us where the cursor is - so the return
// borrows the screen edges as a reference frame (see finishReadingDrag): flood the
// cursor into the top-left corner, then walk half the panel to the exact centre.
// RECENTER_CORNER_COUNTS must exceed the farthest possible corner distance of the panel
// (1080+2400 = 3480 counts) even at a low pointer-speed gain (~0.45x).
private const val RECENTER_CORNER_COUNTS = 8000f

// Half of the controlled phone's panel (1080 x 2400 portrait), in report counts. This
// assumes the controlled device's pointer speed is at its default 1:1 scale. Update
// these when targeting another panel or when reading in landscape.
private const val RECENTER_HALF_WIDTH = 540f
private const val RECENTER_HALF_HEIGHT = 1200f

// A failed report is retried with an exponential backoff (2/4/8/16ms); this many
// consecutive failures means Bluetooth most likely dropped - give up and let the next
// swipe re-center again instead of spinning forever.
private const val RECENTER_SEND_MAX_ATTEMPTS = 10
private const val RECENTER_RETRY_DELAY_MS = 2L

@Composable
fun MousePad(
    mouseSpeed: Float,
    scrollSpeed: Float,
    useReadingMode: Boolean,
    sendMouseAction: (input: MouseAction) -> Unit,
    sendMousePosition: (Float, Float) -> Boolean,
    sendMouseScroll: (Float) -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    val tapStartTimestamp = remember { Ref(0L) }
    val tapFingerCount = remember { Ref(0) }
    val tapAccumulatedDrift = remember { Ref(0f) }
    val scrollAccumulator = remember { Ref(0f) }

    // Reading mode re-centers the cursor to the exact screen centre after every swipe so
    // it can never drift into a screen edge and stop scrolling halfway through a page.
    val readingDragActive = remember { Ref(false) }
    val readingDrift = remember { Ref(0f) }
    // Set when the drag's button was released (lift or second finger) and the re-centering
    // return is still owed. The return runs outside the event scope, see below.
    val readingReturnPending = remember { Ref(false) }

    DefaultElevatedCard(
        modifier = modifier,
        shape = shape
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(mouseSpeed, scrollSpeed, useReadingMode) {
                    // One gesture per loop iteration. The gesture lives inside the restricted
                    // AwaitPointerEventScope (which only allows its own suspending calls), while
                    // the re-centering return needs plain delay() for its retry backoff, so it
                    // runs OUTSIDE, in the loop below. Events that arrive while the return is
                    // sending simply queue and are picked up by the next awaitFirstDown(), which
                    // also guarantees the return reports and the next gesture's button press
                    // never interleave.
                    while (true) {
                        try {
                            awaitPointerEventScope {
                                awaitFirstDown()
                                readingDragActive.value = false
                                readingDrift.value = 0f
                                readingReturnPending.value = false
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val inputChanges = event.changes

                                    if (useReadingMode) {
                                        doReadingScroll(
                                            mouseSpeed = mouseSpeed,
                                            inputChanges = inputChanges,
                                            dragActive = readingDragActive,
                                            drift = readingDrift,
                                            returnPending = readingReturnPending,
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

                                    // The gesture is over once every pointer is up. A second
                                    // finger keeps the loop alive so doWheel can scroll on.
                                    if (inputChanges.all { !it.pressed }) break
                                }
                            }
                        } finally {
                            // If a gesture was interrupted before a clean lift, never leave the
                            // left button held down on the controlled device (doReadingScroll
                            // already releases it on a clean lift or second finger).
                            if (readingDragActive.value) {
                                sendMouseAction(MouseAction.NONE)
                                readingReturnPending.value = true
                            }
                            if (readingReturnPending.value) {
                                finishReadingDrag(readingReturnPending, sendMousePosition)
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
    sendMousePosition: (Float, Float) -> Boolean
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
 * When the finger lifts (or a second finger joins), the button is released HERE, so a
 * following two-finger wheel scroll cannot drag, and the cursor return is merely flagged:
 * it runs after the gesture loop exits, because its retry backoff needs delay(), which is
 * forbidden inside the restricted event scope (see finishReadingDrag).
 */
private fun doReadingScroll(
    mouseSpeed: Float,
    inputChanges: List<PointerInputChange>,
    dragActive: Ref<Boolean>,
    drift: Ref<Float>,
    returnPending: Ref<Boolean>,
    sendMouseAction: (MouseAction) -> Unit,
    sendMousePosition: (Float, Float) -> Boolean
) {
    // Multi-finger gestures keep their original handling (doTap / doWheel); the held
    // button is just released so the two-finger wheel scroll cannot drag.
    if (inputChanges.size != 1) {
        if (dragActive.value) {
            sendMouseAction(MouseAction.NONE)
            dragActive.value = false
            returnPending.value = true
        }
        return
    }

    val change = inputChanges.first()

    // Match doTap: the first down event starts the gesture but carries no movement. Skipping it
    // keeps this drift identical to doTap's tapAccumulatedDrift, so a drag can NEVER be mistaken
    // for a tap and fire a stray click right when the left button is auto-held.
    if (change.changedToDown()) return

    if (!change.pressed) {
        // Finger lifted: release the button; the paced return runs after the gesture loop.
        if (dragActive.value) {
            sendMouseAction(MouseAction.NONE)
            dragActive.value = false
            returnPending.value = true
        }
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
        // The return move is absolute (corner + half panel) and does not depend on the
        // drag's history, so there is nothing to track here.
        sendMousePosition(dx, dy)
    }
}

/**
 * Absolute cursor re-centering: brings the cursor to the exact centre of the controlled
 * screen, no matter where the drag left it or how fast the finger was moving.
 *
 * A relative mouse has no way to say "go to position X,Y" - the device never tells us
 * where the cursor is. So the return borrows the screen edges as a reference frame:
 *
 *  1. Flood the cursor into the TOP-LEFT corner with a huge relative move. Excess counts
 *     are clamped by the screen itself, so this phase can never overshoot, and it is
 *     immune to the device's pointer-speed scaling.
 *  2. From the corner, walk exactly half the panel width and height. The centre is a
 *     known constant offset from the corner.
 *
 * The controlled device is an Android phone without pointer acceleration, so reports can
 * be sent at full speed without affecting accuracy: every report moves the cursor by a
 * fixed counts-to-pixels scale regardless of pacing. Each report's delivery is confirmed
 * by sendMousePosition and retried on failure, so nothing is silently dropped.
 *
 * Must run OUTSIDE the restricted AwaitPointerEventScope: it suspends in delay() while
 * retrying. The gesture loop only gets here after the gesture's events are fully handled,
 * and the next gesture cannot start until this returns, so reports stay ordered.
 */
private suspend fun finishReadingDrag(
    returnPending: Ref<Boolean>,
    sendMousePosition: (Float, Float) -> Boolean
) {
    // Flood into the top-left corner, then walk half the panel to the exact centre.
    sendAbsoluteCounts(-RECENTER_CORNER_COUNTS, -RECENTER_CORNER_COUNTS, sendMousePosition)
    sendAbsoluteCounts(RECENTER_HALF_WIDTH, RECENTER_HALF_HEIGHT, sendMousePosition)
    returnPending.value = false
}

/**
 * Sends (totalX, totalY) relative counts as a sequence of one-report chunks, confirming
 * each chunk and retrying failures with an exponential backoff (2/4/8/16ms). Successful
 * sends are followed immediately by the next chunk - as fast as Bluetooth accepts them.
 * Giving up after RECENTER_SEND_MAX_ATTEMPTS consecutive failures keeps this bounded when
 * Bluetooth has most likely dropped; the next swipe simply re-centers again.
 */
private suspend fun sendAbsoluteCounts(
    totalX: Float,
    totalY: Float,
    sendMousePosition: (Float, Float) -> Boolean
) {
    val targetX = totalX.roundToInt()
    val targetY = totalY.roundToInt()
    var sentX = 0
    var sentY = 0
    while (sentX != targetX || sentY != targetY) {
        val stepX = (targetX - sentX).coerceIn(-HID_REPORT_CHUNK_MAX, HID_REPORT_CHUNK_MAX)
        val stepY = (targetY - sentY).coerceIn(-HID_REPORT_CHUNK_MAX, HID_REPORT_CHUNK_MAX)
        var attempt = 0
        while (true) {
            if (sendMousePosition(stepX.toFloat(), stepY.toFloat())) break
            attempt++
            if (attempt >= RECENTER_SEND_MAX_ATTEMPTS) return
            delay(RECENTER_RETRY_DELAY_MS shl (attempt - 1).coerceAtMost(3))
        }
        sentX += stepX
        sentY += stepY
    }
}

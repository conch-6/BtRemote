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
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt

// A single HID mouse report carries 8-bit signed relative values (-127..127), so drag
// deltas are capped to that range and the cursor re-centering move is split into chunks
// that each fit into one report.
private const val HID_RELATIVE_MAX = 127f
private const val READING_RETURN_STEP = 100f

// The re-centering return is PACED, not instantaneous. Pointer gain on the controlled
// device depends on movement speed (pointer ballistics), so the old instantaneous burst
// return was amplified relative to the finger-paced drag: short drifts overshot, fast
// flicks undershot, and reversal gestures (mixed speeds) were the most erratic. Sending
// each return report READING_RETURN_DELAY_MS after the previous one, with counts sized
// from the drag's average step (see finishReadingDrag), makes the return travel at the
// drag's average speed. Both phases then get the same gain treatment and the landing
// point no longer depends on how fast the finger happened to move.
private const val READING_RETURN_DELAY_MS = 8L
private const val READING_DRAG_FRAME_MS = 1000f / 60f

// Fraction of the accumulated drag displacement sent back after the finger lifts.
// 1.0 is physically exact when the device gain is purely speed-dependent (the common
// case: pointer ballistics apply equally to held and free moves). If real-device testing
// shows the cursor consistently overshooting the start (lands mirrored past it), the
// device also halves held moves - set this to 0.5f.
private const val READING_RETURN_SCALE = 1.0f

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
    // drift into a screen edge and stop scrolling halfway through a page. The return
    // move is paced at the drag's own speed (see finishReadingDrag), so the drag keeps
    // wire-truth stats: how many reports were sent and how far they travelled in total.
    val readingDragActive = remember { Ref(false) }
    val readingDrift = remember { Ref(0f) }
    val readingAccX = remember { Ref(0f) }
    val readingAccY = remember { Ref(0f) }
    val readingReportCount = remember { Ref(0) }
    val readingDistanceSum = remember { Ref(0f) }

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
                        readingReportCount.value = 0
                        readingDistanceSum.value = 0f
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
                                        reportCount = readingReportCount,
                                        distanceSum = readingDistanceSum,
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
                                    readingReportCount, readingDistanceSum,
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
 * the cursor is returned with button-up moves, which only reposition the cursor and do
 * NOT scroll. The return travels straight back towards where the swipe began, paced at
 * the drag's own speed (see finishReadingDrag), so every swipe starts from the same spot
 * and never runs into a screen edge.
 */
private suspend fun doReadingScroll(
    mouseSpeed: Float,
    inputChanges: List<PointerInputChange>,
    dragActive: Ref<Boolean>,
    drift: Ref<Float>,
    accX: Ref<Float>,
    accY: Ref<Float>,
    reportCount: Ref<Int>,
    distanceSum: Ref<Float>,
    sendMouseAction: (MouseAction) -> Unit,
    sendMousePosition: (Float, Float) -> Unit
) {
    // Multi-finger gestures keep their original handling (doTap / doWheel).
    if (inputChanges.size != 1) {
        finishReadingDrag(dragActive, drift, accX, accY, reportCount, distanceSum, sendMouseAction, sendMousePosition)
        return
    }

    val change = inputChanges.first()

    // Match doTap: the first down event starts the gesture but carries no movement. Skipping it
    // keeps this drift identical to doTap's tapAccumulatedDrift, so a drag can NEVER be mistaken
    // for a tap and fire a stray click right when the left button is auto-held.
    if (change.changedToDown()) return

    if (!change.pressed) {
        // Finger lifted: end the drag (or simply reset state when it stayed a tap).
        finishReadingDrag(dragActive, drift, accX, accY, reportCount, distanceSum, sendMouseAction, sendMousePosition)
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
        // The return move must undo exactly what the device received, and it must travel at
        // the drag's own speed, so the stats track the WIRE truth: the integer counts that
        // sendMousePosition actually delivers (it rounds to Int), not the raw finger deltas.
        val wireDx = dx.roundToInt()
        val wireDy = dy.roundToInt()
        sendMousePosition(dx, dy)
        accX.value += wireDx
        accY.value += wireDy
        reportCount.value += 1
        distanceSum.value += sqrt((wireDx * wireDx + wireDy * wireDy).toFloat())
    }
}

/**
 * Releases the button and returns the cursor to where the swipe began.
 *
 * The return is PACED instead of instantaneous. Pointer gain on the controlled device
 * depends on movement speed (pointer ballistics), so an instantaneous burst of reports
 * is amplified relative to the finger-paced drag - that mismatch is what made the old
 * fixed-scale return land short on flicks and past the start on slow swipes, and it hit
 * reversal gestures (fast out, slow back) the hardest.
 *
 * Instead, the total return counts are READING_RETURN_SCALE times the wire-truth net
 * displacement, distributed losslessly with running-total rounding, and the per-report
 * counts are sized from the drag's average step so the return travels at the drag's own
 * speed. Both phases then receive the same gain treatment and the landing point stops
 * depending on how fast the finger happened to move.
 */
private suspend fun finishReadingDrag(
    dragActive: Ref<Boolean>,
    drift: Ref<Float>,
    accX: Ref<Float>,
    accY: Ref<Float>,
    reportCount: Ref<Int>,
    distanceSum: Ref<Float>,
    sendMouseAction: (MouseAction) -> Unit,
    sendMousePosition: (Float, Float) -> Unit
) {
    if (dragActive.value) {
        // Release the button before re-centering, so the return move does not scroll.
        sendMouseAction(MouseAction.NONE)

        val targetX = -accX.value * READING_RETURN_SCALE
        val targetY = -accY.value * READING_RETURN_SCALE
        val avgDragStep = if (reportCount.value > 0) distanceSum.value / reportCount.value else 0f
        // Scaling the drag's average step by the pacing ratio (delay / frame time) gives
        // the return the same counts-per-ms velocity the drag had on the controlled device.
        val returnStep = (avgDragStep * READING_RETURN_DELAY_MS / READING_DRAG_FRAME_MS)
            .coerceIn(1f, READING_RETURN_STEP)
        val steps = ceil(sqrt(targetX * targetX + targetY * targetY) / returnStep).toInt()

        var sentX = 0
        var sentY = 0
        for (i in 1..steps) {
            // Running-total rounding distributes the fractional counts losslessly: the
            // emitted deltas always add up to the rounded target, never drift by rounding.
            val x = (targetX * i / steps).roundToInt()
            val y = (targetY * i / steps).roundToInt()
            if (x != sentX || y != sentY) {
                sendMousePosition((x - sentX).toFloat(), (y - sentY).toFloat())
                sentX = x
                sentY = y
            }
            if (i < steps) delay(READING_RETURN_DELAY_MS)
        }
        dragActive.value = false
    }
    drift.value = 0f
    accX.value = 0f
    accY.value = 0f
    reportCount.value = 0
    distanceSum.value = 0f
}

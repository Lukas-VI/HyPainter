package io.github.lukasvi.hypainter

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Represents the current visible viewport transformation for the canvas.
 *
 * @param pan translation offset of the canvas in screen coordinates.
 * @param scale zoom factor applied to the canvas.
 * @param rotation rotation angle of the canvas in degrees.
 */
internal data class ViewportState(
    val pan: Offset = Offset.Zero,
    val scale: Float = 1f,
    val rotation: Float = 0f,
) {

    /**
     * Calculate a new viewport state from a gesture update.
     *
     * This method keeps the gesture centroid anchored while applying pinch zoom
     * and rotation deltas from the previous gesture frame to the next one.
     */
    fun transformAround(previous: TouchGestureFrame, next: TouchGestureFrame): ViewportState {
        // Convert the previous gesture centroid from screen coordinates into canvas coordinates.
        val canvasAnchor = toCanvas(previous.centroid)

        // Scale based on the ratio of the next gesture distance to the previous one.
        // Clamp the resulting scale so zoom stays within reasonable bounds.
        val nextScale = (scale * next.distance / previous.distance).coerceIn(0.25f, 8f)

        // Apply the rotation delta from the gesture.
        val nextRotation = rotation + next.angleDegrees - previous.angleDegrees

        // The next centroid position in screen coordinates should remain the anchor point.
        val desiredAnchor = next.centroid

        // Compute the new pan so the same canvas point stays under the gesture centroid.
        val nextPan = desiredAnchor - rotate(canvasAnchor * nextScale, nextRotation)

        return ViewportState(
            pan = nextPan,
            scale = nextScale,
            rotation = nextRotation,
        )
    }

    /**
     * Convert a point from screen space into canvas space.
     */
    fun toCanvas(screen: Offset): Offset {
        return rotate(screen - pan, -rotation) / scale
    }

    /**
     * Convert a point from canvas space into screen space.
     */
    fun toScreen(canvas: Offset): Offset {
        return pan + rotate(canvas * scale, rotation)
    }
}

/**
 * Gesture state at a single frame of touch input.
 *
 * @param centroid the midpoint of the touch points in screen coordinates.
 * @param distance the distance between touch points, used for pinch zoom.
 * @param angleDegrees the rotation angle between touch points in degrees.
 */
internal data class TouchGestureFrame(
    val centroid: Offset,
    val distance: Float,
    val angleDegrees: Float,
)

/**
 * Rotate a 2D offset by a given amount of degrees around the origin.
 */
internal fun rotate(offset: Offset, degrees: Float): Offset {
    val radians = degrees * PI.toFloat() / 180f
    val cosValue = cos(radians)
    val sinValue = sin(radians)
    return Offset(
        x = offset.x * cosValue - offset.y * sinValue,
        y = offset.x * sinValue + offset.y * cosValue,
    )
}

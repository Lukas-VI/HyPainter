package io.github.lukasvi.hypainter

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal data class ViewportState(
    val pan: Offset = Offset.Zero,
    val scale: Float = 1f,
    val rotation: Float = 0f,
) {
    fun transformAround(previous: TouchGestureFrame, next: TouchGestureFrame): ViewportState {
        val canvasAnchor = toCanvas(previous.centroid)
        val nextScale = (scale * next.distance / previous.distance).coerceIn(0.25f, 8f)
        val nextRotation = rotation + next.angleDegrees - previous.angleDegrees
        val desiredAnchor = next.centroid
        val nextPan = desiredAnchor - rotate(canvasAnchor * nextScale, nextRotation)
        return ViewportState(
            pan = nextPan,
            scale = nextScale,
            rotation = nextRotation,
        )
    }

    fun toCanvas(screen: Offset): Offset {
        return rotate(screen - pan, -rotation) / scale
    }

    fun toScreen(canvas: Offset): Offset {
        return pan + rotate(canvas * scale, rotation)
    }
}

internal data class TouchGestureFrame(
    val centroid: Offset,
    val distance: Float,
    val angleDegrees: Float,
)

internal fun rotate(offset: Offset, degrees: Float): Offset {
    val radians = degrees * PI.toFloat() / 180f
    val cosValue = cos(radians)
    val sinValue = sin(radians)
    return Offset(
        x = offset.x * cosValue - offset.y * sinValue,
        y = offset.x * sinValue + offset.y * cosValue,
    )
}

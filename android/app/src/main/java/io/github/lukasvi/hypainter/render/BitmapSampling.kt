package io.github.lukasvi.hypainter.render

import androidx.compose.ui.graphics.FilterQuality

internal enum class BitmapSampling {
    PixelPerfect,
    Nearest,
    Linear,
    Bilinear,
    Bicubic,
}

internal data class CanvasRenderOptions(
    val bitmapSampling: BitmapSampling = BitmapSampling.PixelPerfect,
)

internal fun BitmapSampling.toFilterQuality(): FilterQuality {
    return when (this) {
        BitmapSampling.PixelPerfect,
        BitmapSampling.Nearest -> FilterQuality.None
        BitmapSampling.Linear,
        BitmapSampling.Bilinear -> FilterQuality.Medium
        BitmapSampling.Bicubic -> FilterQuality.High
    }
}

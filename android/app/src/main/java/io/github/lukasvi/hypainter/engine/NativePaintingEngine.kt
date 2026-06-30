package io.github.lukasvi.hypainter.engine

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

class NativePaintingEngine private constructor(
    private val handle: Long,
    private val canvasWidth: Int,
    private val canvasHeight: Int,
) : PaintingEngine {
    override val nativeBacked: Boolean = true

    private val fallbackPreview = KotlinPaintingEngine(canvasWidth, canvasHeight)
    private var renderedImage: ImageBitmap? = null

    override fun beginStroke(sample: EngineSample) {
        fallbackPreview.beginStroke(sample)
    }

    override fun appendSample(sample: EngineSample) {
        fallbackPreview.appendSample(sample)
    }

    override fun endStroke() {
        val stroke = fallbackPreview.snapshot().activeStroke ?: return
        val samples = FloatArray(stroke.points.size * SAMPLE_STRIDE)
        stroke.points.forEachIndexed { index, sample ->
            val offset = index * SAMPLE_STRIDE
            samples[offset] = sample.position.x
            samples[offset + 1] = sample.position.y
            samples[offset + 2] = sample.pressure
            samples[offset + 3] = sample.tiltX
            samples[offset + 4] = sample.tiltY
            samples[offset + 5] = sample.timestamp.toFloat()
        }
        nativeAppendStroke(handle, samples)
        fallbackPreview.clear()
        refreshRenderedImage()
    }

    override fun undo() {
        if (nativeUndo(handle)) {
            refreshRenderedImage()
        }
    }

    override fun clear() {
        nativeClear(handle)
        fallbackPreview.clear()
        renderedImage = null
    }

    override fun snapshot(): EngineSnapshot {
        val preview = fallbackPreview.snapshot()
        return EngineSnapshot(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            committedStrokes = emptyList(),
            activeStroke = preview.activeStroke,
            renderedImage = renderedImage,
        )
    }

    @Suppress("unused")
    fun renderRgba(): ByteArray {
        return nativeRenderRgba(handle)
    }

    private fun refreshRenderedImage() {
        renderedImage = rgbaToImageBitmap(nativeRenderRgba(handle), canvasWidth, canvasHeight)
    }

    protected fun finalize() {
        nativeDestroy(handle)
    }

    companion object {
        private const val SAMPLE_STRIDE = 6
        private var libraryLoaded = false

        fun createOrNull(width: Int, height: Int): NativePaintingEngine? {
            if (!ensureLibraryLoaded()) {
                return null
            }

            val handle = nativeCreate(width, height)
            return if (handle == 0L) null else NativePaintingEngine(handle, width, height)
        }

        private fun ensureLibraryLoaded(): Boolean {
            if (libraryLoaded) {
                return true
            }

            return try {
                System.loadLibrary("hyp_ffi")
                libraryLoaded = true
                true
            } catch (_: UnsatisfiedLinkError) {
                false
            }
        }

        @JvmStatic
        private external fun nativeCreate(width: Int, height: Int): Long

        @JvmStatic
        private external fun nativeDestroy(handle: Long)

        @JvmStatic
        private external fun nativeAppendStroke(handle: Long, samples: FloatArray): Boolean

        @JvmStatic
        private external fun nativeClear(handle: Long): Boolean

        @JvmStatic
        private external fun nativeUndo(handle: Long): Boolean

        @JvmStatic
        private external fun nativeRenderRgba(handle: Long): ByteArray

        private fun rgbaToImageBitmap(bytes: ByteArray, width: Int, height: Int): ImageBitmap? {
            val expectedLength = width * height * 4
            if (bytes.size != expectedLength) {
                return null
            }

            val pixels = IntArray(width * height)
            var source = 0
            for (index in pixels.indices) {
                val r = bytes[source].toInt() and 0xff
                val g = bytes[source + 1].toInt() and 0xff
                val b = bytes[source + 2].toInt() and 0xff
                val a = bytes[source + 3].toInt() and 0xff
                pixels[index] = (a shl 24) or (r shl 16) or (g shl 8) or b
                source += 4
            }

            return Bitmap
                .createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
                .asImageBitmap()
        }
    }
}

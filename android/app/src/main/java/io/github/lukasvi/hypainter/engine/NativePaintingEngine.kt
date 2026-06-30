package io.github.lukasvi.hypainter.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.FileOutputStream

class NativePaintingEngine private constructor(
    private val handle: Long,
    private val canvasWidth: Int,
    private val canvasHeight: Int,
) : PaintingEngine {
    override val nativeBacked: Boolean = true

    private val fallbackPreview = KotlinPaintingEngine(canvasWidth, canvasHeight)
    private var renderedImage: ImageBitmap? = null
    private var renderedBitmap: Bitmap? = null
    private var brush = EngineBrush(colorArgb = AndroidColor.BLACK, radiusPx = 8f)
    private val committedStrokes = mutableListOf<EngineStroke>()

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
        committedStrokes.add(stroke)
        fallbackPreview.clear()
        refreshRenderedImage()
    }

    override fun undo() {
        if (nativeUndo(handle)) {
            if (committedStrokes.isNotEmpty()) {
                committedStrokes.removeAt(committedStrokes.lastIndex)
            }
            refreshRenderedImage()
        }
    }

    override fun clear() {
        nativeClear(handle)
        fallbackPreview.clear()
        committedStrokes.clear()
        renderedImage = null
        renderedBitmap = null
    }

    override fun setBrush(brush: EngineBrush) {
        this.brush = brush
        fallbackPreview.setBrush(brush)
        nativeSetBrush(
            handle = handle,
            radiusPx = brush.radiusPx,
            r = (brush.colorArgb shr 16) and 0xff,
            g = (brush.colorArgb shr 8) and 0xff,
            b = brush.colorArgb and 0xff,
            a = (brush.colorArgb ushr 24) and 0xff,
        )
    }

    override fun exportPng(path: String): Boolean {
        return runCatching {
            val bitmap = renderedBitmap ?: Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
                .also { Canvas(it).drawColor(AndroidColor.WHITE) }
            FileOutputStream(path).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        }.getOrDefault(false)
    }

    override fun saveProject(path: String): Boolean {
        return ProjectCodec.save(path, snapshot())
    }

    override fun loadProject(path: String): Boolean {
        val project = ProjectCodec.load(path) ?: return false
        clear()
        project.strokes.forEach { stroke ->
            setBrush(stroke.brush)
            appendNativeStroke(stroke)
            committedStrokes.add(stroke)
        }
        refreshRenderedImage()
        return true
    }

    override fun snapshot(): EngineSnapshot {
        val preview = fallbackPreview.snapshot()
        return EngineSnapshot(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            brush = brush,
            committedStrokes = committedStrokes.toList(),
            activeStroke = preview.activeStroke,
            renderedImage = renderedImage,
        )
    }

    @Suppress("unused")
    fun renderRgba(): ByteArray {
        return nativeRenderRgba(handle)
    }

    private fun refreshRenderedImage() {
        renderedBitmap = rgbaToBitmap(nativeRenderRgba(handle), canvasWidth, canvasHeight)
        renderedImage = renderedBitmap?.asImageBitmap()
    }

    private fun appendNativeStroke(stroke: EngineStroke): Boolean {
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
        return nativeAppendStroke(handle, samples)
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
        private external fun nativeSetBrush(
            handle: Long,
            radiusPx: Float,
            r: Int,
            g: Int,
            b: Int,
            a: Int,
        ): Boolean

        @JvmStatic
        private external fun nativeRenderRgba(handle: Long): ByteArray

        private fun rgbaToBitmap(bytes: ByteArray, width: Int, height: Int): Bitmap? {
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

            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        }
    }
}

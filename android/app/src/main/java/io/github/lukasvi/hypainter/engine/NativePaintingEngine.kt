package io.github.lukasvi.hypainter.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import java.io.FileOutputStream

class NativePaintingEngine private constructor(
    private val handle: Long,
    private val canvasWidth: Int,
    private val canvasHeight: Int,
) : PaintingEngine {
    override val nativeBacked: Boolean = true

    private val fallbackPreview = KotlinPaintingEngine(canvasWidth, canvasHeight)
    private val displayCache = StrokeRasterCache(canvasWidth, canvasHeight)
    private var renderedBitmap: Bitmap? = null
    private var renderedBitmapDirty = false
    private var brush = EngineBrush(colorArgb = AndroidColor.BLACK, radiusPx = 8f)
    private val committedStrokes = mutableListOf<EngineStroke>()
    private val layers = mutableListOf(EngineLayer(id = 1L, name = "Layer 1", visible = true))
    private var activeLayerId = 1L
    private var nextLayerId = 2L

    override fun beginStroke(sample: EngineSample) {
        fallbackPreview.beginStroke(sample)
    }

    override fun appendSample(sample: EngineSample) {
        fallbackPreview.appendSample(sample)
    }

    override fun endStroke() {
        val stroke = fallbackPreview.snapshot().activeStroke ?: return
        val committedStroke = stroke.stableCopyForLayer(activeLayerId)
        val samples = FloatArray(committedStroke.points.size * SAMPLE_STRIDE)
        committedStroke.points.forEachIndexed { index, sample ->
            val offset = index * SAMPLE_STRIDE
            samples[offset] = sample.position.x
            samples[offset + 1] = sample.position.y
            samples[offset + 2] = sample.pressure
            samples[offset + 3] = sample.tiltX
            samples[offset + 4] = sample.tiltY
            samples[offset + 5] = sample.timestamp.toFloat()
        }
        nativeAppendStroke(handle, samples)
        committedStrokes.add(committedStroke)
        displayCache.append(committedStroke, layers)
        fallbackPreview.clear()
        markRenderedBitmapDirty()
    }

    override fun undo() {
        if (nativeUndo(handle)) {
            if (committedStrokes.isNotEmpty()) {
                committedStrokes.removeAt(committedStrokes.lastIndex)
            }
            displayCache.rebuild(committedStrokes, layers)
            markRenderedBitmapDirty()
        }
    }

    override fun clear() {
        nativeClear(handle)
        fallbackPreview.clear()
        committedStrokes.clear()
        displayCache.clear()
        layers.clear()
        layers.add(EngineLayer(id = 1L, name = "Layer 1", visible = true))
        activeLayerId = 1L
        nextLayerId = 2L
        renderedBitmap = null
        renderedBitmapDirty = false
    }

    override fun addLayer() {
        val id = nextLayerId++
        layers.add(EngineLayer(id = id, name = "Layer ${layers.size + 1}", visible = true))
        activeLayerId = id
    }

    override fun selectLayer(layerId: Long) {
        if (layers.any { it.id == layerId }) {
            activeLayerId = layerId
        }
    }

    override fun toggleLayerVisibility(layerId: Long) {
        val index = layers.indexOfFirst { it.id == layerId }
        if (index >= 0) {
            val layer = layers[index]
            layers[index] = layer.copy(visible = !layer.visible)
            displayCache.rebuild(committedStrokes, layers)
            rebuildNativeDocument()
        }
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
            val bitmap = ensureRenderedBitmap() ?: Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
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
        layers.clear()
        layers.addAll(project.layers)
        activeLayerId = project.activeLayerId
        nextLayerId = (layers.maxOfOrNull { it.id } ?: 0L) + 1L
        project.strokes.forEach { stroke ->
            committedStrokes.add(stroke)
        }
        displayCache.rebuild(committedStrokes, layers)
        rebuildNativeDocument()
        return true
    }

    override fun snapshot(): EngineSnapshot {
        val preview = fallbackPreview.snapshot()
        return EngineSnapshot(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            brush = brush,
            layers = layers.toList(),
            activeLayerId = activeLayerId,
            committedStrokes = committedStrokes.toList(),
            activeStroke = preview.activeStroke?.copy(layerId = activeLayerId),
            renderedImage = displayCache.renderedImage,
            activeImage = preview.activeImage,
        )
    }

    override fun canvasSnapshot(): EngineSnapshot {
        val preview = fallbackPreview.canvasSnapshot()
        return EngineSnapshot(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            brush = brush,
            layers = layers,
            activeLayerId = activeLayerId,
            committedStrokes = emptyList(),
            activeStroke = preview.activeStroke?.copy(layerId = activeLayerId),
            renderedImage = displayCache.renderedImage,
            activeImage = preview.activeImage,
        )
    }

    @Suppress("unused")
    fun renderRgba(): ByteArray {
        return nativeRenderRgba(handle)
    }

    private fun refreshRenderedImage() {
        renderedBitmap = rgbaToBitmap(nativeRenderRgba(handle), canvasWidth, canvasHeight)
        renderedBitmapDirty = false
    }

    private fun ensureRenderedBitmap(): Bitmap? {
        if (renderedBitmap == null || renderedBitmapDirty) {
            refreshRenderedImage()
        }
        return renderedBitmap
    }

    private fun markRenderedBitmapDirty() {
        renderedBitmap = null
        renderedBitmapDirty = true
    }

    private fun rebuildNativeDocument() {
        nativeClear(handle)
        val currentBrush = brush
        committedStrokes
            .filter { stroke -> layers.firstOrNull { it.id == stroke.layerId }?.visible == true }
            .forEach { stroke ->
                setBrush(stroke.brush)
                appendNativeStroke(stroke)
            }
        setBrush(currentBrush)
        markRenderedBitmapDirty()
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

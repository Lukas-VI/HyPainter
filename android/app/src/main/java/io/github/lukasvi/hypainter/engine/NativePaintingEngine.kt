package io.github.lukasvi.hypainter.engine

class NativePaintingEngine private constructor(
    private val handle: Long,
) : PaintingEngine {
    override val nativeBacked: Boolean = true

    private val fallbackPreview = KotlinPaintingEngine()

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
        fallbackPreview.endStroke()
    }

    override fun undo() {
        // Native undo needs tile deltas; keep UI safe until that lands.
        fallbackPreview.undo()
    }

    override fun clear() {
        nativeClear(handle)
        fallbackPreview.clear()
    }

    override fun snapshot(): EngineSnapshot {
        return fallbackPreview.snapshot()
    }

    @Suppress("unused")
    fun renderRgba(): ByteArray {
        return nativeRenderRgba(handle)
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
            return if (handle == 0L) null else NativePaintingEngine(handle)
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
        private external fun nativeRenderRgba(handle: Long): ByteArray
    }
}

package io.github.lukasvi.hypainter.engine

class KotlinPaintingEngine : PaintingEngine {
    override val nativeBacked: Boolean = false

    private val committedStrokes = mutableListOf<EngineStroke>()
    private var activeStroke = mutableListOf<EngineSample>()

    override fun beginStroke(sample: EngineSample) {
        activeStroke = mutableListOf(sample)
    }

    override fun appendSample(sample: EngineSample) {
        if (activeStroke.isEmpty()) {
            beginStroke(sample)
            return
        }

        activeStroke.add(sample)
    }

    override fun endStroke() {
        if (activeStroke.isNotEmpty()) {
            committedStrokes.add(EngineStroke(activeStroke.toList()))
            activeStroke = mutableListOf()
        }
    }

    override fun undo() {
        if (committedStrokes.isNotEmpty()) {
            committedStrokes.removeAt(committedStrokes.lastIndex)
        }
    }

    override fun clear() {
        committedStrokes.clear()
        activeStroke.clear()
    }

    override fun snapshot(): EngineSnapshot {
        return EngineSnapshot(
            committedStrokes = committedStrokes.toList(),
            activeStroke = activeStroke.takeIf { it.isNotEmpty() }?.let { EngineStroke(it.toList()) },
        )
    }
}

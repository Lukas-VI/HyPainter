package io.github.lukasvi.hypainter.engine

internal fun EngineStroke.stableCopyForLayer(layerId: Long): EngineStroke {
    return copy(
        points = points.toList(),
        layerId = layerId,
    )
}

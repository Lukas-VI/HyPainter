package io.github.lukasvi.hypainter

internal class StylusControlsHider {
    var hidden: Boolean = false
        private set

    fun hideUntilHover() {
        hidden = true
    }

    fun showForHover() {
        hidden = false
    }
}

package io.github.lukasvi.hypainter

internal class StylusControlsHider {
    var hidden: Boolean = false
        private set

    private var hoverArmed = false

    fun hideUntilHover() {
        hidden = true
        hoverArmed = false
    }

    fun showForHover() {
        hidden = false
        hoverArmed = true
    }

    fun shouldHidePressInControls(): Boolean {
        return !hoverArmed
    }
}

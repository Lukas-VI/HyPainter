package io.github.lukasvi.hypainter

internal class StylusControlsHider {
    var hidden: Boolean = false
        private set

    private var hoverArmed = false

    fun hideUntilHover() {
        hidden = true
        hoverArmed = false
    }

    fun showForHoverInControls() {
        hidden = false
        hoverArmed = true
    }

    fun showForLeaveControls() {
        hidden = false
        hoverArmed = false
    }

    fun shouldHidePressInControls(): Boolean {
        return !hoverArmed
    }
}

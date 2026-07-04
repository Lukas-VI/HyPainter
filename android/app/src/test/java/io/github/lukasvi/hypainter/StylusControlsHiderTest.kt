package io.github.lukasvi.hypainter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StylusControlsHiderTest {
    @Test
    fun hidesUntilHover() {
        val hider = StylusControlsHider()

        hider.hideUntilHover()

        assertTrue(hider.hidden)
    }

    @Test
    fun hoverShowsControlsImmediately() {
        val hider = StylusControlsHider()
        hider.hideUntilHover()

        hider.showForHover()

        assertFalse(hider.hidden)
    }

    @Test
    fun hideStaysHiddenUntilHover() {
        val hider = StylusControlsHider()
        hider.hideUntilHover()

        assertTrue(hider.hidden)
    }

    @Test
    fun pressInControlsWithoutHoverHides() {
        val hider = StylusControlsHider()

        assertTrue(hider.shouldHidePressInControls())
    }

    @Test
    fun pressInControlsAfterHoverIsAllowed() {
        val hider = StylusControlsHider()

        hider.showForHover()

        assertFalse(hider.shouldHidePressInControls())
    }

    @Test
    fun pressInControlsAfterHoverExitHidesAgain() {
        val hider = StylusControlsHider()
        hider.showForHover()

        hider.showAfterHoverExit()

        assertFalse(hider.hidden)
        assertTrue(hider.shouldHidePressInControls())
    }
}

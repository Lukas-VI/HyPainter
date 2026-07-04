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

        hider.showForHoverInControls()

        assertFalse(hider.hidden)
    }

    @Test
    fun leavingUiUsesSameImmediateShowPath() {
        val hider = StylusControlsHider()
        hider.hideUntilHover()

        hider.showForLeaveControls()

        assertFalse(hider.hidden)
    }

    @Test
    fun pressInControlsWithoutHoverHides() {
        val hider = StylusControlsHider()

        assertTrue(hider.shouldHidePressInControls())
    }

    @Test
    fun pressInControlsAfterHoverIsAllowed() {
        val hider = StylusControlsHider()

        hider.showForHoverInControls()

        assertFalse(hider.shouldHidePressInControls())
    }

    @Test
    fun leavingControlsDisarmsHoverPress() {
        val hider = StylusControlsHider()
        hider.showForHoverInControls()

        hider.showForLeaveControls()

        assertTrue(hider.shouldHidePressInControls())
    }
}

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
    fun leavingUiUsesSameImmediateShowPath() {
        val hider = StylusControlsHider()
        hider.hideUntilHover()

        hider.showForHover()

        assertFalse(hider.hidden)
    }
}

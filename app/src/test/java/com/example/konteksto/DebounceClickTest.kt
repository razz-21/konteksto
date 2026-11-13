package com.example.konteksto

import com.example.konteksto.ui.components.DEFAULT_DEBOUNCE_INTERVAL_MS
import org.junit.Assert.assertEquals
import org.junit.Test

class DebounceClickTest {

    private class TestDebouncer(
        private val debounceIntervalMs: Long = DEFAULT_DEBOUNCE_INTERVAL_MS,
    ) {
        private var lastClickTime = 0L
        private var clickCount = 0

        fun processClick(currentTimeMs: Long): Boolean {
            if (currentTimeMs - lastClickTime >= debounceIntervalMs) {
                lastClickTime = currentTimeMs
                clickCount++
                return true
            }
            return false
        }

        fun getClickCount(): Int = clickCount
    }

    @Test
    fun testRapidDoubleTapTriggersActionOnlyOnce() {
        val debouncer = TestDebouncer(debounceIntervalMs = 400L)

        // First click at t=1000ms
        val firstHandled = debouncer.processClick(1000L)
        assertEquals(true, firstHandled)
        assertEquals(1, debouncer.getClickCount())

        // Rapid second tap at t=1050ms (50ms after first tap)
        val secondHandled = debouncer.processClick(1050L)
        assertEquals(false, secondHandled)
        assertEquals(1, debouncer.getClickCount())

        // Rapid third tap at t=1200ms (200ms after first tap)
        val thirdHandled = debouncer.processClick(1200L)
        assertEquals(false, thirdHandled)
        assertEquals(1, debouncer.getClickCount())
    }

    @Test
    fun testClickAfterDebounceIntervalIsAllowed() {
        val debouncer = TestDebouncer(debounceIntervalMs = 400L)

        // First tap at t=1000ms
        debouncer.processClick(1000L)
        assertEquals(1, debouncer.getClickCount())

        // Second tap at t=1450ms (450ms after first tap)
        val secondHandled = debouncer.processClick(1450L)
        assertEquals(true, secondHandled)
        assertEquals(2, debouncer.getClickCount())
    }

    @Test
    fun testCustomDebounceInterval() {
        val debouncer = TestDebouncer(debounceIntervalMs = 200L)

        debouncer.processClick(1000L)
        assertEquals(1, debouncer.getClickCount())

        // 100ms later -> ignored
        debouncer.processClick(1100L)
        assertEquals(1, debouncer.getClickCount())

        // 250ms later -> allowed
        debouncer.processClick(1250L)
        assertEquals(2, debouncer.getClickCount())
    }
}

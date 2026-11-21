package com.gemini.guard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FiltersTest {
    @Test
    fun splitHandlesAllSeparators() {
        assertEquals(
            listOf("a.b", "c.d", "e.f"),
            Filters.split(" a.b, c.d;e.f\n")
        )
    }

    @Test
    fun splitDropsEmptyItems() {
        assertEquals(listOf("x"), Filters.split(" ,, ; \n x \n"))
    }

    @Test
    fun matchesIsCaseInsensitiveAndPartial() {
        assertTrue(Filters.matches("FrameLayout", "android.widget.FrameLayout"))
        assertTrue(Filters.matches("framelayout", "android.widget.FrameLayout"))
        assertFalse(Filters.matches("TextView", "android.widget.FrameLayout"))
    }

    @Test
    fun emptyExcludeAllowsEverything() {
        assertTrue(Filters.allowed("", "", "anything"))
        assertTrue(Filters.allowed("", "MainNavigator", "VoiceOverlay"))
    }

    @Test
    fun excludeWinsOverInclude() {
        assertFalse(Filters.allowed("FrameLayout", "FrameLayout", "android.widget.FrameLayout"))
    }

    @Test
    fun includeMustMatchWhenPresent() {
        assertFalse(Filters.allowed("FrameLayout", "", "android.widget.TextView"))
        assertTrue(Filters.allowed("FrameLayout", "", "android.widget.FrameLayout"))
    }
}

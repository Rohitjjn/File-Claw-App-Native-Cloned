package com.example.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderAppearanceTest {

    @Test
    fun zoomStepsAreBrowserLikeAndClamped() {
        assertEquals(110, ReaderAppearance.nextZoom(100))
        assertEquals(90, ReaderAppearance.previousZoom(100))
        assertEquals(500, ReaderAppearance.nextZoom(500))
        assertEquals(25, ReaderAppearance.previousZoom(25))
        assertEquals(125, ReaderAppearance.nextZoom(112))
    }

    @Test
    fun autoThemeFollowsTheApp() {
        val auto = ReaderAppearance()
        assertEquals(ReaderTheme.LIGHT, auto.resolvedTheme(appDark = false))
        assertEquals(ReaderTheme.DARK, auto.resolvedTheme(appDark = true))
        assertTrue(ReaderAppearance(theme = ReaderTheme.BLACK).isDark(appDark = false))
        assertFalse(ReaderAppearance(theme = ReaderTheme.SEPIA).isDark(appDark = true))
    }

    @Test
    fun resolvedJsonCarriesEverySetting() {
        val resolved = ReaderAppearance(zoomPercent = 900, fontSizePx = 0, readableWidth = true)
            .resolve(appDark = true, appFontSetting = "Large", kind = ReaderKind.TEXT)
        assertEquals(500, resolved.zoomPercent)
        assertEquals("dark", resolved.themeKey)
        assertTrue(resolved.json.contains("\"fontSize\":18"))
        assertTrue(resolved.json.contains("\"readableWidth\":true"))
        assertTrue(resolved.json.contains("\"zoom\":500"))
        assertTrue(resolved.rootStyle.contains("--fc-maxw: 42em"))
        assertTrue(resolved.isDark)
    }
}

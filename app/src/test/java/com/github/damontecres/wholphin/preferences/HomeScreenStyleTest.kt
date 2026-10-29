package com.github.damontecres.wholphin.preferences

import com.github.damontecres.wholphin.ui.theme.isLiquid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeScreenStyleTest {
    @Test
    fun defaultsToClassic() {
        val prefs = AppPreferences.getDefaultInstance()
        assertEquals(HomeScreenStyle.STYLE_CLASSIC, prefs.interfacePreferences.homeScreenStyle)
        assertEquals(HomeScreenStyle.STYLE_CLASSIC, AppPreference.HomeScreenStylePref.defaultValue)
        assertFalse(prefs.interfacePreferences.homeScreenStyle.isLiquid)
    }

    @Test
    fun choosingAStyleOnlyChangesThatField() {
        val before =
            AppPreferences.getDefaultInstance().updateInterfacePreferences {
                appThemeColors = AppThemeColors.BLUE
                showHomeHero = true
            }
        for (style in listOf(HomeScreenStyle.STYLE_MODERN, HomeScreenStyle.STYLE_SQUARED)) {
            val after = AppPreference.HomeScreenStylePref.setter(before, style)
            assertEquals(style, AppPreference.HomeScreenStylePref.getter(after))
            assertTrue(style.isLiquid)
            assertEquals(AppThemeColors.BLUE, after.interfacePreferences.appThemeColors)
            assertTrue(after.interfacePreferences.showHomeHero)
            // Survives being written to and read from the DataStore
            val reread = AppPreferences.parseFrom(after.toByteArray())
            assertEquals(style, reread.interfacePreferences.homeScreenStyle)
        }
    }

    @Test
    fun choicesMatchTheEnum() {
        for (style in HomeScreenStyle.entries.filterNot { it == HomeScreenStyle.UNRECOGNIZED }) {
            assertEquals(style, AppPreference.HomeScreenStylePref.indexToValue(AppPreference.HomeScreenStylePref.valueToIndex(style)))
        }
    }

    @Test
    fun topBarStyleIsIndependentOfTheHomeScreenStyle() {
        val prefs = AppPreferences.getDefaultInstance()
        assertEquals(TopBarStyle.TOPBAR_CLASSIC, prefs.interfacePreferences.topBarStyle)
        for (style in listOf(TopBarStyle.TOPBAR_MODERN, TopBarStyle.TOPBAR_SQUARED)) {
            val after = AppPreference.TopBarStylePref.setter(prefs, style)
            assertEquals(style, AppPreferences.parseFrom(after.toByteArray()).interfacePreferences.topBarStyle)
            assertEquals(HomeScreenStyle.STYLE_CLASSIC, after.interfacePreferences.homeScreenStyle)
        }
    }
}

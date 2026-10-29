package com.github.damontecres.wholphin.ui.preferences

import androidx.annotation.StringRes
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.preferences.AppPreference
import com.github.damontecres.wholphin.preferences.AppPreferences
import com.github.damontecres.wholphin.preferences.HomeScreenStyle
import com.github.damontecres.wholphin.preferences.NavigationStyle
import com.github.damontecres.wholphin.ui.theme.isAlternative
import com.github.damontecres.wholphin.ui.theme.isLiquid

/**
 * The reason a setting does nothing with the chosen home screen style or the other settings, or null when it applies.
 * Such a setting is shown disabled with this text instead of its value.
 */
@StringRes
fun preferenceDisabledReason(
    pref: AppPreference<AppPreferences, *>,
    prefs: AppPreferences,
): Int? {
    val ui = prefs.interfacePreferences
    val style = ui.homeScreenStyle
    return when (pref) {
        // The Alternative UI always has its side rail
        AppPreference.NavigationStylePref -> {
            R.string.navigation_style_disabled_alternative.takeIf { style.isAlternative }
        }

        AppPreference.TopBarStylePref -> {
            when {
                style.isAlternative -> R.string.top_bar_style_disabled_alternative
                ui.navigationStyle == NavigationStyle.NAVIGATION_SIDE_DRAWER -> R.string.top_bar_style_disabled_drawer
                else -> null
            }
        }

        // Only the details panel of the Alternative UI reads it
        AppPreference.DetailsTextScalePref -> {
            R.string.details_text_size_disabled.takeIf { !style.isAlternative }
        }

        // The Home page of the Alternative UI has no hero
        AppPreference.ShowHomeHero -> {
            R.string.show_home_hero_disabled_alternative.takeIf { style.isAlternative }
        }

        // No trailer plays anywhere when both trailer settings are off
        AppPreference.HeroTrailerSound -> {
            R.string.hero_trailer_sound_disabled.takeIf { ui.disableHeroTrailer && !ui.cardTrailers }
        }

        else -> {
            null
        }
    }
}

/**
 * A note for a setting that only applies to some pages with the chosen home screen style or the other settings, shown
 * under its value. Null when it applies everywhere.
 */
@StringRes
fun preferencePartialNote(
    pref: AppPreference<AppPreferences, *>,
    prefs: AppPreferences,
): Int? {
    val ui = prefs.interfacePreferences
    val style = ui.homeScreenStyle
    return when (pref) {
        AppPreference.CardProgressStylePref,
        AppPreference.CardInfo,
        -> {
            R.string.card_settings_note_liquid.takeIf { style.isLiquid }
        }

        AppPreference.CustomizeHome -> {
            R.string.customize_home_note_alternative.takeIf { style.isAlternative }
        }

        AppPreference.UserPinnedNavDrawerItems -> {
            R.string.nav_drawer_pins_note_alternative.takeIf { style.isAlternative }
        }

        AppPreference.CardTrailers -> {
            R.string.card_trailers_note_liquid.takeIf {
                style == HomeScreenStyle.STYLE_MODERN || style == HomeScreenStyle.STYLE_SQUARED
            }
        }

        // Without the hero the trailer still plays on the details pages
        AppPreference.HeroTrailer -> {
            R.string.hero_trailer_note_no_hero.takeIf { !ui.showHomeHero && !style.isAlternative }
        }

        else -> {
            null
        }
    }
}

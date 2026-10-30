# Fork customizations

Baseline: upstream Wholphin `upstream/main` = merge-base `0aab8398` ("Update Gradle to v9.8.2 (#2103)",
2026-10-22). The fork is 24 commits ahead (`git log 0aab8398..HEAD`); one of them (`924209a2`, DV7 bitstream filter)
is by another author (Sergey K), the rest by MadDinu93. Status relative to `upstream/main` was taken from local refs —
nothing was fetched, so upstream may have moved since.

Find fork code with `git diff 0aab8398 HEAD --stat`. `// Thorphin:` comments are sparse (only in `app/build.gradle.kts`,
`HomeSettingsService.kt` and `HomeSettingsViewModel.kt`), so most fork edits in upstream files are only visible in the diff.
`W/` = `app/src/main/java/com/github/damontecres/wholphin/`.

## Summary

| Area | Status | Main files | Details |
|---|---|---|---|
| Branding & identity | modified | `app/build.gradle.kts` (`applicationId`, APK names), `res/mipmap-*`, `res/values*/strings.xml`, `UpdateChecker`, `AppPreference.UpdateUrl`, `DebugLogTree`, `WholphinApplication` (crash dialog text) | below |
| Navigation: top bar + Library page | new + modified | `ui/nav/TopNavBar.kt`, `NavDrawer.kt`, `NavDrawerService.kt`, `ui/detail/LibrariesPage.kt`, `AllItemsPages.kt` | [ui-and-navigation.md](ui-and-navigation.md) |
| Library folder pages simplified | modified / removed | `CollectionFolderMovie.kt`, `CollectionFolderTv.kt`; deleted `RecommendedMovie.kt`, `RecommendedTvShow.kt` | [ui-and-navigation.md](ui-and-navigation.md) |
| Hero card, glass theme, cards | new + modified | `ui/main/HomePage.kt`, `ui/components/RecommendedContent.kt`, `ui/cards/*`, `ui/theme/*` | [ui-and-navigation.md](ui-and-navigation.md) |
| Two-pane settings, new prefs, style-dependent disabled settings | new + modified | `ui/preferences/PreferencesContent.kt`, `ui/preferences/PreferenceAvailability.kt`, `ComposablePreference.kt`, `SwitchPreference.kt`, `preferences/AppPreference.kt`, proto | [ui-and-navigation.md](ui-and-navigation.md) |
| Alternative UI (side rail) | new + small hooks | `ui/nav/AlternativeNavigation.kt`, `ui/detail/AlternativeBrowse.kt`, `ui/preferences/AlternativeSettings.kt`; hooks in `NavDrawer.kt`, `LiquidStyle.kt`, `PreferencesContent.kt`, `DestinationContent.kt`, `ApplicationContent.kt` | [ui-and-navigation.md](ui-and-navigation.md) |
| Alternative UI Minimalist (list layout of the same UI) | new + small hooks | `ui/detail/AlternativeMinimalistBrowse.kt`; hand-over in `AlternativeBrowse.kt`, `isAlternative` in `LiquidStyle.kt`, proto `STYLE_ALTERNATIVE_MINIMALIST` = 4 | [ui-and-navigation.md](ui-and-navigation.md) |
| Home screen style (Modern / Squared) + shared new Library look | new + small hooks | `ui/theme/LiquidStyle.kt`, `ui/main/LiquidHome.kt`, `ui/detail/LiquidLibrary.kt`; hooks in `HomePage.kt`, `LibrariesPage.kt`, `AllItemsPages.kt`, `DestinationContent.kt`, `ApplicationContent.kt`, `ui/cards/*` | [ui-and-navigation.md](ui-and-navigation.md) |
| Trailers & theme music | new | `services/HeroTrailerService.kt`, `ThemeMusicAddonService.kt`, `ui/nav/HeroTrailer.kt`, `ui/playback/TrailerVideoPage.kt`; modified `TrailerService`, `ThemeSongPlayer` | [trailers-and-theme-music.md](trailers-and-theme-music.md) |
| "Recently added" row titles | modified | `services/HomeSettingsService.kt`, `ui/main/settings/HomeSettingsViewModel.kt` | rows use the library's own name instead of "Recently added in ..." |
| Home startup speed | new + modified | `services/HomeRowsCache.kt`, `HomeViewModel.kt`, `NavDrawerService` (libraries cache), `ServerRepository`, `UserSwitchListener`, `MainActivity` (preload) | [ui-and-navigation.md](ui-and-navigation.md) |
| Remux PlaybackInfo streams | new | `ui/playback/PlaybackInfoStreams.kt`, `PlaybackViewModel.kt` | [playback.md](playback.md) |
| Fallback subtitles & list order | modified | `StreamChoiceService.kt`, `CurrentMediaInfo.kt`, `JellyfinServer.kt` (Room v42), `UserProfileSettings.kt` | [playback.md](playback.md) |
| Version switch in player, SUB button, debug info | modified | `PlaybackDialog.kt`, `PlaybackControls.kt`, `PlaybackDebugOverlay.kt` | [playback.md](playback.md) |
| Dolby Vision 7 → 8.1 | new | `util/DolbyVisionProfile7*.kt`, `PlayerFactory.kt`, `DeviceProfileService.kt`, lib `exoplayerhdrutils` | [playback.md](playback.md) |
| Build/release | new + modified | `app/build.gradle.kts`, `:baselineprofile`, `store/` | [build-and-development.md](build-and-development.md) |

## Details not covered by the feature docs

### Branding and identity
- `applicationId = io.github.maddinu93.thorphin`; Kotlin package / `namespace` remain `com.github.damontecres.wholphin`
  (also the `wholphin://` deep-link scheme and intent action names in [Intents.md](../../Intents.md)).
- Update checks point at `MadDinu93/Thorphin` GitHub releases with asset name `Thorphin`. `AppUpgradeHandler`
  migrates a stored Wholphin update URL to Thorphin's default unless the user had customized it.
- User-visible "Wholphin" strings renamed to "Thorphin" in all locales.

### Settings and data-format changes (merge-sensitive)
- Proto (`app/src/main/proto/WholphinDataStore.proto`): `AppThemeColors` renumbered — `GLASS = 0` (was `PURPLE = 0`),
  `PURPLE = 8`, fork colors `LAVENDER = 100`, `AMBER = 101`; new enums `NavigationStyle`, `CardProgressStyle`; `InterfacePreferences` fields 16–24;
  `PlaybackPreferences.only_preferred_subtitles = 27`; `ExperimentalPreferences.convert_dolby_vision_profile_7 = 6`.
  Several booleans are deliberately inverted (`disable_hero_trailer`, `hide_card_info`) so the proto3 default (false)
  means "on" without a migration.
- Room: version 41 → 42 via `AutoMigration(41, 42)` adding `fallbackSubtitleLanguage`.
- New-install defaults (`AppPreferencesSerializer`, from each preference's `defaultValue`): hero on, experimental
  settings on, DV7 conversion on, prefer Dolby Vision over HDR10+ on.

### Startup/network parallelism
Requests that used to be sequential now run concurrently: public system info in `ServerRepository`, user views +
recording folders in `NavDrawerService`, server-plugin check alongside home settings in `UserSwitchListener` (home
settings only await it when they must come from the plugin). Locale is only set when it changes.

### Untracked / non-code items in the working tree
- `Improvement Ideas.txt`: personal notes, git-ignored on purpose.

## Implications for future changes

- Merging upstream: expect conflicts in `AppPreference.kt`, `PreferencesContent.kt`, `HomePage.kt`,
  `HomeViewModel.kt`, `NavDrawer.kt`, `PlaybackViewModel.kt`, the proto and `AppDatabase` — see
  [known-issues.md](known-issues.md) for numbering hazards.
- Prefer new files for new fork features and minimal, commented hooks in upstream files.

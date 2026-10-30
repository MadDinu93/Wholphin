# Trailers and theme music *(fork feature)*

`W/` = `app/src/main/java/com/github/damontecres/wholphin/`. Entirely fork code except `TrailerService` and
`ThemeSongPlayer`, which are upstream files the fork extended.

## Where trailers play

| Surface | Composable / entry | Triggered by |
|---|---|---|
| Behind the home hero (backdrop area) | `HeroTrailerBackdrop` in `W/ui/nav/HeroTrailer.kt`, placed by `ApplicationContent` | `HomePage` / `RecommendedContent` calling `heroTrailerService.focus(item, inCard = false)` |
| Inside the focused card (card grows to 16:9) | `rememberCardTrailers` / `rememberCardTrailer` + `LocalCardTrailers`; provided and consumed only in `W/ui/main/HomePage.kt` (home rows) | `focus(item, inCard = true)` when setting `card_trailers` is on |
| Behind a details page | `DetailsTrailer` (used by `MovieDetails`, `SeriesDetails`) | page composition; stops theme music when the trailer starts with sound |
| Full screen, from the trailer button | `Destination.TrailerVideo` → `W/ui/playback/TrailerVideoPage.kt` | `TrailerService` when the URL is a direct video; other URLs (YouTube) open via `ACTION_VIEW` |
| Theme music only, no trailer | `FocusedCardThemeMusic` (e.g. Search) | `focus(item, trailers = false, themeMusic = true)` |

## Flow (`W/services/HeroTrailerService.kt`, singleton)

`focus(item, owner, inCard, trailers, themeMusic)`:
1. Cancels the previous job, clears `current`/`playing`, stops theme music it started.
2. After `PREPARE_DELAY` (1 s) resolves a source via `resolve()`; cached per trailer-owning item (movie/series; episodes
   and seasons use the series).
   - Local trailer on the server → `HeroTrailerSource.Local` (static stream URL, played with the auth client).
   - Else first remote trailer URL that `isVideoUrl()` → `HeroTrailerSource.Direct` (played with the standard client).
     YouTube pages are **not** played in the hero/cards.
3. For `Direct`, sends a HEAD request (`prepare`) so the add-on warms the video up.
4. At `START_DELAY` (5 s) publishes `current`; the UI player reports `onPlaying` / `finished`.
5. After the trailer (or when none) plays the item's theme music once if requested; it is pre-fetched during the trailer.

`owner`: a page's `focus(null, owner)` on dispose only clears if no other owner has asked since — keeps navigation
transitions from killing the next page's trailer.

URL helpers (same file): `unwrapTrailerUrl()` (Remux wraps every add-on trailer as
`youtube.com/watch?v=<real URL>`), `isVideoUrl()`, `isAudioUrl()` (theme music add-ons put audio links among trailers).

## Rendering details (`W/ui/nav/HeroTrailer.kt`)

- `HeroTrailerArea` / `LocalHeroTrailerArea`: the home page reports where the hero ends so the video stops above the
  first row; fades on every side via `fadeStops`.
- `TrailerPlayer` detects letterbox bars from video frames (`findPicture` → `PictureBounds`) and only shows the video
  once the picture is not black, so the backdrop stays until real content appears.
- `ZoomedTrailerPlayer` (used by the three Alternative layouts' details window) builds on that: it scales and centers
  the video so the picture covers the window, which zooms past letterbox bars and only fits a video without bars.
- `ApplicationContent` hides the backdrop image while a hero (not in-card) trailer plays.
- `HeroTrailerBackdrop` and `TrailerPlayer` stop the trailer on `ON_PAUSE` (like `PlaybackLifecycleObserver` does for
  the player and theme music), not `ON_STOP`: a TV input switch may only pause the activity, and the sound played on.

## Theme music

- `W/services/ThemeSongPlayer.kt`: server theme songs first (auth client), else `ThemeMusicAddonService` URLs
  (standard client). `playThemeFor` / `prepareThemeFor` / `stop(owner)` are owner-scoped.
- `W/services/ThemeMusicAddonService.kt`: add-on contract `GET {addonUrl}/themes/{movie|series}/{tmdb:ID|ttIMDB}.json`
  → `{"themes":[{"url":...}]}`. Falls back to audio links among the item's remote trailers. 404 is cached as empty;
  other errors are not cached.
- The add-on itself (Thorphin Trailers & Themes) lives in a separate private repo, not in this codebase.

## Settings (proto `InterfacePreferences`, UI in `AppPreference.kt`)

`show_home_hero`, `disable_hero_trailer` (inverted), `mute_hero_trailer`, `card_trailers`, `card_theme_music`,
`theme_music_addon_url`, `hide_details_during_trailer` (Alternative UI Minimalist only: the title and details of the
hero card disappear while its trailer plays instead of dimming to 35%). Delays are constants in
`HeroTrailerService.Companion`.

## Pitfalls

- Never play add-on/YouTube URLs with `@AuthOkHttpClient` — it would leak the Jellyfin token.
- Rows don't fetch trailer fields or provider IDs; both services fetch the full item (`userLibraryApi.getItem`).
- Card trailers rely on `LocalCardTrailers` being provided by the page; cards outside such pages never get one.

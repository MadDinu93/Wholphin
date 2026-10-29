# UI, navigation and focus

`W/` = `app/src/main/java/com/github/damontecres/wholphin/`. Compose for TV (`androidx.tv.material3`) on a
Material 3 base. Trailers have their own doc: [trailers-and-theme-music.md](trailers-and-theme-music.md).

## Two navigation levels (Navigation 3)

| Level | Keys | Back stack owner | Renderer |
|---|---|---|---|
| Setup | `SetupDestination` (`Loading`, `ServerList`, `UserList`, `AppContent`) | `SetupNavigationManager` | `W/MainContent.kt` |
| App | `Destination` sealed class, `W/ui/nav/Destination.kt` (`@Serializable`, saved in `onSaveInstanceState`) | `NavigationManager` (`navigateTo`, `navigateToFromDrawer`, `goBack`, `goToHome`, `reloadHome`, `replace`) | `W/ui/nav/ApplicationContent.kt` → `DestinationContent.kt` |

`Destination(fullScreen)`: full-screen destinations go straight to `DestinationContent`; others go through
`NavDrawer(...)`, which chooses the navigation chrome. The `when (destination)` mapping to screens is in
`DestinationContent.kt` — the place to add a new screen. Fork destinations: `Libraries`, `ServerCatalogs`,
`AllMovies`, `AllSeries`, `TrailerVideo`.

Each `NavEntry` content key includes server and user IDs, so switching user rebuilds screens and their ViewModels.

## Top bar vs side drawer *(fork)*

`NavDrawer` (`W/ui/nav/NavDrawer.kt`) returns `TopNavigation` (`W/ui/nav/TopNavBar.kt`) unless the proto setting
`navigation_style` is `NAVIGATION_SIDE_DRAWER`; top bar is the default (enum value 0).

- Items (Home, Library, pinned items, Search, Settings, profile) come from `NavDrawerViewModel` / `NavDrawerService`,
  shared with the drawer.
- The bar shows only on root pages: Home, Search, Libraries, ServerCatalogs, AllMovies, AllSeries, Favorites,
  Discover, a library opened from the Library page, and pinned server items. Other pages get the full screen.
- Pages report scroll position through `LocalTopNavBarState` (`TopNavBarState.contentAtTop`); the bar slides away
  when false unless it has focus. Home sets it false as soon as focus leaves the first row.
- Content is padded by `TopNavBarHeight` with a layout trick that measures once and only animates placement — keep
  that when editing (avoids per-frame remeasure).
- Back on Home moves focus to the bar; a second back exits (mirrors the drawer behavior).

## Focus conventions

- D-pad focus is managed with `FocusRequester`s and `tryRequestFocus()` (extension in `W/ui/Extensions.kt`). The bar uses
  `focusGroup()` + `focusProperties { onEnter = ... }` to land on the selected tab.
- Rows/grids add their own 16 dp start inset; `TopNavContentStartInset` compensates so pages look centered.
- Settings is a two-pane layout (`W/ui/preferences/PreferencesContent.kt`): categories left, settings right; back
  from a setting returns to its category, focus is restored when returning from a sub-screen.

## Home page

- `W/ui/main/HomePage.kt` + `HomeViewModel.kt`; rows configured by `HomeSettingsService` (local or server plugin),
  data from `HomeDataService` / `LatestNextUpService`.
- *(fork)* Optional hero (`show_home_hero`, on by default for fresh installs): details of the focused item above the
  rows; `RecommendedContent.kt` has the same hero logic for library recommendation pages.
- *(fork)* Startup cache: `HomeViewModel.showCachedRows()` shows `HomeRowsCache` rows immediately (`cachedFor` user
  id in `HomeState`), then `loadHomeRows` replaces rows one by one as each request completes (select loop) and saves
  the cache. An "Updating..." indicator shows while cached rows are displayed. Rows are shown as soon as the first
  row is ready, not when all are.

## Library pages *(fork)*

Replaces per-library drawer rows. `W/ui/detail/LibrariesPage.kt`: `LibrariesPage` (three cards: server catalogs,
all movies, all series, each with count + newest item image, `LibrariesViewModel`) and `ServerCatalogsPage` (grid of
every library). `W/ui/detail/AllItemsPages.kt`: `AllMoviesPage` (movies + collections tab) and `AllSeriesPage`, using
a synthetic non-item key so sort/filter are saved separately. Movie/TV library folders (`CollectionFolderMovie`,
`CollectionFolderTv`) now open straight on the grid — upstream's Recommended/Genres tabs were removed
(`RecommendedMovie.kt`, `RecommendedTvShow.kt` deleted).

## Cards and theme *(fork look)*

- `W/ui/cards/`: `PosterCardShape` (12 dp), `Modifier.glassHighlight()` (rim + gloss + one-time sheen on focus, draw
  only), `CardTopOverlay` / `CardPill` / watched tab in `CardBadges.kt` (everything kept at the top of posters).
  `LocalCardProgressStyle` and `LocalShowCardInfo` come from interface preferences via `ApplicationContent`.
- `W/ui/theme/colors/GlassThemeColors.kt`: grey/white/black scheme; `AppThemeColors.GLASS` (proto value 0) is the
  default; `isMonochrome` covers GLASS and OLED_BLACK for components that must not disappear in greys.
- `Theme Designs/` (untracked) holds the Stitch design references ("Aetheria TV"); the Modern/Squared style below uses its tokens.

## Home screen style: Modern and Squared *(fork)*

Proto `InterfacePreferences.home_screen_style` (field 25, `HomeScreenStyle`: `STYLE_CLASSIC` 0 default, `STYLE_MODERN`,
`STYLE_SQUARED`), set by `AppPreference.HomeScreenStylePref` in the Appearance group. It is separate from the color
theme (`AppThemeColors`); changing it writes only that field. `ApplicationContent` provides it as `LocalHomeScreenStyle`.

- `W/ui/theme/LiquidStyle.kt`: `LiquidStyleScope { }` wraps a page. With Classic it just composes the content; with
  Modern/Squared it applies the Stitch "liquid glass" colors (`LiquidColors`) and the card corners
  (`LocalPosterCardShape`: 16 dp Modern, 4 dp Squared) and sets `LocalLiquidStyle`. Pages that are not wrapped never change.
- Home: `HomePage` wraps `HomePageContent` in the scope. Inside it the default `headerComposable` shows `LiquidHomeHero`
  (`W/ui/main/LiquidHome.kt`: title/logo and genres at the top, quick details, stream badges (4K/DV/HDR/
  ATMOS, only when the item carries stream info), overview, focusable Play button) and each card is a
  `LiquidHomeCard`: Modern = 16 dp round cards, poster rows with the text under them; Squared = 4 dp landscape cards
  with a short information panel (title, year, rating). Continue watching rows are landscape in both, with
  progress bar and time left. "Customize home page" applies (height, aspect ratio, image type, titles, spacing),
  with heights scaled down (`PortraitScale`/`LandscapeScale`) and tall ratios turned landscape for continue rows and
  in Squared; its live preview is wrapped in the scope too. Card trailers are not drawn in these styles; "Image with dynamic color" backdrops are drawn image-only (neutral black) while a Modern or Squared home screen style is chosen. Moving between rows scrolls with an eased animation (`LiquidBringIntoViewSpec`), and the hero text cross-fades between items. Rows, focus,
  cache and trailers otherwise are the classic code. `TopNavBar` follows the separate "Topbar style" setting (proto `top_bar_style` field 26, `TopBarStyle`: Classic, Modern,
  Squared; `LocalTopBarStyle`), not the home screen style: Modern and Squared share one layout (Squared has square corners): text tabs, icon-only search and settings, clock
  and profile in one capsule (white active tab) and hides the corner clock/profile.
- *(fork)* **Alternative UI** (`STYLE_ALTERNATIVE` = 3, "Alternative UI" in the same setting): `NavDrawer` hands over to
  `AlternativeNavigation` (`W/ui/nav/AlternativeNavigation.kt`) whatever `navigation_style` says. It is a permanent 72 dp
  left rail (profile, Home, Movies, Series, Sets and Lists when the server has such
  libraries, Favorites/Discover, Search, Settings at the bottom; no "Library" or "Catalog" entry) and the page
  next to it, with the clock floating over the top right corner. The rail shows on the same root pages as the top bar, and on the main
  Settings page (`ApplicationContent` treats `Settings(BASIC)` as not full screen in this style). The "Topbar style"
  preference is shown disabled while it is selected (`enabled` on `ChoicePreference`/`ComposablePreference`, decided in
  `PreferencesContent`). It is `isLiquid`, so Home and the library pages reuse the Modern code with 8 dp cards, and
  `LiquidColors` switches to the amber "Thorphin TV" palette (`LiquidColors.alternative`, set in `ApplicationContent`).
  - Movies and Series (`AllMovies`/`AllSeries`) open `AlternativeBrowsePage` (`W/ui/detail/AlternativeBrowse.kt`)
    instead of the grid: the catalogues on the left are "All movies" (or "All TV shows") on top, then only the libraries
    of that kind (`CollectionType.MOVIES`/`TVSHOWS` from `NavDrawerService.allLibraries`), with counts; focusing one loads
    it after 250 ms. In the middle a 3 column poster grid, on the right the details of the focused title (backdrop,
    quick details, stream badges, genres, overview, Play/Resume). A `FilterByButton` (the classic filter dropdown, with
    `DefaultFilterOptions`/`DefaultTvFilterOptions`) sits above the section title on these two pages only, not on Home;
    the filter applies to every catalogue and to their counts. It uses its own `ApiRequestPager`, so the
    sort/filter/view options of the classic grid do not apply there.
  - Trailers and theme music play in the details panel instead of its picture (`AlternativeBrowseLayout` calls
    `heroTrailerService.focus(item, owner, inCard = true)` and shows `TrailerPlayer`; `inCard = true` keeps the app-wide
    `HeroTrailerBackdrop` quiet). Same settings as elsewhere (`disable_hero_trailer`, `card_trailers`, `mute_hero_trailer`).
  - Home (`W/ui/main/AlternativeHome.kt`, hooked in `HomePage`) uses the same `AlternativeBrowseLayout`; its catalogues
    are the home rows. No hero, card trailers or context menu there.
  - The catalogue column is 215 dp while it has focus and shrinks to 100 dp (the first 6 characters of each name) once focus
    moves to the posters or details; entering it, or Back from them, always lands on the shown catalogue (`onEnter` + `BackHandler`); posters and details share the freed room (weights 1 : 1.3). Its width is read in the layout phase so only
    that panel is laid out during the animation. There are no item counts in it. The filter button is always expanded
    (`alwaysExpanded` on `FilterByButton`/`ExpandableFilterButton`).
  - Names from the server (libraries, home rows) go through `localizeServerName` (`W/util/ServerNames.kt`) with the
    `server_name_translations` array of the language ("English=Translation" pairs; whole name first, then phrases,
    longest first). Only `values-ro` has one, English has an empty array.
  - Appearance has "Details text size" (`AppPreference.DetailsTextScalePref`, proto `details_text_scale`, percent, 0 = 100)
    which scales the text of the details panel.
  - Settings (`AlternativeSettings.kt`): categories are amber rows and the main page shows a server/user card.
  - Not done from the designs: the sort dropdown of the grid, the static
    4K/HDR/ATMOS chips and "Jellyfin Direct" pill of the header (the server name is shown instead), the metric tiles
    of the server settings (latency etc., no data behind them).
  - **Alternative UI Minimalist List / Cards** (`STYLE_ALTERNATIVE_MINIMALIST` = 4 "List", `STYLE_ALTERNATIVE_MINIMALIST_CARDS` = 5 "Cards", `isMinimalist`; Cards shows the titles as a row of 88 dp posters (`BrowsePoster`) instead of the text list, 50/50 split; the rest of this paragraph applies to both) (design in `Theme Designs/Alternative UI Minimalist`) are a
    second, selectable look of the same UI: everything above (rail, amber palette, settings page, Home/Movies/Series data,
    filters, trailers) applies through `HomeScreenStyle.isAlternative`; only the page layout differs.
    `AlternativeBrowseLayout` hands over to `MinimalistBrowseLayout` (`W/ui/detail/AlternativeMinimalistBrowse.kt`) when
    `LocalHomeScreenStyle` is Minimalist: fixed 190 dp catalogues, then a hero card for the focused title (the trailer or
    backdrop fills the section, 16:9 covering it, with the text over a left scrim and Play fixed bottom left) over a compact list of titles (title, year, badges, rating). Not done: match %, the
    Trailer/Watchlist/Audio buttons, the remote hints and the "Jellyfin Direct" pill (no data behind them).
- Library: `DestinationContent` wraps `LibrariesPage`, `ServerCatalogsPage`, `AllMoviesPage` and `AllSeriesPage`. They
  use `LibraryTitle` (`W/ui/detail/LiquidLibrary.kt`), which is the plain title for Classic and a large title with real
  counts in a pill otherwise (`GridTitle` of the folder pages is large and start-aligned in these styles); the
  sort/filter/grid logic is shared. There is no separate Library setting.
- Image cards (`BannerCard`, `GridCard`, `GenreCard`, `StudioCard`, `glassHighlight`) read `LocalPosterCardShape`
  instead of `PosterCardShape` directly.
- Not implemented from the designs (no data behind them): match %, IMDb badge, footer text, "Sort by"/"Explore all"
  row links, the third (save) hero button, filter chips, curated collection rows, Inter font, backdrop blur.

## Strings and i18n

`app/src/main/res/values*/strings.xml`. The fork adds English and Romanian (`values-ro`) strings and renamed
"Wholphin" → "Thorphin" in user-visible strings across locales. `MissingTranslation` lint is disabled.

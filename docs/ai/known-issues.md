# Known issues, mismatches and open questions

Found during the 2026-10-29 documentation audit. Remove items once resolved.

## Docs that disagree with the code

| Doc | Says | Code does |
|---|---|---|
| `README.md` ("Background trailers") | Falls back to the YouTube trailer from metadata, shown without YouTube controls | `HeroTrailerService.resolve` only plays direct video links (`isVideoUrl()` excludes YouTube); YouTube trailers open in an external app only from the trailer button (`TrailerService`) |
| `README.md` ("Installation") | `default` flavor's updater still checks Wholphin's releases | `AppPreference.UpdateUrl` defaults to `MadDinu93/Thorphin` releases; `AppUpgradeHandler` migrates the old URL |
| `README.md` | "There are no store listings or release builds yet" | `store/` and commit `ea4b5db1` set up a Google Play closed beta; may still be accurate for public listings |
| `Intents.md` | `-n 'com.github.damontecres.wholphin/.MainActivity'` | The installed package is `io.github.maddinu93.thorphin` (`.debug` for debug). The component must be `io.github.maddinu93.thorphin/com.github.damontecres.wholphin.MainActivity`. URI form (`wholphin://...`) is unaffected |
| `DEVELOPMENT.md` | Titled/written as Wholphin's guide | Still accurate for structure and settings; Thorphin-specific setup is in [build-and-development.md](build-and-development.md) |

These were not edited (documentation-only audit limited to new files).

## Upstream-merge hazards

- **Proto enum renumbering**: `AppThemeColors.GLASS = 0`, `PURPLE = 8`, fork-only `LAVENDER = 100`, `AMBER = 101`.
  Upstream colors added later also need a `ThemePalette` and a place in `ThemeColorChoices` (the settings list is not
  ordered by number), and upstream's own color schemes are no longer used. If upstream adds a color with value 8, or
  any new enum/field in `InterfacePreferences` (16–26; 25 is `home_screen_style`, 26 is `top_bar_style`), `PlaybackPreferences` (27) or `ExperimentalPreferences` (6),
  numbers collide. Always renumber the fork's side, never reuse a number for a different meaning on existing installs.
- **Room version**: fork uses v42 (`AutoMigration(41, 42)`). An upstream v42 with a different schema conflicts with
  `app/schemas/.../42.json`; the fork's change would need to move to a later version with a migration.
- `HomeViewModel.loadHomeRows` was restructured (always incremental select loop); upstream edits there will not apply
  cleanly.
- Upstream's movie/TV library tabs (Recommended, Genres, ...) were removed; upstream fixes to `RecommendedMovie` /
  `RecommendedTvShow` no longer have a target.

## Repository-state notes

- `.gitignore` ignores `docs/*` (upstream had `docs/`) with an exception for `docs/ai/`. It must stay `docs/*`, not
  `docs/`: git cannot re-include a path inside an ignored directory.
- `Theme Designs/` is untracked and not referenced by code.
- Local branches `fix/playbackinfo-subtitle-streams`, `pr-2017`, and `main` exist; the active branch is `thorphin`,
  tracking `thorphin/main`. The repo has three remotes: `origin` (Wholphin fork), `thorphin` (this project),
  `upstream` (damontecres/Wholphin).

## Open questions (not verified)

- DV7 conversion correctness on devices other than the tested MediaTek TV (README: Philips TA5) is unknown.
- `HomeRowsCache` keys on server+user only; behavior after server-side library permission changes relies on the
  fresh load replacing rows.
- Trailer delays (5 s start, 1 s prepare) and the add-on contract are fork conventions; no tests cover them.

# Thorphin — Claude Code entry point

Thorphin is an **Android TV Jellyfin client**, a fork of [Wholphin](https://github.com/damontecres/Wholphin)
(not Jellyfin Media Player). It targets the [Remux](https://github.com/lostb1t/remux) server (Jellyfin API) and plain
Jellyfin 10.10/10.11. The fork adds an Apple TV style UI (top nav bar, hero, glass cards), background/card trailers
and theme music via an add-on, Dolby Vision profile 7 → 8.1 conversion, Remux-specific stream handling and a
cached home page. Almost everything else is upstream Wholphin.

**Stack (verified in [app/build.gradle.kts](app/build.gradle.kts), [gradle/libs.versions.toml](gradle/libs.versions.toml)):**
Kotlin, Jetpack Compose + androidx.tv Material, Navigation 3, Hilt, Room + Proto DataStore, Media3/ExoPlayer
(+ optional libmpv, ffmpeg/av1 decoders), Jellyfin Kotlin SDK, OkHttp, Coil 3, WorkManager. Single activity, MVVM.

## How to use these docs

Read **only** what the task needs: pick rows from the routing table, read those docs, then open the source files
they point to. Do not read all of `docs/ai/`. Source paths in the docs written as `W/...` mean
`app/src/main/java/com/github/damontecres/wholphin/...`.

| Task | Read |
|---|---|
| Orientation, startup, DI, Jellyfin API/auth, persistence, background work | [architecture.md](docs/ai/architecture.md) |
| "What did the fork change?", merging upstream Wholphin, deciding if code is fork or upstream | [fork-customizations.md](docs/ai/fork-customizations.md) |
| Screens, navigation, top bar / drawer, focus, home page, cards, theme, settings UI | [ui-and-navigation.md](docs/ai/ui-and-navigation.md) |
| Hero / card / details-page trailers, theme music, the add-on | [trailers-and-theme-music.md](docs/ai/trailers-and-theme-music.md) |
| Player, track/subtitle choice, Remux PlaybackInfo streams, Dolby Vision, versions | [playback.md](docs/ai/playback.md) |
| Building, flavors, signing, versionCode, tests, lint, baseline profile, Play Store | [build-and-development.md](docs/ai/build-and-development.md) |
| Doc/code mismatches, merge hazards, open questions | [known-issues.md](docs/ai/known-issues.md) |

Existing upstream docs still valid for their topic: [DEVELOPMENT.md](DEVELOPMENT.md) (how to add an app setting,
settings storage options), [Intents.md](Intents.md) (deep links; see known-issues for the stale package name),
[wholphin-mpv-stub/README.md](wholphin-mpv-stub/README.md), [store/README.md](store/README.md) (Play listing).

## Essential constraints

- Package/namespace stays `com.github.damontecres.wholphin`; only `applicationId` is `io.github.maddinu93.thorphin`
  (debug: `.debug` suffix, label "Thorphin (Debug)"). Do not rename packages — it would make upstream merges impossible.
- Keep upstream files close to upstream. Mark fork edits inside upstream files with a `// Thorphin:` comment where the
  reason is not obvious; put larger fork features in new files.
- Proto enums/field numbers and the Room version are shared with upstream's numbering; see merge hazards in
  [known-issues.md](docs/ai/known-issues.md) before touching `WholphinDataStore.proto` or `AppDatabase`.
- Never send the Jellyfin token to non-server hosts: use `@StandardOkHttpClient` for third-party URLs,
  `@AuthOkHttpClient` only for the Jellyfin server (see [architecture.md](docs/ai/architecture.md)).
- Code style: ktlint 1.8.0 via pre-commit; comments are plain explanatory sentences, matching surrounding code.
- New user-facing strings go in `app/src/main/res/values/strings.xml`; the fork also maintains `values-ro`.
- `.gitignore` ignores `docs/*` (inherited from upstream) except `docs/ai/`; put new AI docs only there.

## Keeping docs current

When a change alters behavior described here, update the one doc that owns that topic in the same change (no
copies in other docs — cross-link instead). Add new fork features to the table in
[fork-customizations.md](docs/ai/fork-customizations.md). Remove statements that become false rather than appending
corrections. Keep this file a router: no feature detail here.

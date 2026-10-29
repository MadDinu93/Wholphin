# Playback

`W/` = `app/src/main/java/com/github/damontecres/wholphin/`. Mostly upstream; fork parts flagged *(fork)*.

## Main pieces

| Piece | File | Role |
|---|---|---|
| Screen | `W/ui/playback/PlaybackPage.kt` | Compose UI; maps `PlaybackAction`s (from `overlay/PlaybackControls.kt`) to ViewModel calls |
| State machine | `W/ui/playback/PlaybackViewModel.kt` | `init` → `play` (choose source/tracks, build URL, create player) → `changeStreams` / `changeAudioStream` / `changeSubtitleStream` / `changeMediaSource` *(fork)*, progress reporting, next up, segments, trickplay, websocket playstate commands |
| Player construction | `W/services/PlayerFactory.kt` | ExoPlayer (`WholphinRenderersFactory`, extractors, libass for ASS, audio offload) or `MpvPlayer`; keeps `currentPlayer` |
| Backend choice | `PlaybackViewModel.choosePlayerBackend` | ExoPlayer / MPV / prefer-MPV (ExoPlayer for HDR or 4K+software decode) / external player |
| Device profile | `W/services/DeviceProfileService.kt`, `W/util/profile/` | What the server may direct-play vs transcode; `MediaCodecCapabilitiesTest` probes codecs. See `util/profile/README.md` |
| Track defaults | `W/services/StreamChoiceService.kt` | Picks source, audio and subtitle from user prefs + saved per-series choices |
| Menus/info | `CurrentMediaInfo.kt`, `PlaybackDialog.kt`, `overlay/PlaybackDebugOverlay.kt` | Track lists, dialogs, debug overlay |
| Session/lifecycle | `MediaSessionPlayer.kt`, `services/PlaybackLifecycleObserver.kt` | Media session, pause/stop with activity |

Trailers use separate lightweight ExoPlayers, not this pipeline: see
[trailers-and-theme-music.md](trailers-and-theme-music.md).

## Remux PlaybackInfo streams *(fork)* — `W/ui/playback/PlaybackInfoStreams.kt`

Remux lists subtitles (incl. add-on external ones) only in the PlaybackInfo response, not in the item's media
sources.
1. `withPlaybackInfoStreams`: if the chosen source has no subtitle streams, request PlaybackInfo with the **same device
   profile and max bitrate** as the real playback request (`deviceProfileFor`, `maxStreamingBitrate`) and use its
   streams. Remembered in `playbackInfoStreamsSource`.
2. `remapToPlayedSource`: the server may still answer playback with a different release; chosen audio/subtitle
   indexes are matched by `(language, codec, title, external, forced, HI)` against the source actually played. An
   unmatched track is dropped (no subtitle / default audio) rather than playing another language.

Changing how the playback request is built requires keeping step 1's request identical, or tracks will mismatch.

## Subtitles *(fork additions)*

- Per-user fallback subtitle language (`JellyfinUserPreferences.fallbackSubtitleLanguage`, Room v42), used by
  `StreamChoiceService` when nothing matches the preferred language. `isSameLanguage` treats ISO 639-2 B/T pairs
  (e.g. `rum`/`ron`) as equal.
- `getSubtitleStreams` (`CurrentMediaInfo.kt`) orders: preferred embedded, preferred external, fallback embedded,
  fallback external, rest — stable within groups. `only_preferred_subtitles` marks the rest `hidden`; the current
  track always stays visible in the dialog.
- Controls show a "SUB" text button instead of the CC icon.

## Choose version while playing *(fork)*

`PlaybackDialogType.VERSION` in `PlaybackDialog.kt` lists `mediaSources`; `PlaybackAction.ChangeVersion` →
`PlaybackViewModel.changeMediaSource(sourceId)`.

## Dolby Vision profile 7 → 8.1 *(fork)*

For devices with a DV decoder that does not list profile 7 (e.g. MediaTek TVs). Two halves that must agree, both
gated by `DolbyVisionProfile7Conversion.isWanted(experimental prefs, MediaCodecCapabilitiesTest)` in
`W/util/DolbyVisionProfile7Conversion.kt`:

| Half | File | Effect |
|---|---|---|
| Extractor | `W/util/DolbyVisionProfile7ExtractorsFactory.kt` | Rewrites `Format.codecs` `dvhe.07.xx` → `dvhe.08.xx` so media3 picks the DV decoder. Applied outermost in `PlayerFactory` (also wraps libass's MKV extractor). Skips DRM tracks |
| Bitstream | `W/util/DolbyVisionProfile7Filter.kt` | In `BitstreamFilteringCodecAdapterFactory`, drops NAL type 63 (EL) and rewrites the RPU (type 62) via libdovi (`LibDoviRpuRewriter`, from `exoplayerhdrutils`). Runs before `Hdr10PlusMaskingFilter` |
| Device profile | `DeviceProfileService` | Adds DV profile 7 range types so the server direct-plays instead of transcoding |

Setting: `ExperimentalPreferences.convert_dolby_vision_profile_7`. New installs get both it and
`ExperimentalPreferences.enabled` set to true in `AppPreferencesSerializer` (the conversion is gated by `enabled`). ExoPlayer only — MPV is unaffected. Tests: `app/src/test/.../util/DolbyVisionProfile7*Test.kt`.
Failure mode: an access unit libdovi refuses passes through untouched; `LinkageError` on ABIs without the native lib.

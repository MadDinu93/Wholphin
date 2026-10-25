# Thorphin - an Android TV client built for Remux

Thorphin is a fork of [Wholphin](https://github.com/damontecres/Wholphin), the open-source Android TV client for Jellyfin. It aims to be the main player for [Remux](https://github.com/lostb1t/remux), with an interface closer to the Apple TV apps.

Remux is a self-hosted media server that speaks the Jellyfin API. Instead of scanning a library of files, it resolves streams on demand from Stremio addons, WebDAV shares and local storage. Jellyfin clients can connect to it, but they assume a Jellyfin server and miss things such as the subtitles addons provide. Thorphin fixes those gaps, and plays the Dolby Vision remuxes these catalogs are full of on TVs that cannot play them in Dolby Vision on their own.

Thorphin still works with a regular Jellyfin server. Everything Wholphin does is still here: ExoPlayer and MPV playback, a customizable home page, Live TV, music, Seerr, trickplay and more, see the [Wholphin README](https://github.com/damontecres/Wholphin#features).

## What Thorphin adds

### Remux

- **Addon subtitles.** Remux lists subtitle streams, including the external ones from addons, only in the PlaybackInfo response and not in the item's media sources. Thorphin reads them from there, so they show up in the subtitle menu and in the automatic subtitle choice.
- **The right tracks for the release that plays.** Remux picks the release and numbers the subtitle tracks based on the device profile and bitrate limit. Thorphin sends the same ones as the playback request, then matches the chosen audio and subtitle tracks against the source actually played. An "English" entry plays English instead of another language from a different release.
- **A Library page.** Remux catalogs often add dozens of libraries. Instead of one navigation row per library, a single Library page shows them all. You can still pin individual libraries.

### Dolby Vision

- **Profile 7 to 8.1 conversion.** UHD Blu-ray remuxes use Dolby Vision profile 7, which many TV decoders (MediaTek ones, for example) do not play in Dolby Vision. Thorphin rewrites the RPU into its profile 8.1 form and drops the enhancement layer while playing, the way Kodi, Vimu and Dune do. Nothing is re-encoded. It is on by default for new installs (Settings > Experimental) and only applies to devices without a profile 7 decoder.

### Apple TV style interface

- **Top navigation bar.** Pill tabs (Home, Library, pinned items, Search, Settings) with the profile on the left replace the side drawer. The bar slides away while you browse the rows. The side drawer is still available under Settings > Interface > Navigation style.
- **Hero card** (optional). The details of the focused item are shown above the rows: logo, year, ratings, runtime, genres and overview.
- **Background trailers.** After 5 seconds on an item, its trailer replaces the hero image, with sound, Netflix style. Thorphin uses a trailer on the server when there is one, otherwise the YouTube trailer from the metadata, shown without any YouTube player controls. It can be turned off, or muted, in Settings > Interface.
- **Cards.** Rounder posters with "Genre · ★ rating" along the bottom, and titles with the year below them.

## Installation

There are no store listings or release builds yet. Build the APK and sideload it:

```sh
./gradlew :app:assembleAppstoreDebug
adb install -r app/build/outputs/apk/appstore/debug/Thorphin-appstore-debug-<version>-armeabi-v7a.apk
```

- Pick the APK matching the device's ABI (`adb shell getprop ro.product.cpu.abilist`). Most TVs are `armeabi-v7a` or `arm64-v8a`.
- Use the `appstore` flavor. The built-in updater of the `default` flavor still checks Wholphin's releases.
- A release build (`assembleAppstoreRelease`) needs a signing config, set with the `release.signing.config` Gradle property, or the APK comes out unsigned.
- The debug build installs as a separate app (`io.github.maddinu93.thorphin.debug`) next to the release one (`io.github.maddinu93.thorphin`).

See [DEVELOPMENT.md](DEVELOPMENT.md) for the development setup.

## Compatibility

- Android 6+ / Fire TV OS 6+.
- Remux, or a Jellyfin server `10.10.x` / `10.11.x`.
- Tested mostly on a Philips Google TV (TA5, MediaTek).

## Acknowledgements

- [Wholphin](https://github.com/damontecres/Wholphin) by damontecres, which Thorphin is built on. Almost all of the app is their work.
- [Remux](https://github.com/lostb1t/remux) by lostb1t.
- The Jellyfin team, the official Jellyfin Android TV client developers and the Jellyfin Kotlin SDK developers, as credited by Wholphin.
- ExoplayerHdrUtils (`com.suyashbelekar:exoplayerhdrutils`, libdovi), used for the Dolby Vision conversion.

## License

Thorphin is licensed under the GNU General Public License v2, like Wholphin. See [LICENSE](LICENSE).

# Build, test and release

Authoritative files: [app/build.gradle.kts](../../app/build.gradle.kts),
[gradle/libs.versions.toml](../../gradle/libs.versions.toml), [settings.gradle.kts](../../settings.gradle.kts),
[.pre-commit-config.yaml](../../.pre-commit-config.yaml), [.github/workflows/](../../.github/workflows). Read them for
versions instead of copying them here. Upstream setup notes: [DEVELOPMENT.md](../../DEVELOPMENT.md).

## Environment

JDK 21 works (verified locally; code targets JVM 11). Android SDK path in `local.properties` (`sdk.dir`). Gradle
wrapper (`./gradlew`, or `gradlew.bat` on Windows). The Gradle root project is still named `Wholphin`.

## Variants

- Flavors (`version` dimension): `default` (in-app updater on, no leanback requirement), `appstore` (leanback, no
  updater — the Play Store / sideload flavor), `firetv` (leanback, no updater, no Discover/Seerr). Feature flags are
  `BuildConfig.UPDATING_ENABLED` / `DISCOVER_ENABLED`.
- `debug`: `applicationIdSuffix = ".debug"`, label "Thorphin (Debug)" from `app/src/debug/res/values/strings.xml`.
- APK names: `Thorphin-<flavor>-<buildType>-<versionName>-<versionCode>[-<abi>].apk`; ABI splits
  (`armeabi-v7a`, `arm64-v8a`, `x86_64`, universal) are disabled when building bundles.

## Versioning *(fork)*

`versionCode` = `git rev-list --count HEAD`; `versionName` from `git describe --tags --long --match=v*`. A shallow
clone or missing tags change both. `HomeRowsCache` discards its cache when `versionCode` changes.

Thorphin continues the upstream number line instead of restarting at 0.x: the first fork release is `v1.1.0` (on top
of Wholphin 1.0.9). Installed copies already report `1.0.9-N-g…`, so a lower version would never be offered by the
in-app updater, and `AppUpgradeHandler` migrations are keyed on that line. Release with semver tags: minor for
features, patch for fixes (`git tag -a vX.Y.Z`, then push the tag to the `thorphin` remote). Do not reuse upstream
tag numbers above 1.0.9 for upstream merges; Thorphin's `1.1.x` stays ahead of them.

## Common commands

| Goal | Command |
|---|---|
| Debug APK to sideload | `./gradlew :app:assembleAppstoreDebug` |
| What CI runs on PRs | `./gradlew clean assembleDefaultDebug testDefaultDebugUnitTest` |
| Unit tests (one class pattern) | `./gradlew :app:testDefaultDebugUnitTest --tests 'com.github.damontecres.wholphin.util.DolbyVisionProfile7*'` |
| Play bundle | `./gradlew :app:bundleAppstoreRelease` (see [store/README.md](../../store/README.md)) |
| Baseline profile | one `am instrument` run per home screen style, see [Baseline profile](#baseline-profile-fork) |
| Lint/format | `pre-commit run --all-files` (ktlint 1.8.0 via `pretty-format-kotlin`, also rejects `val DEBUG = true` / `debugLogging = true`) |

Validated during the documentation audit (2026-10-29): the unit-test command above passed (25 tests, 3 classes,
~2 min incl. compile). No other command was executed.

## Tests

- Unit tests: `app/src/test/java/com/github/damontecres/wholphin/` (Robolectric, MockK, Hilt testing; `ui/TestModule.kt`
  provides fakes). Fork-added: `util/DolbyVisionProfile7{Conversion,ExtractorsFactory,Filter}Test.kt`.
- Instrumented: `app/src/androidTest/...` (`WholphinTestRunner`), need a device.
- No tests exist for the fork's UI, trailers, home cache or PlaybackInfo stream matching.
- Room migrations: `TestDbMigrations` uses exported schemas in `app/schemas/` (v42 is the fork's).

## Native extensions

ffmpeg / av1 decoders and libmpv come from `app/libs/*.aar` if present, else from the `wholphin-extensions` Maven repo
when the `WholphinExtensionsUsername` Gradle property is set, else ffmpeg/av1 are omitted and MPV uses
`:wholphin-mpv-stub` (throws at runtime). Builds work without them; playback coverage is reduced.

## Signing and release *(fork)*

- CI signs when `CI` and `KEY_ALIAS` env vars are set (upstream mechanism).
- Locally, `local.properties` may define `upload.storeFile`, `upload.storePassword`, `upload.keyAlias`,
  `upload.keyPassword` and `release.signing.config=upload` → the `upload` signing config for Play uploads. Never commit
  `local.properties` or keystores (both ignored).
- `nonMinifiedRelease` / `benchmarkRelease` (baseline-profile build types) are forced to the debug key.
- Store assets and listing text: `store/` ( `store/screenshots/` is ignored).

## Baseline profile *(fork)*

Module `:baselineprofile` (`BaselineProfileGenerator.kt`), `useConnectedDevices = true` (needs a real TV/device, no
Gradle-managed emulator), profiles only the `default` flavor and merges into `main`
(`app/src/main/generated/baselineProfiles/`, committed).

The generator drives the TV with the D-pad and finds the rail / top bar entries by their labels (English or
Romanian). In one home screen style it browses Home, Library or Movies/TV shows, catalogues and Favorites, opens a few
titles and plays a movie and a series for some seconds; Search, Settings and the sort / filter / view menus are not
visited. It needs a signed-in server with favorites, and leaves the played titles in progress.

`generateBaselineProfile` reinstalls the app, which loses the sign-in, and one run through all six styles is not
reliable on a real TV (the style dialog is a separate window, keys get lost after a style change). What works is one
`am instrument` run per style, with the APKs installed by hand and the app signed in once:

1. `./gradlew :app:assembleDefaultNonMinifiedRelease :baselineprofile:assembleDefaultNonMinifiedRelease`, then
   `adb install -r` the app APK (armeabi-v7a on the Philips TV) and `adb install -r -t` the baselineprofile APK.
2. Set the home screen style in the app (Settings → Appearance), then run
   `adb shell am instrument -w -e look <style number> -e apply false -e androidx.benchmark.enabledRules baselineprofile -e class com.github.damontecres.wholphin.baselineprofile.BaselineProfileGenerator com.github.damontecres.wholphin.baselineprofile/androidx.test.runner.AndroidJUnitRunner`
   (style numbers as in `HomeScreenStyle`: 0 Classic … 5 Minimalist Cards; without `-e apply false` the test sets the
   style itself, which works for the top bar styles only).
3. Pull `/sdcard/Android/media/com.github.damontecres.wholphin.baselineprofile/BaselineProfileGenerator_generate-startup-prof.txt`
   after each run (it holds the whole profile).
4. Merge the six files into `baseline-prof.txt` and `startup-prof.txt` (both the same): `awk 1 look*.txt | sort -u`;
   the pulled files do not end with a newline, plain `cat` joins lines and R8 rejects the profile. Check with
   `./gradlew :app:minifyDefaultReleaseWithR8`.

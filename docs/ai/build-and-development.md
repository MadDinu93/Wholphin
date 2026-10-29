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

## Common commands

| Goal | Command |
|---|---|
| Debug APK to sideload | `./gradlew :app:assembleAppstoreDebug` |
| What CI runs on PRs | `./gradlew clean assembleDefaultDebug testDefaultDebugUnitTest` |
| Unit tests (one class pattern) | `./gradlew :app:testDefaultDebugUnitTest --tests 'com.github.damontecres.wholphin.util.DolbyVisionProfile7*'` |
| Play bundle | `./gradlew :app:bundleAppstoreRelease` (see [store/README.md](../../store/README.md)) |
| Baseline profile | `./gradlew :app:generateBaselineProfile` with a TV connected over adb *(task exists; not run during this audit)* |
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

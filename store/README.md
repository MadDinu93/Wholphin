# Google Play listing

Everything the Play Console asks for, ready to copy. The app is published from the `appstore` flavor:

```
./gradlew :app:bundleAppstoreRelease
```

The bundle is in `app/build/outputs/bundle/appstoreRelease/`, signed with the upload key set in `local.properties` (`release.signing.config=upload`). Google re-signs it with the app signing key it keeps (Play App Signing).

## Graphics

| Play Console field | File |
|---|---|
| App icon (512 × 512) | `icon-512x512.png` |
| Feature graphic (1024 × 500) | `feature-graphic-1024x500.png` |
| TV banner (1280 × 720) | `tv-banner-1280x720.png` |
| TV screenshots (1920 × 1080) | `screenshots/` |

## Store listing

**App name:** Thorphin

**Short description** (80 characters at most)

- English: `A Jellyfin client for Android TV with an Apple TV style interface`
- Romanian: `Client Jellyfin pentru Android TV, cu o interfață în stilul Apple TV`

**Full description, English**

```
Thorphin is a client for your own Jellyfin media server, made for the TV and the remote.

• A clean, Apple TV inspired look: glass effects on focus, a top navigation bar and a calm grey theme that lets the artwork stand out.
• A home page you arrange yourself, with an optional hero that shows the details of the focused title, and trailers that play behind it or inside the card.
• Progress, time left and watched marks on every card, with the genre and rating of movies and series.
• Plays almost anything: ExoPlayer with FFmpeg and AV1 decoders, or MPV, with HDR10, HDR10+ and Dolby Vision support, subtitles including ASS and PGS, and skipping of intros and recaps.
• Next up, continue watching, live TV and music.

Thorphin needs a Jellyfin server, it does not provide any content.

Thorphin is based on Wholphin, the open source Jellyfin client by damontecres, and is licensed under the GPL v2.
```

**Full description, Romanian**

```
Thorphin este un client pentru propriul tău server media Jellyfin, făcut pentru televizor și telecomandă.

• Un aspect curat, inspirat de Apple TV: efecte de sticlă la focus, o bară de navigare sus și o temă gri care lasă imaginile să iasă în evidență.
• O pagină principală pe care o aranjezi singur, cu un hero opțional cu detaliile titlului selectat și trailere care rulează în spatele lui sau în card.
• Progresul, timpul rămas și marcajul de văzut pe fiecare card, plus genul și nota filmelor și serialelor.
• Redă aproape orice: ExoPlayer cu decodoare FFmpeg și AV1 sau MPV, cu HDR10, HDR10+ și Dolby Vision, subtitrări inclusiv ASS și PGS, și sărirea introurilor și a rezumatelor.
• Episodul următor, continuă vizionarea, TV live și muzică.

Thorphin are nevoie de un server Jellyfin și nu oferă niciun conținut.

Thorphin se bazează pe Wholphin, clientul Jellyfin open source creat de damontecres, și este licențiat sub GPL v2.
```

**Category:** Video Players & Editors. **Tags:** Video player, Streaming.

## Data safety

The app has no backend of its own. Everything it sends goes to servers the user signs in to: their Jellyfin server, and their Seerr server if they set one up.

| Question | Answer | Why |
|---|---|---|
| Does the app collect or share user data? | Yes, collected | The crash and app log reports below are sent off the device |
| Crash logs | Collected, optional, not shared | Sent to the user's own Jellyfin server, only after they agree in the crash dialog |
| Diagnostics | Collected, optional, not shared | App logs, sent to the user's own Jellyfin server only from Settings → System |
| Is data encrypted in transit? | Yes, when the server uses HTTPS | The connection is to the address the user enters |
| Can users ask for data to be deleted? | Yes | The reports are on the user's own server, they delete them there |
| Audio (voice search) | Not collected | Speech is turned into text by Android's speech recognition service, the app only receives the text |

Account details (server address, user name, access token) stay on the device and on the user's server, and are not collected by the developer.

## Content rating

Answer the questionnaire as a media player that shows content from the user's own server: no user generated content shared with others, no ads, no purchases. Select that the app does not contain the content itself.

## Privacy policy

Published at **https://trailers.mad-homelab.app/privacy**, served by the Thorphin Trailers add-on. The text is in `privacy-policy.md` here and in `privacy.html` of the add-on; change both, then redeploy the add-on.

## Closed testing

1. Testing → Closed testing → Create track (or use Internal testing for up to 100 testers without review).
2. Upload the `.aab`, add release notes.
3. Testers: an email list or a Google Group. Send them the opt-in link, they accept it in a browser and then find Thorphin in the Play Store on their TV.
4. New personal developer accounts must run a closed test with at least 12 testers for 14 days before they can publish to production.

A device that has Thorphin installed from another source (such as a debug or sideloaded build) must uninstall it before installing from Play, because Play builds are signed with a different key.

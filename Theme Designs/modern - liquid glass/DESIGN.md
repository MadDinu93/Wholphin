---
name: Aetheria TV
colors:
  surface: '#121316'
  surface-dim: '#121316'
  surface-bright: '#38393d'
  surface-container-lowest: '#0d0e11'
  surface-container-low: '#1b1b1f'
  surface-container: '#1f1f23'
  surface-container-high: '#292a2d'
  surface-container-highest: '#343538'
  on-surface: '#e3e2e6'
  on-surface-variant: '#c7c4d7'
  inverse-surface: '#e3e2e6'
  inverse-on-surface: '#2f3034'
  outline: '#918fa0'
  outline-variant: '#464554'
  surface-tint: '#c2c1ff'
  primary: '#c2c1ff'
  on-primary: '#1a00a7'
  primary-container: '#8482ff'
  on-primary-container: '#150093'
  inverse-primary: '#4e4ad4'
  secondary: '#c5c6d1'
  on-secondary: '#2e3039'
  secondary-container: '#474952'
  on-secondary-container: '#b7b8c2'
  tertiary: '#ffb873'
  on-tertiary: '#4a2800'
  tertiary-container: '#d17c12'
  on-tertiary-container: '#412200'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#e2dfff'
  primary-fixed-dim: '#c2c1ff'
  on-primary-fixed: '#0c006a'
  on-primary-fixed-variant: '#342dbc'
  secondary-fixed: '#e1e1ed'
  secondary-fixed-dim: '#c5c6d1'
  on-secondary-fixed: '#191b23'
  on-secondary-fixed-variant: '#45464f'
  tertiary-fixed: '#ffdcbf'
  tertiary-fixed-dim: '#ffb873'
  on-tertiary-fixed: '#2d1600'
  on-tertiary-fixed-variant: '#6a3b00'
  background: '#121316'
  on-background: '#e3e2e6'
  surface-variant: '#343538'
typography:
  display-hero:
    fontFamily: Inter
    fontSize: 56px
    fontWeight: '800'
    lineHeight: 64px
    letterSpacing: -0.02em
  headline-xl:
    fontFamily: Inter
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 48px
    letterSpacing: -0.015em
  headline-lg:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 30px
    letterSpacing: 0em
  body-lg:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '400'
    lineHeight: 30px
    letterSpacing: 0em
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.01em
  label-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: 0.02em
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 18px
    letterSpacing: 0.04em
  badge-spec:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.08em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1.75rem
  margin: 3.5rem
  space-xs: 0.5rem
  space-sm: 0.75rem
  space-md: 1.25rem
  space-lg: 2rem
  space-xl: 3rem
---

## Brand & Style

This design system defines an immersive, cinematic media center interface specifically calibrated for the 10-foot viewing experience on Android TV (1080p canvas with dynamic 4K upscaling). It blends the fluid material refraction of Liquid Glass with the stark, theatre-grade focus of premium streaming interfaces. 

### Brand Personality & Core Tenets
- **Cinematic Submersion:** The interface acts as a transparent lens over rich visual media. UI structures exist only to orient and elevate content, dissolving when idle.
- **Precision Living-Room Ergonomics:** Designed entirely around directional D-pad navigation, instantaneous feedback, and effortless readability from 3 meters away.
- **Sculpted Glass Refraction:** Surfaces utilize optical highlights, specular rim lighting, and deep charcoal frosting to create clear depth hierarchy without visual noise or flat opacity layers.

### Visual Aesthetic
The style combines **Glassmorphism** with **Atmospheric Vignettes**. Deep obsidian and twilight-charcoal foundations are overlaid with soft frosted glass capsules, glowing focus highlights, and dynamic ambient glows that project the dominant chromatic tones of the active media trailer across the surface.

## Colors

The palette establishes an absolute contrast environment tuned for OLED and HDR living-room displays, ensuring deep black levels and sharp contrast without eye strain.

### Surface System
- **Canvas Base:** `#08090C` (Absolute deep neutral black; anchors the canvas and hero falloffs).
- **Secondary Base:** `#111218` (Recessed containers, horizontal rail backdrops, drawer states).
- **Elevated Surface:** `#191B23` (Card backings, metadata drawer surfaces, modal dialogs).
- **Liquid Glass Surface:** `rgba(17, 18, 24, 0.72)` paired with `backdrop-filter: blur(28px) saturate(140%)`.

### Accents & Dynamic Focus
- **Primary Accent:** `#7775FF` (Vibrant electric periwinkle; acts as the primary glow driver, active playheads, and selection indicators).
- **Active Focus Ring:** `#F5F5F7` solid highlight with an outer dynamic aura of `rgba(119, 117, 255, 0.45)`.
- **Specular Glass Highlight:** `rgba(255, 255, 255, 0.16)` applied on top-edge interior borders for liquid reflection.
- **Glass Rim Border:** `rgba(255, 255, 255, 0.12)` for standard unselected component delineations.

### Typography & Content
- **Text Primary:** `#F5F5F7` (Pure display contrast ratio of 16.5:1 against base).
- **Text Secondary:** `#A7A9B4` (De-emphasized metadata, running time, cast & crew).
- **Text Tertiary / Disabled:** `rgba(167, 169, 180, 0.45)`.
- **Media Badging (4K, HDR, Atmos):** Background `rgba(255, 255, 255, 0.10)`, text `#F5F5F7`, border `rgba(255, 255, 255, 0.20)`.

## Typography

Typography is calibrated to avoid anti-aliasing shimmer on high-resolution television screens while ensuring immediate scannability from 8 to 12 feet away.

- **Baseline Font:** Inter is utilized across all tiers for its tall x-height, neutral aperture, and precise kerning at display scales.
- **10-Foot Readability Strategy:** No critical interactive text is sized below `16px`. Informational badges use uppercase styling at `12px` with bold weights and generous tracking (`+0.08em`) to guarantee crisp rendering across various TV panel configurations.
- **Hero Stacking:** `display-hero` uses optical kerning and tight negative tracking (`-0.02em`) to produce maximum visual impact for title logos and primary content highlights.

## Layout & Spacing

The canvas is tailored for a fixed 1920x1080 (16:9) viewport, adhering strictly to television overscan standards.

### TV Safe Zones & Grid Geometry
- **Overscan Margin:** A mandatory outer safe zone of `3.5rem` (56px) on horizontal borders and `3rem` (48px) on vertical borders protects against panel edge clipping.
- **Rail Layout:** Horizontal rails implement fixed-height tracks with dynamic horizontal off-screen spillover. Column gutters default to `1.75rem` (28px) between cards, accommodating scale transforms during D-pad focus states without overlapping adjacent media cards.
- **Vertical Rhythm:** Vertical category rails are separated by `3rem` (48px) of vertical space. Hero sections consume the top 640px of canvas space, with smooth vertical gradient falloffs fading into the first content rail.

## Elevation & Depth

Visual hierarchy on Android TV relies on spatial transformation and layered optical light physics rather than flat z-index shifting.

### Optical Layering (Z-Index Scale)
1. **Background Layer (Level 0):** Native `#08090C` canvas and ambient media backdrops with heavy horizontal/vertical vignettes (`rgba(8,9,12,0) 0%, rgba(8,9,12,0.95) 100%`).
2. **Resting Rails & Cards (Level 1):** Flat surfaces with low-contrast glass borders (`rgba(255, 255, 255, 0.12)`).
3. **Floating Topbar Navigation (Level 2):** Capsule pill floating 48px from the top edge with `rgba(17, 18, 24, 0.72)` fill, 28px backdrop blur, specular top highlight, and `0 16px 32px rgba(0, 0, 0, 0.6)`.
4. **Focused D-Pad Target (Level 3 - Interactive Peak):** Scale up to `1.08x` with an elevated shadow of `0 24px 48px rgba(0, 0, 0, 0.8)`, an electric halo of `0 0 32px rgba(119, 117, 255, 0.5)`, and an interior white perimeter ring.

## Shapes

The design system maintains a refined, architectural curvature with a roundedness factor of 2.

- **Base Corner Radius (0.5rem / 8px):** Applied to metadata badges, inline progress trackers, and small media indicators.
- **Card Radius (1rem / 16px):** Standard across horizontal landscape cards, 2:3 vertical posters, and modal prompt panels.
- **Capsule Pill Radius (9999px):** Applied exclusively to floating navigation menus, primary hero action buttons, and active filter chips.

## Components

### 1. Floating Capsule Liquid Glass Topbar
- **Structure:** Floating, centered horizontal capsule bar situated 48px from the canvas top.
- **Surface:** `rgba(17, 18, 24, 0.72)` background, `backdrop-filter: blur(28px)`, subtle border of `1px solid rgba(255, 255, 255, 0.12)`, top inner highlight `1px inset rgba(255, 255, 255, 0.16)`.
- **Items:** Capsule pill buttons with `label-lg` typography. Unfocused items render in `#A7A9B4`. Focused item transitions to a luminous white background `#F5F5F7` with deep charcoal `#08090C` typography and a soft `#7775FF` outer bloom.

### 2. Cinematic Hero Trailer Container
- **Structure:** Full-width container occupying 640px height with trailer video/still backdrop.
- **Vignettes:** Multi-stop masking gradient: bottom linear fade `to bottom, transparent 0%, #08090C 95%`, and left radial gradient `rgba(8,9,12,0.85) 0%, transparent 70%` protecting metadata text contrast.
- **Content Block:** Aligned to the left margin safe zone; houses studio brand watermark, `display-hero` title, high-contrast badges (4K UHD, HDR10+, Dolby Vision, Dolby Atmos), short synopsis in `body-lg`, and primary action triggers.

### 3. Action Buttons & Focus Engine
- **Primary Hero Button:** Pill shape, 56px height, minimum width 160px.
  - *Default:* Glass elevated container (`#191B23`) with white icon and `#F5F5F7` label.
  - *D-Pad Focused:* Translates `scale(1.08)`. Surface transitions to `#F5F5F7` with dark `#08090C` text, a 2px outer outline in `#F5F5F7`, and a 24px diffuse aura in `#7775FF`.
- **Secondary Action Button:** Circular 56px glass button for "Add to List", "Trailer", and "Info" with matching D-pad focus scaling and illumination.

### 4. Horizontal Media Rails
- **Landscape "Continue Watching" Rail (16:9):**
  - Dimensions: 384px width by 216px height.
  - Includes bottom overlay progress bar: 4px track height, unplayed track `rgba(255,255,255,0.2)`, watched track filled with `#7775FF`.
  - When focused: Card scales to `1.06x`, borders illuminate to `#F5F5F7`, and progress bar increases to 6px height with an active play indicator glow.
- **Portrait Movie Poster Rail (2:3):**
  - Dimensions: 220px width by 330px height.
  - Surface: High-resolution media cover with 16px corner radius and 1px glass rim border.
  - Focused State: Card scales up, raising its z-index above neighbor cards, casting an ambient shadow and revealing an immediate contextual preview drawer below.

### 5. High-Contrast Metadata Badges
- **Form:** 24px height, inline-flex, 4px corner radius.
- **Styling:** Inset `1px solid rgba(255, 255, 255, 0.20)`, background `rgba(255, 255, 255, 0.08)`, text `#F5F5F7`, `badge-spec` uppercase font.
- **Types:** Resolution (`4K`), Dynamic Range (`HDR`, `DV`), Audio (`ATMOS`, `5.1`), Age Rating (`PG-13`, `TV-MA`).

### 6. Spacious TV Content Grid
- **Geometry:** 5-column grid layout for full-catalogue views with 28px horizontal and vertical gaps.
- **Navigation Behavior:** Scroll viewport maintains the focused card centered vertically and horizontally to prevent motion disorientation on large screens.
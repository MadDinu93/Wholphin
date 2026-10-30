---
name: Thorphin TV
colors:
  surface: '#111317'
  surface-dim: '#111317'
  surface-bright: '#37393e'
  surface-container-lowest: '#0c0e12'
  surface-container-low: '#1a1c20'
  surface-container: '#1e2024'
  surface-container-high: '#282a2e'
  surface-container-highest: '#333539'
  on-surface: '#e2e2e8'
  on-surface-variant: '#d4c4af'
  inverse-surface: '#e2e2e8'
  inverse-on-surface: '#2f3035'
  outline: '#9c8f7b'
  outline-variant: '#504535'
  surface-tint: '#f9bc49'
  primary: '#ffd796'
  on-primary: '#422c00'
  primary-container: '#f3b744'
  on-primary-container: '#694900'
  inverse-primary: '#7d5700'
  secondary: '#b9c8de'
  on-secondary: '#233143'
  secondary-container: '#39485a'
  on-secondary-container: '#a7b6cc'
  tertiary: '#b8e3ff'
  on-tertiary: '#00354a'
  tertiary-container: '#6bccff'
  on-tertiary-container: '#005575'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffdeaa'
  primary-fixed-dim: '#f9bc49'
  on-primary-fixed: '#271900'
  on-primary-fixed-variant: '#5f4100'
  secondary-fixed: '#d4e4fa'
  secondary-fixed-dim: '#b9c8de'
  on-secondary-fixed: '#0d1c2d'
  on-secondary-fixed-variant: '#39485a'
  tertiary-fixed: '#c4e7ff'
  tertiary-fixed-dim: '#7bd0ff'
  on-tertiary-fixed: '#001e2c'
  on-tertiary-fixed-variant: '#004c69'
  background: '#111317'
  on-background: '#e2e2e8'
  surface-variant: '#333539'
typography:
  display-xl:
    fontFamily: Space Grotesk
    fontSize: 56px
    fontWeight: '700'
    lineHeight: 64px
  display-lg:
    fontFamily: Space Grotesk
    fontSize: 44px
    fontWeight: '700'
    lineHeight: 52px
  headline-lg:
    fontFamily: Space Grotesk
    fontSize: 36px
    fontWeight: '600'
    lineHeight: 44px
  headline-md:
    fontFamily: Space Grotesk
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
  headline-sm:
    fontFamily: Space Grotesk
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Space Grotesk
    fontSize: 20px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Space Grotesk
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-lg:
    fontFamily: Space Grotesk
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 20px
  label-md:
    fontFamily: Space Grotesk
    fontSize: 13px
    fontWeight: '600'
    lineHeight: 16px
  label-sm:
    fontFamily: Space Grotesk
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 14px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  gutter: 1.5rem
  margin: 3.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2.5rem
---

## Brand & Style
The design system establishes a premium, theater-grade 10-foot television experience tailored for lean-back spatial navigation. It merges Nordic minimalism with cinematic restraint, stripping away visual clutter to elevate high-resolution cover art, rich typography, and metadata clarity. The visual language evokes the stillness of a high-end private cinema: deep obsidian environments, razor-sharp typographic hierarchies readable across a living room, and warm amber illumination simulating optical projection warmth.

The aesthetic combines restrained matte surfaces, structural precision, and subtle dark glassmorphism. Interfaces remain dark and quiet until summoned via remote control. Every interaction is architected specifically around directional pad (D-pad) physics—delivering immediate spatial feedback through micro-scaling, ambient amber focus halos, and deliberate border illumination, never leaving the viewer disoriented across a 4K canvas.

## Colors
The palette is calibrated strictly for high dynamic range displays, OLED deep-blacks, and ambient low-light room viewing. 

### Canvas & Surfaces
- **Void Canvas (`#0B0D11`):** The true baseline canvas. Absorbs display edges and provides an infinite-depth backdrop for full-bleed backdrops and poster art.
- **Surface Low (`#12151C`):** Rails, inactive sidebars, and structural container tracks.
- **Surface Mid (`#181C26`):** Standard resting state for unselected media cards, metadata chips, and modal sheets.
- **Surface High (`#222736`):** Elevated cards, active playback overlays, and header bars.

### Primary Accents & Focus
- **Nordic Amber (`#F3B744`):** The active spatial beacon. Reserved strictly for active D-pad focus outlines, scrubbing playheads, primary action pills, and active filter states.
- **Amber Glow (`rgba(243, 183, 68, 0.25)`): Optical bloom applied underneath focused tiles to simulate backlighting.

### Secondary & Semantic Accents
- **Muted Ice / Slate (`#94A3B8`):** Unfocused labels, sub-metadata (year, runtime, audio codecs), and secondary icons.
- **Atmospheric Blue (`#38BDF8`):** Technical badges (Dolby Vision, HDR10+, DTS-HD, 4K UHD indicators).
- **Destructive / Live (`#EF4444`):** Live recording dots and item deletion states.

## Typography
Typography is engineered for legibility at a standard 10-foot viewing distance. `Space Grotesk` is selected for its technological character, robust geometry, and wide counters, ensuring that technical video specs, episode synopses, and media titles remain crisp and readable on large-format 1080p and 4K displays.

- **Display XL & LG:** Dedicated to featured hero titles on the home canvas and media detail splash screens.
- **Headline Levels:** Drive shelf headers ("Continue Watching", "Recently Added Movies"), row titles, and dialog anchors.
- **Body Large & Medium:** Engineered for overview paragraphs, director notes, and user reviews with generous line spacing to prevent optical fatigue.
- **Label Scales:** Used for audio/video codec pills, duration chips, release years, and D-pad control legends. Labels utilize uppercase tracking (`letter-spacing: 0.05em`) for enhanced instant scanning.

## Layout & Spacing
Television safe-zones mandate an uncompromising boundary model. The layout strictly enforces an overscan margin of `3.5rem` (56dp) horizontally and vertically across the entire viewport. All primary media shelves and active interaction targets remain within this perimeter.

- **Horizontal Shelf Rail:** Infinite horizontal scrolling rows powered by standard TV lazy lists. Items spaced via `gutter` (1.5rem / 24dp).
- **Vertical Shelf Stacking:** Consecutive media shelves are separated by `space-xl` (2.5rem / 40dp) to allow dynamic card scaling on focus without clipping neighboring rows.
- **Navigation Dock:** Collapsed left rail at resting state (`width: 4rem`), expanding smoothly to `18rem` on explicit leftward D-pad navigate, overlaying content with a frosted backdrop.
- **Aspect Ratio Modules:** 
  - Poster Cards: 2:3 ratio.
  - Landscape/Episode Cards: 16:9 ratio.
  - Music/Artist Tiles: 1:1 ratio.

## Elevation & Depth
Depth on a 10-foot UI is dynamic rather than static, communicating spatial location directly to the user's remote actions. The system abandons heavy skeuomorphic drop-shadows in favor of tonal stepping, subtle luminescence, and physical card lift.

- **Resting Layer (Elevation 0):** Media items sit flat within their shelves on `Surface Mid` (`#181C26`) with a 1px ghost border of `rgba(255, 255, 255, 0.06)`.
- **D-Pad Focused Layer (Elevation 1):** Upon receiving spatial focus, the card scales up by 1.08x (`transform: scale(1.08)`), elevates above adjacent siblings in the z-index, receives an outer border of 2.5px solid Nordic Amber (`#F3B744`), and projects an ambient diffuse glow: `0 8px 32px rgba(243, 183, 68, 0.22), 0 2px 8px rgba(0, 0, 0, 0.8)`.
- **Media Player OSD (Elevation 2):** On-screen display bars and playback scrubbers float atop running video with a glassmorphic shield: background `rgba(11, 13, 17, 0.82)`, backdrop-filter `blur(24px)`, and an upper edge rim-light of `1px solid rgba(255, 255, 255, 0.12)`.

## Shapes
The design uses restrained, architectural corners. Media cards, dialogs, and video surfaces employ crisp 6px to 8px radii (`roundedness: 1`), keeping the visual tone technical, cinematic, and disciplined. Pill-shaped rounding is reserved exclusively for interactive button tags and playback scrubbers to delineate operable controls from media art.

## Components

### Media Poster & Landscape Cards
- **Resting:** Flat aspect container (2:3 or 16:9), 8px corner radius, image masked cleanly. Subtle bottom gradient overlay for title text if rendered over artwork.
- **Focused:** Scaled 1.08x smoothly over 150ms. High-contrast 2.5px solid `#F3B744` stroke. Amber ambient glow beneath. Title shifts from secondary ice `#94A3B8` to pure white `#FFFFFF` with bold weight.
- **Badge Accents:** Resolution and codec chips (e.g., "4K", "HDR", "DV") anchored at top-right with 4px inner padding, `Surface Low` background at 85% opacity, and clean typographic labels.

### Interactive Buttons & Remote Triggers
- **Primary Button (Play/Resume):** Pill shape. Background `#F3B744`, text `#0B0D11` in weight 700. When focused, expands by 1.05x with white rim-highlight.
- **Secondary Action (Trailer, Audio Selector):** Pill shape. Matte glass background `rgba(255, 255, 255, 0.08)`, border `1px solid rgba(255, 255, 255, 0.14)`. When focused, background shifts to `#222736` and border turns to `#F3B744`.

### Scrub Bar & Video Player OSD
- **Track:** 4dp height, matte dark `#222736`.
- **Buffered Progress:** Light slate track `rgba(148, 163, 184, 0.4)`.
- **Active Progress:** `#F3B744`.
- **Thumb:** Hidden during passive playback. Enlarges to a 16dp circular amber disc with dynamic timecode flag (`label-md`) floating directly above when scrubbing via D-pad left/right.

### Metadata Chips & Pills
- Compact 6px rounded rectangles containing technical badges (Dolby Atmos, 1080p, DTS). Outlined in `1px solid rgba(148, 163, 184, 0.25)` with text color `#94A3B8`. Never focusable individually; focus belongs to the parent screen or playback track.

### Side Navigation Rail
- Vertical icon dock pinned to the safe left boundary. Active section indicated by an amber vertical bar (3px width) on the inner border and filled white iconography. Unselected categories rest at 50% opacity.
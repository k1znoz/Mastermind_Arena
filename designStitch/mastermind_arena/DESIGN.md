---
name: Mastermind Arena
colors:
  surface: '#0b1326'
  surface-dim: '#0b1326'
  surface-bright: '#31394d'
  surface-container-lowest: '#060e20'
  surface-container-low: '#131b2e'
  surface-container: '#171f33'
  surface-container-high: '#222a3d'
  surface-container-highest: '#2d3449'
  on-surface: '#dae2fd'
  on-surface-variant: '#bcc9cd'
  inverse-surface: '#dae2fd'
  inverse-on-surface: '#283044'
  outline: '#869397'
  outline-variant: '#3d494c'
  surface-tint: '#4cd7f6'
  primary: '#4cd7f6'
  on-primary: '#003640'
  primary-container: '#06b6d4'
  on-primary-container: '#00424f'
  inverse-primary: '#00687a'
  secondary: '#b9c7e0'
  on-secondary: '#233144'
  secondary-container: '#3c4a5e'
  on-secondary-container: '#abb9d2'
  tertiary: '#4edea3'
  on-tertiary: '#003824'
  tertiary-container: '#1bbd85'
  on-tertiary-container: '#00452e'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#acedff'
  primary-fixed-dim: '#4cd7f6'
  on-primary-fixed: '#001f26'
  on-primary-fixed-variant: '#004e5c'
  secondary-fixed: '#d5e3fd'
  secondary-fixed-dim: '#b9c7e0'
  on-secondary-fixed: '#0d1c2f'
  on-secondary-fixed-variant: '#3a485c'
  tertiary-fixed: '#6ffbbe'
  tertiary-fixed-dim: '#4edea3'
  on-tertiary-fixed: '#002113'
  on-tertiary-fixed-variant: '#005236'
  background: '#0b1326'
  on-background: '#dae2fd'
  surface-variant: '#2d3449'
typography:
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.05em
  label-sm:
    fontFamily: JetBrains Mono
    fontSize: 10px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.05em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  unit: 4px
  gutter-desktop: 24px
  margin-desktop: 32px
  gutter-mobile: 16px
  margin-mobile: 16px
---

## Brand & Style
The design system is engineered for a high-performance gaming environment that balances technical sophistication with extreme clarity. It adopts a **Modern High-Tech** aesthetic, utilizing deep values and vibrant accents to create a focused, low-eye-strain experience during long sessions. 

The interface prioritizes information density without clutter, drawing inspiration from developer tools and tactical HUDs. The emotional response is one of precision, control, and intelligence. Key stylistic drivers include high-contrast typography against dark backgrounds, subtle neon-tinted glows for active states, and a structural layout that feels modular and scalable.

## Colors
The palette is rooted in a "Deep Space" hierarchy. The primary background uses a deep slate (#0f172a) to establish a canvas where light and color have maximum impact.

- **Primary (Cyan):** Used exclusively for interactive elements, primary actions, and active progress indicators. It should be paired with a subtle outer glow (0px 0px 8px rgba(6, 182, 212, 0.4)) in its "On" state.
- **Secondary (Slate):** Used for structural borders and inactive UI components to maintain a low-profile hierarchy.
- **Semantic Colors:** Success is represented by Emerald (#10b981), Warning by Amber (#f59e0b), and Error by Rose (#f43f5e). These colors should be slightly desaturated to prevent jarring "vibrancy clash" against the cyan.

## Typography
This design system utilizes a dual-font approach. **Inter** handles all primary interface elements, providing a neutral and highly legible foundation for fast reading. For technical data, system logs, and code-based views (like the JsonViewer), **JetBrains Mono** is introduced to provide a distinctive "engine-room" feel.

Headlines should use tight letter-spacing to feel more "instrument-like" and engineered. All labels and metadata should be rendered in uppercase JetBrains Mono to emphasize the tool-based nature of the application.

## Layout & Spacing
The layout follows a **Fluid Grid** model with a preference for a 12-column structure on desktop. Spacing is based on a 4px baseline grid to ensure mathematical alignment of technical elements.

- **Desktop:** 12-column grid, 24px gutters. Use sidebar navigation (fixed 280px) with fluid content area.
- **Tablet:** 8-column grid, 20px gutters. Sidebar collapses into a hamburger menu or icon-only rail.
- **Mobile:** 4-column grid, 16px gutters. Elements stack vertically; horizontal scrolling is permitted only for data tables and code blocks.

Internal component spacing should be generous to maintain the "clean" aesthetic. Use `16px` (4 units) for standard padding within cards and forms.

## Elevation & Depth
Depth is achieved through **Tonal Layers** rather than heavy shadows. In a dark UI, shadows are less effective, so the system uses color steps to indicate "height."

- **Level 0 (Base):** #0f172a (The canvas).
- **Level 1 (Cards/Sidebar):** #1e293b (The primary surface).
- **Level 2 (Popovers/Tooltips):** #334155 (The floating surface).

To separate elements, use 1px solid borders in #334155 (Secondary Slate). For active or "elevated" states, apply a subtle 1px border using the Primary Cyan at 30% opacity.

## Shapes
The shape language is "Soft-Technical." Use a consistent 4px (0.25rem) radius for standard components like buttons, inputs, and cards. This provides a professional, modern feel that avoids the "toy-like" look of fully rounded corners while remaining more accessible than sharp 90-degree angles. Status indicators and small avatars may use circular (pill) shapes to differentiate them from functional UI blocks.

## Components

### StatusBadge
Badges use a "Dim-Bright" contrast pattern. The background is a 10% opacity version of the semantic color, with a 1px border at 30% opacity and text at 100% opacity.
- **Success:** Emerald background/border. Includes a small pulsing dot icon.
- **Error:** Rose background/border.
- **Warning:** Amber background/border.

### ActionForm
Forms are structured vertically. Labels are positioned above inputs in uppercase JetBrains Mono (Label-SM). Input fields use the #0f172a background with a 1px #334155 border. On focus, the border transitions to Primary Cyan with a subtle 4px outer glow.

### JsonViewer
The code block component uses a #020617 (near black) background. Syntax highlighting:
- **Keys:** #94a3b8 (Slate-400)
- **Strings:** #22d3ee (Cyan-400)
- **Numbers/Booleans:** #fbbf24 (Amber-400)
- **Brackets:** #64748b (Slate-500)
Line numbers should be visible on the left in a dimmed slate color.

### Buttons
- **Primary:** Solid Cyan background with dark Slate text. High-contrast.
- **Secondary:** Transparent background with 1px Slate border and White text.
- **Ghost:** No background or border. Text only, transitions to Primary Cyan on hover.
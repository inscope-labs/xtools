---
name: Precision Utility
colors:
  surface: '#FDFCFF'
  surface-dim: '#dadadc'
  surface-bright: '#faf9fc'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f4f3f6'
  surface-container: '#eeedf0'
  surface-container-high: '#e8e8eb'
  surface-container-highest: '#e3e2e5'
  on-surface: '#1A1C1E'
  on-surface-variant: '#414750'
  inverse-surface: '#2f3033'
  inverse-on-surface: '#f1f0f3'
  outline: '#74777F'
  outline-variant: '#c1c7d2'
  surface-tint: '#0061a4'
  primary: '#00497d'
  on-primary: '#ffffff'
  primary-container: '#0061a4'
  on-primary-container: '#c0dbff'
  inverse-primary: '#9fcaff'
  secondary: '#535f70'
  on-secondary: '#ffffff'
  secondary-container: '#d7e3f8'
  on-secondary-container: '#596576'
  tertiary: '#52405f'
  on-tertiary: '#ffffff'
  tertiary-container: '#6b5778'
  on-tertiary-container: '#e9d0f7'
  error: '#BA1A1A'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d1e4ff'
  primary-fixed-dim: '#9fcaff'
  on-primary-fixed: '#001d36'
  on-primary-fixed-variant: '#00497d'
  secondary-fixed: '#d7e3f8'
  secondary-fixed-dim: '#bbc7db'
  on-secondary-fixed: '#101c2b'
  on-secondary-fixed-variant: '#3c4858'
  tertiary-fixed: '#f3daff'
  tertiary-fixed-dim: '#d6bee4'
  on-tertiary-fixed: '#251431'
  on-tertiary-fixed-variant: '#523f5f'
  background: '#faf9fc'
  on-background: '#1a1c1e'
  surface-variant: '#E0E2EC'
  success: '#006E1C'
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
  headline-sm:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 24px
  title-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
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
  label-lg:
    fontFamily: JetBrains Mono
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
  label-md:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  base: 8px
  margin-mobile: 16px
  margin-desktop: 24px
  gutter: 12px
  touch-target: 48px
---

## Brand & Style

This design system is engineered for a utility-focused Android ecosystem. The brand personality is rooted in reliability, transparency, and technical precision. It prioritizes function over form, ensuring that users feel in total control of their device's underlying architecture.

The chosen design style is **Corporate / Modern (Material 3)**. It leverages the structured logic of Google’s Material Design 3 but strips away excessive ornamentation to focus on high-density information display and clear action hierarchy. The aesthetic is "surgical"—clean white surfaces, vibrant functional accents, and rigorous alignment. It avoids playful distractions in favor of a professional, tool-like interface that evokes trust in its security and performance capabilities.

## Colors

The palette is anchored by a vibrant **Material Blue** as the primary color, used exclusively for high-intent actions and active states to provide a clear cognitive path. Following a "Light Mode Only" constraint, the system utilizes a high-brightness neutral foundation to maintain maximum legibility and a sense of cleanliness.

- **Primary:** The "Vibrant Blue" (#0061A4) acts as the functional engine of the UI.
- **Surface Strategy:** We use a pure white or near-white background to ensure the "Utility" aspect feels lightweight and fast.
- **Semantic Colors:** Success (Green) and Error (Red) are strictly reserved for system status and security validations, crucial for a plugin-based architecture.
- **Contrast:** High contrast ratios (exceeding WCAG AA) are maintained between text and backgrounds to ensure professional readability in various lighting conditions.

## Typography

The typography system uses **Inter** for all primary UI elements to provide a neutral, highly legible foundation. To emphasize the technical nature of the app (bridge contracts, SHA-256 verification, and plugin manifests), **JetBrains Mono** is introduced for labels and technical metadata.

- **Scale:** A tight typographic scale ensures that data-heavy screens remain organized.
- **Weights:** Heavy weights (700) are used for headlines to anchor the page, while medium weights (500) in monospaced fonts highlight status and technical IDs.
- **Mobile Optimization:** Headlines scale down slightly on mobile to maintain three to four words per line, preventing awkward breaks in technical terminology.

## Layout & Spacing

The layout follows a **Fluid Grid** model based on an 8px square baseline grid. This ensures perfect alignment across the Material 3 "Native Shell."

- **Structure:** Content is organized in a single-column list for mobile utility or a multi-pane layout for tablets/desktop.
- **Rhythm:** Vertical spacing between cards or list items uses 12px (gutter) to maintain a high-density feel while keeping tap targets distinct.
- **Safe Zones:** A 16px horizontal margin is enforced on all mobile screens to prevent text from hitting the edge of the display, increasing focus on the central content.
- **Touch Targets:** All interactive elements (buttons, toggles) maintain a minimum 48x48px footprint, regardless of their visual size, to ensure reliability in "field" usage.

## Elevation & Depth

This design system utilizes **Tonal Layers** rather than heavy shadows to convey depth, aligning with modern Material 3 principles.

- **Surfaces:** The primary background is the lowest level. Cards and plugin containers use a "Surface-Variant" (light grey) or a subtle 1px "Outline" to define boundaries.
- **Shadows:** Soft, ambient shadows (low blur, 4-8% opacity) are reserved only for floating action buttons (FABs) or active dialogs that require immediate user focus.
- **State Changes:** Elevation is signaled by a change in color fill rather than a change in shadow height. For example, a pressed card becomes slightly darker (Surface-Variant) to acknowledge the interaction.

## Shapes

The shape language is **Soft** (Level 1). While Material 3 often defaults to very rounded corners, this design system uses tighter radii (4px to 12px) to reflect a professional, "tool-like" personality.

- **Buttons & Small Components:** Use a 4px (0.25rem) radius for a sharp, precise look.
- **Cards & Containers:** Use a 8px (0.5rem) radius to provide enough softness to feel modern without losing the "structured" grid feel.
- **Selection Controls:** Checkboxes maintain a slight 2px radius, while radio buttons remain perfect circles for standard platform recognition.

## Components

### Buttons
- **Primary:** Filled with Primary Blue (#0061A4), white text, 4px rounded corners. Used for "Install," "Activate," and "Verify."
- **Secondary:** Outlined with a 1px border (#74777F), Primary Blue text. Used for "Cancel" or "View Logs."

### Cards
- Used for Plugin Catalog items. High-contrast white background with a 1px "Surface-Variant" border. No shadow in its default state.

### Input Fields
- Filled style with a bottom-only indicator or a full subtle outline. Labels use **Inter** at 12px. Technical inputs (hashes, paths) use **JetBrains Mono**.

### Chips
- Used for plugin status (e.g., "Active," "Sandboxed," "Legacy"). Small, 4px radius, using semantic background tints (Success-Green for Active).

### Lists
- High-density list items with 16px padding. Dividers are 1px thick, low-opacity grey.

### Progress Indicators
- Linear progress bars for plugin downloads, using the Primary Blue. No rounded caps; square ends to emphasize the technical aesthetic.

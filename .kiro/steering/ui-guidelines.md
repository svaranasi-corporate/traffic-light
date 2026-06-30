# UI Guidelines

## Design System

- **Framework**: Material Design 3 (Material You)
- **Components**: Use Material 3 composables for menu and settings (buttons, sliders, top app bar)
- **Custom rendering**: Traffic light display uses Canvas/custom drawing (not Material components)

## Color Palette

### Traffic Light Colors
| Element | Color | Hex |
|---------|-------|-----|
| Red light (active) | Bright red | `#FF0000` |
| Yellow light (active) | Bright amber | `#FFBF00` |
| Green light (active) | Bright green | `#00CC00` |
| Housing | Goldenrod yellow | `#DAA520` |
| Inactive light (dim) | Dark gray | `#2A2A2A` |
| Background | Near-black | `#1A1A1A` |

### Menu / Settings Colors
- Follow Material 3 dynamic color theming defaults
- Dark theme preferred (matches the traffic light screen's dark background)

## Layout

### Traffic Light Screen
- Full-screen, no system bars (immersive sticky mode)
- Portrait orientation locked
- Housing centered horizontally
- Three circles stacked vertically within housing: red (top), yellow (middle), green (bottom)
- Lights evenly spaced within housing
- Housing is a rounded rectangle

### Menu Screen
- Two buttons vertically centered
- App title/branding above buttons
- Standard Material 3 filled buttons

### Settings Screen
- Material 3 sliders for each duration value
- Current value displayed as a label beside each slider
- "Reset to Defaults" button at the bottom
- Standard top app bar with back navigation

## Animation Specifications

### Light Transitions
- **Duration**: ~300ms per transition (fade out + fade in)
- **Fade-out curve**: Decelerate interpolator (fast start, slow end — simulates cooling filament)
- **Fade-in curve**: Accelerate interpolator (slow start, fast end — simulates heating filament)
- **Target frame rate**: 60fps (use Android ValueAnimator tied to choreographer)

### Brightness Values
| State | Brightness |
|-------|-----------|
| Active (full) | 1.0 |
| Inactive (dim) | 0.15 |

### Rendering Approach
- Color blend: `blend(dimColor, activeColor, brightness)` for each light
- No bitmaps or image assets — purely programmatic shapes and colors
- Use Canvas `drawRoundRect` for housing, `drawCircle` for lights

## Accessibility Notes

- This app is primarily visual (simulating a physical traffic light)
- No text content on the traffic light screen to require content descriptions
- Menu and settings screens should have proper content labels for screen readers
- Buttons and sliders should have descriptive accessibility labels

## Screen Behavior

- Traffic light screen keeps the display on (no auto-sleep)
- Back button is the only exit mechanism from the traffic light screen
- No on-screen controls overlay the traffic light display

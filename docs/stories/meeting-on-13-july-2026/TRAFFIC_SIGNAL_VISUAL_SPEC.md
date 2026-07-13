# Traffic Signal Visual Design Specification

## Purpose

This document defines the visual design requirements for the Android
traffic signal component. The goal is to emulate a **real North American
12-inch incandescent traffic signal**, not a flat icon or an LED
display.

The UI is intentionally stylized with a **yellow housing on a black
background**, while preserving the construction and optical
characteristics of a real signal head.

------------------------------------------------------------------------

# Design Goals

The signal should resemble a real roadside traffic signal photographed
in daylight.

Avoid:

-   Flat vector graphics
-   LED-style lighting
-   Neon glow
-   Cartoon aesthetics
-   Futuristic UI styling

Prioritize:

-   Physical depth
-   Realistic shadows
-   Incandescent optics
-   Convex glass
-   Layered construction

------------------------------------------------------------------------

# Background

-   Solid black (#111111--#1A1A1A)

------------------------------------------------------------------------

# Housing

Color:

``` text
#D9A520 (traffic-signal yellow)
```

Requirements:

-   Matte painted metal appearance
-   Slight texture
-   Rounded corners (14--18 px)
-   Minimal reflections

Body proportions:

-   Width : Height ≈ 1 : 3.05

------------------------------------------------------------------------

# Module Construction

Each signal module is built from separate layers:

1.  Housing
2.  Visor
3.  Bezel
4.  Reflector
5.  Convex Fresnel lens
6.  Incandescent glow
7.  Glass highlight

Do **not** draw a single colored circle.

------------------------------------------------------------------------

# Visor

The visor is the most important realism feature.

Requirements:

-   Extends 40--45% of lens diameter
-   Thick curved visor
-   Elliptical opening
-   Casts a shadow over the upper lens

Without the visor the signal immediately looks artificial.

------------------------------------------------------------------------

# Bezel

-   Thick black ring
-   12--16 px wide
-   Slight inward shadow

------------------------------------------------------------------------

# Lens

-   Convex glass
-   Approximately 80% of housing width
-   Visible depth

Texture:

-   Concentric Fresnel rings
-   Fine prism texture
-   Slight imperfections

Do not use perfectly smooth gradients.

------------------------------------------------------------------------

# Incandescent Light Model

Simulate a **single incandescent bulb inside a mirrored reflector**.

Brightness:

-   Brightest at center
-   Smooth falloff
-   Darker edges

No LED pixel pattern.

------------------------------------------------------------------------

# Lens Colors

## Red

Off

``` text
#3C0D0D
```

On

Center

``` text
#FFEFB0
```

Middle

``` text
#FF3030
```

Edge

``` text
#A00000
```

------------------------------------------------------------------------

## Yellow

Off

``` text
#49380F
```

On

Center

``` text
#FFF3B0
```

Middle

``` text
#FFBF1C
```

Edge

``` text
#A65B00
```

------------------------------------------------------------------------

## Green

Off

``` text
#14361C
```

On

Center

``` text
#D7FFF0
```

Middle

``` text
#2BE060
```

Edge

``` text
#007B2D
```

------------------------------------------------------------------------

# Glass Reflections

Even while illuminated:

-   Small soft white reflection
-   Upper-left quadrant
-   Elliptical
-   Low opacity

------------------------------------------------------------------------

# Shadows

Add subtle shadows:

-   Under visor
-   Around bezel
-   Between modules
-   Inside visor

Use ambient occlusion to create depth.

------------------------------------------------------------------------

# Module Spacing

Gap between modules:

-   6--10 px

Modules should nearly touch.

------------------------------------------------------------------------

# Animation

Simulate incandescent filament behavior.

Turn ON:

-   0%
-   10%
-   40%
-   75%
-   100%

Duration:

-   120--180 ms

Turn OFF:

-   100%
-   60%
-   25%
-   5%
-   0%

Do not switch instantly.

------------------------------------------------------------------------

# Implementation Notes

Prefer a layered rendering pipeline:

Housing → Visor → Bezel → Reflector → Fresnel Lens → Incandescent Glow →
Glass Highlight

This produces a much more convincing result than drawing a single
illuminated circle.

------------------------------------------------------------------------

# Acceptance Criteria

The signal should:

-   Look immediately recognizable as a North American incandescent
    traffic signal.
-   Show obvious physical depth.
-   Have realistic visors.
-   Display convex textured lenses.
-   Produce warm incandescent illumination.
-   Preserve a clean yellow housing against a black application
    background.
-   Feel like a miniature real traffic signal rather than a UI icon.

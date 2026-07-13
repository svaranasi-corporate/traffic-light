# Bugfix Requirements Document

## Introduction

Two rendering effects that should visibly affect the goldenrod housing are currently confined inside the module lens area: (1) the active bulb's glow halo, and (2) the visor's cast shadow. Both stop at the bezel edge instead of spilling onto the surrounding housing.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN a bulb is active (brightness > 0.5) THEN the glow halo terminates at the bezel boundary — no colored light spills onto the goldenrod housing

1.2 WHEN the visor casts a shadow THEN the shadow is clipped to the lens circle — it does not darken the housing area above/around the module

### Expected Behavior (Correct)

2.1 WHEN a bulb is active (brightness > 0.5) THEN a subtle colored tint SHALL appear on the housing around that module's bezel, proportional to brightness, fading to transparent

2.2 WHEN the visor casts a shadow THEN the shadow SHALL extend onto the goldenrod housing above the module, darkening it subtly

### Unchanged Behavior (Regression Prevention)

3.1 Inactive lights (brightness ≤ 0.5) produce no housing tint

3.2 Existing module layers (bezel, reflector, Fresnel, glow, highlight) render identically

3.3 Inter-module spacing and adjacent modules remain unaffected

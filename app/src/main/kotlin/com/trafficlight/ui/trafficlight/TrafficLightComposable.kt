package com.trafficlight.ui.trafficlight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.tooling.preview.Preview
import com.trafficlight.model.LightState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ── Housing constants ─────────────────────────────────────────────────────────

/** Background fill — solid near-black. */
private val COLOR_BACKGROUND = Color(0xFF111111)

/** Housing body — matte traffic-signal yellow. */
private val COLOR_HOUSING = Color(0xFFD9A520)

/** Housing corner radius fraction of housing width (maps to ~16 px on typical screen). */
private const val HOUSING_CORNER_FRACTION = 0.085f

/**
 * The housing occupies this fraction of the canvas width.
 * Aspect ratio 1 : 3.05 is enforced by the aspectRatio modifier in the composable.
 */
@Suppress("unused")
private const val HOUSING_WIDTH_FRACTION = 1.0f

/** Width-to-height ratio: 1 : 3.05 */
private const val HOUSING_ASPECT = 1f / 3.05f

// ── Module geometry constants ─────────────────────────────────────────────────

/** Lens radius as a fraction of the housing width (80% diameter → radius 40%). */
private const val LENS_RADIUS_FRACTION = 0.40f

/** Gap between adjacent module centres as a fraction of housing width. */
private const val MODULE_GAP_FRACTION = 0.04f // ~8 px gap in the rendered gap area

/** Bezel ring width as a fraction of lens radius (maps to ~14 px). */
private const val BEZEL_WIDTH_FRACTION = 0.14f

// ── Visor constants ───────────────────────────────────────────────────────────

/** How far the visor projects forward, as a fraction of the lens diameter (40–50% per spec). */
private const val VISOR_PROJECTION_FRACTION = 0.45f

/** Visor half-width as a fraction of the lens radius. */
private const val VISOR_HALF_WIDTH_FRACTION = 1.30f

/** Metal wall thickness as a fraction of lens radius (~4–5 px). */
private const val VISOR_METAL_THICKNESS_FRACTION = 0.04f

/** Near-black outside shell surface. */
private val COLOR_VISOR_OUTSIDE = Color(0xFF1A1A1A)

/** Darker inside cavity surface. */
private val COLOR_VISOR_INSIDE = Color(0xFF0D0D0D)

/** Slightly lighter front rim edge — catches the light. */
private val COLOR_VISOR_RIM = Color(0xFF2A2A2A)

// ── Incandescent color model ──────────────────────────────────────────────────

/**
 * Per-state color stops for the active incandescent glow.
 * Order: [center, middle, edge].
 */
private val ACTIVE_COLORS: Map<LightState, Triple<Color, Color, Color>> =
    mapOf(
        LightState.RED to Triple(Color(0xFFFFEFB0), Color(0xFFFF3030), Color(0xFFA00000)),
        LightState.YELLOW to Triple(Color(0xFFFFF3B0), Color(0xFFFFBF1C), Color(0xFFA65B00)),
        LightState.GREEN to Triple(Color(0xFFD7FFF0), Color(0xFF2BE060), Color(0xFF007B2D)),
    )

/** Solid dim colors — no glow, no gradient. */
private val INACTIVE_COLORS: Map<LightState, Color> =
    mapOf(
        LightState.RED to Color(0xFF3C0D0D),
        LightState.YELLOW to Color(0xFF49380F),
        LightState.GREEN to Color(0xFF14361C),
    )

/** Reflector backing — mirrored silver. */
private val COLOR_REFLECTOR_CENTER = Color(0xFF555555)
private val COLOR_REFLECTOR_EDGE = Color(0xFF222222)

/** Bezel / module body. */
private val COLOR_BEZEL = Color(0xFF111111)

/** Glass highlight — soft white. */
private val COLOR_GLASS_HIGHLIGHT = Color(0x33FFFFFF)

// ── Animation keyframe constants ──────────────────────────────────────────────

/** Turn-on brightness ramp keyframes: time-fraction → brightness-fraction pairs. */
val TURN_ON_KEYFRAMES = listOf(0f to 0f, 0.08f to 0.10f, 0.35f to 0.40f, 0.65f to 0.75f, 1f to 1f)

/** Turn-off brightness ramp keyframes. */
val TURN_OFF_KEYFRAMES = listOf(0f to 1f, 0.25f to 0.60f, 0.55f to 0.25f, 0.80f to 0.05f, 1f to 0f)

/** Turn-on total duration in ms (mid-range of 120–180 ms spec). */
const val TURN_ON_DURATION_MS = 150L

/** Turn-off total duration in ms. */
const val TURN_OFF_DURATION_MS = 150L

// ── Public API ────────────────────────────────────────────────────────────────

/**
 * Draws a physically accurate North American incandescent traffic signal.
 *
 * Rendering layers per module (bottom → top):
 *   1. Housing backing
 *   2. Visor (cowl)
 *   3. Bezel ring
 *   4. Reflector
 *   5. Fresnel lens texture
 *   6. Incandescent glow (brightness-modulated radial gradient)
 *   7. Glass highlight
 *
 * All drawing is programmatic — no bitmaps, no external assets.
 *
 * @param activeState  Which light is currently "on".
 * @param brightnesses Per-light brightness values in [0.15, 1.0].
 * @param modifier     Compose modifier forwarded to the containing [Box].
 */
@Suppress("FunctionName")
@Composable
fun TrafficLightComposable(
    activeState: LightState,
    brightnesses: Map<LightState, Float> = defaultBrightnesses(activeState),
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(COLOR_BACKGROUND),
        contentAlignment = Alignment.Center,
    ) {
        // Canvas sized to a 1:3.05 housing within 60% of screen width
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth(0.60f)
                    .aspectRatio(HOUSING_ASPECT),
        ) {
            drawIncandescentSignal(brightnesses)
        }
    }
}

// ── Top-level signal drawing ──────────────────────────────────────────────────

private fun DrawScope.drawIncandescentSignal(brightnesses: Map<LightState, Float>) {
    val w = size.width
    val h = size.height
    val cornerRadius = w * HOUSING_CORNER_FRACTION

    // ── 1. Housing body ───────────────────────────────────────────────────────
    drawHousing(w, h, cornerRadius)

    // ── 2. Module geometry ───────────────────────────────────────────────────
    val lensRadius = w * LENS_RADIUS_FRACTION
    val centerX = w / 2f

    // Distribute three module centres evenly with a gap between them.
    // Gap in pixels (we use a fraction of lens diameter for the inter-module spacing).
    val gapPx = lensRadius * 2f * MODULE_GAP_FRACTION * 2f + lensRadius * 0.12f // ~8–10 px practical gap
    val totalContentHeight = lensRadius * 6f + gapPx * 2f
    val startY = (h - totalContentHeight) / 2f + lensRadius

    val lightOrdering = listOf(LightState.RED, LightState.YELLOW, LightState.GREEN)
    lightOrdering.forEachIndexed { index, state ->
        val centerY = startY + index * (lensRadius * 2f + gapPx)
        val brightness = brightnesses[state] ?: DIM_BRIGHTNESS
        drawSignalModule(
            centerX = centerX,
            centerY = centerY,
            lensRadius = lensRadius,
            state = state,
            brightness = brightness,
        )
    }

    // ── Inter-module gap shadow ───────────────────────────────────────────────
    lightOrdering.forEachIndexed { index, _ ->
        if (index < 2) {
            val topModuleCenterY = startY + index * (lensRadius * 2f + gapPx)
            val gapTop = topModuleCenterY + lensRadius
            val gapBottom = gapTop + gapPx
            drawRect(
                brush =
                    Brush.verticalGradient(
                        colors = listOf(Color(0x66000000), Color.Transparent, Color(0x66000000)),
                        startY = gapTop,
                        endY = gapBottom,
                    ),
                topLeft = Offset(0f, gapTop),
                size = Size(w, gapPx),
            )
        }
    }
}

// ── Housing ───────────────────────────────────────────────────────────────────

private fun DrawScope.drawHousing(
    w: Float,
    h: Float,
    cornerRadius: Float,
) {
    val cr = CornerRadius(cornerRadius, cornerRadius)

    // Main housing body
    drawRoundRect(
        color = COLOR_HOUSING,
        topLeft = Offset(0f, 0f),
        size = Size(w, h),
        cornerRadius = cr,
    )

    // Subtle matte texture: faint top highlight fading to a subtle bottom shadow
    val housingTopHighlight = Color(0x18FFFFFF)
    val housingBottomShadow = Color(0x22000000)
    drawRoundRect(
        brush =
            Brush.verticalGradient(
                colors =
                    listOf(
                        housingTopHighlight,
                        Color.Transparent,
                        housingBottomShadow,
                    ),
            ),
        topLeft = Offset(0f, 0f),
        size = Size(w, h),
        cornerRadius = cr,
    )

    // Housing edge shadow (inner border darkness to give depth at edges)
    drawRoundRect(
        brush =
            Brush.horizontalGradient(
                colors =
                    listOf(
                        Color(0x44000000),
                        Color.Transparent,
                        Color.Transparent,
                        Color(0x44000000),
                    ),
            ),
        topLeft = Offset(0f, 0f),
        size = Size(w, h),
        cornerRadius = cr,
    )
}

// ── Signal module ─────────────────────────────────────────────────────────────

/**
 * Draws one complete signal module at ([centerX], [centerY]) with the given [lensRadius].
 *
 * Layer order (back → front):
 *   bezel shadow → bezel ring → reflector → Fresnel lens → incandescent glow → glass highlight
 *   → cylindrical visor (outside shell + inside cavity + front rim + lens shadow)
 *
 * The visor is drawn last so it physically overlaps the lens and bezel, matching reality
 * (the visor hood protrudes in front of the glass).
 */
private fun DrawScope.drawSignalModule(
    centerX: Float,
    centerY: Float,
    lensRadius: Float,
    state: LightState,
    brightness: Float,
) {
    val bezelWidth = lensRadius * BEZEL_WIDTH_FRACTION

    // Layer 1: Bezel + ambient occlusion shadow
    drawBezel(centerX, centerY, lensRadius, bezelWidth)

    // Layer 2: Reflector backing (inside the bezel)
    drawReflector(centerX, centerY, lensRadius - bezelWidth)

    // Layer 3: Fresnel lens texture
    drawFresnelLens(centerX, centerY, lensRadius - bezelWidth, state, brightness)

    // Layer 4: Incandescent glow (brightness-modulated)
    drawIncandescentGlow(centerX, centerY, lensRadius - bezelWidth, state, brightness)

    // Layer 5: Glass highlight (upper-left convex reflection)
    drawGlassHighlight(centerX, centerY, lensRadius - bezelWidth)

    // Layer 6: Cylindrical visor drawn on top of lens layers — projects forward like a real hood
    drawCylindricalVisor(centerX, centerY, lensRadius)
}

// ── Visor (cylindrical hood) ──────────────────────────────────────────────────

/**
 * Draws a deep cylindrical visor hood that projects forward from the housing like a hollow
 * metal pipe or camera lens hood.
 *
 * Rendering sub-layers (back → front):
 *   1. Outside shell (top/outer face of the cylinder — the visible "roof")
 *   2. Inside surface (concave underside of the cylinder — darker cavity)
 *   3. Front rim (elliptical opening edge — shows metal thickness ~3–6 px)
 *   4. Lens shadow (gradient cast from visor onto upper 20–30% of lens)
 *
 * The visor must be called AFTER all lens layers so it overlaps them correctly.
 */
private fun DrawScope.drawCylindricalVisor(
    centerX: Float,
    centerY: Float,
    lensRadius: Float,
) {
    val lensDiameter = lensRadius * 2f
    // How far the visor "roof" projects upward/forward in canvas space
    val projectionDepth = lensDiameter * VISOR_PROJECTION_FRACTION
    val visorHalfWidth = lensRadius * VISOR_HALF_WIDTH_FRACTION
    val metalThickness = lensRadius * VISOR_METAL_THICKNESS_FRACTION

    // ── Attachment geometry ───────────────────────────────────────────────────
    // The rear attachment sits at the top of the housing circle where the visor
    // meets the bezel. The front rim is shifted upward by projectionDepth (in
    // canvas y this means a smaller y value = higher up the screen).

    val rearY = centerY - lensRadius * 0.85f // where visor merges with housing
    val frontRimY = rearY - projectionDepth // front rim, further toward viewer

    val rearLeft = centerX - visorHalfWidth
    val rearRight = centerX + visorHalfWidth

    // Front rim is slightly wider than the rear attachment due to slight flare
    val frontLeft = centerX - visorHalfWidth * 1.05f
    val frontRight = centerX + visorHalfWidth * 1.05f

    // ── Step 1: Outside shell (top face of the cylinder) ──────────────────────
    // A filled trapezoid viewed from slightly above: rear edge at rearY,
    // front edge at frontRimY, giving the top surface visible from our viewpoint.
    val outsidePath =
        Path().apply {
            moveTo(rearLeft, rearY)
            lineTo(rearRight, rearY)
            lineTo(frontRight, frontRimY)
            lineTo(frontLeft, frontRimY)
            close()
        }
    // Subtle gradient on the top surface — slightly lighter at the front edge
    drawPath(
        path = outsidePath,
        brush =
            Brush.verticalGradient(
                colors = listOf(Color(0xFF252525), COLOR_VISOR_OUTSIDE),
                startY = rearY,
                endY = frontRimY,
            ),
    )

    // ── Step 2: Inside / underside surface (concave cavity) ───────────────────
    // The curved underside visible from below: a shallow arc running from the
    // rear attachment to the front rim, filled darker than the outside shell.
    // We model it as a slightly curved quad — the curvature evokes the hollow tube.
    val insideBottomY = rearY + metalThickness * 2f // underside of the rear attachment
    val insideFrontY = frontRimY + metalThickness * 2f // underside of the front rim

    val insidePath =
        Path().apply {
            moveTo(rearLeft + metalThickness, insideBottomY)
            // gentle concave curve of the inner cylinder wall
            quadraticBezierTo(
                centerX,
                insideBottomY + projectionDepth * 0.18f, // shallow sag in the middle
                rearRight - metalThickness,
                insideBottomY,
            )
            // front rim underside — slightly lower than the outside front rim
            lineTo(frontRight - metalThickness * 2f, insideFrontY)
            lineTo(frontLeft + metalThickness * 2f, insideFrontY)
            close()
        }
    // Darker than outside — it's the shadowed inner cavity
    drawPath(path = insidePath, color = COLOR_VISOR_INSIDE)

    // ── Step 3: Front rim (elliptical opening edge) ───────────────────────────
    // The front of the tube opening: an ellipse foreshortened by perspective.
    // Width = full visor opening; height = foreshortened circle (~25% of radius).
    val rimCenterX = centerX
    val rimCenterY = frontRimY + metalThickness
    val rimHalfWidth = visorHalfWidth * 1.05f
    val rimHalfHeight = lensRadius * 0.18f // foreshortening — ellipse not circle
    val rimStroke = (lensRadius * VISOR_METAL_THICKNESS_FRACTION * 1.5f).coerceAtLeast(3f)

    // Filled ellipse for the metal face of the front rim
    drawOval(
        color = COLOR_VISOR_RIM,
        topLeft = Offset(rimCenterX - rimHalfWidth, rimCenterY - rimHalfHeight),
        size = Size(rimHalfWidth * 2f, rimHalfHeight * 2f),
    )
    // Stroke outline to define the rim edge cleanly
    drawOval(
        color = COLOR_VISOR_OUTSIDE,
        topLeft = Offset(rimCenterX - rimHalfWidth, rimCenterY - rimHalfHeight),
        size = Size(rimHalfWidth * 2f, rimHalfHeight * 2f),
        style = Stroke(width = rimStroke),
    )

    // ── Step 4: Lens shadow cast by the visor ─────────────────────────────────
    // A gradient shadow starting at the inner edge of the visor and fading out
    // over the upper 20–30% of the lens. Clipped to the lens circle for realism.
    val shadowTop = centerY - lensRadius
    val shadowBottom = shadowTop + lensRadius * 0.55f // covers ~25% of lens height

    val lensClipPath =
        Path().apply {
            addOval(
                Rect(
                    left = centerX - lensRadius,
                    top = centerY - lensRadius,
                    right = centerX + lensRadius,
                    bottom = centerY + lensRadius,
                ),
            )
        }

    // Clip to lens circle and draw the shadow gradient across the upper portion
    clipPath(lensClipPath) {
        drawRect(
            brush =
                Brush.verticalGradient(
                    colors = listOf(Color(0xCC000000), Color.Transparent),
                    startY = shadowTop,
                    endY = shadowBottom,
                ),
            topLeft = Offset(centerX - lensRadius, shadowTop),
            size = Size(lensRadius * 2f, shadowBottom - shadowTop),
        )
    }
}

// ── Bezel ─────────────────────────────────────────────────────────────────────

private fun DrawScope.drawBezel(
    centerX: Float,
    centerY: Float,
    lensRadius: Float,
    bezelWidth: Float,
) {
    val center = Offset(centerX, centerY)

    // Outer bezel ambient-occlusion shadow (slightly larger than the bezel)
    drawCircle(
        brush =
            Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x88000000)),
                center = center,
                radius = lensRadius + bezelWidth * 0.6f,
            ),
        radius = lensRadius + bezelWidth * 0.6f,
        center = center,
    )

    // Bezel body (solid black ring)
    drawCircle(color = COLOR_BEZEL, radius = lensRadius, center = center)

    // Inward shadow on bezel inner edge to convey depth
    drawCircle(
        brush =
            Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0xBB000000)),
                center = center,
                radius = lensRadius,
            ),
        radius = lensRadius,
        center = center,
    )
}

// ── Reflector ─────────────────────────────────────────────────────────────────

private fun DrawScope.drawReflector(
    centerX: Float,
    centerY: Float,
    innerRadius: Float,
) {
    val center = Offset(centerX, centerY)
    // Mirrored reflector — dark center, slightly lighter edge to suggest parabolic mirror
    drawCircle(
        brush =
            Brush.radialGradient(
                colors = listOf(COLOR_REFLECTOR_EDGE, COLOR_REFLECTOR_CENTER, COLOR_REFLECTOR_EDGE),
                center = center,
                radius = innerRadius,
            ),
        radius = innerRadius,
        center = center,
    )
}

// ── Fresnel lens ──────────────────────────────────────────────────────────────

/**
 * Draws concentric Fresnel ring texture over the lens area.
 * Rings simulate the prismatic texture of a real traffic signal lens.
 */
private fun DrawScope.drawFresnelLens(
    centerX: Float,
    centerY: Float,
    innerRadius: Float,
    state: LightState,
    brightness: Float,
) {
    val center = Offset(centerX, centerY)

    // Base lens color (dim or tinted active color)
    val baseColor =
        if (brightness <= DIM_BRIGHTNESS + 0.05f) {
            INACTIVE_COLORS[state] ?: Color(0xFF2A2A2A)
        } else {
            val (_, midColor, _) = ACTIVE_COLORS[state] ?: Triple(Color.White, Color.White, Color.White)
            midColor.copy(alpha = (brightness * 0.35f).coerceIn(0f, 0.35f))
        }

    drawCircle(color = baseColor, radius = innerRadius, center = center)

    // Concentric Fresnel rings — alternating slight lighter/darker bands
    val ringCount = 6
    for (i in 1..ringCount) {
        val ringRadius = innerRadius * (i.toFloat() / ringCount)
        val ringAlpha = if (i % 2 == 0) 0.10f else 0.06f
        val ringColor =
            if (brightness > DIM_BRIGHTNESS + 0.1f) {
                Color(1f, 1f, 1f, ringAlpha)
            } else {
                Color(0f, 0f, 0f, ringAlpha)
            }
        drawCircle(
            color = ringColor,
            radius = ringRadius,
            center = center,
            style = Stroke(width = innerRadius * 0.055f),
        )
    }

    // Fine prism texture — slight radial imperfection lines
    val imperfectionCount = 8
    for (i in 0 until imperfectionCount) {
        val angle = (PI * 2.0 * i / imperfectionCount).toFloat()
        val lineAlpha = if (i % 3 == 0) 0.07f else 0.04f
        val startOffset =
            Offset(
                centerX + cos(angle) * innerRadius * 0.30f,
                centerY + sin(angle) * innerRadius * 0.30f,
            )
        val endOffset =
            Offset(
                centerX + cos(angle) * innerRadius * 0.85f,
                centerY + sin(angle) * innerRadius * 0.85f,
            )
        drawLine(
            color = Color(1f, 1f, 1f, lineAlpha),
            start = startOffset,
            end = endOffset,
            strokeWidth = 0.8f,
            cap = StrokeCap.Round,
        )
    }
}

// ── Incandescent glow ─────────────────────────────────────────────────────────

/**
 * Draws the brightness-modulated incandescent glow.
 *
 * Active: multi-stop radial gradient (center near-white → mid color → dark edge).
 * Inactive: flat dim color fill, no glow.
 */
private fun DrawScope.drawIncandescentGlow(
    centerX: Float,
    centerY: Float,
    innerRadius: Float,
    state: LightState,
    brightness: Float,
) {
    val center = Offset(centerX, centerY)
    val isActive = brightness > DIM_BRIGHTNESS + 0.05f

    if (!isActive) {
        // Inactive: flat dim color (ensure reflector and fresnel show through slightly)
        val dimColor = INACTIVE_COLORS[state] ?: Color(0xFF2A2A2A)
        drawCircle(
            color = dimColor.copy(alpha = 0.85f),
            radius = innerRadius,
            center = center,
        )
        return
    }

    val (centerColor, midColor, edgeColor) =
        ACTIVE_COLORS[state]
            ?: Triple(Color.White, Color.White, Color.White)

    // Blend all three stops by brightness
    val blendedCenter = centerColor.copy(alpha = brightness.coerceIn(0f, 1f))
    val blendedMid = midColor.copy(alpha = (brightness * 0.95f).coerceIn(0f, 1f))
    val blendedEdge = edgeColor.copy(alpha = (brightness * 0.80f).coerceIn(0f, 1f))

    // Multi-stop radial gradient: center (hot) → mid (color) → edge (deep color)
    drawCircle(
        brush =
            Brush.radialGradient(
                colorStops =
                    arrayOf(
                        0.00f to blendedCenter,
                        0.30f to blendedCenter.copy(alpha = brightness * 0.95f),
                        0.55f to blendedMid,
                        0.85f to blendedEdge,
                        1.00f to edgeColor.copy(alpha = (brightness * 0.60f).coerceIn(0f, 1f)),
                    ),
                center = center,
                radius = innerRadius,
            ),
        radius = innerRadius,
        center = center,
    )

    // Subtle outer glow halo (brightness-gated)
    if (brightness > 0.5f) {
        val glowAlpha = ((brightness - 0.5f) / 0.5f * 0.25f).coerceIn(0f, 0.25f)
        drawCircle(
            brush =
                Brush.radialGradient(
                    colors =
                        listOf(
                            midColor.copy(alpha = glowAlpha),
                            Color.Transparent,
                        ),
                    center = center,
                    radius = innerRadius * 1.15f,
                ),
            radius = innerRadius * 1.15f,
            center = center,
        )
    }
}

// ── Glass highlight ───────────────────────────────────────────────────────────

/**
 * Draws a soft elliptical specular highlight in the upper-left quadrant of the lens,
 * simulating convex glass reflection. Always rendered regardless of active state.
 */
private fun DrawScope.drawGlassHighlight(
    centerX: Float,
    centerY: Float,
    innerRadius: Float,
) {
    val highlightW = innerRadius * 0.55f
    val highlightH = innerRadius * 0.35f

    // Upper-left quadrant: offset from center
    val hlLeft = centerX - innerRadius * 0.55f
    val hlTop = centerY - innerRadius * 0.62f

    drawOval(
        brush =
            Brush.radialGradient(
                colors = listOf(COLOR_GLASS_HIGHLIGHT, Color.Transparent),
                center = Offset(hlLeft + highlightW / 2f, hlTop + highlightH / 2f),
                radius = highlightW * 0.75f,
            ),
        topLeft = Offset(hlLeft, hlTop),
        size = Size(highlightW, highlightH),
    )
}

// ── Private utilities ─────────────────────────────────────────────────────────

/**
 * Produces the default steady-state brightness map: [activeState] is fully bright,
 * the other two are at dim brightness.
 */
fun defaultBrightnesses(activeState: LightState): Map<LightState, Float> =
    LightState.entries.associateWith { state ->
        if (state == activeState) FULL_BRIGHTNESS else DIM_BRIGHTNESS
    }

// ── Previews ──────────────────────────────────────────────────────────────────

@Suppress("FunctionName")
@Preview(showBackground = true, backgroundColor = 0xFF111111)
@Composable
private fun PreviewRedActive() {
    TrafficLightComposable(activeState = LightState.RED)
}

@Suppress("FunctionName")
@Preview(showBackground = true, backgroundColor = 0xFF111111)
@Composable
private fun PreviewYellowActive() {
    TrafficLightComposable(activeState = LightState.YELLOW)
}

@Suppress("FunctionName")
@Preview(showBackground = true, backgroundColor = 0xFF111111)
@Composable
private fun PreviewGreenActive() {
    TrafficLightComposable(activeState = LightState.GREEN)
}

@Suppress("FunctionName")
@Preview(showBackground = true, backgroundColor = 0xFF111111)
@Composable
private fun PreviewMidTransition() {
    TrafficLightComposable(
        activeState = LightState.GREEN,
        brightnesses =
            mapOf(
                LightState.RED to 0.15f,
                LightState.YELLOW to 0.60f,
                LightState.GREEN to 0.40f,
            ),
    )
}

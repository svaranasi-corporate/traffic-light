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

// ── Visor geometry constants ──────────────────────────────────────────────────

/**
 * Visor arc radius as a fraction of lens radius.
 * The visor arc sits just outside the bezel outer edge, surrounding the lens.
 */
private const val VISOR_RADIUS_FRACTION = 1.18f

/**
 * Visor arc stroke width as a fraction of lens radius.
 * Represents the visible tube wall thickness viewed straight-on.
 */
private const val VISOR_STROKE_FRACTION = 0.22f

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
 *   bezel shadow → bezel ring → reflector → Fresnel lens → incandescent glow → glass highlight → visor
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

    // Layer 6: Visor — straight-on arc ring drawn on top of lens layers
    drawVisor(centerX, centerY, lensRadius)
}

// ── Visor ─────────────────────────────────────────────────────────────────────

/**
 * Draws a cylindrical visor (cowl) as seen straight-on from the front.
 *
 * From a front-on viewpoint, the forward-projecting cylinder appears as a
 * thick semi-circular arc surrounding the top half of the lens. The three
 * elements drawn are:
 *   1. Thick dark arc — the tube wall of the hood, from 180° to 360° (top half).
 *   2. Inner edge highlight — a thin slightly-lighter arc at the inner diameter
 *      to hint at depth.
 *   3. Shadow gradient — covers the upper ~25% of the lens interior, simulating
 *      the shade cast by the hood blocking light from above.
 */
private fun DrawScope.drawVisor(
    centerX: Float,
    centerY: Float,
    lensRadius: Float,
) {
    // ── Step 1: Thick semi-circular arc (hood wall viewed front-on) ───────────
    // The arc radius is slightly larger than the lens/bezel, so the visor
    // surrounds the lens without obscuring its face.
    val visorRadius = lensRadius * VISOR_RADIUS_FRACTION
    val visorStrokeWidth = lensRadius * VISOR_STROKE_FRACTION

    drawArc(
        color = Color(0xFF1A1A1A),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(centerX - visorRadius, centerY - visorRadius),
        size = Size(visorRadius * 2f, visorRadius * 2f),
        style = Stroke(width = visorStrokeWidth, cap = StrokeCap.Butt),
    )

    // ── Step 2: Inner edge highlight (tube inner surface hint) ───────────────
    val visorInnerRadius = visorRadius - visorStrokeWidth * 0.5f
    drawArc(
        color = Color(0xFF2E2E2E),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(centerX - visorInnerRadius, centerY - visorInnerRadius),
        size = Size(visorInnerRadius * 2f, visorInnerRadius * 2f),
        style = Stroke(width = 2f, cap = StrokeCap.Butt),
    )

    // ── Step 3: Shadow on upper lens (visor interior blocks light from above) ─
    val lensClipPath = Path().apply {
        addOval(
            Rect(
                centerX - lensRadius,
                centerY - lensRadius,
                centerX + lensRadius,
                centerY + lensRadius,
            )
        )
    }
    clipPath(lensClipPath) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xBB000000), Color.Transparent),
                startY = centerY - lensRadius,
                endY = centerY - lensRadius * 0.45f,
            ),
            topLeft = Offset(centerX - lensRadius, centerY - lensRadius),
            size = Size(lensRadius * 2f, lensRadius * 0.6f),
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

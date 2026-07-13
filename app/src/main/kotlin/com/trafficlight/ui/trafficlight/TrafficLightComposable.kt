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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import com.trafficlight.model.LightState

// ── Constants ────────────────────────────────────────────────────────────────

// DIM_BRIGHTNESS and FULL_BRIGHTNESS are defined in BrightnessAnimator.kt and
// re-used here — they are package-level constants in the same package.

private val COLOR_RED_ACTIVE = Color(0xFFFF0000)
private val COLOR_YELLOW_ACTIVE = Color(0xFFFFBF00)
private val COLOR_GREEN_ACTIVE = Color(0xFF00CC00)
private val COLOR_HOUSING = Color(0xFFDAA520)
private val COLOR_DIM = Color(0xFF2A2A2A)
private val COLOR_BACKGROUND = Color(0xFF1A1A1A)
private val COLOR_VISOR = Color(0xFF111111)

/** Fraction of canvas width occupied by the housing. */
private const val HOUSING_WIDTH_FRACTION = 0.30f

/** Fraction of canvas height occupied by the housing. */
private const val HOUSING_HEIGHT_FRACTION = 0.80f

/** Circle radius as a fraction of housing width. */
private const val CIRCLE_RADIUS_FRACTION = 0.35f

/** Corner radius of the housing as a fraction of housing width. */
private const val HOUSING_CORNER_FRACTION = 0.12f

/** Visor height as a fraction of the circle radius. */
private const val VISOR_HEIGHT_FRACTION = 0.55f

/** Visor half-width as a fraction of the circle radius. */
private const val VISOR_WIDTH_FRACTION = 1.25f

/** Alpha for the reflected glow on the visor underside when a light is active. */
private const val VISOR_GLOW_ALPHA = 0.35f

// ── Public API ───────────────────────────────────────────────────────────────

/**
 * Draws a complete traffic light: goldenrod housing, three blended light circles
 * (red/yellow/green), and a curved visor above each light that shows a reflected
 * glow when the corresponding light is active.
 *
 * All drawing is programmatic — no bitmaps.
 *
 * @param activeState   Which light is currently "on".
 * @param brightnesses  Per-light brightness values in [0.15, 1.0].
 *                      Defaults to FULL for [activeState] and DIM for the others,
 *                      which is the correct steady-state. Pass animated values
 *                      from Task 8 for smooth transitions.
 * @param modifier      Compose modifier forwarded to the containing [Box].
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
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth(HOUSING_WIDTH_FRACTION / 0.30f)
                    .aspectRatio(HOUSING_WIDTH_FRACTION / HOUSING_HEIGHT_FRACTION),
        ) {
            drawTrafficLight(brightnesses)
        }
    }
}

// ── Drawing helpers ───────────────────────────────────────────────────────────

private fun DrawScope.drawTrafficLight(brightnesses: Map<LightState, Float>) {
    val canvasW = size.width
    val canvasH = size.height

    val housingWidth = canvasW
    val housingHeight = canvasH
    val housingLeft = 0f
    val housingTop = 0f
    val cornerRadius = housingWidth * HOUSING_CORNER_FRACTION

    // ── Housing ──────────────────────────────────────────────────────────────
    drawRoundRect(
        color = COLOR_HOUSING,
        topLeft = Offset(housingLeft, housingTop),
        size = Size(housingWidth, housingHeight),
        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
    )

    // ── Circle geometry ───────────────────────────────────────────────────────
    val circleRadius = housingWidth * CIRCLE_RADIUS_FRACTION
    val circleCenterX = housingLeft + housingWidth / 2f

    // Divide housing into 3 equal vertical slots and centre a circle in each
    val slotHeight = housingHeight / 3f
    val lightOrdering = listOf(LightState.RED, LightState.YELLOW, LightState.GREEN)

    lightOrdering.forEachIndexed { index, state ->
        val slotCenterY = housingTop + slotHeight * index + slotHeight / 2f
        val brightness = brightnesses[state] ?: DIM_BRIGHTNESS
        val activeColor = activeColorFor(state)
        val blendedColor = lerp(COLOR_DIM, activeColor, brightness)

        // ── Visor (drawn first so the circle renders on top) ─────────────────
        drawVisor(
            centerX = circleCenterX,
            centerY = slotCenterY,
            circleRadius = circleRadius,
            activeColor = activeColor,
            brightness = brightness,
        )

        // ── Light circle ─────────────────────────────────────────────────────
        drawCircle(
            color = blendedColor,
            radius = circleRadius,
            center = Offset(circleCenterX, slotCenterY),
        )
    }
}

/**
 * Draws a visor (cowl/hood) above [centerY] using a curved path, plus an optional
 * reflected-glow arc on the visor underside when [brightness] is meaningfully above dim.
 */
private fun DrawScope.drawVisor(
    centerX: Float,
    centerY: Float,
    circleRadius: Float,
    activeColor: Color,
    brightness: Float,
) {
    val visorHalfWidth = circleRadius * VISOR_WIDTH_FRACTION
    val visorHeight = circleRadius * VISOR_HEIGHT_FRACTION

    // Visor top edge sits just above the circle's top
    val visorBottom = centerY - circleRadius * 0.85f
    val visorTop = visorBottom - visorHeight

    val visorLeft = centerX - visorHalfWidth
    val visorRight = centerX + visorHalfWidth

    // Build a curved visor shape: flat on top, curved (concave) on the underside
    val visorPath =
        Path().apply {
            moveTo(visorLeft, visorBottom)
            // curved underside — a shallow upward arc
            quadraticBezierTo(
                centerX,
                visorTop + visorHeight * 0.3f,
                visorRight,
                visorBottom,
            )
            // flat top edge connecting right side back to left
            lineTo(visorRight, visorTop)
            lineTo(visorLeft, visorTop)
            close()
        }

    drawPath(path = visorPath, color = COLOR_VISOR)

    // ── Reflected glow on visor underside ─────────────────────────────────
    if (brightness > DIM_BRIGHTNESS + 0.01f) {
        // How bright is the glow — scales linearly with brightness above dim
        val glowFraction = (brightness - DIM_BRIGHTNESS) / (FULL_BRIGHTNESS - DIM_BRIGHTNESS)
        val glowAlpha = VISOR_GLOW_ALPHA * glowFraction
        val glowColor = activeColor.copy(alpha = glowAlpha)

        // Draw a thin oval arc hugging the underside curve of the visor
        val glowOvalLeft = centerX - visorHalfWidth * 0.8f
        val glowOvalRight = centerX + visorHalfWidth * 0.8f
        val glowOvalTop = visorBottom - visorHeight * 0.35f
        val glowOvalBottom = visorBottom + visorHeight * 0.15f

        val glowRect =
            Rect(
                left = glowOvalLeft,
                top = glowOvalTop,
                right = glowOvalRight,
                bottom = glowOvalBottom,
            )

        // startAngle=0 sweeps the bottom half of the ellipse (180 degrees)
        drawArc(
            color = glowColor,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(glowRect.left, glowRect.top),
            size = Size(glowRect.width, glowRect.height),
        )
    }
}

// ── Private utilities ─────────────────────────────────────────────────────────

private fun activeColorFor(state: LightState): Color =
    when (state) {
        LightState.RED -> COLOR_RED_ACTIVE
        LightState.YELLOW -> COLOR_YELLOW_ACTIVE
        LightState.GREEN -> COLOR_GREEN_ACTIVE
    }

/**
 * Produces the default steady-state brightness map: [activeState] is fully bright,
 * the other two are at dim brightness.
 */
fun defaultBrightnesses(activeState: LightState): Map<LightState, Float> =
    LightState.entries.associateWith { state ->
        if (state == activeState) FULL_BRIGHTNESS else DIM_BRIGHTNESS
    }

// ── Previews ─────────────────────────────────────────────────────────────────

@Suppress("FunctionName")
@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
private fun PreviewRedActive() {
    TrafficLightComposable(activeState = LightState.RED)
}

@Suppress("FunctionName")
@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
private fun PreviewYellowActive() {
    TrafficLightComposable(activeState = LightState.YELLOW)
}

@Suppress("FunctionName")
@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
private fun PreviewGreenActive() {
    TrafficLightComposable(activeState = LightState.GREEN)
}

@Suppress("FunctionName")
@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
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

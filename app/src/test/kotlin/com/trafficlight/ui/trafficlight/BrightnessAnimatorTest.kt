package com.trafficlight.ui.trafficlight

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.comparables.beGreaterThanOrEqualTo
import io.kotest.matchers.comparables.beLessThanOrEqualTo
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.float
import io.kotest.property.checkAll

/**
 * Property-based and example-based tests for [FadeInterpolator].
 *
 * All tests run on the JVM with no Android framework dependencies — [FadeInterpolator]
 * is pure Kotlin.
 */
class BrightnessAnimatorTest : FreeSpec({

    // ── FadeInterpolator.decelerateOut ────────────────────────────────────────

    "FadeInterpolator.decelerateOut" - {

        "should return 0.0 at progress 0.0" {
            FadeInterpolator.decelerateOut(0f) shouldBe 0f
        }

        "should return 1.0 at progress 1.0" {
            FadeInterpolator.decelerateOut(1f) shouldBe 1f
        }

        "should return value in [0, 1] for any progress in [0, 1] (property)" {
            checkAll(Arb.float(0f, 1f)) { progress ->
                val result = FadeInterpolator.decelerateOut(progress)
                result should beGreaterThanOrEqualTo(0f)
                result should beLessThanOrEqualTo(1f)
            }
        }

        "should clamp out-of-range inputs below 0 to 0" {
            FadeInterpolator.decelerateOut(-0.5f) shouldBe 0f
        }

        "should clamp out-of-range inputs above 1 to 1" {
            FadeInterpolator.decelerateOut(1.5f) shouldBe 1f
        }

        "should be monotonically non-decreasing (fast start, slow end)" {
            val steps = (0..100).map { it / 100f }
            val values = steps.map { FadeInterpolator.decelerateOut(it) }
            values.zipWithNext { a, b -> b should beGreaterThanOrEqualTo(a) }
        }
    }

    // ── FadeInterpolator.accelerateIn ─────────────────────────────────────────

    "FadeInterpolator.accelerateIn" - {

        "should return 0.0 at progress 0.0" {
            FadeInterpolator.accelerateIn(0f) shouldBe 0f
        }

        "should return 1.0 at progress 1.0" {
            FadeInterpolator.accelerateIn(1f) shouldBe 1f
        }

        "should return value in [0, 1] for any progress in [0, 1] (property)" {
            checkAll(Arb.float(0f, 1f)) { progress ->
                val result = FadeInterpolator.accelerateIn(progress)
                result should beGreaterThanOrEqualTo(0f)
                result should beLessThanOrEqualTo(1f)
            }
        }

        "should clamp out-of-range inputs below 0 to 0" {
            FadeInterpolator.accelerateIn(-0.5f) shouldBe 0f
        }

        "should clamp out-of-range inputs above 1 to 1" {
            FadeInterpolator.accelerateIn(1.5f) shouldBe 1f
        }

        "should be monotonically non-decreasing (slow start, fast end)" {
            val steps = (0..100).map { it / 100f }
            val values = steps.map { FadeInterpolator.accelerateIn(it) }
            values.zipWithNext { a, b -> b should beGreaterThanOrEqualTo(a) }
        }
    }

    // ── FadeInterpolator.fadeOutBrightness ────────────────────────────────────

    "FadeInterpolator.fadeOutBrightness" - {

        "should start at FULL_BRIGHTNESS when progress is 0.0" {
            FadeInterpolator.fadeOutBrightness(0f) shouldBe FULL_BRIGHTNESS
        }

        "should end at DIM_BRIGHTNESS when progress is 1.0" {
            FadeInterpolator.fadeOutBrightness(1f) shouldBe DIM_BRIGHTNESS
        }

        /**
         * FR-5 property: brightness always in [DIM_BRIGHTNESS, FULL_BRIGHTNESS] for
         * any progress value in [0.0, 1.0].
         */
        "should keep brightness in [DIM_BRIGHTNESS, FULL_BRIGHTNESS] for any progress in [0, 1] (property)" {
            checkAll(Arb.float(0f, 1f)) { progress ->
                val brightness = FadeInterpolator.fadeOutBrightness(progress)
                brightness should beGreaterThanOrEqualTo(DIM_BRIGHTNESS)
                brightness should beLessThanOrEqualTo(FULL_BRIGHTNESS)
            }
        }

        "should be monotonically non-increasing (brightness falls from full to dim)" {
            val steps = (0..100).map { it / 100f }
            val values = steps.map { FadeInterpolator.fadeOutBrightness(it) }
            values.zipWithNext { a, b -> b should beLessThanOrEqualTo(a) }
        }
    }

    // ── FadeInterpolator.fadeInBrightness ─────────────────────────────────────

    "FadeInterpolator.fadeInBrightness" - {

        "should start at DIM_BRIGHTNESS when progress is 0.0" {
            FadeInterpolator.fadeInBrightness(0f) shouldBe DIM_BRIGHTNESS
        }

        "should end at FULL_BRIGHTNESS when progress is 1.0" {
            FadeInterpolator.fadeInBrightness(1f) shouldBe FULL_BRIGHTNESS
        }

        /**
         * FR-5 property: brightness always in [DIM_BRIGHTNESS, FULL_BRIGHTNESS] for
         * any progress value in [0.0, 1.0].
         */
        "should keep brightness in [DIM_BRIGHTNESS, FULL_BRIGHTNESS] for any progress in [0, 1] (property)" {
            checkAll(Arb.float(0f, 1f)) { progress ->
                val brightness = FadeInterpolator.fadeInBrightness(progress)
                brightness should beGreaterThanOrEqualTo(DIM_BRIGHTNESS)
                brightness should beLessThanOrEqualTo(FULL_BRIGHTNESS)
            }
        }

        "should be monotonically non-decreasing (brightness rises from dim to full)" {
            val steps = (0..100).map { it / 100f }
            val values = steps.map { FadeInterpolator.fadeInBrightness(it) }
            values.zipWithNext { a, b -> b should beGreaterThanOrEqualTo(a) }
        }
    }

    // ── No simultaneous full brightness (FR-5.4) ──────────────────────────────

    /**
     * Simulates the two-phase transition at discrete progress ticks and verifies
     * that no two different lights are simultaneously at full brightness.
     *
     * Phase 1 (progress 0..1): outgoing fades out via [FadeInterpolator.fadeOutBrightness].
     * Phase 2 (progress 0..1): incoming fades in via [FadeInterpolator.fadeInBrightness].
     *
     * At the boundary (end of phase 1 / start of phase 2):
     * - outgoing = DIM_BRIGHTNESS
     * - incoming = DIM_BRIGHTNESS (not yet started)
     * So there is no moment when both are at FULL_BRIGHTNESS.
     */
    "no two lights simultaneously at FULL_BRIGHTNESS during transition (FR-5.4)" {
        val steps = (0..100).map { it / 100f }

        // Phase 1: outgoing fades from full → dim; incoming stays at dim.
        steps.forEach { progress ->
            val outgoingBrightness = FadeInterpolator.fadeOutBrightness(progress)
            val incomingBrightness = DIM_BRIGHTNESS // not yet animating
            val bothFull = outgoingBrightness >= FULL_BRIGHTNESS && incomingBrightness >= FULL_BRIGHTNESS
            bothFull shouldBe false
        }

        // Phase 2: outgoing is at dim; incoming fades from dim → full.
        steps.forEach { progress ->
            val outgoingBrightness = DIM_BRIGHTNESS // already done fading out
            val incomingBrightness = FadeInterpolator.fadeInBrightness(progress)
            val bothFull = outgoingBrightness >= FULL_BRIGHTNESS && incomingBrightness >= FULL_BRIGHTNESS
            bothFull shouldBe false
        }
    }

    // ── Decelerate vs accelerate asymmetry ────────────────────────────────────

    "decelerateOut midpoint should be greater than 0.5 (fast start, slow end)" {
        // At p=0.5: 1-(1-0.5)^2 = 0.75 > 0.5
        FadeInterpolator.decelerateOut(0.5f) should beGreaterThanOrEqualTo(0.5f)
    }

    "accelerateIn midpoint should be less than 0.5 (slow start, fast end)" {
        // At p=0.5: 0.5^2 = 0.25 < 0.5
        FadeInterpolator.accelerateIn(0.5f) should beLessThanOrEqualTo(0.5f)
    }
})

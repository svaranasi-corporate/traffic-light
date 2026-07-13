package com.trafficlight.data

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

class TimingPreferencesTest : DescribeSpec({

    describe("default values") {
        it("should have red default of 10 seconds") {
            TimingPreferences().redDurationSeconds shouldBe 10
        }

        it("should have green default of 20 seconds") {
            TimingPreferences().greenDurationSeconds shouldBe 20
        }

        it("should have yellow default of 3 seconds") {
            TimingPreferences().yellowDurationSeconds shouldBe 3
        }
    }

    describe("validateAndClamp — red and green (range 3–60)") {
        it("should clamp red below minimum to 3") {
            TimingPreferences(redDurationSeconds = 0)
                .validateAndClamp().redDurationSeconds shouldBe 3
        }

        it("should clamp red above maximum to 60") {
            TimingPreferences(redDurationSeconds = 999)
                .validateAndClamp().redDurationSeconds shouldBe 60
        }

        it("should preserve red at minimum boundary value 3") {
            TimingPreferences(redDurationSeconds = 3)
                .validateAndClamp().redDurationSeconds shouldBe 3
        }

        it("should preserve red at maximum boundary value 60") {
            TimingPreferences(redDurationSeconds = 60)
                .validateAndClamp().redDurationSeconds shouldBe 60
        }

        it("should clamp green below minimum to 3") {
            TimingPreferences(greenDurationSeconds = -5)
                .validateAndClamp().greenDurationSeconds shouldBe 3
        }

        it("should clamp green above maximum to 60") {
            TimingPreferences(greenDurationSeconds = 100)
                .validateAndClamp().greenDurationSeconds shouldBe 60
        }

        it("should preserve green at minimum boundary value 3") {
            TimingPreferences(greenDurationSeconds = 3)
                .validateAndClamp().greenDurationSeconds shouldBe 3
        }

        it("should preserve green at maximum boundary value 60") {
            TimingPreferences(greenDurationSeconds = 60)
                .validateAndClamp().greenDurationSeconds shouldBe 60
        }
    }

    describe("validateAndClamp — yellow (range 1–10)") {
        it("should clamp yellow below minimum to 1") {
            TimingPreferences(yellowDurationSeconds = 0)
                .validateAndClamp().yellowDurationSeconds shouldBe 1
        }

        it("should clamp yellow above maximum to 10") {
            TimingPreferences(yellowDurationSeconds = 99)
                .validateAndClamp().yellowDurationSeconds shouldBe 10
        }

        it("should preserve yellow at minimum boundary value 1") {
            TimingPreferences(yellowDurationSeconds = 1)
                .validateAndClamp().yellowDurationSeconds shouldBe 1
        }

        it("should preserve yellow at maximum boundary value 10") {
            TimingPreferences(yellowDurationSeconds = 10)
                .validateAndClamp().yellowDurationSeconds shouldBe 10
        }
    }

    describe("validateAndClamp — values already in range are unchanged") {
        it("should not alter a mid-range red value") {
            TimingPreferences(redDurationSeconds = 30)
                .validateAndClamp().redDurationSeconds shouldBe 30
        }

        it("should not alter a mid-range green value") {
            TimingPreferences(greenDurationSeconds = 15)
                .validateAndClamp().greenDurationSeconds shouldBe 15
        }

        it("should not alter a mid-range yellow value") {
            TimingPreferences(yellowDurationSeconds = 5)
                .validateAndClamp().yellowDurationSeconds shouldBe 5
        }
    }

    describe("validateAndClamp — idempotency (property test)") {
        it("should produce the same result when applied twice for any red/green input") {
            checkAll(
                Arb.int(-1000..1000),
                Arb.int(-1000..1000),
            ) { red, green ->
                val prefs =
                    TimingPreferences(
                        redDurationSeconds = red,
                        greenDurationSeconds = green,
                    )
                prefs.validateAndClamp() shouldBe prefs.validateAndClamp().validateAndClamp()
            }
        }

        it("should produce the same result when applied twice for any yellow input") {
            checkAll(Arb.int(-1000..1000)) { yellow ->
                val prefs = TimingPreferences(yellowDurationSeconds = yellow)
                prefs.validateAndClamp() shouldBe prefs.validateAndClamp().validateAndClamp()
            }
        }
    }

    describe("validateAndClamp — output always within valid ranges (property test)") {
        it("should always clamp red/green to 3–60 for any arbitrary input") {
            checkAll(Arb.int(-10_000..10_000)) { value ->
                val clamped =
                    TimingPreferences(redDurationSeconds = value)
                        .validateAndClamp()
                        .redDurationSeconds
                clamped shouldBeGreaterThanOrEqualTo TimingPreferences.RED_GREEN_MIN
                clamped shouldBeLessThanOrEqualTo TimingPreferences.RED_GREEN_MAX
            }
        }

        it("should always clamp yellow to 1–10 for any arbitrary input") {
            checkAll(Arb.int(-10_000..10_000)) { value ->
                val clamped =
                    TimingPreferences(yellowDurationSeconds = value)
                        .validateAndClamp()
                        .yellowDurationSeconds
                clamped shouldBeGreaterThanOrEqualTo TimingPreferences.YELLOW_MIN
                clamped shouldBeLessThanOrEqualTo TimingPreferences.YELLOW_MAX
            }
        }
    }

    describe("default TimingPreferences is already valid") {
        it("should be unchanged after validateAndClamp") {
            val defaults = TimingPreferences()
            defaults.validateAndClamp() shouldBe defaults
        }
    }
})

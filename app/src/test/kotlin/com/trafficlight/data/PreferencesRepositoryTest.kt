package com.trafficlight.data

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

/**
 * Unit tests for [PreferencesRepository] using a fake in-memory [android.content.SharedPreferences].
 *
 * [FakeSharedPreferences] is a hand-rolled in-memory implementation that avoids any
 * Android framework dependency, keeping these tests as pure JVM tests.
 */
class PreferencesRepositoryTest : DescribeSpec({

    fun makeRepo(): PreferencesRepository = PreferencesRepository(FakeSharedPreferences())

    describe("getTimingPreferences — no saved values (default fallback)") {
        it("should return red default of 10 when nothing is saved") {
            makeRepo().getTimingPreferences().redDurationSeconds shouldBe TimingPreferences.DEFAULT_RED
        }

        it("should return green default of 20 when nothing is saved") {
            makeRepo().getTimingPreferences().greenDurationSeconds shouldBe TimingPreferences.DEFAULT_GREEN
        }

        it("should return yellow default of 3 when nothing is saved") {
            makeRepo().getTimingPreferences().yellowDurationSeconds shouldBe TimingPreferences.DEFAULT_YELLOW
        }
    }

    describe("saveTimingPreferences + getTimingPreferences roundtrip") {
        it("should persist and retrieve red duration") {
            val repo = makeRepo()
            repo.saveTimingPreferences(TimingPreferences(redDurationSeconds = 45))
            repo.getTimingPreferences().redDurationSeconds shouldBe 45
        }

        it("should persist and retrieve green duration") {
            val repo = makeRepo()
            repo.saveTimingPreferences(TimingPreferences(greenDurationSeconds = 30))
            repo.getTimingPreferences().greenDurationSeconds shouldBe 30
        }

        it("should persist and retrieve yellow duration") {
            val repo = makeRepo()
            repo.saveTimingPreferences(TimingPreferences(yellowDurationSeconds = 7))
            repo.getTimingPreferences().yellowDurationSeconds shouldBe 7
        }

        it("should persist all three durations together") {
            val repo = makeRepo()
            repo.saveTimingPreferences(
                TimingPreferences(
                    redDurationSeconds = 15,
                    greenDurationSeconds = 25,
                    yellowDurationSeconds = 5,
                )
            )
            val loaded = repo.getTimingPreferences()
            loaded.redDurationSeconds shouldBe 15
            loaded.greenDurationSeconds shouldBe 25
            loaded.yellowDurationSeconds shouldBe 5
        }
    }

    describe("saveTimingPreferences — out-of-range values are clamped before persisting") {
        it("should clamp red below minimum before saving") {
            val repo = makeRepo()
            repo.saveTimingPreferences(TimingPreferences(redDurationSeconds = 0))
            repo.getTimingPreferences().redDurationSeconds shouldBe TimingPreferences.RED_GREEN_MIN
        }

        it("should clamp red above maximum before saving") {
            val repo = makeRepo()
            repo.saveTimingPreferences(TimingPreferences(redDurationSeconds = 999))
            repo.getTimingPreferences().redDurationSeconds shouldBe TimingPreferences.RED_GREEN_MAX
        }

        it("should clamp yellow above maximum before saving") {
            val repo = makeRepo()
            repo.saveTimingPreferences(TimingPreferences(yellowDurationSeconds = 99))
            repo.getTimingPreferences().yellowDurationSeconds shouldBe TimingPreferences.YELLOW_MAX
        }
    }

    describe("resetToDefaults") {
        it("should restore red to default after custom value was saved") {
            val repo = makeRepo()
            repo.saveTimingPreferences(TimingPreferences(redDurationSeconds = 55))
            repo.resetToDefaults()
            repo.getTimingPreferences().redDurationSeconds shouldBe TimingPreferences.DEFAULT_RED
        }

        it("should restore green to default after custom value was saved") {
            val repo = makeRepo()
            repo.saveTimingPreferences(TimingPreferences(greenDurationSeconds = 50))
            repo.resetToDefaults()
            repo.getTimingPreferences().greenDurationSeconds shouldBe TimingPreferences.DEFAULT_GREEN
        }

        it("should restore yellow to default after custom value was saved") {
            val repo = makeRepo()
            repo.saveTimingPreferences(TimingPreferences(yellowDurationSeconds = 9))
            repo.resetToDefaults()
            repo.getTimingPreferences().yellowDurationSeconds shouldBe TimingPreferences.DEFAULT_YELLOW
        }

        it("should return fully default preferences after reset") {
            val repo = makeRepo()
            repo.saveTimingPreferences(
                TimingPreferences(
                    redDurationSeconds = 60,
                    greenDurationSeconds = 60,
                    yellowDurationSeconds = 10,
                )
            )
            repo.resetToDefaults()
            repo.getTimingPreferences() shouldBe TimingPreferences()
        }
    }
})

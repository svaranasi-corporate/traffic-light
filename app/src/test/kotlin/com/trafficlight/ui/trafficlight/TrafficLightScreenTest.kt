package com.trafficlight.ui.trafficlight

import com.trafficlight.controller.FakeScheduler
import com.trafficlight.controller.TrafficLightController
import com.trafficlight.data.TimingPreferences
import com.trafficlight.model.AnimationType
import com.trafficlight.model.LightState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Unit tests for the pure-Kotlin logic extracted from [TrafficLightScreen].
 *
 * All tests run on the JVM with no Android framework dependencies.
 * Lifecycle events are simulated directly on the controller.
 */
class TrafficLightScreenTest : DescribeSpec({

    // ── previousStateFor ─────────────────────────────────────────────────────

    describe("previousStateFor") {
        it("should return RED when incoming state is GREEN") {
            previousStateFor(LightState.GREEN) shouldBe LightState.RED
        }

        it("should return GREEN when incoming state is YELLOW") {
            previousStateFor(LightState.YELLOW) shouldBe LightState.GREEN
        }

        it("should return YELLOW when incoming state is RED") {
            previousStateFor(LightState.RED) shouldBe LightState.YELLOW
        }

        it("should be the inverse of LightState.next() for every state") {
            LightState.entries.forEach { state ->
                previousStateFor(state.next()) shouldBe state
            }
        }
    }

    // ── onResume restart logic ────────────────────────────────────────────────

    describe("onResume restart behaviour (FR-8.4)") {

        fun makeController(scheduler: FakeScheduler = FakeScheduler()): TrafficLightController =
            TrafficLightController(
                preferences = TimingPreferences(),
                scheduler = scheduler,
            ) { _, _ -> }

        it("should report isRunning=true immediately after startCycle") {
            val ctrl = makeController()
            ctrl.startCycle()
            ctrl.isRunning.shouldBeTrue()
        }

        it("should report isRunning=false immediately after stopCycle") {
            val ctrl = makeController()
            ctrl.startCycle()
            ctrl.stopCycle()
            ctrl.isRunning.shouldBeFalse()
        }

        it("should not restart if controller is already running on resume") {
            // Simulates: app comes to foreground, timer still alive
            val scheduler = FakeScheduler()
            val ctrl = makeController(scheduler)
            ctrl.startCycle()
            val pendingBefore = scheduler.pendingCount

            // Simulate onResume: controller is running → do NOT call startCycle again
            if (!ctrl.isRunning) {
                ctrl.startCycle()
            }

            scheduler.pendingCount shouldBe pendingBefore
            ctrl.isRunning.shouldBeTrue()
        }

        it("should restart from RED when controller is not running on resume") {
            // Simulates: app was backgrounded, timer was killed
            val scheduler = FakeScheduler()
            val events = mutableListOf<Pair<LightState, AnimationType>>()
            val ctrl = TrafficLightController(
                preferences = TimingPreferences(),
                scheduler = scheduler,
            ) { state, anim -> events.add(Pair(state, anim)) }

            // Start, advance to GREEN, then simulate being killed
            ctrl.startCycle()
            scheduler.runNext() // RED → GREEN
            ctrl.stopCycle()
            events.clear()

            // Simulate onResume with dead timer
            if (!ctrl.isRunning) {
                ctrl.startCycle()
            }

            ctrl.currentState shouldBe LightState.RED
            events.first().first shouldBe LightState.RED
            events.first().second shouldBe AnimationType.NONE
        }

        it("should resume cycling normally after restart from background kill") {
            val scheduler = FakeScheduler()
            val ctrl = makeController(scheduler)

            ctrl.startCycle()
            scheduler.runNext() // RED → GREEN
            ctrl.stopCycle()

            // Restart simulating onResume
            ctrl.startCycle()
            scheduler.runNext() // RED → GREEN again
            ctrl.currentState shouldBe LightState.GREEN
        }
    }

    // ── back-button cleanup ───────────────────────────────────────────────────

    describe("back button cleanup semantics (FR-8.1, FR-8.5)") {

        it("stopCycle should leave no pending callbacks after back is pressed") {
            val scheduler = FakeScheduler()
            val ctrl = TrafficLightController(
                preferences = TimingPreferences(),
                scheduler = scheduler,
            ) { _, _ -> }

            ctrl.startCycle()
            scheduler.hasPending.shouldBeTrue()

            // Simulate back-press cleanup
            ctrl.stopCycle()

            scheduler.hasPending.shouldBeFalse()
            ctrl.isRunning.shouldBeFalse()
        }

        it("stopCycle at any point in the cycle should leave no pending work") {
            // Advance through one full cycle then stop mid-second-cycle
            val scheduler = FakeScheduler()
            val ctrl = TrafficLightController(
                preferences = TimingPreferences(),
                scheduler = scheduler,
            ) { _, _ -> }

            ctrl.startCycle()
            scheduler.runNext() // RED → GREEN
            scheduler.runNext() // GREEN → YELLOW

            ctrl.stopCycle()

            scheduler.hasPending.shouldBeFalse()
            ctrl.isRunning.shouldBeFalse()
        }
    }
})

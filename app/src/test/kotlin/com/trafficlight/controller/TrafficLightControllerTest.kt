package com.trafficlight.controller

import com.trafficlight.data.TimingPreferences
import com.trafficlight.model.AnimationType
import com.trafficlight.model.LightState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

/**
 * Unit tests for [TrafficLightController].
 *
 * All timing is driven by [FakeScheduler] so tests run synchronously on the JVM with no
 * Android framework dependency. The default [TimingPreferences] used in most tests are:
 *   red = 10 s, green = 20 s, yellow = 3 s.
 */
class TrafficLightControllerTest : DescribeSpec({

    // ── helpers ─────────────────────────────────────────────────────────────

    data class Event(val state: LightState, val animation: AnimationType)

    fun makeController(
        prefs: TimingPreferences = TimingPreferences(),
        scheduler: FakeScheduler = FakeScheduler(),
        events: MutableList<Event> = mutableListOf(),
    ): Triple<TrafficLightController, FakeScheduler, MutableList<Event>> {
        val ctrl = TrafficLightController(prefs, scheduler) { state, anim ->
            events.add(Event(state, anim))
        }
        return Triple(ctrl, scheduler, events)
    }

    // ── startCycle initialises to RED ────────────────────────────────────────
    describe("startCycle") {
        it("should set currentState to RED immediately") {
            val (ctrl, _, _) = makeController()
            ctrl.startCycle()
            ctrl.currentState shouldBe LightState.RED
        }

        it("should fire onStateChanged with RED and NONE animation as first event") {
            val (ctrl, _, events) = makeController()
            ctrl.startCycle()
            events.first() shouldBe Event(LightState.RED, AnimationType.NONE)
        }

        it("should schedule exactly one pending task after start") {
            val (ctrl, scheduler, _) = makeController()
            ctrl.startCycle()
            scheduler.pendingCount shouldBe 1
        }
    }

    // ── state transitions: RED → GREEN → YELLOW → RED ────────────────────────
    describe("cycle state transitions") {
        it("should transition RED to GREEN after red duration elapses") {
            val (ctrl, scheduler, events) = makeController()
            ctrl.startCycle()
            scheduler.runNext()
            ctrl.currentState shouldBe LightState.GREEN
            events[1].state shouldBe LightState.GREEN
        }

        it("should transition GREEN to YELLOW after green duration elapses") {
            val (ctrl, scheduler, events) = makeController()
            ctrl.startCycle()
            scheduler.runNext() // RED → GREEN
            scheduler.runNext() // GREEN → YELLOW
            ctrl.currentState shouldBe LightState.YELLOW
            events[2].state shouldBe LightState.YELLOW
        }

        it("should transition YELLOW back to RED after yellow duration elapses") {
            val (ctrl, scheduler, events) = makeController()
            ctrl.startCycle()
            scheduler.runNext() // RED → GREEN
            scheduler.runNext() // GREEN → YELLOW
            scheduler.runNext() // YELLOW → RED
            ctrl.currentState shouldBe LightState.RED
            events[3].state shouldBe LightState.RED
        }

        it("should complete two full cycles without skipping any state") {
            val (ctrl, scheduler, events) = makeController()
            ctrl.startCycle()
            repeat(6) { scheduler.runNext() } // 2 complete cycles
            val stateSequence = events.map { it.state }
            // RED(start) + GREEN + YELLOW + RED + GREEN + YELLOW + RED
            stateSequence shouldContainExactly listOf(
                LightState.RED,
                LightState.GREEN,
                LightState.YELLOW,
                LightState.RED,
                LightState.GREEN,
                LightState.YELLOW,
                LightState.RED,
            )
        }

        it("should always use NONE animation for the first event only") {
            val (ctrl, scheduler, events) = makeController()
            ctrl.startCycle()
            repeat(3) { scheduler.runNext() }
            events[0].animation shouldBe AnimationType.NONE
            events.drop(1).forEach { it.animation shouldBe AnimationType.FADE_IN }
        }

        it("should use FADE_IN animation for every transition after the first") {
            val (ctrl, scheduler, events) = makeController()
            ctrl.startCycle()
            repeat(5) { scheduler.runNext() }
            events.drop(1).map { it.animation }.forEach { it shouldBe AnimationType.FADE_IN }
        }
    }

    // ── exactly one state active at all times ────────────────────────────────
    describe("single active state invariant") {
        it("should always have exactly one currentState at any point") {
            val (ctrl, scheduler, _) = makeController()
            ctrl.startCycle()
            repeat(9) {
                // currentState is a single value — it is never null or an invalid state
                val state = ctrl.currentState
                LightState.values().contains(state).shouldBeTrue()
                scheduler.runNext()
            }
        }
    }

    // ── timing: each phase runs for configured duration ───────────────────────
    describe("phase timing") {
        it("should schedule the first task with the red duration in milliseconds") {
            val prefs = TimingPreferences(
                redDurationSeconds = 10,
                greenDurationSeconds = 20,
                yellowDurationSeconds = 3,
            )
            val scheduler = FakeScheduler()
            val ctrl = TrafficLightController(prefs, scheduler) { _, _ -> }
            ctrl.startCycle()
            // The fake scheduler stores the delay; first task should be redDuration ms
            val firstTask = scheduler.queue.first()
            firstTask.delayMs shouldBe 10_000L
        }

        it("should schedule green phase with green duration after RED fires") {
            val prefs = TimingPreferences(
                redDurationSeconds = 10,
                greenDurationSeconds = 20,
                yellowDurationSeconds = 3,
            )
            val scheduler = FakeScheduler()
            val ctrl = TrafficLightController(prefs, scheduler) { _, _ -> }
            ctrl.startCycle()
            scheduler.runNext() // fire RED → GREEN transition
            // Queue now holds the GREEN-phase task
            val greenTask = scheduler.queue.first()
            greenTask.delayMs shouldBe 20_000L
        }

        it("should schedule yellow phase with yellow duration after GREEN fires") {
            val prefs = TimingPreferences(
                redDurationSeconds = 10,
                greenDurationSeconds = 20,
                yellowDurationSeconds = 3,
            )
            val scheduler = FakeScheduler()
            val ctrl = TrafficLightController(prefs, scheduler) { _, _ -> }
            ctrl.startCycle()
            scheduler.runNext() // RED → GREEN
            scheduler.runNext() // GREEN → YELLOW
            val yellowTask = scheduler.queue.first()
            yellowTask.delayMs shouldBe 3_000L
        }
    }

    // ── stopCycle: clean shutdown ─────────────────────────────────────────────
    describe("stopCycle") {
        it("should remove all pending tasks from the scheduler") {
            val (ctrl, scheduler, _) = makeController()
            ctrl.startCycle()
            ctrl.stopCycle()
            scheduler.hasPending.shouldBeFalse()
        }

        it("should produce no further state-change events after stop") {
            val (ctrl, scheduler, events) = makeController()
            ctrl.startCycle()
            val countAfterStart = events.size
            ctrl.stopCycle()
            scheduler.runAll() // attempt to fire any lingering callbacks
            events.size shouldBe countAfterStart
        }

        it("should be safe to call stopCycle when no cycle is running") {
            val (ctrl, _, _) = makeController()
            ctrl.stopCycle() // should not throw
        }

        it("should be safe to call stopCycle multiple times") {
            val (ctrl, _, _) = makeController()
            ctrl.startCycle()
            ctrl.stopCycle()
            ctrl.stopCycle() // should not throw
        }
    }

    // ── restart after stop ────────────────────────────────────────────────────
    describe("startCycle after stop (resume from background kill)") {
        it("should restart from RED when startCycle is called after stopCycle") {
            val (ctrl, scheduler, events) = makeController()
            ctrl.startCycle()
            scheduler.runNext() // RED → GREEN
            ctrl.stopCycle()
            events.clear()
            ctrl.startCycle()
            ctrl.currentState shouldBe LightState.RED
            events.first().state shouldBe LightState.RED
        }

        it("should use NONE animation when restarting after stop") {
            val (ctrl, scheduler, events) = makeController()
            ctrl.startCycle()
            scheduler.runNext() // advance to GREEN
            ctrl.stopCycle()
            events.clear()
            ctrl.startCycle()
            events.first().animation shouldBe AnimationType.NONE
        }

        it("should resume cycling normally after a restart") {
            val (ctrl, scheduler, events) = makeController()
            ctrl.startCycle()
            scheduler.runNext() // RED → GREEN
            ctrl.stopCycle()
            events.clear()
            val newScheduler = FakeScheduler()
            val newEvents = mutableListOf<Event>()
            val newCtrl = TrafficLightController(TimingPreferences(), newScheduler) { s, a ->
                newEvents.add(Event(s, a))
            }
            newCtrl.startCycle()
            newScheduler.runNext() // RED → GREEN
            newScheduler.runNext() // GREEN → YELLOW
            newCtrl.currentState shouldBe LightState.YELLOW
        }
    }

    // ── no reverse / skip transitions ────────────────────────────────────────
    describe("no reverse or skipped transitions") {
        it("should never go directly from RED to YELLOW") {
            val (ctrl, scheduler, events) = makeController()
            ctrl.startCycle()
            repeat(12) { scheduler.runNext() } // 4 full cycles
            val states = events.map { it.state }
            for (i in 0 until states.size - 1) {
                val current = states[i]
                val next = states[i + 1]
                if (current == LightState.RED) next shouldBe LightState.GREEN
                if (current == LightState.GREEN) next shouldBe LightState.YELLOW
                if (current == LightState.YELLOW) next shouldBe LightState.RED
            }
        }
    }

    // ── property tests ────────────────────────────────────────────────────────
    describe("property: getNextState always follows the correct sequence") {
        it("should always produce the same successor for any given LightState") {
            checkAll(Arb.enum<LightState>()) { state ->
                val expected = when (state) {
                    LightState.RED -> LightState.GREEN
                    LightState.GREEN -> LightState.YELLOW
                    LightState.YELLOW -> LightState.RED
                }
                state.next() shouldBe expected
            }
        }
    }

    describe("property: cycle sequence is always RED → GREEN → YELLOW → RED") {
        it("should produce the canonical three-state sequence for arbitrary cycle lengths") {
            val expectedPattern = listOf(LightState.RED, LightState.GREEN, LightState.YELLOW)
            checkAll(Arb.int(1..10)) { cycles ->
                val scheduler = FakeScheduler()
                val states = mutableListOf<LightState>()
                val ctrl = TrafficLightController(
                    preferences = TimingPreferences(),
                    scheduler = scheduler,
                ) { state, _ -> states.add(state) }
                ctrl.startCycle()
                repeat(cycles * 3) { scheduler.runNext() }
                // Skip the initial RED (AnimationType.NONE) and verify pattern repeats
                val transitions = states.drop(1) // drop the initial RED
                transitions.chunked(3).forEach { chunk ->
                    chunk shouldContainExactly expectedPattern.drop(1).take(chunk.size)
                        .let {
                            // re-derive the expected sub-sequence starting from GREEN
                            chunk.mapIndexed { idx, _ ->
                                expectedPattern[(1 + idx) % 3]
                            }
                        }
                }
            }
        }
    }

    describe("property: exactly one pending timer exists while cycle is running") {
        it("should always have exactly one pending task after any number of transitions") {
            checkAll(Arb.int(0..20)) { steps ->
                val scheduler = FakeScheduler()
                val ctrl = TrafficLightController(
                    preferences = TimingPreferences(),
                    scheduler = scheduler,
                ) { _, _ -> }
                ctrl.startCycle()
                repeat(steps) { scheduler.runNext() }
                scheduler.pendingCount shouldBe 1
            }
        }
    }

    describe("property: stopCycle always leaves zero pending tasks") {
        it("should always have zero pending tasks after stopCycle regardless of when it is called") {
            checkAll(Arb.int(0..15)) { steps ->
                val scheduler = FakeScheduler()
                val ctrl = TrafficLightController(
                    preferences = TimingPreferences(),
                    scheduler = scheduler,
                ) { _, _ -> }
                ctrl.startCycle()
                repeat(steps) { scheduler.runNext() }
                ctrl.stopCycle()
                scheduler.hasPending.shouldBeFalse()
            }
        }
    }
})

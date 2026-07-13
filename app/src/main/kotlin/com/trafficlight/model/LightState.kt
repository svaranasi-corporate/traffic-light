package com.trafficlight.model

/**
 * Represents the three possible states of the traffic light.
 * The fixed cycle sequence is RED → GREEN → YELLOW → RED (FR-3.2).
 */
enum class LightState {
    RED,
    GREEN,
    YELLOW,
    ;

    /** Returns the next state in the fixed cycle sequence. Pure function, no side effects. */
    fun next(): LightState =
        when (this) {
            RED -> GREEN
            GREEN -> YELLOW
            YELLOW -> RED
        }
}

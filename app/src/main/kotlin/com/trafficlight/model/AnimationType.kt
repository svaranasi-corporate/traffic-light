package com.trafficlight.model

/**
 * Describes the type of brightness animation to apply during a light transition (FR-5).
 */
enum class AnimationType {
    /** Light is turning on — accelerate curve (slow start, fast end). */
    FADE_IN,

    /** Light is turning off — decelerate curve (fast start, slow end). */
    FADE_OUT,

    /** Initial state; no animation applied. */
    NONE,
}

package com.trafficlight.data

/**
 * Holds the per-phase timing configuration for the traffic light cycle.
 *
 * Default values (FR-4.1, FR-4.2, FR-4.3):
 *   red    = 10 s
 *   green  = 20 s
 *   yellow =  3 s
 *
 * Valid ranges enforced by [validateAndClamp]:
 *   red / green  : 3–60 s
 *   yellow       : 1–10 s
 */
data class TimingPreferences(
    val redDurationSeconds: Int = DEFAULT_RED,
    val greenDurationSeconds: Int = DEFAULT_GREEN,
    val yellowDurationSeconds: Int = DEFAULT_YELLOW,
) {
    companion object {
        const val DEFAULT_RED = 10
        const val DEFAULT_GREEN = 20
        const val DEFAULT_YELLOW = 3

        const val RED_GREEN_MIN = 3
        const val RED_GREEN_MAX = 60
        const val YELLOW_MIN = 1
        const val YELLOW_MAX = 10
    }

    /**
     * Returns a copy of this [TimingPreferences] with all values clamped to their
     * valid ranges. Calling this function on an already-valid instance is a no-op
     * (idempotent). (FR-7.3)
     */
    fun validateAndClamp(): TimingPreferences = copy(
        redDurationSeconds = redDurationSeconds.coerceIn(RED_GREEN_MIN, RED_GREEN_MAX),
        greenDurationSeconds = greenDurationSeconds.coerceIn(RED_GREEN_MIN, RED_GREEN_MAX),
        yellowDurationSeconds = yellowDurationSeconds.coerceIn(YELLOW_MIN, YELLOW_MAX),
    )
}

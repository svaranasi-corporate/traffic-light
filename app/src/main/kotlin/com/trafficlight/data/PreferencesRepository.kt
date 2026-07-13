package com.trafficlight.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Reads and writes traffic-light timing preferences to local SharedPreferences.
 *
 * All data is stored on-device only — no network access, no cloud sync (FR-7.4).
 * Values are validated and clamped on every read to silently recover from corruption.
 *
 * The primary constructor accepts a raw [SharedPreferences] instance to allow
 * easy substitution of a fake or in-memory implementation in unit tests.
 * The secondary constructor (taking a [Context]) is the production entry point.
 */
class PreferencesRepository(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
    )

    /**
     * Returns the currently saved [TimingPreferences], or defaults if nothing has been
     * saved yet. Values are always passed through [TimingPreferences.validateAndClamp]
     * before being returned (FR-7.1).
     */
    fun getTimingPreferences(): TimingPreferences =
        TimingPreferences(
            redDurationSeconds = prefs.getInt(KEY_RED, TimingPreferences.DEFAULT_RED),
            greenDurationSeconds = prefs.getInt(KEY_GREEN, TimingPreferences.DEFAULT_GREEN),
            yellowDurationSeconds = prefs.getInt(KEY_YELLOW, TimingPreferences.DEFAULT_YELLOW),
        ).validateAndClamp()

    /**
     * Persists all three durations from [preferences] to SharedPreferences (FR-7.2).
     * Values are clamped before saving so the store never contains out-of-range data.
     */
    fun saveTimingPreferences(preferences: TimingPreferences) {
        val clamped = preferences.validateAndClamp()
        prefs.edit()
            .putInt(KEY_RED, clamped.redDurationSeconds)
            .putInt(KEY_GREEN, clamped.greenDurationSeconds)
            .putInt(KEY_YELLOW, clamped.yellowDurationSeconds)
            .apply()
    }

    /**
     * Clears all saved values. The next call to [getTimingPreferences] will return
     * the compiled-in defaults (FR-7.3).
     */
    fun resetToDefaults() {
        prefs.edit()
            .remove(KEY_RED)
            .remove(KEY_GREEN)
            .remove(KEY_YELLOW)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "traffic_light_prefs"
        private const val KEY_RED = "red_duration_seconds"
        private const val KEY_GREEN = "green_duration_seconds"
        private const val KEY_YELLOW = "yellow_duration_seconds"
    }
}

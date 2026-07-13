package com.trafficlight.data

import android.content.SharedPreferences

/**
 * In-memory implementation of [SharedPreferences] used in unit tests.
 *
 * Avoids any Android framework dependency while providing the exact behaviour
 * the production code relies on: getInt with a default, putInt, and remove.
 */
class FakeSharedPreferences : SharedPreferences {
    private val store: MutableMap<String, Any?> = mutableMapOf()

    override fun getInt(
        key: String,
        defValue: Int,
    ): Int = store[key] as? Int ?: defValue

    override fun contains(key: String): Boolean = store.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor(store)

    // region — unused SharedPreferences methods (not exercised by production code)
    override fun getAll(): Map<String, *> = store.toMap()

    override fun getString(
        key: String,
        defValue: String?,
    ): String? = store[key] as? String ?: defValue

    override fun getStringSet(
        key: String,
        defValues: Set<String>?,
    ): Set<String>? = null

    override fun getLong(
        key: String,
        defValue: Long,
    ): Long = store[key] as? Long ?: defValue

    override fun getFloat(
        key: String,
        defValue: Float,
    ): Float = store[key] as? Float ?: defValue

    override fun getBoolean(
        key: String,
        defValue: Boolean,
    ): Boolean = store[key] as? Boolean ?: defValue

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit
    // endregion

    private class FakeEditor(private val store: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val pending: MutableMap<String, Any?> = mutableMapOf()
        private val removals: MutableSet<String> = mutableSetOf()

        override fun putInt(
            key: String,
            value: Int,
        ): SharedPreferences.Editor = apply { pending[key] = value }

        override fun remove(key: String): SharedPreferences.Editor = apply { removals.add(key) }

        override fun commit(): Boolean {
            flush()
            return true
        }

        override fun apply() = flush()

        private fun flush() {
            removals.forEach { store.remove(it) }
            store.putAll(pending)
            removals.clear()
            pending.clear()
        }

        // region — unused Editor methods
        override fun putString(
            key: String,
            value: String?,
        ): SharedPreferences.Editor = apply { pending[key] = value }

        override fun putStringSet(
            key: String,
            values: Set<String>?,
        ): SharedPreferences.Editor = apply { pending[key] = values }

        override fun putLong(
            key: String,
            value: Long,
        ): SharedPreferences.Editor = apply { pending[key] = value }

        override fun putFloat(
            key: String,
            value: Float,
        ): SharedPreferences.Editor = apply { pending[key] = value }

        override fun putBoolean(
            key: String,
            value: Boolean,
        ): SharedPreferences.Editor = apply { pending[key] = value }

        override fun clear(): SharedPreferences.Editor = apply { store.clear() }
        // endregion
    }
}

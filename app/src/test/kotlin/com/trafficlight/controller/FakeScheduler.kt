package com.trafficlight.controller

/**
 * A synchronous, deterministic fake [Scheduler] for unit tests.
 *
 * Instead of executing callbacks after a real delay, it queues them as [PendingTask]
 * entries. Tests control execution explicitly via [runNext], [runAll], and [advanceBy].
 *
 * This keeps all tests pure JVM — no Android framework, no real clocks.
 */
class FakeScheduler : Scheduler {

    data class PendingTask(val delayMs: Long, val action: () -> Unit)

    /** Ordered list of tasks waiting to execute; sorted by scheduled delay ascending. */
    val queue: MutableList<PendingTask> = mutableListOf()

    /** Virtual clock in milliseconds — advanced via [advanceBy] or [runNext]. */
    var currentTimeMs: Long = 0L
        private set

    /**
     * Schedules [action] to execute after [delayMs] virtual milliseconds.
     * The task itself (a [PendingTask]) is returned as the cancellation token.
     */
    override fun schedule(delayMs: Long, action: () -> Unit): Any {
        val task = PendingTask(delayMs, action)
        queue.add(task)
        queue.sortBy { it.delayMs }
        return task
    }

    /** Removes the task identified by [token] from the queue. */
    override fun cancel(token: Any) {
        if (token is PendingTask) {
            queue.remove(token)
        }
    }

    /** True if there are any pending tasks in the queue. */
    val hasPending: Boolean get() = queue.isNotEmpty()

    /** Number of tasks currently queued. */
    val pendingCount: Int get() = queue.size

    /**
     * Executes the next pending task (the one with the smallest delay), advancing
     * the virtual clock to match its delay. Does nothing if the queue is empty.
     */
    fun runNext() {
        val task = queue.removeFirstOrNull() ?: return
        currentTimeMs = task.delayMs
        task.action()
    }

    /**
     * Advances the virtual clock by [ms] milliseconds and executes all tasks whose
     * delay falls within the new time window [currentTimeMs, currentTimeMs + ms].
     * Tasks are executed in delay order.
     */
    fun advanceBy(ms: Long) {
        val target = currentTimeMs + ms
        while (queue.isNotEmpty() && queue.first().delayMs <= target) {
            val task = queue.removeFirst()
            currentTimeMs = task.delayMs
            task.action()
        }
        currentTimeMs = target
    }

    /** Executes all currently queued tasks in delay order. */
    fun runAll() {
        while (queue.isNotEmpty()) {
            runNext()
        }
    }
}

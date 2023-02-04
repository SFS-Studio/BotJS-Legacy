package com.sifsstudio.botjs.util.concurrent

import java.util.concurrent.locks.LockSupport

/**
 * Utility class holding park/unpark control
 */
class Parker {
    private val holder: Thread = Thread.currentThread()
    private var unparkExpected: Boolean = false
    var parking: Boolean = false
        private set

    fun park(): Boolean {
        check(!parking) { "The current thread is already parking on this parker! This should not be possible." }
        parking = true
        val result: Boolean
        while (true) {
            LockSupport.park(this)

            result = if (holder.isInterrupted) {
                false
            } else if (unparkExpected) {
                true
            } else continue

            break
        }
        parking = false
        return result
    }

    @Synchronized
    fun unpark() {
        check(parking) { "The holder thread is currently not blocked on this parker!" }
        unparkExpected = true
        LockSupport.unpark(holder)
    }
}
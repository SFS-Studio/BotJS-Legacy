package com.sifsstudio.botjs.util.concurrent

import java.util.concurrent.locks.LockSupport

/**
 * Utility class holding park/unpark control
 * for the thread creating this Parker.
 */
class Parker {
    private val holder: Thread = Thread.currentThread()
    private var dead: Boolean = false
    private var unparkExpected: Boolean = false
    private var parking: Boolean = false

    @Synchronized
    fun park(): Boolean {
        check(!dead) {"Parker not reset to initial state!"}
        check(!parking) {"The current thread is already parking on this parker! This should not be possible."}
        val result: Boolean
        while(true) {
            LockSupport.park(this)
            if(holder.isInterrupted) {
                result = false
                break
            } else if(unparkExpected) {
                result = true
                break
            }
        }
        dead = true
        return result
    }

    @Synchronized
    fun reset() {
        dead = false
        unparkExpected = false
    }

    @Synchronized
    fun unpark() {
        check(!dead) {"Parker not reset to initial state!"}
        check(parking) {"The holder thread is currently not blocked on this parker!"}
        unparkExpected = true
        LockSupport.unpark(holder)
    }

    @Synchronized
    fun unparkNoThrow() {
        if(dead or !parking) return
        unparkExpected = true
        LockSupport.unpark(holder)
    }
}
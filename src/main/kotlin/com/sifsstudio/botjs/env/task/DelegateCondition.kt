package com.sifsstudio.botjs.env.task

import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import kotlin.concurrent.withLock

class DelegateCondition(private val lock: Lock) {
    private val condition: Condition = lock.newCondition()
    private var released = false
    var waitCount = 0
        private set

    fun await() {
        lock.withLock {
            if (released) {
                return
            }
            waitCount++
            condition.await()
        }
    }

    fun signal() {
        lock.withLock {
            condition.signalAll()
            waitCount = 0
        }
    }

    fun release() {
        lock.withLock {
            condition.signalAll()
            released = true
        }
    }
}
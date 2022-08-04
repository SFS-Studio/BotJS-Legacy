package com.sifsstudio.botjs.env.task

import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import kotlin.concurrent.withLock

class DelegateCondition(private val lock: Lock) {
    private val condition: Condition = lock.newCondition()
    private var released = false
    fun await() {
        if (!released) {
            lock.lock()
            condition.await()
        }
    }

    fun signal() {
        if (!released) {
            lock.withLock {
                condition.signalAll()
                released = true
            }
        }
    }
}
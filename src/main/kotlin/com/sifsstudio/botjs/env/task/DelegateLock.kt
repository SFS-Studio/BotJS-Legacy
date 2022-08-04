package com.sifsstudio.botjs.env.task

import java.util.concurrent.locks.Lock

class DelegateLock(val lock: Lock) {
    private var lockCount: Int = 0
    fun lock() {
        synchronized(this) {
            if(lockCount >= 0) {
                lockCount++
                lock.lock()
            }
        }
    }

    fun unlock() {
        synchronized(this) {
            if(lockCount > 0) {
                lock.unlock()
            }
            lockCount--
        }
    }
}
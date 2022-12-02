package com.sifsstudio.botjs.env

import java.util.concurrent.locks.LockSupport

object TickSynchronizer {

    init {
        LockSupport::class.java
    }

    private const val lock = "BotJS TickSynchronizer Lock"
    private var unpark = false
    private var ticking = false
    private var waiting: MutableSet<Thread> = HashSet()

    fun await(): Boolean {
        var current = Thread.currentThread()
        synchronized(lock) {
            waiting.add(current)
            while(!unpark) {
                LockSupport.park()
                if(Thread.interrupted()) {
                    waiting.remove(current)
                    return false
                }
            }
        }
        return ticking
    }

    fun tick() {
        if(NotificationThread.unpark) {
            println("Notification Thread cannot catch up with the server. Ignoring one tick...")
            return
        }
        NotificationThread.unpark = true
        LockSupport.unpark(NotificationThread)
        while(NotificationThread.unpark && !NotificationThread.isInterrupted);
    }

    fun enable() {
        ticking = true
        NotificationThread.start()
    }

    fun finalize() {
        ticking = false
        NotificationThread.unpark = true
        LockSupport.unpark(NotificationThread)
        while(NotificationThread.unpark && !NotificationThread.isInterrupted);
    }

    private object NotificationThread: Thread("BotJS Ticking Thread") {

        var unpark = false

        override fun run() {
            while (!isInterrupted && ticking) {
                while (!unpark) {
                    LockSupport.park(lock)
                    if (isInterrupted) {
                        break
                    }
                }
                synchronized(lock) {
                    waiting.forEach(LockSupport::unpark)
                }
                unpark = false
            }
        }
    }
}
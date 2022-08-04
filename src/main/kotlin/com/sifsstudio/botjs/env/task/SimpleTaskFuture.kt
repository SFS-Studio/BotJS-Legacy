package com.sifsstudio.botjs.env.task

import java.util.concurrent.locks.ReentrantLock

class SimpleTaskFuture<T: Any>: TaskFuture<T> {
    override var done = false

    //TODO: Coroutine implementation
    val condition = DelegateCondition(ReentrantLock())

    override lateinit var result: T

    //TODO: Coroutine implementation
    override fun join() = condition.await().let { result }

    //TODO: Coroutine implementation
    fun done(result: T) {
        done = true
        this.result = result
        condition.signal()
    }
}
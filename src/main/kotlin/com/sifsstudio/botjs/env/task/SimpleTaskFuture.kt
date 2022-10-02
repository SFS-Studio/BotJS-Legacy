package com.sifsstudio.botjs.env.task

import java.util.concurrent.locks.ReentrantLock

class SimpleTaskFuture<T: Any>: TaskFuture<T> {
    override var done = false

    private val condition = DelegateCondition(ReentrantLock())

    lateinit var result: T
    lateinit var failReason: Throwable

    override fun result(): Result<T>? {
        if(!done) {
            return null
        }
        return if(this::failReason.isInitialized) {
            Result.failure(failReason)
        } else Result.success(result)
    }

    override fun join() = condition.await().let { result()!! }

    fun done(result: T) {
        done = true
        this.result = result
        condition.release()
    }

    fun fail(reason: Throwable) {
        done = true
        this.failReason = reason
        condition.release()
    }
}
package com.sifsstudio.botjs.env.task

import java.util.concurrent.locks.ReentrantLock

abstract class TaskBase<T>: Task<T> {
    override var done = false
    override val lock = DelegateLock(ReentrantLock())
    override var result: T? = null
}
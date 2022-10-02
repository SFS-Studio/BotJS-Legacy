package com.sifsstudio.botjs.env.task

import com.sifsstudio.botjs.env.BotEnv

abstract class TaskBase<T: Any>: Task<T> {
    override val future: SimpleTaskFuture<T> = SimpleTaskFuture()
    protected lateinit var env: BotEnv

    override fun accepts(env: BotEnv) =
        if(super.accepts(env)) {
            this.env = env
            true
        } else false

    protected fun done(result: T) = future.done(result)

    override fun discard(reason: Throwable) = future.fail(reason)
}
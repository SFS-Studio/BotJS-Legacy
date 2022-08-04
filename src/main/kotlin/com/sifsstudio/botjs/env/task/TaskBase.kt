package com.sifsstudio.botjs.env.task

import com.sifsstudio.botjs.env.BotEnv

abstract class TaskBase<T: Any>: Task<T> {
    override val future: SimpleTaskFuture<T> = SimpleTaskFuture()
    protected lateinit var envIn: BotEnv

    override fun accepts(envIn: BotEnv) =
        if(super.accepts(envIn)) {
            this.envIn = envIn
            true
        } else false

    protected fun done(result: T) = future.done(result)
}
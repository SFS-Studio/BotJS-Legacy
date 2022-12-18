package com.sifsstudio.botjs.env.api.ability

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.TickableTask

abstract class AbilityBase(private val environment: BotEnv) {

    abstract val id: String

    protected fun <T: Any> submit(task: TickableTask<T>) = environment.submit(task)

    protected fun <T : Any> block(task: TickableTask<T>): T {
        val future = environment.submit(task)
        return environment.block(future)
    }

    protected fun suspendIfNecessary(data: Any?) {
        environment.suspendIfNecessary(data)
    }
}
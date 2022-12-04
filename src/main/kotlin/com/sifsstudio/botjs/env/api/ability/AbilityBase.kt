package com.sifsstudio.botjs.env.api.ability

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.TickableTask

abstract class AbilityBase(private val environment: BotEnv) {

    abstract val id: String

    protected fun submit(task: TickableTask<*>) = environment.submit(task)

    protected fun<T: Any> block(task: TickableTask<T>): T {
        val future = environment.submit(task)
        environment.block<Any>(future)
        return future.result as T
    }
}
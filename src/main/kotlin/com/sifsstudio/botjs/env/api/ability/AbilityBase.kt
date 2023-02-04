package com.sifsstudio.botjs.env.api.ability

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.task.TickableTask

abstract class AbilityBase(protected val environment: BotEnv) {

    abstract val id: String

    protected fun <T : Any> submit(task: TickableTask<T>) = environment.submit(task)

    protected fun <T : Any> block(task: TickableTask<T>): T {
        val future = environment.submit(task)
        return environment.block(future)
    }

}
package com.sifsstudio.botjs.env.api.ability

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.SuspensionContext
import com.sifsstudio.botjs.env.block
import com.sifsstudio.botjs.env.submit
import com.sifsstudio.botjs.env.task.TickableTask

abstract class AbilityBase(protected val environment: BotEnv) {

    abstract val id: String

    protected fun <T : Any> submit(task: TickableTask<T>) = environment.submit(task, false)

    protected suspend fun <T : Any> SuspensionContext.block(task: TickableTask<T>): T {
        val future = environment.submit(task, true)
        return environment.block(future, this)
    }

}

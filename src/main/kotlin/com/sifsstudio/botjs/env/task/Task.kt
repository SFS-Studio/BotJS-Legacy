package com.sifsstudio.botjs.env.task

import com.sifsstudio.botjs.env.BotEnv

interface Task<T: Any> {
    val future: TaskFuture<T>
    fun accepts(env: BotEnv): Boolean = true
    fun tick()
    fun discard(reason: Throwable)
}
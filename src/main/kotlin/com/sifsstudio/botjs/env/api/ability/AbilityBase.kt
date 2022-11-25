package com.sifsstudio.botjs.env.api.ability

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.TickableTask
import org.mozilla.javascript.Context
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

abstract class AbilityBase(private val environment: BotEnv) {
    abstract val id: String

    fun setPendingTaskAndWait(task: TickableTask) {
        val reentrantLock = ReentrantLock()
        val condition = reentrantLock.newCondition()
        environment.setPendingTask(task, reentrantLock, condition)
        reentrantLock.withLock {
            condition.await()
            if (!environment.isFree()) {
                environment.removeBotFromScope()
                val cx = Context.getCurrentContext()
                val pending = cx.captureContinuation()
                throw pending
            }
        }
    }
}
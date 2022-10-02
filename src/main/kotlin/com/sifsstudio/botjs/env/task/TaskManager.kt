package com.sifsstudio.botjs.env.task

import com.sifsstudio.botjs.env.BotEnv
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap
import java.util.*

class TaskManager(val env: BotEnv) {

    private val tasks: MutableMap<Task<*>, Boolean> = Collections.synchronizedMap(Object2BooleanOpenHashMap())
    private var available: Boolean = true
    private lateinit var discardReason: Throwable

    fun <T : Task<R>, R : Any> pending(tsk: T) = synchronized(this) {
        if(available) {
            tasks[tsk] = false
            tsk.future
        } else TaskFuture.failedFuture(discardReason)
    }

    fun discard(reason: Throwable) {
        discardReason = reason
        available = false
        tasks.keys.iterator().run {
            while(hasNext()) next().discard(reason)
        }
    }

    fun tick() = tasks.iterator().run {
        if(!available) return@run
        var entry: MutableMap.MutableEntry<Task<*>, Boolean>
        var task: Task<*>
        while (hasNext()) {
            entry = next()
            task = entry.key
            if (!entry.value) {
                if (!task.accepts(env)) {
                    remove()
                } else entry.setValue(true)
            } else {
                task.tick()
                if (task.future.done) {
                    remove()
                }
            }
        }
    }
}
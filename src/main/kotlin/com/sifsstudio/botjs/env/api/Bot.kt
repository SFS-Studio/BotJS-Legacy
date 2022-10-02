package com.sifsstudio.botjs.env.api

import com.sifsstudio.botjs.env.ability.Ability
import com.sifsstudio.botjs.env.task.DelegateCondition
import dev.latvian.mods.rhino.util.HideFromJS
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.HashMap

class Bot @HideFromJS constructor(abilities: Set<Ability>) {

    @HideFromJS
    private val memories: MutableMap<Any, Any> = HashMap()

    @HideFromJS
    var tickable: Boolean = true

    @HideFromJS
    var tickWaiter = DelegateCondition(ReentrantLock())

    @HideFromJS
    fun releaseWait(hasNext: Boolean) {
        tickable = hasNext
        if(hasNext) {
            tickWaiter.signal()
        } else {
            tickWaiter.release()
        }
    }

    @HideFromJS
    @Suppress("unused")
    val abilities: Map<String, Ability> = Collections.unmodifiableMap(abilities.associateBy { it.id })

    @Suppress("unused")
    fun getMemory(memId: Any): Any? {
        return memories[memId]
    }

    @Suppress("unused")
    fun setMemory(memId: Any, mem: Any) {
        memories[memId] = mem
    }

    @Suppress("unused")
    fun consumeNextTick(): Boolean {
        tickWaiter.await()
        val cache = tickable
        tickable = false
        return cache
    }
}
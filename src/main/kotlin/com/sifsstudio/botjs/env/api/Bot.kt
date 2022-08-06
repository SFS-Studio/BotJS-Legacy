package com.sifsstudio.botjs.env.api

import com.sifsstudio.botjs.env.ability.Ability
import java.util.*
import kotlin.collections.HashMap

class Bot(abilities: Set<Ability>) {
    private val memories: MutableMap<Any, Any> = HashMap()
    val abilities: Map<String, Ability> = Collections.unmodifiableMap(abilities.associateBy { it.id })

    @Suppress("unused")
    fun getMemory(memId: Any): Any? {
        return memories[memId]
    }

    @Suppress("unused")
    fun setMemory(memId: Any, mem: Any) {
        memories[memId] = mem
    }
}
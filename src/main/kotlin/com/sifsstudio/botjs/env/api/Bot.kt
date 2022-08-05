package com.sifsstudio.botjs.env.api

import com.sifsstudio.botjs.env.ability.Ability

class Bot(abilities: Set<Ability>) {
    private val nameToAbility: MutableMap<String, Ability> = HashMap()
    private val memories: MutableMap<Any, Any> = HashMap()

    init {
        abilities.forEach { nameToAbility[it.id] = it }
    }

    @Suppress("unused")
    fun getAbility(id: String): Ability? {
        return nameToAbility[id]
    }

    @Suppress("unused")
    fun getMemory(memId: Any): Any? {
        return memories[memId]
    }

    @Suppress("unused")
    fun setMemory(memId: Any, mem: Any) {
        memories[memId] = mem
    }
}
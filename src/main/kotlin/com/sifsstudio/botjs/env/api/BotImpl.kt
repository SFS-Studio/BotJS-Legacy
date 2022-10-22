package com.sifsstudio.botjs.env.api

import com.sifsstudio.botjs.env.TickSynchronizer
import com.sifsstudio.botjs.env.ability.Ability
import com.sifsstudio.botjs.env.api.storage.BotStorage
import com.sifsstudio.botjs.env.api.storage.StorageManager
import dev.latvian.mods.rhino.util.HideFromJS
import java.util.*

class BotImpl @HideFromJS constructor(abilities: Set<Ability>, private val uid: UUID):
    Bot {

    @HideFromJS
    private val memories: MutableMap<Any, Any> = HashMap()

    @HideFromJS
    private val storage: BotStorage = StorageManager.load(uid)

    @HideFromJS
    var tickable: Boolean = true

    @HideFromJS
    @Suppress("unused")
    val abilities: Map<String, Ability> = Collections.unmodifiableMap(abilities.associateBy { it.id })

    @Suppress("unused")
    override fun getMemory(memId: Any): Any? {
        return memories[memId]
    }

    @Suppress("unused")
    override fun setMemory(memId: Any, mem: Any) {
        memories[memId] = mem
    }

    @Suppress("unused")
    override fun consumeNextTick(): Boolean {
        return tickable && TickSynchronizer.await()
    }
}
package com.sifsstudio.botjs.env.api

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.TaskFuture
import com.sifsstudio.botjs.env.api.ability.AbilityBase
import java.util.*

class Bot(private val environment: BotEnv, abilities: Map<String, AbilityBase>) {

    @JvmField
    val abilities: Map<String, AbilityBase>

    init {
        this.abilities = Collections.unmodifiableMap(abilities)
    }

    @Suppress("unused")
    val x
        get() = environment.entity.x

    @Suppress("unused")
    val y
        get() = environment.entity.y

    @Suppress("unused")
    val z
        get() = environment.entity.z

    @Suppress("unused")
    fun <T : Any> block(future: TaskFuture<T>): T {
        return environment.block(future)
    }
}
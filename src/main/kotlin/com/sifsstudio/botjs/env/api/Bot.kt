package com.sifsstudio.botjs.env.api

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.api.ability.AbilityBase
import com.sifsstudio.botjs.env.task.TaskFuture
import java.util.*

class Bot(private val environment: BotEnv, abilities: Map<String, AbilityBase>) {

    @Suppress("unused")
    @JvmField
    val abilities: Map<String, AbilityBase> = Collections.unmodifiableMap(abilities)

    @Suppress("unused")
    val x by environment.entity::x

    @Suppress("unused")
    val y by environment.entity::y

    @Suppress("unused")
    val z by environment.entity::z

    @Suppress("unused")
    fun <T : Any> block(future: TaskFuture<T>): T {
        return environment.block(future)
    }
}
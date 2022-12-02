package com.sifsstudio.botjs.env.api

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.api.ability.AbilityBase

class Bot(private val environment: BotEnv, @Suppress("unused") val abilities: Map<String, AbilityBase>) {
    @Suppress("unused")
    val x
        get() = environment.entity.x

    @Suppress("unused")
    val y
        get() = environment.entity.y

    @Suppress("unused")
    val z
        get() = environment.entity.z
}
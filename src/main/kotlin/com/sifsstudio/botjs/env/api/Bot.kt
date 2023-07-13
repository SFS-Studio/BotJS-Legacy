package com.sifsstudio.botjs.env.api

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.SuspensionContext
import com.sifsstudio.botjs.env.api.ability.AbilityBase
import com.sifsstudio.botjs.env.block
import com.sifsstudio.botjs.env.suspensionContext
import com.sifsstudio.botjs.env.task.TaskFuture
import java.util.*

class Bot(private val environment: BotEnv, abilities: Map<String, AbilityBase>) {

    @Suppress("unused")
    @JvmField
    val abilities: Map<String, AbilityBase> = Collections.unmodifiableMap(abilities)

    //TODO: see why delegate do not compile

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
    fun <T : Any> block(future: TaskFuture<T>): T = SuspensionContext.invokeSuspend {
        environment.block(future, suspensionContext!!)
    }
}
package com.sifsstudio.botjs.env.ability

import com.sifsstudio.botjs.env.BotEnv

abstract class AbilityBase: Ability {
    protected lateinit var env: BotEnv
    override fun bind(env: BotEnv) {
        check(!this::env.isInitialized) {"The ability $id is bound to an environment more than once!"}
        this.env = env
    }
}
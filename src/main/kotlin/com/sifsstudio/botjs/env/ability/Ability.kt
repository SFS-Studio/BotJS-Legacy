package com.sifsstudio.botjs.env.ability

import com.sifsstudio.botjs.env.BotEnv

interface Ability {
    val id: String
    fun bind(env: BotEnv)
}
package com.sifsstudio.botjs

import com.sifsstudio.botjs.entity.Entities
import net.minecraftforge.fml.common.Mod
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(BotJS.ID)
object BotJS {
    const val ID = "botjs"

    init {
        Entities.REGISTRY.register(MOD_BUS)
    }
}
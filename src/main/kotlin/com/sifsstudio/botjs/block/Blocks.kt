package com.sifsstudio.botjs.block

import com.sifsstudio.botjs.BotJS
import net.minecraft.world.level.block.Block
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import thedarkcolour.kotlinforforge.forge.registerObject

object Blocks {
    val REGISTRY: DeferredRegister<Block> = DeferredRegister.create(ForgeRegistries.BLOCKS, BotJS.ID)

    val CONTROLLER_BLOCK: Block
            by REGISTRY.registerObject("wireless_ap") { WirelessAPBlock() }
}
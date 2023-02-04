package com.sifsstudio.botjs

import com.sifsstudio.botjs.block.Blocks
import com.sifsstudio.botjs.block.entity.BlockEntities
import com.sifsstudio.botjs.client.gui.screen.inventory.BotMountScreen
import com.sifsstudio.botjs.entity.Entities
import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.inventory.MenuTypes
import com.sifsstudio.botjs.item.Items
import com.sifsstudio.botjs.network.NetworkManager
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import thedarkcolour.kotlinforforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(BotJS.ID)
object BotJS {
    const val ID = "botjs"

    val TAB = object : CreativeModeTab("botjs") {
        override fun makeIcon(): ItemStack {
            return Items.WRENCH.defaultInstance
        }
    }

    init {
        Entities.REGISTRY.register(MOD_BUS)
        Items.REGISTRY.register(MOD_BUS)
        MenuTypes.REGISTRY.register(MOD_BUS)
        Blocks.REGISTRY.register(MOD_BUS)
        BlockEntities.REGISTRY.register(MOD_BUS)
        NetworkManager.registerPackets()
        MOD_BUS.addListener(this::setupClient)
        FORGE_BUS.addListener(BotEnv.Companion::onServerSetup)
        FORGE_BUS.addListener(BotEnv.Companion::onServerStop)
    }

    private fun setupClient(event: FMLClientSetupEvent) = event.enqueueWork {
        MenuScreens.register(MenuTypes.BOT_MENU, ::BotMountScreen)
    }
}
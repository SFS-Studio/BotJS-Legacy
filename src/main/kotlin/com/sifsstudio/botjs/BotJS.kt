package com.sifsstudio.botjs

import com.sifsstudio.botjs.client.gui.screen.inventory.BotMountScreen
import com.sifsstudio.botjs.entity.Entities
import com.sifsstudio.botjs.inventory.MenuTypes
import net.minecraft.client.gui.screens.MenuScreens
import com.sifsstudio.botjs.item.Items
import com.sifsstudio.botjs.network.NetworkManager
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(BotJS.ID)
object BotJS {
    const val ID = "botjs"

    init {
        Entities.REGISTRY.register(MOD_BUS)
        Items.REGISTRY.register(MOD_BUS)
        MenuTypes.REGISTRY.register(MOD_BUS)
        NetworkManager.registerPackets()
        MOD_BUS.addListener(this::setupClient)
    }

    private fun setupClient(event: FMLClientSetupEvent) = event.enqueueWork {
        MenuScreens.register(MenuTypes.BOT_MENU, ::BotMountScreen)
    }
}
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
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraftforge.event.CreativeModeTabEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import thedarkcolour.kotlinforforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(BotJS.ID)
@Mod.EventBusSubscriber(modid = BotJS.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
object BotJS {
    const val ID = "botjs"

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

    @SubscribeEvent
    fun buildCreativeTab(event: CreativeModeTabEvent.Register) {
        event.registerCreativeModeTab(ResourceLocation(ID, "botjs")) {
            it.title(Component.translatable("item_group.$ID"))
                .icon { ItemStack(Items.WRENCH) }
                .displayItems { _, output ->
                    Items.REGISTRY.entries.forEach { item ->
                        output.accept(item.get())
                    }
                    Blocks.REGISTRY.entries.forEach { block ->
                        output.accept(block.get())
                    }
                }
        }
    }
}
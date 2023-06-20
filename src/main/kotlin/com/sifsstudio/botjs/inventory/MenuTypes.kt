package com.sifsstudio.botjs.inventory

import com.sifsstudio.botjs.BotJS
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.inventory.MenuType
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import thedarkcolour.kotlinforforge.forge.registerObject

object MenuTypes {
    val REGISTRY: DeferredRegister<MenuType<*>> = DeferredRegister.create(ForgeRegistries.MENU_TYPES, BotJS.ID)

    val BOT_MENU by REGISTRY.registerObject("bot_mount_menu") {
        MenuType(::BotMountMenu, FeatureFlags.DEFAULT_FLAGS)
    }
}
package com.sifsstudio.botjs.item

import com.sifsstudio.botjs.BotJS
import com.sifsstudio.botjs.env.ability.TimingAbility
import com.sifsstudio.botjs.env.ability.MovementAbility
import com.sifsstudio.botjs.env.ability.OutputAbility
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import thedarkcolour.kotlinforforge.forge.registerObject

object Items {
    val REGISTRY: DeferredRegister<Item> = DeferredRegister.create(ForgeRegistries.ITEMS, BotJS.ID)

    val TAB = object : CreativeModeTab("botjs") {
        override fun makeIcon(): ItemStack {
            return WRENCH.defaultInstance
        }
    }

    val TIMING_UPGRADE: Item
            by REGISTRY.registerObject("timing_upgrade") { UpgradeItem.withAbility { TimingAbility() } }

    val MOVEMENT_UPGRADE: Item
            by REGISTRY.registerObject("movement_upgrade") { UpgradeItem.withAbility { MovementAbility() }}

    val OUTPUT_UPGRADE: Item
            by REGISTRY.registerObject("output_upgrade") { UpgradeItem.withAbility { OutputAbility() }}

    val WRENCH: Item
            by REGISTRY.registerObject("wrench") {
                Item(Item.Properties().stacksTo(1).tab(TAB))
            }

    val PROGRAMMER: Item
            by REGISTRY.registerObject("programmer") {
                Item(Item.Properties().stacksTo(1).tab(TAB))
            }

    val SWITCH: Item
            by REGISTRY.registerObject("switch") {
                Item(Item.Properties().stacksTo(1).tab(TAB))
            }
}
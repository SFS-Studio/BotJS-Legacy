package com.sifsstudio.botjs.item

import com.sifsstudio.botjs.BotJS
import com.sifsstudio.botjs.env.ability.SpeakAbility
import net.minecraft.world.item.Item
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import thedarkcolour.kotlinforforge.forge.registerObject

object Items {
    val REGISTRY: DeferredRegister<Item> = DeferredRegister.create(ForgeRegistries.ITEMS, BotJS.ID)

//    val INTERRUPT_UPGRADE: Item
//            by REGISTRY.registerObject("interrupt_upgrade") { UpgradeItem.withAbility { DelayAbility() }}
//
//    val MOVEMENT_UPGRADE: Item
//            by REGISTRY.registerObject("movement_upgrade") { UpgradeItem.withAbility { MovementAbility() }}
//
    val OUTPUT_UPGRADE: Item
            by REGISTRY.registerObject("output_upgrade") { UpgradeItem.withAbility { SpeakAbility() }}

    val WRENCH: Item
            by REGISTRY.registerObject("wrench") {
                Item(Item.Properties().stacksTo(1))
            }

    val PROGRAMMER: Item
            by REGISTRY.registerObject("programmer") {
                Item(Item.Properties().stacksTo(1))
            }

    val SWITCH: Item
            by REGISTRY.registerObject("switch") {
                Item(Item.Properties().stacksTo(1))
            }
}
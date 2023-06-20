package com.sifsstudio.botjs.item

import com.sifsstudio.botjs.BotJS
import com.sifsstudio.botjs.block.Blocks
import com.sifsstudio.botjs.env.api.ability.*
import com.sifsstudio.botjs.env.intrinsic.ConnectionProperties
import com.sifsstudio.botjs.env.intrinsic.EnvCharacteristic
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import thedarkcolour.kotlinforforge.forge.registerObject

object Items {
    val REGISTRY: DeferredRegister<Item> = DeferredRegister.create(ForgeRegistries.ITEMS, BotJS.ID)

    val TIMING_UPGRADE: Item
            by REGISTRY.registerObject("timing_upgrade") {
                UpgradeItem.applies { it.install(::TimingAbility) }
            }

    val MOVEMENT_UPGRADE: Item
            by REGISTRY.registerObject("movement_upgrade") {
                UpgradeItem.applies { it.install(::MovementAbility) }
            }

    val OUTPUT_UPGRADE: Item
            by REGISTRY.registerObject("output_upgrade") {
                UpgradeItem.applies { it.install(::OutputAbility) }
            }

    val INTERACTION_UPGRADE: Item
            by REGISTRY.registerObject("interaction_upgrade") {
                UpgradeItem.applies { it.install(::InteractionAbility) }
            }

    val SENSING_UPGRADE: Item
            by REGISTRY.registerObject("sensing_upgrade") {
                UpgradeItem.applies { it.install(::SensingAbility) }
            }

    val CONNECTION_UPGRADE: Item
            by REGISTRY.registerObject("connection_upgrade") {
                UpgradeItem.applies {
                    it.install(::NetworkAbility)
                    it[EnvCharacteristic.CONNECTION] =
                        ConnectionProperties(128.0, mutableMapOf())
                }
            }

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

    val CONTROLLER_BLOCK: BlockItem
            by REGISTRY.registerObject("controller_block") {
                BlockItem(Blocks.CONTROLLER_BLOCK, Item.Properties())
            }
}
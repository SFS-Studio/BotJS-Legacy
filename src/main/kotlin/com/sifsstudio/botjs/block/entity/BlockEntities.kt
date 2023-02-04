package com.sifsstudio.botjs.block.entity

import com.sifsstudio.botjs.BotJS
import com.sifsstudio.botjs.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import thedarkcolour.kotlinforforge.forge.registerObject

object BlockEntities {
    val REGISTRY: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, BotJS.ID)

    val CONTROLLER: BlockEntityType<ControllerBlockEntity>
            by REGISTRY.registerObject("controller") {
                BlockEntityType.Builder.of(::ControllerBlockEntity, Blocks.CONTROLLER_BLOCK).build(null)
            }
}
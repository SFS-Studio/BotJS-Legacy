package com.sifsstudio.botjs.block.entity

import com.sifsstudio.botjs.BotJS
import com.sifsstudio.botjs.block.Blocks
import com.sifsstudio.botjs.util.nonnull
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import thedarkcolour.kotlinforforge.forge.registerObject

object BlockEntities {
    val REGISTRY: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BotJS.ID)

    val CONTROLLER: BlockEntityType<WAPBlockEntity>
            by REGISTRY.registerObject("controller") {
                BlockEntityType.Builder.of(::WAPBlockEntity, Blocks.CONTROLLER_BLOCK).build(nonnull())
            }
}
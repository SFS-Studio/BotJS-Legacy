package com.sifsstudio.botjs.entity

import com.sifsstudio.botjs.BotJS
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import thedarkcolour.kotlinforforge.forge.registerObject

object Entities {
    val REGISTRY: DeferredRegister<EntityType<*>> = DeferredRegister.create(ForgeRegistries.ENTITIES, BotJS.ID)

    val BOT: EntityType<BotEntity> by REGISTRY.registerObject("bot") {
        EntityType.Builder.of(::BotEntity, MobCategory.MISC).build("bot")
    }

}
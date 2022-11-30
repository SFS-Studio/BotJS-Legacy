package com.sifsstudio.botjs.entity

import com.sifsstudio.botjs.BotJS
import net.minecraft.world.entity.Mob
import net.minecraftforge.event.entity.EntityAttributeCreationEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = BotJS.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
object EntityAttributeRegistry {
    @SubscribeEvent
    fun attributeCreation(event: EntityAttributeCreationEvent) {
        event.put(Entities.BOT, Mob.createMobAttributes().build())
    }
}
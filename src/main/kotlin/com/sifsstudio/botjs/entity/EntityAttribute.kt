package com.sifsstudio.botjs.entity

import com.sifsstudio.botjs.BotJS
import net.minecraft.world.entity.LivingEntity
import net.minecraftforge.event.entity.EntityAttributeCreationEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = BotJS.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
object EntityAttribute {
    @SubscribeEvent
    fun attributeCreation(event: EntityAttributeCreationEvent) {
        event.put(Entities.BOT, LivingEntity.createLivingAttributes().build())
    }
}
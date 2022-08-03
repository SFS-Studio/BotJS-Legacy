package com.sifsstudio.botjs.client

import com.sifsstudio.botjs.BotJS
import com.sifsstudio.botjs.client.model.geom.BotJSModelLayers
import com.sifsstudio.botjs.client.renderer.entity.BotEntityRenderer
import com.sifsstudio.botjs.entity.Entities
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.builders.CubeDeformation
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = BotJS.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
object RenderRegistry {
    @SubscribeEvent
    fun registerLayerDefinitions(event: RegisterLayerDefinitions) {
        event.registerLayerDefinition(BotJSModelLayers.BOT) {
            LayerDefinition.create(
                HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f),
                64,
                64
            )
        }
    }

    @SubscribeEvent
    fun registerRenderers(event: RegisterRenderers) {
        event.registerEntityRenderer(Entities.BOT, ::BotEntityRenderer)
    }
}
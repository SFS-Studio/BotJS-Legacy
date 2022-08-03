package com.sifsstudio.botjs.client.renderer.entity

import com.sifsstudio.botjs.client.model.BotModel
import com.sifsstudio.botjs.client.model.geom.BotJSModelLayers
import com.sifsstudio.botjs.entity.BotEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
class BotEntityRenderer(pContext: EntityRendererProvider.Context): LivingEntityRenderer<BotEntity, BotModel>(pContext, BotModel(pContext.bakeLayer(BotJSModelLayers.BOT)), 0.5F) {
    override fun getTextureLocation(pEntity: BotEntity): ResourceLocation {
        return ResourceLocation("textures/entity/steve.png")
    }
}
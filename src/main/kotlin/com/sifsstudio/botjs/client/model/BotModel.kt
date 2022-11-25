package com.sifsstudio.botjs.client.model

import com.sifsstudio.botjs.entity.BotEntity
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
class BotModel(pRoot: ModelPart) : HumanoidModel<BotEntity>(pRoot)
package com.sifsstudio.botjs.client.gui.screen.inventory

import com.mojang.blaze3d.vertex.PoseStack
import com.sifsstudio.botjs.inventory.BotMenu
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.world.entity.player.Inventory
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
class BotScreen(pMenu: BotMenu, pInventory: Inventory, pTitle: Component): AbstractContainerScreen<BotMenu>(pMenu, pInventory, pTitle) {

    init {
        addRenderableWidget(Button(0, 0, 100, 20, TranslatableComponent("botjs.menu.bot.eval")) {

        })
    }

    override fun renderBg(pPoseStack: PoseStack, pPartialTick: Float, pMouseX: Int, pMouseY: Int) = Unit
    override fun render(pPoseStack: PoseStack, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick)
        renderTooltip(pPoseStack, pMouseX, pMouseY)
    }
}
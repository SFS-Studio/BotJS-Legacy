package com.sifsstudio.botjs.client.gui.screen.inventory

import com.mojang.blaze3d.vertex.PoseStack
import com.sifsstudio.botjs.inventory.BotMountMenu
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
class BotMountScreen(pMenu: BotMountMenu, pInventory: Inventory, pTitle: Component): AbstractContainerScreen<BotMountMenu>(pMenu, pInventory, pTitle) {

    override fun renderBg(pPoseStack: PoseStack, pPartialTick: Float, pMouseX: Int, pMouseY: Int) = renderBackground(pPoseStack)
    override fun render(pPoseStack: PoseStack, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick)
        renderTooltip(pPoseStack, pMouseX, pMouseY)
    }
}
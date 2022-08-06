package com.sifsstudio.botjs.client.gui.screen.inventory

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.sifsstudio.botjs.BotJS
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.resources.ResourceLocation

class ProgrammerScreen: Screen(TranslatableComponent("${BotJS.ID}.menu.programmer.title")) {

    companion object {
        const val PROGRAMMER_WIDTH: Int = 176
        const val PROGRAMMER_HEIGHT: Int = 165
        val GUI_TEXTURE_RESOURCE: ResourceLocation = ResourceLocation(BotJS.ID, "textures/gui/program_window.png")
    }

    override fun init() {
        super.init()
    }

    override fun render(pPoseStack: PoseStack, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        renderBackground(pPoseStack)
        RenderSystem.setShader(GameRenderer::getPositionTexShader)
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F)
        RenderSystem.setShaderTexture(0, GUI_TEXTURE_RESOURCE)
        val i = (width - PROGRAMMER_WIDTH) / 2
        val j = (height - PROGRAMMER_HEIGHT) / 2
        blit(pPoseStack, i, j, 0, 0, PROGRAMMER_WIDTH, PROGRAMMER_HEIGHT)
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick)
    }

}
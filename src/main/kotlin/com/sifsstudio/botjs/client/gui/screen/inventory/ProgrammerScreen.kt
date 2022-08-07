package com.sifsstudio.botjs.client.gui.screen.inventory

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.sifsstudio.botjs.BotJS
import com.sifsstudio.botjs.network.NetworkManager
import com.sifsstudio.botjs.network.ServerboundScriptChangedPacket
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FormattedCharSequence

class ProgrammerScreen(private val entityId: Int): Screen(TranslatableComponent("${BotJS.ID}.menu.programmer.title")) {

    companion object {
        private const val PROGRAMMER_WIDTH: Int = 176
        private const val PROGRAMMER_HEIGHT: Int = 165
        private val GUI_TEXTURE_RESOURCE: ResourceLocation = ResourceLocation(BotJS.ID, "textures/gui/program_window.png")
        val CURSOR_WHITE: FormattedCharSequence = FormattedCharSequence.forward("_", Style.EMPTY.withColor(ChatFormatting.WHITE))
        val CURSOR_GRAY: FormattedCharSequence = FormattedCharSequence.forward("_", Style.EMPTY.withColor(ChatFormatting.GRAY))
    }

    private lateinit var doneButton: Button
    private var isModified: Boolean = false

    override fun init() {
        val i = (width - PROGRAMMER_WIDTH) / 2
        val j = (height - PROGRAMMER_HEIGHT) / 2
        doneButton = addRenderableWidget(Button(i + 4, j + 141, 79, 20, CommonComponents.GUI_DONE) {
            NetworkManager.INSTANCE.sendToServer(ServerboundScriptChangedPacket(entityId, """
                bot.abilities['speak'].speak('Hello World!') 
            """))
            this.minecraft!!.setScreen(null)
        })
        doneButton.active = false
        addRenderableWidget(Button(i + 93, j + 141, 79, 20, CommonComponents.GUI_CANCEL) {
            this.minecraft!!.setScreen(null)
        })
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
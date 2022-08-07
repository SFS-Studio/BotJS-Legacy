package com.sifsstudio.botjs.network

import com.sifsstudio.botjs.client.gui.screen.inventory.ProgrammerScreen
import net.minecraft.client.Minecraft

object ClientPacketHandler {
    fun handleOpenProgrammerScreen(msg: ClientboundOpenProgrammerScreenPacket) {
        Minecraft.getInstance().setScreen(ProgrammerScreen(msg.entityId))
    }
}
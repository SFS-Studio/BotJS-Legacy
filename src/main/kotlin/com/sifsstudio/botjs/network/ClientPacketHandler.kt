package com.sifsstudio.botjs.network

import com.sifsstudio.botjs.client.gui.screen.inventory.ProgrammerScreen
import com.sifsstudio.botjs.entity.BotEntity
import net.minecraft.client.Minecraft
import net.minecraft.core.particles.ParticleTypes

object ClientPacketHandler {
    fun handleOpenProgrammerScreen(msg: ClientboundOpenProgrammerScreenPacket) {
        Minecraft.getInstance().setScreen(ProgrammerScreen(msg.entityId, msg.script))
    }

    fun handleBotParticle(msg: ClientboundBotParticlePacket) {
        Minecraft.getInstance().level?.getEntity(msg.entityId)?.let {
            (it as BotEntity).genIndicateParticle(when (msg.type) {
                ClientboundBotParticlePacket.CANCEL -> ParticleTypes.CRIT
                ClientboundBotParticlePacket.TIMEOUT -> ParticleTypes.SMOKE
                else -> throw IllegalStateException("undefined particle ID")
            })
        }
    }
}

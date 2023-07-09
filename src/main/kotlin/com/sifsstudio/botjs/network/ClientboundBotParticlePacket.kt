package com.sifsstudio.botjs.network

import com.sifsstudio.botjs.entity.BotEntity
import com.sifsstudio.botjs.util.writeByte
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.fml.DistExecutor
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

class ClientboundBotParticlePacket private constructor(val entityId: Int, val type: Byte) {
    companion object {
        const val CANCEL: Byte = 0
        const val TIMEOUT: Byte = 1

        fun encode(msg: ClientboundBotParticlePacket, buf: FriendlyByteBuf) = buf.apply {
            writeInt(msg.entityId)
            writeByte(msg.type)
        }

        fun decode(buf: FriendlyByteBuf) = ClientboundBotParticlePacket(buf.readInt(), buf.readByte())

        fun handle(msg: ClientboundBotParticlePacket, ctx: Supplier<NetworkEvent.Context>) {
            ctx.get().apply {
                enqueueWork {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT) {
                        Runnable {
                            ClientPacketHandler.handleBotParticle(msg)
                        }
                    }
                }
                packetHandled = true
            }
        }
    }

    constructor(bot: BotEntity, type: Byte) : this(bot.id, type)
}

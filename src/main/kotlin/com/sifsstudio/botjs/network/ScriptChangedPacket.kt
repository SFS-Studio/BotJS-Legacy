package com.sifsstudio.botjs.network

import com.sifsstudio.botjs.entity.BotEntity
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.network.NetworkEvent
import java.util.UUID
import java.util.function.Supplier

data class ScriptChangedPacket(
    val entityId: UUID,
    val script: String,
) {
    companion object {
        fun encode(msg: ScriptChangedPacket, buf: FriendlyByteBuf) = buf.apply {
            writeUUID(msg.entityId)
            writeUtf(msg.script)
        }

        fun decode(buf: FriendlyByteBuf) = ScriptChangedPacket(buf.readUUID(), buf.readUtf())

        fun handle(msg: ScriptChangedPacket, ctx: Supplier<NetworkEvent.Context>) {
            ctx.get().apply {
                enqueueWork {
                    val bot = sender!!.getLevel().getEntity(msg.entityId) as BotEntity
                    bot.environment.script = msg.script
                }
                packetHandled = true
            }
        }
    }
}

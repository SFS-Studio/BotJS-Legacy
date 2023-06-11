package com.sifsstudio.botjs.network

import com.sifsstudio.botjs.entity.BotEntity
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

data class ServerboundScriptChangedPacket(
    val entityId: Int,
    val script: String,
) {
    companion object {
        fun encode(msg: ServerboundScriptChangedPacket, buf: FriendlyByteBuf) = buf.apply {
            writeInt(msg.entityId)
            writeUtf(msg.script)
        }

        fun decode(buf: FriendlyByteBuf) = ServerboundScriptChangedPacket(buf.readInt(), buf.readUtf())

        fun handle(msg: ServerboundScriptChangedPacket, ctx: Supplier<NetworkEvent.Context>) {
            ctx.get().apply {
                enqueueWork {
                    val bot = sender!!.getLevel().getEntity(msg.entityId) as BotEntity?
                    bot?.environment?.script = msg.script
                }
                packetHandled = true
            }
        }
    }
}

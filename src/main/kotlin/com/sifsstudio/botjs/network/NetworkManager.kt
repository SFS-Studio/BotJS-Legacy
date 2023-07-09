package com.sifsstudio.botjs.network

import com.sifsstudio.botjs.BotJS
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.network.NetworkEvent
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.simple.SimpleChannel
import java.util.function.Supplier
import kotlin.reflect.KClass

object NetworkManager {
    private const val PROTOCOL_VERSION = "1"
    private var id = 0

    val INSTANCE: SimpleChannel = NetworkRegistry.newSimpleChannel(
        ResourceLocation(BotJS.ID, "main"),
        { PROTOCOL_VERSION },
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    )

    fun registerPackets() {
        registerPacket(
            ServerboundScriptChangedPacket::class,
            ServerboundScriptChangedPacket::encode,
            ServerboundScriptChangedPacket::decode,
            ServerboundScriptChangedPacket::handle
        )
        registerPacket(
            ClientboundOpenProgrammerScreenPacket::class,
            ClientboundOpenProgrammerScreenPacket::encode,
            ClientboundOpenProgrammerScreenPacket::decode,
            ClientboundOpenProgrammerScreenPacket::handle
        )
        registerPacket(
            ClientboundBotParticlePacket::class,
            ClientboundBotParticlePacket::encode,
            ClientboundBotParticlePacket::decode,
            ClientboundBotParticlePacket::handle
        )
    }

    private fun <MSG : Any> registerPacket(
        clazz: KClass<MSG>,
        encoder: (MSG, FriendlyByteBuf) -> Unit,
        decoder: (FriendlyByteBuf) -> MSG,
        handler: (MSG, Supplier<NetworkEvent.Context>) -> Unit
    ) {
        @Suppress("INACCESSIBLE_TYPE")
        INSTANCE.registerMessage(id++, clazz.java, encoder, decoder, handler)
    }

}

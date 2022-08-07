package com.sifsstudio.botjs.env.ability

import net.minecraft.Util
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.TextComponent
import org.apache.logging.log4j.LogManager

class SpeakAbility: AbilityBase() {
    override val id = "speak"

    @Suppress("unused")
    fun speak(content: String) = env.entity.server!!.playerList.broadcastMessage(TextComponent(content), ChatType.CHAT, Util.NIL_UUID)

    @Suppress("unused")
    fun log(content: String) = LOGGER.info(content)

    companion object {
        private val LOGGER = LogManager.getLogger("BOT")
    }
}
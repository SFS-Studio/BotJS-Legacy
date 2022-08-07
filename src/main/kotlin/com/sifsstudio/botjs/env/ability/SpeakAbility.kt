package com.sifsstudio.botjs.env.ability

import com.sifsstudio.botjs.env.task.TaskBase
import net.minecraft.Util
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.TextComponent
import org.apache.logging.log4j.LogManager

class SpeakAbility: AbilityBase() {
    override val id = "speak"

    @Suppress("unused")
    fun speak(content: String) = env.pending(SpeakTask(content)).join()

    @Suppress("unused")
    fun log(content: String) = LOGGER.info(content)

    companion object {
        private val LOGGER = LogManager.getLogger("BOT")

        // for thread safety
        class SpeakTask(private val content: String): TaskBase<Unit>() {
            override fun tick() {
                env.entity.server!!.playerList.broadcastMessage(TextComponent(content), ChatType.CHAT, Util.NIL_UUID)
                done(Unit)
            }

        }
    }
}
package com.sifsstudio.botjs.env.api.ability

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.task.PollResult
import com.sifsstudio.botjs.env.task.TickableTask
import com.sifsstudio.botjs.util.asStringTag
import net.minecraft.Util
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.TextComponent
import org.apache.logging.log4j.LogManager

class OutputAbility internal constructor(environment: BotEnv) : AbilityBase(environment) {

    override val id = "output"

    @Suppress("unused")
    fun speak(content: String) {
        submit(SpeakTask(content, environment))
    }

    @Suppress("unused")
    fun log(content: String) {
        LOGGER.info(content)
    }

    companion object {
        private val LOGGER = LogManager.getLogger("BOT")
    }
}

class SpeakTask internal constructor(private var content: String, private val environment: BotEnv) :
    TickableTask<Unit> {
    companion object {
        const val ID = "speak"
    }

    @Suppress("unused")
    constructor(environment: BotEnv) : this("", environment)

    override val id = ID

    override fun tick(): PollResult<Unit> {
        environment.entity.level.server!!.playerList.broadcastMessage(
            TextComponent(content),
            ChatType.CHAT,
            Util.NIL_UUID
        )
        return PollResult.done(Unit)
    }

    override fun serialize(): Tag = content.asStringTag()

    override fun deserialize(tag: Tag) {
        content = (tag as StringTag).asString
    }
}

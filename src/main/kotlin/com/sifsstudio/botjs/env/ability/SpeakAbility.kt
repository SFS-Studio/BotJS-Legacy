package com.sifsstudio.botjs.env.ability

import org.apache.logging.log4j.LogManager

class SpeakAbility: AbilityBase() {
    override val id = "speak"

    @Suppress("unused")
    fun speak(content: String) = LOGGER.info(content)

    companion object {
        private val LOGGER = LogManager.getLogger("BOT")
    }
}
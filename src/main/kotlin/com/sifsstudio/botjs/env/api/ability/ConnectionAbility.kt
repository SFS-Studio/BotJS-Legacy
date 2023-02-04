package com.sifsstudio.botjs.env.api.ability

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.intrinsic.EnvCharacteristic
import com.sifsstudio.botjs.env.intrinsic.conn.MessageManager
import com.sifsstudio.botjs.env.intrinsic.conn.ReachabilityTest
import com.sifsstudio.botjs.env.intrinsic.conn.RemoteLocator

@Suppress("unused")
class ConnectionAbility internal constructor(environment: BotEnv) : AbilityBase(environment) {

    override val id = "connection"

    private val property = environment[EnvCharacteristic.CONNECTION]!!

    fun scan(): Set<String> {
        return RemoteLocator.findNearby(
            environment,
            environment[EnvCharacteristic.CONNECTION]!!.range,
            ReachabilityTest(property.remote)
        ).mapTo(HashSet()) { it.uid }
    }

    fun send(remote: String, message: String): Boolean {
        val another = RemoteLocator.searchRemote(property.remote, remote) ?: return false
        MessageManager.send(property.remote, another, message)
        return true
    }

    fun recv() = MessageManager.poll(property.remote)

    fun setDescription(key: String, value: String) {
        property.descriptor[key] = value
    }

    fun getDescription(key: String) = property.descriptor[key]
}
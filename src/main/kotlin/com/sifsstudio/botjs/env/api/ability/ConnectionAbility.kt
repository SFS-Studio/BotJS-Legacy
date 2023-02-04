package com.sifsstudio.botjs.env.api.ability

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.intrinsic.EnvCharacteristic
import com.sifsstudio.botjs.env.intrinsic.conn.MessageManager
import com.sifsstudio.botjs.env.intrinsic.conn.ReachabilityTest
import com.sifsstudio.botjs.env.intrinsic.conn.RemoteEnv
import com.sifsstudio.botjs.env.intrinsic.conn.RemoteLocator

@Suppress("unused")
class ConnectionAbility internal constructor(environment: BotEnv) : AbilityBase(environment) {

    override val id = "connection"

    private var descriptor = mutableMapOf<String, String>()
    private val infoOfThis = RemoteEnv(environment.entity.stringUUID, descriptor, environment)

    fun scan(): Set<String> {
        return RemoteLocator.findNearby(
            environment,
            environment[EnvCharacteristic.CONNECTION]!!.range,
            ReachabilityTest(infoOfThis)
        ).mapTo(HashSet()) { it.uid }
    }

    fun send(remote: String, message: String): Boolean {
        val another = RemoteLocator.searchRemote(infoOfThis, remote) ?: return false
        MessageManager.send(infoOfThis, another, message)
        return true
    }

    fun recv() = MessageManager.poll(infoOfThis)
}
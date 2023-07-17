package com.sifsstudio.botjs.env.api.ability

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.api.conn.Remote
import com.sifsstudio.botjs.env.intrinsic.EnvCharacteristic
import com.sifsstudio.botjs.env.intrinsic.conn.MessageManager
import com.sifsstudio.botjs.env.intrinsic.conn.ReachabilityTest
import com.sifsstudio.botjs.env.intrinsic.conn.RemoteLocator

@Suppress("unused")
class NetworkAbility internal constructor(environment: BotEnv) : AbilityBase(environment) {

    override val id = "network"

    private val property = environment.controller[EnvCharacteristic.CONNECTION]!!

    fun scan() =
        mutableSetOf<Remote>().apply {
            RemoteLocator.findNearby(
                property.remote,
                property.range,
                this,
                ReachabilityTest(property.remote)
            )
        }.mapTo(mutableSetOf()) { it.uid.toString() }

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
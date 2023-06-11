package com.sifsstudio.botjs.env.intrinsic

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.api.conn.Remote
import com.sifsstudio.botjs.env.intrinsic.conn.RemoteEnv
import com.sifsstudio.botjs.env.intrinsic.conn.RemoteLocator

abstract class EnvCharacteristic {
    class Key<T : EnvCharacteristic>
    companion object {
        val CONNECTION = Key<ConnectionProperties>()
    }

    open fun onAddedToEnv(env: BotEnv) {}

    open fun onRemovedFromEnv(env: BotEnv) {}

    open fun onActive(env: BotEnv) {}

    open fun onDeactive(env: BotEnv) {}
}

class ConnectionProperties(val range: Double, val descriptor: MutableMap<String, String>) :
    EnvCharacteristic() {

    lateinit var remote: Remote

    override fun onAddedToEnv(env: BotEnv) {
        remote = RemoteEnv(UidRegistry.generate(), descriptor, env)
    }

    override fun onRemovedFromEnv(env: BotEnv) {
        UidRegistry.dispose(remote.uid)
        remote = Remote.UNINITIALIZED_VALUE
    }

    override fun onActive(env: BotEnv) {
        RemoteLocator.add(remote)
    }

    override fun onDeactive(env: BotEnv) {
        RemoteLocator.remove(remote)
    }
}
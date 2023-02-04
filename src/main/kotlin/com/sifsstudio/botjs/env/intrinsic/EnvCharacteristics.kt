package com.sifsstudio.botjs.env.intrinsic

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.intrinsic.conn.RemoteEnv
import com.sifsstudio.botjs.env.intrinsic.conn.RemoteLocator

open class EnvCharacteristic(protected val env: BotEnv) {
    class Key<T : EnvCharacteristic>
    companion object {
        val CONNECTION = Key<ConnectionProperties>()
    }

    open fun onEnvAdded() {}
    open fun onEnvRemoved() {}
}

class ConnectionProperties(env: BotEnv, val range: Double, uid: String, val descriptor: MutableMap<String, String>) :
    EnvCharacteristic(env) {
    val remote = RemoteEnv(uid, descriptor, env)
    override fun onEnvAdded() {
        RemoteLocator.add(remote)
    }

    override fun onEnvRemoved() {
        RemoteLocator.dispose(remote)
    }
}
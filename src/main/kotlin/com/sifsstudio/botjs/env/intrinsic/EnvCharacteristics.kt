package com.sifsstudio.botjs.env.intrinsic

sealed interface EnvCharacteristic {
    class Key<T : EnvCharacteristic>
    companion object {
        val CONNECTION = Key<ConnectionProperties>()
    }
}

data class ConnectionProperties(val range: Double) : EnvCharacteristic
package com.sifsstudio.botjs.env.api.conn

import net.minecraft.core.Position

abstract class Remote(@JvmField val uid: String, private val descriptors: Map<String, String>) {
    fun getDescriptors() = descriptors.keys

    fun getDescriptor(key: String) = descriptors[key]

    abstract val position: Position

    abstract val radius: Double
}
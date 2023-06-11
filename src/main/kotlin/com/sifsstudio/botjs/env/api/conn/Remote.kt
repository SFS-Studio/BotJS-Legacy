package com.sifsstudio.botjs.env.api.conn

import com.sifsstudio.botjs.util.position
import net.minecraft.Util
import net.minecraft.core.BlockPos
import net.minecraft.core.Position
import java.util.*

abstract class Remote(@JvmField val uid: UUID, private val descriptors: Map<String, String>) {
    fun getDescriptors() = descriptors.keys

    fun getDescriptor(key: String) = descriptors[key]

    abstract val position: Position

    abstract val radius: Double

    companion object {
        val UNINITIALIZED_VALUE = object : Remote(Util.NIL_UUID, emptyMap()) {
            override val position = BlockPos.ZERO.position
            override val radius = Double.NEGATIVE_INFINITY
        }
    }
}
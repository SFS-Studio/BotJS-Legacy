package com.sifsstudio.botjs.env.api.wrapper

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import java.io.Serializable

@Suppress("unused")
class BlockSnapshot(pos: BlockPos, state: BlockState) : Serializable {

    @JvmField
    val x = pos.x

    @JvmField
    val y = pos.y

    @JvmField
    val z = pos.z

    @JvmField
    val id: String = state.block.registryName!!.toString()

    val properties by lazy(LazyThreadSafetyMode.NONE) { state.properties.associate { it.name to state.getValue(it) } }
}
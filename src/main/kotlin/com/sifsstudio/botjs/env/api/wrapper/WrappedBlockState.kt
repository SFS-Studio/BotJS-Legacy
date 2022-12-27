package com.sifsstudio.botjs.env.api.wrapper

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState

@Suppress("unused")
class WrappedBlockState(pos: BlockPos, state: BlockState) {

    @JvmField
    val x = pos.x

    @JvmField
    val y = pos.y

    @JvmField
    val z = pos.z

    @JvmField
    val id: String = state.block.registryName!!.toString()

    @JvmField
    val properties = state.properties.associate { it.name to state.getValue(it) }

}
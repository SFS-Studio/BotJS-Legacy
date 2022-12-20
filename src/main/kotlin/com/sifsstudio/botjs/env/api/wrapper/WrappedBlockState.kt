package com.sifsstudio.botjs.env.api.wrapper

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState

@Suppress("unused")
class WrappedBlockState(internal val pos: BlockPos, internal val state: BlockState) {

    val x: Int by pos::x

    val y: Int by pos::y

    val z: Int by pos::z

    val id: String = state.block.registryName!!.toString()

    val properties
        get() = state.properties.associate { it.name to state.getValue(it) }
}
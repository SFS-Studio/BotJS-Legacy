package com.sifsstudio.botjs.env.api.wrapper

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.registries.ForgeRegistries
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
    val id: String = ForgeRegistries.BLOCKS.getKey(state.block).toString()

    val properties by lazy(LazyThreadSafetyMode.NONE) { state.properties.associate { it.name to state.getValue(it) } }
}
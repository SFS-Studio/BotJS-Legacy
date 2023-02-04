package com.sifsstudio.botjs.block.entity

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.state.BlockState

class ControllerBlockEntity(pPos: BlockPos, pBlockState: BlockState) :
    BlockEntity(BlockEntities.CONTROLLER, pPos, pBlockState) {
    object Ticker : BlockEntityTicker<ControllerBlockEntity> {
        override fun tick(pLevel: Level, pPos: BlockPos, pState: BlockState, pBlockEntity: ControllerBlockEntity) {

        }
    }
}